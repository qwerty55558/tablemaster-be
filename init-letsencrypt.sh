#!/bin/bash

# ============================================
# Let's Encrypt 초기 인증서 발급 스크립트
# ============================================
# 사용법: ./init-letsencrypt.sh
# ============================================

set -e

# 도메인 설정
domains=(clauminirockpt.tech api.clauminirockpt.tech app.clauminirockpt.tech)
email="amajang2012@gmail.com"
staging=0  # 테스트 시 1로 설정 (rate limit 방지)

# 경로 설정
data_path="./data/certbot"
compose_file="docker-compose.ghcr.yml"

# Docker Compose 명령어 감지 (V2 vs V1)
if docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
else
    DOCKER_COMPOSE="docker-compose"
fi

# 색상
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN} Let's Encrypt 인증서 발급 스크립트${NC}"
echo -e "${GREEN}============================================${NC}"

# 기존 인증서 확인
if [ -d "$data_path/conf/live/${domains[0]}" ]; then
    echo -e "${YELLOW}기존 인증서가 존재합니다.${NC}"
    read -p "기존 인증서를 삭제하고 새로 발급하시겠습니까? (y/N) " decision
    if [ "$decision" != "Y" ] && [ "$decision" != "y" ]; then
        echo "취소되었습니다."
        exit 0
    fi
fi

# 디렉토리 생성
echo -e "${GREEN}[1/6] 디렉토리 생성...${NC}"
mkdir -p "$data_path/conf"
mkdir -p "$data_path/www"

# TLS 파라미터 다운로드
echo -e "${GREEN}[2/6] TLS 파라미터 다운로드...${NC}"
if [ ! -e "$data_path/conf/options-ssl-nginx.conf" ] || [ ! -e "$data_path/conf/ssl-dhparams.pem" ]; then
    curl -s https://raw.githubusercontent.com/certbot/certbot/master/certbot-nginx/certbot_nginx/_internal/tls_configs/options-ssl-nginx.conf > "$data_path/conf/options-ssl-nginx.conf"
    curl -s https://raw.githubusercontent.com/certbot/certbot/master/certbot/certbot/ssl-dhparams.pem > "$data_path/conf/ssl-dhparams.pem"
fi

# 더미 인증서 생성
echo -e "${GREEN}[3/6] 더미 인증서 생성...${NC}"
path="/etc/letsencrypt/live/${domains[0]}"
mkdir -p "$data_path/conf/live/${domains[0]}"

$DOCKER_COMPOSE -f "$compose_file" run --rm --entrypoint "\
    openssl req -x509 -nodes -newkey rsa:4096 -days 1 \
    -keyout '$path/privkey.pem' \
    -out '$path/fullchain.pem' \
    -subj '/CN=localhost'" certbot

echo -e "${GREEN}[4/6] Nginx 시작...${NC}"
$DOCKER_COMPOSE -f "$compose_file" up -d nginx
sleep 5

# 더미 인증서 삭제
echo -e "${GREEN}[5/6] 더미 인증서 삭제...${NC}"
$DOCKER_COMPOSE -f "$compose_file" run --rm --entrypoint "\
    rm -rf /etc/letsencrypt/live/${domains[0]} && \
    rm -rf /etc/letsencrypt/archive/${domains[0]} && \
    rm -rf /etc/letsencrypt/renewal/${domains[0]}.conf" certbot

# 실제 인증서 발급
echo -e "${GREEN}[6/6] 실제 인증서 발급...${NC}"

# 도메인 파라미터 생성
domain_args=""
for domain in "${domains[@]}"; do
    domain_args="$domain_args -d $domain"
done

# staging 옵션
staging_arg=""
if [ $staging != "0" ]; then
    staging_arg="--staging"
fi

$DOCKER_COMPOSE -f "$compose_file" run --rm --entrypoint "\
    certbot certonly --webroot -w /var/www/certbot \
    $staging_arg \
    $domain_args \
    --email $email \
    --rsa-key-size 4096 \
    --agree-tos \
    --non-interactive \
    --force-renewal" certbot

# Nginx 재시작
echo -e "${GREEN}Nginx 재시작...${NC}"
$DOCKER_COMPOSE -f "$compose_file" exec nginx nginx -s reload

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN} 인증서 발급 완료!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "다음 명령어로 전체 서비스를 시작하세요:"
echo -e "${YELLOW}  $DOCKER_COMPOSE -f $compose_file up -d${NC}"
echo ""
echo -e "인증서 갱신 (90일마다 실행):"
echo -e "${YELLOW}  $DOCKER_COMPOSE -f $compose_file run --rm certbot renew${NC}"
echo -e "${YELLOW}  $DOCKER_COMPOSE -f $compose_file exec nginx nginx -s reload${NC}"
