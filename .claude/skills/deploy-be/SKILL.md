---
name: deploy-be
description: colima 메모리 부스트 → buildx amd64 빌드 → ghcr.io 푸시 → colima 원복 (멀티태스크 대응)
user_invocable: true
---

# Backend Docker Build & Push

colima에 메모리를 몰아서 buildx로 amd64 이미지를 빌드하고 ghcr.io에 푸시한다.
멀티태스크 환경을 고려하여, colima가 이미 부스트 상태면 재시작하지 않고 그대로 사용한다.

## Steps

### 1. GitHub 사용자 확인 & ghcr.io 로그인
- `gh auth status`에서 **Active account**의 username을 가져온다. 하드코딩 금지.
```bash
GH_USER=$(gh api user --jq '.login')
echo $(gh auth token) | docker login ghcr.io -u "$GH_USER" --password-stdin
```

### 2. colima 메모리 부스트 (조건부)
- `colima list`로 현재 메모리 확인
- **이미 16GB 이상이면 재시작 스킵** (다른 빌드 태스크가 사용 중일 수 있음)
- 2GB(기본)이면 stop → `colima start --cpu 4 --memory 16`

### 3. 이미지 태그 결정
- `git remote get-url origin`에서 `owner/repo` 추출 → `ghcr.io/<owner>/<repo>:latest`
```bash
REPO_SLUG=$(git remote get-url origin | sed 's/.*github\.com[:/]\(.*\)\.git/\1/' | tr '[:upper:]' '[:lower:]')
IMAGE="ghcr.io/${REPO_SLUG}:latest"
```

### 4. buildx 빌드 & 푸시 (Spring Boot)
- Dockerfile은 멀티스테이지 빌드(temurin JDK → bootJar → temurin JRE)로 구성되어 있다.
- build-arg 없이 Dockerfile 그대로 빌드한다.
```bash
docker buildx create --name amd64builder --use 2>/dev/null || docker buildx use amd64builder
docker buildx build --platform linux/amd64 \
  -t "$IMAGE" \
  --push .
```

## Notes
- Spring Boot 멀티스테이지 Dockerfile을 그대로 사용하며, build-arg는 불필요.
- colima 부스트 메모리는 16GB 고정.
- 빌드 후 colima 원복이나 builder 정리는 사용자가 직접 수행한다.
