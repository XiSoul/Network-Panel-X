FROM node:22-alpine

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --omit=dev

COPY src ./src
COPY sql ./sql

EXPOSE 8787

CMD ["sh", "-c", "node src/migrate.mjs && exec node src/index.mjs"]
