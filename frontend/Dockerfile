# ── Stage 1: Build ─────────────────────────────────────────────────────────────
FROM node:22-alpine AS build
WORKDIR /app

ARG NPM_REGISTRY=https://registry.npmjs.org/
ARG NPM_STRICT_SSL=true
RUN npm config set registry "$NPM_REGISTRY" \
    && npm config set strict-ssl "$NPM_STRICT_SSL"

# Install dependencies (layer cache)
COPY package.json package-lock.json ./
RUN npm ci

# Copy source and build static assets
COPY . .
RUN npx ng build --configuration docker

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend-tcc/browser/ /usr/share/nginx/html
EXPOSE 80
