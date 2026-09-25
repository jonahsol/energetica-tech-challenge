# Enron email search

Search the Enron mailbox with PostgreSQL full-text search. A run-once ingest job streams `emails.csv` into Postgres. The API resolves misspelled keywords against a term vocabulary, then looks up documents with a `tsvector` index.

## Architecture

```text
emails.csv
    ↓
streaming CSV parser
    ↓
MIME parser
    ↓
PostgreSQL
    ├── emails + tsvector
    └── search_terms + pg_trgm
              ↑
              │ fuzzy term resolution
              │
HTTP API ─────┘
```

```text
                    ┌──────────────┐
                    │  PostgreSQL  │
                    │    + FTS     │
                    │    + pg_trgm │
                    └──────▲───────┘
                           │
              ┌────────────┴────────────┐
              │                         │
       ┌──────┴──────┐           ┌──────┴──────┐
       │     API     │           │    ingest   │
       │   service   │           │   run once  │
       └─────────────┘           └─────────────┘
```

Ingest reads each CSV row, parses the raw MIME message, and batch-inserts canonical fields plus the original message. PostgreSQL maintains `search_vector` as a stored generated column: subject at weight A, body at weight B. The indexed text is capped below PostgreSQL's 1 MB `to_tsvector` limit. `body` and `raw_message` keep the full original text. After each batch, new lexemes are inserted into `search_terms` with `ON CONFLICT DO NOTHING`.

Search stays split across HTTP, application, and infrastructure code:

```text
HTTP request
    ↓
SearchService
    ↓
PostgresSearchRepository
    ↓
PostgreSQL tsvector/tsquery
```

`POST /enron-data/search` accepts `{"searchTerm":"gas contract"}` and returns `id`, `sender`, `date`, `subject`, `body`, and `score`. The `id` is the dataset file path.

## Why PostgreSQL?

PostgreSQL covers this challenge in one database:

- inverted full-text indexing through `tsvector` and a GIN index
- `tsquery` for keyword and boolean lookup
- `pg_trgm` for similarity matching
- an indexed vocabulary for fuzzy term lookup
- one operational database for ingest and search

The text search configuration is `simple`. It lowercases and splits tokens, and it does not stem or drop stop words, so the vocabulary lexemes stay aligned with the search vector. Subject matches are weighted above body matches.

A dedicated search engine such as Lucene or OpenSearch is a better evolution once the corpus reaches very large or petabyte scale. Those systems split indexing, sharding, and query serving from the system of record. For this mailbox, PostgreSQL keeps indexing and retrieval in one place without a second cluster.

## Why a vocabulary?

`pg_trgm` answers “which term did the user probably mean?”. It runs against `search_terms`, not against every email body. Exact terms win. A term of at least four characters can fall back to the closest trigram match above the similarity threshold.

PostgreSQL full-text search then answers “which documents contain that term?”. The GIN index on `search_vector` does the document retrieval. Fuzzy matching never scans the mailbox.

`gas contract` is an AND of two keywords, so the words may appear in any order and do not need to be a phrase. `gas AND contract` and `gas OR contract` use the same query tree. `AND` binds more tightly than `OR`.

## Why streaming ingestion?

The search process has to stay viable with a small JVM heap, about 256 MB. The CSV is hundreds of megabytes and the `message` field is quoted and multiline. Ingest uses Apache Commons CSV to stream records and Apache Mime4j to parse one message at a time. Rows are inserted in bounded batches (`BATCH_SIZE`, default 50) and then discarded. Vocabulary terms are written from the rows just inserted, not accumulated in a set. The `run` task sets `-Xmx256m`.

## Local workflow

Start PostgreSQL:

```bash
docker compose up -d postgres
```

The database is `enron`, with user and password `enron`, published on port 5432. Data is stored in the `postgres-data` volume, so it survives container restarts. The first startup applies `services/db/schema.sql`, which creates `pg_trgm`, the tables, the generated `search_vector`, and both indexes. Ingest and the API also apply that script on startup.

Connection settings are environment variables for both processes:

```text
DB_HOST      default localhost
DB_PORT      default 5432
DB_NAME      default enron
DB_USER      default enron
DB_PASSWORD  default enron
```

Ingest also accepts `INGEST_CSV` and `BATCH_SIZE`.

Load the mailbox. This process exits when the CSV has been written:

```bash
./gradlew :services:ingest:run
```

Start the API:

```bash
./gradlew :services:api:bootRun
```

The web app reads `ENRON_API_URL` (default `http://localhost:8080`).

## Tests

```bash
./gradlew test
```

Ingest tests cover multiline CSV fields, commas inside messages, MIME extraction, bounded batches, and duplicate vocabulary rows. API tests cover keyword order, ranking, misspelling resolution, full-text lookup, rejected blank queries, and parameterized SQL.
