# Entity Audit

## Goal
- DTO 도입 이후 엔티티 책임을 재정렬하고, 유지가 필요한 것과 제거/축소 가능한 것을 구분한다.

## Keep (현재 필수)
- `board/domain/Board`
  - 게시글 핵심 모델.
  - 게시글 목록/상세/수정/삭제에 직접 사용.
- `board/domain/Comments`
  - 댓글/대댓글 핵심 모델.
  - 댓글 생성/조회/삭제 및 카운트 집계에 직접 사용.
- `board/domain/Likes`
  - 게시글 좋아요 핵심 모델.
  - 좋아요 중복 방지/카운트 집계에 직접 사용.
- `board/domain/Images`
  - 이미지 업로드 메타데이터(TEMP/ATTACHED/DELETED) 저장 모델.
  - 본문 이미지 동기화/정리 로직에 직접 사용.
- `board/domain/BoardSection`
  - 관리자 게시판 탭(추가/비활성화/삭제) 관리 모델.
- `board/domain/BoardPolicy`
  - 추천 게시물 임계값 정책 모델.
- `user/domain/Users`
  - 인증 principal, 작성자/북마크/좋아요 연관의 기준 엔티티.
- `user/domain/Bookmark`
  - 레시피 북마크 핵심 모델.
- `food/entity/Recipe_INFO`, `food/entity/Recipe_CRSE`, `food/entity/Recipe_IRDNT`
  - 레시피 조회 및 외부 API 적재(import) 핵심 모델.

## Removed (정리 완료)
- `board/domain/Files`
- `board/repository/FilesRepository`
- `board/service/FileService`
- `board/dto/FilesDto`
- `board/dto/ImagesDto`
- `board/dto/LikesDto`
- `user/dto/response/BookmarkResponse`
  - 관련 변환 메서드(`Likes#toResponseDto`, `Bookmark#toResponseDto`) 제거 완료.
- `user/dto/request/UserUpdateDto#toEntity()`
  - 미사용 변환 메서드 제거 완료.

## Completed Refactor
- 엔티티 -> DTO 변환 메서드 제거
  - `Board#toResponseDto()` 제거 후 `BoardService` 매핑으로 이전.
  - `Recipe_INFO#toResponseDto()`, `Recipe_CRSE#toResponseDto()`, `Recipe_IRDNT#toResponseDto()` 제거 후 `RecipeService` 매핑으로 이전.
- DTO 엔티티 직접 의존 제거
  - `BoardDetailResponse.userId`를 `Long`으로 변경하고 `userName` 필드 추가.
  - `BoardSaveRequest.userId`를 `Long`으로 변경.
  - `Recipe_*_ResponseDto#toEntity()` 제거로 Response DTO의 엔티티 의존 제거.
- `Bookmark` 식별 키 정리
  - `Bookmark` 연관 조인을 `email`에서 `user_id`로 전환.
  - `BookmarkCreateRequest` 저장 입력을 `email`에서 `userId`로 전환.
- soft delete 상태값 타입 개선
  - `Board.state`, `Comments.state`를 enum(`SoftDeleteState`)으로 전환.
  - DB는 기존 `"1"/"0"` 값을 유지하고, `SoftDeleteStateConverter`로 매핑.

## Notes
- `Users`의 역방향 컬렉션(`bookmarks`, `boards`, `comments`, `likes`)은 현재 조회 로직에서 직접 사용 빈도가 낮다.
  - 대량 연관 로딩/영속성 전파 리스크를 줄이려면 필요한 관계만 유지하는 방향이 안전하다.
- `Bookmark` 매핑은 `user_id` 조인으로 전환 완료.
  - 기존 데이터가 `email` 컬럼만 보유한 경우 `docs/sql/bookmark_user_id_backfill.sql` 실행 후 운영 권장.

## Recommended Sequence
1. 미사용 엔티티/리포지토리/서비스/DTO 제거 (영향 범위 작음)
2. DTO에서 엔티티 직접 노출 제거 (`BoardDetailResponse`, `BoardSaveRequest`) - 완료
3. 엔티티의 `toResponseDto()` 제거 및 mapper/service 변환으로 이동 - 완료
4. `state` 문자열을 enum/boolean으로 마이그레이션 - 완료(enum + converter)
