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

# 2. 자유 게시판 기능
요리 커뮤니티 느낌으로 구상해 본 자유 게시판입니다.
로그인 된 사용자만 글의 작성 / 수정 / 삭제나 글에 추천이 가능합니다.
![neddlo](https://github.com/user-attachments/assets/514e761e-aa86-4e18-b5b3-15a04cd618a3)

![113](https://github.com/user-attachments/assets/208ca850-157c-4b84-9d0a-03ce15580fc9)

![234234](https://github.com/user-attachments/assets/bcf8ef94-2fe2-4f35-bb49-15076678bcbc)

Summernote 에디터를 사용하여 HTML 형식으로 글을 저장하며, 이미지를 업로드 할 수 있습니다.
사용자는 댓글에 답글을 달 수 있습니다.
한 페이지에 15개씩 게시글을 열람할 수 있습니다.


# 3. 스프링 시큐리티와 OAuth 2.0을 이용한 로그인 및 회원가입
사용자는 자신의 구글 이메일을 이용하여 소셜 로그인이 가능합니다.
소셜 로그인을 사용하지 않는다면 회원가입을 통한 로그인이 가능합니다.

