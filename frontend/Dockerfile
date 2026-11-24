# Étape 1 : build du frontend avec Node.js
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_DATA_MODE=api
ARG VITE_API_BASE_URL=
ENV VITE_DATA_MODE=$VITE_DATA_MODE
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# Étape 2 : Nginx sert le frontend et relaie /api vers le backend
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80 443
CMD ["nginx", "-g", "daemon off;"]
