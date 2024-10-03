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

# 1. 농림축산식품 오픈 API 레시피
공공데이터포털 오픈 API에서 제공하는 농림축산식품 레시피 데이터베이스를 보여줍니다.
사용자는 레시피를 카테고리별로 열람하거나 레시피명을 통해서 검색할 수 있으며, 로그인 된 사용자는 레시피 데이터를 북마크할 수 있습니다.
우측의 사용자 닉네임을 클릭하면 드롭다운이 열리며 북마크 한 레시피를 찾아볼 수 있습니다.
한 페이지에 15개씩 레시피를 열람할 수 있습니다.

# 2. 자유 게시판 기능
요리 커뮤니티 느낌으로 구상해 본 자유 게시판입니다.
로그인 된 사용자만 글의 작성 / 수정 / 삭제나 글에 추천이 가능합니다.
Summernote 에디터를 사용하여 HTML 형식으로 글을 저장하며, 이미지를 업로드 할 수 있습니다.
사용자는 댓글에 답글을 달 수 있습니다.
한 페이지에 15개씩 게시글을 열람할 수 있습니다.

# 3. 스프링 시큐리티와 OAuth 2.0을 이용한 로그인 및 회원가입
사용자는 자신의 구글 이메일을 이용하여 소셜 로그인이 가능합니다.
소셜 로그인을 사용하지 않는다면 회원가입을 통한 로그인이 가능합니다.

