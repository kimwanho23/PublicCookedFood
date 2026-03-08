# 공공데이터포털 데이터베이스 기반 레시피 및 자유 게시판 프로젝트
카테고리와 검색 기능을 통해 원하는 레시피를 검색할 수 있습니다.

# 사용 기술
<img src="https://img.shields.io/badge/HTML-E34F26?style=for-the-badge&logo=HTML5&logoColor=white"> <img src="https://img.shields.io/badge/CSS-1572B6?style=for-the-badge&logo=CSS3&logoColor=white"> <img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=JavaScript&logoColor=white"> <img src="https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=Thymeleaf&logoColor=white"> <img src="https://img.shields.io/badge/spring boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"> <img src="https://img.shields.io/badge/Spring Security-6DB33F?style=for-the-badge&logo=Spring Security&logoColor=white"> <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=MySQL&logoColor=white">
## 프론트엔드
HTML / CSS / Javascript / Thymeleaf / BootStrap 5
## 백엔드
Spring Boot 3 / Spring Security / Spring Data JPA / OAuth2.0 / QueryDSL
## DB
MySQL

## 환경 변수
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `MAIL_USERNAME`, `MAIL_PASSWORD`
- `SPRING_PROFILES_ACTIVE`
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`
- `GOOGLE_REDIRECT_URI`, `NAVER_REDIRECT_URI`
- `FILE_DIR`

## 로컬 실행 (.env)
1. 루트에 `.env` 파일을 직접 생성합니다.
2. 위 환경 변수 목록 중 필요한 값을 `.env`에 채우고, 로컬 실행은 `SPRING_PROFILES_ACTIVE=dev`로 맞춥니다.
3. 앱 실행: `./gradlew bootRun`

## WSL 실행 원칙
- 프로젝트는 가능하면 `/mnt/c/...`가 아니라 WSL Linux 파일시스템 경로에서 실행하는 것이 맞습니다.
- `/mnt/c/...` 경로에서는 Gradle 캐시/락 파일 때문에 `Input/output error`나 파일 잠금 문제가 날 수 있습니다.
- 권장 경로 예시:
  - `~/workspace/PublicCookedFood`
- 현재 경로를 바로 옮기기 어렵다면 `./gradlew-local`은 임시 우회용으로만 사용합니다.

## DB/Migration 원칙
- 애플리케이션은 `.env`의 `DB_URL`이 가리키는 단일 DB를 기준으로 실행합니다.
- 이 저장소에는 추가 검증용 DB를 생성하는 보조 스크립트를 유지하지 않습니다.
- 스키마 변경은 `src/main/resources/db/migration` 아래 migration으로만 관리합니다.
# ERD DIAGRAM
![food data](https://github.com/user-attachments/assets/f49d8d6e-270d-4446-80ad-7641c599b0a6)

# 개발 주요 사항
- 공공데이터포털 레시피 데이터베이스 API 파싱
- 사용자는 레시피를 북마크하여 원할 때 찾아볼 수 있음
- 이미지로 레시피를 올릴 수 있는 커뮤니티 기능
- 세션을 통한 로그인 / OAuth2.0 구글 소셜 로그인
- 상단 고정형 navBar로 주요 정보 표시
- Summernote 에디터로 게시글 작성 / 수정


# 1. 농림축산식품 오픈 API 레시피 데이터베이스
공공데이터포털 오픈 API에서 제공하는 농림축산식품 레시피 데이터베이스를 보여줍니다.
RestClient를 이용해서 API를 호출하여 데이터베이스에 파싱하였습니다.

![image (4)](https://github.com/user-attachments/assets/6a90b9d7-e5b1-4fb6-b6d2-93258eb059f1)


사용자는 레시피를 카테고리별로 열람하거나 레시피명을 통해서 검색할 수 있으며, 로그인 된 사용자는 레시피 데이터를 북마크할 수 있습니다.
우측의 사용자 닉네임을 클릭하면 드롭다운이 열리며 북마크 한 레시피를 찾아볼 수 있습니다.
한 페이지에 15개씩 레시피를 열람할 수 있습니다.

![image (1)](https://github.com/user-attachments/assets/dca17d9d-6b66-4d96-b488-78de2e9e73b5)

![image (2)](https://github.com/user-attachments/assets/279a125a-1306-4086-8a0e-2eec4343a1a8)

## Docker

로컬 빌드 기반 스택은 아래처럼 실행합니다.

```bash
docker compose up -d --build
```

Default public entry point is `http://localhost:8080` through NGINX. `app1` and `app2` stay on the internal Docker network.
Session sharing works through Redis, but SSE notifications are still node-local until they are moved to a shared pub/sub path.

