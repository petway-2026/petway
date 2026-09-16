# 프론트엔드 정적 파일을 빌드한 뒤 웹 서버 이미지에 복사해 서비스한다.
FROM node:24-alpine AS build

WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .

ARG VITE_API_URL
ARG VITE_KAKAO_MAP_KEY
ENV VITE_API_URL=${VITE_API_URL}
ENV VITE_KAKAO_MAP_KEY=${VITE_KAKAO_MAP_KEY}

RUN test -n "$VITE_API_URL" \
    && test -n "$VITE_KAKAO_MAP_KEY" \
    && npm run build

FROM nginx:1.27-alpine

COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY nginx/proxy_params /etc/nginx/proxy_params
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 80 443
