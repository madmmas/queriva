# Queriva UI — production image (SPEC §12, issue #27)
# Build context: repository root (needed for npm workspaces)

FROM node:20.20.2-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
COPY packages/api/package.json ./packages/api/
COPY packages/embed-sidecar/package.json ./packages/embed-sidecar/
COPY packages/ingest-cli/package.json ./packages/ingest-cli/
COPY packages/ui/package.json ./packages/ui/

RUN npm ci

COPY packages/ui ./packages/ui

# Browser calls host-mapped API port (SPEC §13 VITE_API_URL)
ARG VITE_API_URL=http://localhost:8080
ARG VITE_DEFAULT_COLLECTION=news_radar
ARG VITE_SEARCH_MIN_SCORE=0.40

ENV VITE_API_URL=${VITE_API_URL}
ENV VITE_DEFAULT_COLLECTION=${VITE_DEFAULT_COLLECTION}
ENV VITE_SEARCH_MIN_SCORE=${VITE_SEARCH_MIN_SCORE}

WORKDIR /app/packages/ui
RUN npm run build

FROM nginx:1.27.4-alpine

RUN apk add --no-cache curl

COPY infra/docker/ui-nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/packages/ui/dist /usr/share/nginx/html

EXPOSE 3000

HEALTHCHECK --interval=10s --timeout=5s --retries=5 --start-period=10s \
    CMD curl -f http://localhost:3000/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
