# ---------- Build stage ----------
FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./

RUN npm install --legacy-peer-deps

COPY . .

RUN npm run build


# ---------- Production stage ----------
FROM nginx:alpine

# Copy compiled React app
COPY --from=build /app/build /usr/share/nginx/html

# Copy custom nginx config with /api proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]