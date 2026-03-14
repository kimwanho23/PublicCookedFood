# PublicCookedFood

<div align="center">
  <p><strong>공공 레시피 탐색과 사용자 커뮤니티를 함께 담은 요리 플랫폼</strong></p>
  <p>레시피를 찾고, 저장하고, 직접 올리고, 게시판과 알림으로 소통할 수 있는 웹 서비스를 구현했습니다.</p>
  <p>
    <img src="https://img.shields.io/badge/Java_17-437291?style=flat-square&logo=openjdk&logoColor=white" alt="Java 17" />
    <img src="https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot" />
    <img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white" alt="Spring Data JPA" />
    <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white" alt="Spring Security" />
    <img src="https://img.shields.io/badge/Thymeleaf-005F0F?style=flat-square&logo=thymeleaf&logoColor=white" alt="Thymeleaf" />
    <img src="https://img.shields.io/badge/QueryDSL-0F4C81?style=flat-square" alt="QueryDSL" />
    <img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL" />
    <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white" alt="Redis" />
    <img src="https://img.shields.io/badge/Flyway-CC0200?style=flat-square" alt="Flyway" />
    <img src="https://img.shields.io/badge/SpotBugs-F29D38?style=flat-square" alt="SpotBugs" />
  </p>
</div>

## 프로젝트 소개

PublicCookedFood는 공공데이터포털 레시피를 바탕으로 만든 요리 서비스입니다.  
사용자는 원하는 레시피를 **검색**하고 **북마크**할 수 있고, 직접 레시피를 올리거나 자유 게시판에서 글을 작성하며 다른 사용자와 **소통**할 수 있습니다.

단순한 레시피 조회에 그치지 않고, **댓글**, **리뷰**, **실시간 알림**, **관리자 운영 기능**까지 하나의 흐름으로 이어지도록 구성했습니다.

> **핵심 목표**
>
> 공공 레시피를 찾아보는 경험에서 끝나지 않고, 사용자가 직접 레시피를 올리고 커뮤니티 안에서 계속 활동할 수 있는 서비스를 만드는 데 초점을 맞췄습니다.

## 주요 기능

| 기능 | 설명 |
| --- | --- |
| 공공 레시피 조회 | 공공데이터포털 레시피를 카테고리와 검색어로 찾아볼 수 있습니다. |
| 북마크 | 관심 있는 레시피를 저장하고 다시 확인할 수 있습니다. |
| 사용자 레시피 | 사용자가 직접 레시피를 등록하고 수정하거나 삭제할 수 있습니다. |
| 자유 게시판 | 글 작성, 이미지 업로드, 댓글과 답글, 좋아요, 스크랩, 신고 기능을 제공합니다. |
| 계정 기능 | 일반 로그인, 소셜 로그인, 프로필 수정, 차단, 이메일 찾기, 비밀번호 재설정을 지원합니다. |
| 알림 | 새 소식이 생기면 실시간 알림으로 확인할 수 있습니다. |
| 관리자 기능 | 게시판 탭 관리, 신고 처리, 게시판 정책 관리 기능을 제공합니다. |

## 화면 예시

<table>
  <tr>
    <td align="center">
      <strong>공공 레시피 목록</strong><br />
      <img src="https://github.com/user-attachments/assets/dca17d9d-6b66-4d96-b488-78de2e9e73b5" alt="공공 레시피 목록" width="420" />
    </td>
    <td align="center">
      <strong>레시피 상세</strong><br />
      <img src="https://github.com/user-attachments/assets/279a125a-1306-4086-8a0e-2eec4343a1a8" alt="레시피 상세" width="420" />
    </td>
  </tr>
  <tr>
    <td align="center">
      <strong>자유 게시판 목록</strong><br />
      <img src="https://github.com/user-attachments/assets/514e761e-aa86-4e18-b5b3-15a04cd618a3" alt="자유 게시판 목록" width="420" />
    </td>
    <td align="center">
      <strong>게시글 상세</strong><br />
      <img src="https://github.com/user-attachments/assets/208ca850-157c-4b84-9d0a-03ce15580fc9" alt="게시글 상세" width="420" />
    </td>
  </tr>
  <tr>
    <td align="center">
      <strong>게시글 작성</strong><br />
      <img src="https://github.com/user-attachments/assets/bcf8ef94-2fe2-4f35-bb49-15076678bcbc" alt="게시글 작성" width="420" />
    </td>
    <td align="center">
      <strong>검색 화면</strong><br />
      <img src="https://github.com/user-attachments/assets/0274195b-f750-43a6-a3e5-06c125e8782e" alt="검색 화면" width="420" />
    </td>
  </tr>
</table>

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| **화면** | HTML, CSS, JavaScript, Thymeleaf, Bootstrap 5, Summernote |
| **서버** | Java 17, Spring Boot 3, Spring Web, Spring Security, Spring Validation, Spring Mail |
| **데이터 처리** | Spring Data JPA, QueryDSL, MySQL, Flyway |
| **부가 기능** | Redis, Spring Session, SSE, Caffeine Cache, SpringDoc OpenAPI |
| **테스트와 품질** | JUnit 5, H2, Testcontainers, SpotBugs |
| **빌드** | Gradle, Lombok |

## 구조 요약

```text
src/main/java/kwh/PublicCookedFood
├── account        로그인, 프로필, 차단, 계정 복구, 북마크
├── board          자유 게시판, 댓글, 신고, 관리자 정책
├── food           공공 레시피 조회와 검색
├── notification   실시간 알림과 읽음 처리
├── userrecipe     사용자 레시피, 댓글, 리뷰
├── metrics        인기글, 조회수, 추천 통계
└── storage        파일 저장 처리
```

## 대표 경로

| 경로 | 설명 |
| --- | --- |
| `/recipes` | 공공 레시피 목록과 검색 |
| `/recipes/{id}` | 공공 레시피 상세와 리뷰 |
| `/boards` | 자유 게시판 목록 |
| `/boards/{id}` | 게시글 상세, 댓글, 좋아요, 스크랩, 신고 |
| `/user-recipes` | 사용자 레시피 목록, 작성, 수정, 삭제 |
| `/u/*` | 회원가입, 로그인, 프로필, 복구, 차단 |
| `/bookmarks` | 북마크 목록과 관리 |
| `/api/notifications` | 실시간 알림, 읽음 처리, 알림 설정 |
| `/admin/boards` | 게시판 관리자 화면 |
| `/api/admin/boards` | 게시판 탭과 정책 관리 API |
