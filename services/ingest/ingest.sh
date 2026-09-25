#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")/../.."
./gradlew :services:ingest:run
