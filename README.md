# 공공데이터포털 데이터베이스 기반 레시피 및 자유 게시판 프로젝트
카테고리와 검색 기능을 통해 원하는 레시피를 검색할 수 있습니다.

# 사용 기술
<img src="https://img.shields.io/badge/HTML-E34F26?style=for-the-badge&logo=HTML5&logoColor=white"> <img src="https://img.shields.io/badge/CSS-1572B6?style=for-the-badge&logo=CSS3&logoColor=white"> <img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=JavaScript&logoColor=white"> <img src="https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=Thymeleaf&logoColor=white"> <img src="https://img.shields.io/badge/spring boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"> <img src="https://img.shields.io/badge/Spring Security-6DB33F?style=for-the-badge&logo=Spring Security&logoColor=white"> <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=MySQL&logoColor=white">
## 프론트엔드
HTML / CSS / Javascript / Thymeleaf
## 백엔드
Spring Boot 3 / Spring Security
## DB
MySQL

## 환경 변수
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `MAIL_USERNAME`, `MAIL_PASSWORD`
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`
- `GOOGLE_REDIRECT_URI`, `NAVER_REDIRECT_URI`
- `RECIPE_API_KEY`
- `FILE_DIR`

## 로컬 실행 (.env)
1. 루트에서 `.env.example`을 복사해 `.env`를 만듭니다.
2. `.env`의 `DB_PASSWORD` 등 값을 실제 값으로 채웁니다.
3. 앱 실행: `./gradlew bootRun`

