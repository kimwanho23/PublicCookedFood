# Controller Role Audit

## Result
- `@RestController`
  - `recipeSaveLogic/RecipeSaveController`
  - `board/contoller/ImageController`
- `@Controller` (Thymeleaf view rendering)
  - `food/controller/MainController`
  - `food/controller/RecipeController`
  - `board/contoller/BoardController`
  - `user/controller/UserController`
  - `user/controller/BookmarkController`

## Validation
- JSON/HTTP body 응답(`ResponseEntity`)은 `@RestController`에만 존재한다.
- View template 이름 반환 로직은 `@Controller`에 위치한다.
- 현재 코드 기준으로 `@Controller`에 있어야 할 로직과 `@RestController`에 있어야 할 로직의 역할 혼재는 확인되지 않았다.

## Note
- 단일 경로로 통일 완료 (레거시 별칭 제거).
  - `GET /recipes`, `GET /recipes/{id}`
  - `GET /boards`, `GET /boards/{id}`
  - `POST /boards`, `PATCH /boards/{id}`, `PUT /boards/{id}/likes`
  - `POST /boards/{id}/comments`
  - `GET /bookmarks`, `POST /bookmarks/{recipeId}`, `DELETE /bookmarks/{recipeId}`
  - `POST /api/images`
  - `POST /api/admin/recipes/import/courses`
  - `POST /api/admin/recipes/import/infos`
  - `POST /api/admin/recipes/import/ingredients`
