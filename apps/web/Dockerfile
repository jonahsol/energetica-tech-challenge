FROM oven/bun:1 AS build
WORKDIR /app
COPY package.json bun.lock .npmrc ./
RUN bun install --frozen-lockfile
COPY . .
RUN bun run build

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
ENV HOST=0.0.0.0
COPY --from=build --chown=node:node /app/.output ./.output
EXPOSE 3000
USER node
CMD ["node", ".output/server/index.mjs"]
