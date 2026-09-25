import {
  createFileRoute,
  Link,
  useNavigate,
  useRouterState,
} from "@tanstack/react-router"
import { MailSearchIcon, SearchIcon } from "lucide-react"
import { useEffect, useState } from "react"

import { EnronMark } from "@/components/enron-mark"
import {
  Empty,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Field, FieldLabel } from "@/components/ui/field"
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
} from "@/components/ui/input-group"
import {
  Item,
  ItemContent,
  ItemDescription,
  ItemGroup,
  ItemHeader,
  ItemSeparator,
  ItemTitle,
} from "@/components/ui/item"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import {
  emailPreview,
  formatEmailDate,
  validateDashboardSearch,
  type Email,
} from "@/lib/emails"
import { searchEmails } from "@/lib/search"
import { cn } from "@/lib/utils"

export const Route = createFileRoute("/")({
  validateSearch: validateDashboardSearch,
  loaderDeps: ({ search }) => ({ q: search.q ?? "" }),
  loader: async ({ deps }) => {
    if (!deps.q.trim()) {
      return { emails: [] as Email[] }
    }
    return { emails: await searchEmails({ data: { searchTerm: deps.q } }) }
  },
  component: Dashboard,
  errorComponent: SearchError,
})

function Dashboard() {
  const { q, email: emailId } = Route.useSearch()
  const { emails } = Route.useLoaderData()
  const selected = emails.find((email) => email.id === emailId)

  return (
    <main className="flex h-svh min-h-0 bg-background">
      <SearchPanel query={q} emails={emails} selectedId={emailId} />
      <section className="flex min-w-0 flex-1 flex-col">
        {selected ? <EmailDetail email={selected} /> : <StartEmpty />}
      </section>
    </main>
  )
}

function SearchPanel({
  query,
  emails,
  selectedId,
  pending = false,
}: {
  query?: string
  emails: Email[]
  selectedId?: string
  pending?: boolean
}) {
  return (
    <aside className="flex h-full w-[22rem] max-w-[22rem] min-w-[22rem] shrink-0 flex-col overflow-x-hidden border-r border-sidebar-border bg-sidebar">
      <div className="flex items-center gap-2.5 px-4 pt-4 pb-3">
        <EnronMark className="size-8 shrink-0" />
        <h1 className="min-w-0 text-lg font-semibold tracking-tight text-brand">
          Enron Data Spelunking
        </h1>
      </div>
      <div className="px-4 pb-3">
        <SearchField query={query} />
      </div>
      <Separator />
      <ScrollArea className="min-h-0 w-full flex-1">
        <Results
          query={query}
          emails={emails}
          selectedId={selectedId}
          pending={pending}
        />
      </ScrollArea>
    </aside>
  )
}

function SearchField({ query }: { query?: string }) {
  const navigate = useNavigate({ from: "/" })
  const [value, setValue] = useState(query ?? "")

  useEffect(() => {
    setValue(query ?? "")
  }, [query])

  useEffect(() => {
    const next = value.trim()
    const current = (query ?? "").trim()
    if (next === current) return

    const timeout = window.setTimeout(() => {
      void navigate({
        to: "/",
        search: { q: next || undefined },
      })
    }, 300)

    return () => window.clearTimeout(timeout)
  }, [navigate, query, value])

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        const next = value.trim()
        void navigate({
          to: "/",
          search: { q: next || undefined },
        })
      }}
    >
      <Field>
        <FieldLabel htmlFor="email-search" className="sr-only">
          Search emails
        </FieldLabel>
        <InputGroup className="h-9 rounded-full bg-background">
          <InputGroupInput
            id="email-search"
            value={value}
            placeholder="Search emails..."
            onChange={(event) => setValue(event.target.value)}
          />
          <InputGroupAddon>
            <SearchIcon />
          </InputGroupAddon>
        </InputGroup>
      </Field>
    </form>
  )
}

