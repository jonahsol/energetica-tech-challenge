import { createServerFn } from "@tanstack/react-start"

import type { Email } from "@/lib/emails"

const apiUrl = process.env.ENRON_API_URL ?? "http://localhost:8080"

export const searchEmails = createServerFn({ method: "POST" })
  .validator((data: { searchTerm: string }) => {
    const searchTerm = data.searchTerm.trim()
    if (!searchTerm) {
      throw new Error("searchTerm must not be blank")
    }
    return { searchTerm }
  })
  .handler(async ({ data }): Promise<Email[]> => {
    const response = await fetch(`${apiUrl}/enron-data/search`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ searchTerm: data.searchTerm }),
    })

    if (!response.ok) {
      throw new Error("Search failed")
    }

    return (await response.json()) as Email[]
  })