Docker Hub에 푸시된 이미지를 기준으로 실행하려면 `APP_IMAGE`를 지정하고 전용 compose 파일을 사용합니다.

```bash
APP_IMAGE=32up/public-cooked-food:latest docker compose -f compose.dockerhub.yml up -d
```

If you want a different public port, set `APP_HOST_PORT` before starting the stack.

GitHub Actions로 Docker Hub에 이미지를 자동 푸시하려면 아래 설정이 필요합니다.

- Repository variable `DOCKERHUB_USERNAME`
- Repository variable `DOCKERHUB_REPOSITORY`
- Repository secret `DOCKERHUB_TOKEN`

`DOCKERHUB_TOKEN`은 계정 비밀번호가 아니라 Docker Hub access token을 넣는 기준입니다.

워크플로우는 [docker-publish.yml](./.github/workflows/docker-publish.yml) 에 있습니다. `CI`가 `main`에서 성공하면 `latest`와 `sha-<commit>` 태그를 Docker Hub에 푸시합니다.

# 2. 자유 게시판 기능
요리 커뮤니티 느낌으로 구상해 본 자유 게시판입니다.
로그인 된 사용자만 글의 작성 / 수정 / 삭제나 글에 추천이 가능합니다.

![neddlo](https://github.com/user-attachments/assets/514e761e-aa86-4e18-b5b3-15a04cd618a3)

![113](https://github.com/user-attachments/assets/208ca850-157c-4b84-9d0a-03ce15580fc9)

![234234](https://github.com/user-attachments/assets/bcf8ef94-2fe2-4f35-bb49-15076678bcbc)

Summernote 에디터를 사용하여 HTML 형식으로 글을 저장하며, 이미지를 업로드 할 수 있습니다.

사용자는 댓글에 답글을 달 수 있습니다.

한 페이지에 15개씩 게시글을 열람할 수 있습니다.

## 댓글 트리 모델
- 댓글은 `parent_id`, `root_parent_id`, `depth`, `comment_path`를 함께 저장하는 하이브리드 구조를 사용합니다.
- `parent_id`는 직접 부모 댓글, `root_parent_id`는 최상위 스레드 댓글, `depth`는 중첩 깊이를 의미합니다.
- `comment_path`는 고정 길이 세그먼트를 이어 붙인 materialized path 형태이며, 댓글 트리 정렬 순서를 안정적으로 유지하기 위한 컬럼입니다.
- 게시글 상세는 상위 댓글 페이지를 먼저 고른 뒤, 선택된 `root_parent_id` 묶음만 조회합니다.
- 댓글 직링크와 알림 이동 경로도 `root_parent_id`와 `comment_path`를 기준으로 현재 댓글이 속한 스레드 페이지를 계산합니다.

# 3. 스프링 시큐리티와 OAuth 2.0을 이용한 로그인 및 회원가입
사용자는 자신의 구글 이메일을 이용하여 소셜 로그인이 가능합니다.
소셜 로그인을 사용하지 않는다면 회원가입을 통한 로그인이 가능합니다.

# @RequestParam 통합

메인 페이지에서 검색을 해야 할 때, 기존 @RequestParam은 세 가지가 존재했습니다.
그러다보니 쿼리 파라미터가 복잡해졌고, 이는 하나의 Keyword로 통합할 수 있는 여지가 되었습니다.

[http://localhost:8080/foods?nationNmList=퓨전](http://localhost:8080/foods?nationNmList=%ED%93%A8%EC%A0%84)

[http://localhost:8080/foods?irdntCodeList=밀가루](http://localhost:8080/foods?irdntCodeList=%EB%B0%80%EA%B0%80%EB%A3%A8)

[http://localhost:8080/foods?tyNmList=만두/면류](http://localhost:8080/foods?tyNmList=%EB%A7%8C%EB%91%90/%EB%A9%B4%EB%A5%98)

이 쿼리 파라미터는 하나의 카테고리로 생각하고 keyword로 통합하였습니다.
그러므로 search와 keyword 카테고리 내에서 검색을 할 때는 두 개를 파라미터를 쿼리 스트링으로 사용합니다.

ex) http://localhost:8080/foods?keyword=한식search=고기

![키워드](https://github.com/user-attachments/assets/0274195b-f750-43a6-a3e5-06c125e8782e)