function Results({
  query,
  emails,
  selectedId,
  pending,
}: {
  query?: string
  emails: Email[]
  selectedId?: string
  pending: boolean
}) {
  const isSearchLoading = useRouterState({
    select: (state) => {
      if (state.status !== "pending") return false
      const nextQuery = (state.location.search as { q?: string }).q ?? ""
      const currentQuery =
        (state.resolvedLocation?.search as { q?: string } | undefined)?.q ?? ""
      return nextQuery !== currentQuery
    },
  })
  const showPending = pending || isSearchLoading

  if (showPending) {
    return (
      <div className="flex flex-col gap-3 p-4">
        {Array.from({ length: 4 }, (_, index) => (
          <Skeleton key={index} className="h-16 w-full" />
        ))}
      </div>
    )
  }

  if (!query?.trim()) return null

  if (emails.length === 0) {
    return (
      <p className="px-4 py-6 text-sm text-muted-foreground">
        No emails matched “{query}”.
      </p>
    )
  }

  return (
    <ItemGroup className="w-full min-w-0 gap-0">
      {emails.map((email, index) => (
        <ResultRow
          key={email.id}
          email={email}
          selected={email.id === selectedId}
          separator={index < emails.length - 1}
        />
      ))}
    </ItemGroup>
  )
}

function ResultRow({
  email,
  selected,
  separator,
}: {
  email: Email
  selected: boolean
  separator: boolean
}) {
  return (
    <>
      <Item
        variant="default"
        className={cn(
          "w-full max-w-full min-w-0 items-start overflow-hidden rounded-none px-4 py-5 text-left",
          selected && "bg-background [a]:hover:bg-background"
        )}
        render={
          <Link
            to="/"
            resetScroll={false}
            search={(previous) => ({
              q: previous.q,
              email: email.id,
            })}
          />
        }
      >
        <ItemContent className="w-full min-w-0 gap-1">
          <ItemHeader className="w-full min-w-0 text-xs text-muted-foreground">
            <span className="min-w-0 truncate">From: {email.sender}</span>
            <time dateTime={email.date ?? undefined} className="shrink-0">
              {formatEmailDate(email.date)}
            </time>
          </ItemHeader>
          <ItemTitle className="line-clamp-2 w-full min-w-0 text-base leading-snug font-semibold wrap-break-word whitespace-normal">
            {email.subject || "(no subject)"}
          </ItemTitle>
          <ItemDescription className="line-clamp-1 w-full min-w-0 text-sm">
            {emailPreview(email.body)}
          </ItemDescription>
        </ItemContent>
      </Item>
      {separator ? <ItemSeparator className="my-0" /> : null}
    </>
  )
}

function EmailDetail({ email }: { email: Email }) {
  return (
    <ScrollArea className="min-h-0 flex-1">
      <article className="mx-auto flex max-w-3xl flex-col gap-4 px-8 py-10">
        <h2 className="text-2xl font-semibold tracking-tight">
          {email.subject || "(no subject)"}
        </h2>
        <div className="flex flex-col gap-1 text-sm text-muted-foreground">
          <div className="flex flex-wrap items-baseline justify-between gap-2">
            <p>From: {email.sender}</p>
            <time dateTime={email.date ?? undefined}>
              {formatEmailDate(email.date)}
            </time>
          </div>
          {email.xTo ? <p>To: {email.xTo}</p> : null}
          {email.xCc ? <p>Cc: {email.xCc}</p> : null}
          {email.xBcc ? <p>Bcc: {email.xBcc}</p> : null}
        </div>
        <Separator />
        <p className="text-sm leading-relaxed whitespace-pre-wrap">
          {email.body}
        </p>
      </article>
    </ScrollArea>
  )
}

function StartEmpty() {
  return (
    <Empty className="border-0">
      <EmptyHeader>
        <EmptyMedia className="text-foreground [&_svg]:size-12">
          <MailSearchIcon />
        </EmptyMedia>
        <EmptyTitle className="max-w-40 text-base font-semibold">
          Search and select an email to get started
        </EmptyTitle>
      </EmptyHeader>
    </Empty>
  )
}

function SearchError() {
  const { q } = Route.useSearch()

  return (
    <main className="flex h-svh min-h-0 bg-background">
      <SearchPanel query={q} emails={[]} />
      <section className="flex min-w-0 flex-1 items-center justify-center p-8">
        <p className="max-w-sm text-center text-sm text-muted-foreground">
          Search is unavailable. Check that the Enron API is running, then try
          again.
        </p>
      </section>
    </main>
  )
}
