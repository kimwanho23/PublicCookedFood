# DTO Separation Strategy (Request / Response)

## Goal
- 입력용 DTO(Request)와 출력용 DTO(Response)를 분리해 책임을 명확히 한다.
- 검증(`@Valid`)은 Request DTO 중심으로 두고, View/API 응답은 Response DTO로 고정한다.

## Package Convention
- `.../dto/request/*`
- `.../dto/response/*`
- `.../dto/shared/*` (양방향 전환 중 임시 보관, 최종적으로 제거 목표)

## Phase 1 (완료)
- 검색 Query DTO를 Request로 이동
  - `common/dto/request/BoardSearchQuery`
  - `common/dto/request/RecipeSearchQuery`
- 사용자 입력 DTO를 Request로 이동
  - `user/dto/request/LoginDto`
  - `user/dto/request/UserSaveDto`
  - `user/dto/request/UserUpdateDto`
- 레시피 응답 DTO를 Response로 이동
  - `food/dto/response/recipe_info/Recipe_INFO_ResponseDto`
  - `food/dto/response/recipe_crse/Recipe_CRSE_ResponseDto`
  - `food/dto/response/recipe_irdnt/Recipe_IRDNT_ResponseDto`

## Phase 2 (완료)
- 게시판 입력 DTO 분리 적용
  - Request: `board/dto/request/BoardWriteRequest`
  - Request: `board/dto/request/BoardUpdateRequest`
  - Request: `board/dto/request/BoardSaveRequest`
  - Request: `board/dto/request/CommentCreateRequest`
- `BoardController`의 `@ModelAttribute` 입력은 Request DTO로 교체 완료
- `CommentsService#createComment` 입력을 Request DTO로 교체 완료
- 게시판 응답 DTO 분리 적용
  - Response: `board/dto/response/BoardDetailResponse`
  - Response: `board/dto/response/CommentResponse`
- `BoardDto`, `CommentsDto` 제거 완료
- 남은 작업
  - (선택) 게시판 목록 전용 응답 DTO(`BoardListItemResponse`) 추가

## Phase 3 (완료)
- 북마크 DTO 분리 적용
  - Request: `user/dto/request/BookmarkCreateRequest`
  - Response: `user/dto/response/BookmarkResponse`
- `BookMarkDto` 제거 완료

## Phase 4 (완료)
- 카테고리 응답 DTO 분리 적용
  - Response: `food/dto/response/RecipeCategoryGroupResponse`
- `MainController#createCategories()`의 `Map<String, Object>` 반환 제거
- 템플릿(`foods/index.html`)의 카테고리 렌더링은 타입 DTO 기반으로 유지

## Rule
- Controller 입력 파라미터: Request DTO만 사용
- Service 반환/Controller 모델: Response DTO만 사용
- Entity -> Response 변환은 Mapper(또는 `toResponse`)에서만 수행
