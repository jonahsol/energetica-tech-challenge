export type Email = {
  id: string
  sender: string
  xTo: string | null
  xCc: string | null
  xBcc: string | null
  date: string | null
  subject: string
  body: string
  score: number
}

export type DashboardSearch = {
  q?: string
  email?: string
}

export function validateDashboardSearch(
  search: Record<string, unknown>
): DashboardSearch {
  const q = typeof search.q === "string" ? search.q : undefined
  const email = parseEmailId(search.email)

  return {
    q: q && q.length > 0 ? q : undefined,
    email,
  }
}

export function formatEmailDate(iso: string | null): string {
  if (!iso) return ""
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso

  const day = new Intl.DateTimeFormat("en-GB", {
    day: "numeric",
    month: "short",
    year: "numeric",
    timeZone: "UTC",
  }).format(date)
  const hours = String(date.getUTCHours()).padStart(2, "0")
  const minutes = String(date.getUTCMinutes()).padStart(2, "0")
  const meridiem = date.getUTCHours() >= 12 ? "pm" : "am"

  return `${day}, ${hours}:${minutes}${meridiem}`
}

export function emailPreview(body: string): string {
  return body.replace(/\s+/g, " ").trim()
}

function parseEmailId(value: unknown): string | undefined {
  if (typeof value === "string" && value.length > 0) {
    return value
  }
  if (typeof value === "number" && Number.isFinite(value)) {
    return String(value)
  }
  return undefined
}
