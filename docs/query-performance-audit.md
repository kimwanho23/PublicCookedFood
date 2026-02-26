# Query Performance Audit

## Scope
- Board detail comment loading (`BoardController#boardDetail`, `CommentsService#getCommentListWithReplies`)
- Bookmark list loading (`BookmarkController#bookmarkList`)
- Recipe search paging (`Recipe_INFO_RepositoryImpl#findRecipesByConditions`)

## Applied Improvements

### 1) Board detail comment loading
- Problem:
  - 기존 구조는 게시글의 전체 댓글/답글을 모두 조회한 뒤 애플리케이션 메모리에서 상위 댓글 페이징을 수행했다.
  - 댓글이 많은 게시글에서 불필요한 DB/메모리 비용이 발생한다.
- Change:
  - 상위 댓글: DB 레벨 페이징 + 작성자 fetch join
  - 답글: 별도 일괄 조회(작성자/부모 fetch join)
  - 댓글 DTO 변환에서 `boardId`는 파라미터로 주입해 불필요한 board lazy 접근 제거
- Files:
  - `src/main/java/kwh/PublicCookedFood/board/repository/CommentsRepository.java`
  - `src/main/java/kwh/PublicCookedFood/board/service/CommentsService.java`

### 2) Board detail main entity load
- Problem:
  - 게시글 상세 조회 시 `Board -> user` 접근이 lazy 초기화 쿼리를 유발할 수 있다.
- Change:
  - 상세 조회 전용 `findByIdWithUser`(fetch join) 추가.
- Files:
  - `src/main/java/kwh/PublicCookedFood/board/repository/BoardRepository.java`
  - `src/main/java/kwh/PublicCookedFood/board/service/BoardService.java`

### 3) Bookmark list query trimming
- Problem:
  - 북마크 목록에서 `JOIN FETCH b.user`는 결과 렌더링에 불필요한 조인이었다.
- Change:
  - recipe fetch join만 유지.
- File:
  - `src/main/java/kwh/PublicCookedFood/user/repository/BookmarkRepository.java`

### 4) Recipe search query efficiency
- Problem:
  - 정확 일치 필터(`type`, `nation`, `ingredient`)에 `equalsIgnoreCase` 사용으로 인덱스 활용이 불리할 수 있다.
  - 필터 검색 정렬이 고정되지 않아 페이지 일관성이 떨어질 수 있다.
  - count 쿼리가 항상 강제 실행된다.
- Change:
  - 정확 필터는 `eq`로 전환.
  - 결과 정렬을 `rowNUM ASC`로 고정.
  - `PageableExecutionUtils` 적용으로 조건에 따라 count 쿼리 지연/생략.
- File:
  - `src/main/java/kwh/PublicCookedFood/food/repository/Recipe_INFO_RepositoryImpl.java`

## Remaining Risk
- 답글 조회는 현재 "게시글의 모든 답글"을 한 번에 로딩한다.
- 상위 댓글만 페이지 단위로 줄였지만, 답글이 극단적으로 많은 게시글에서는 추가 최적화(예: recursive CTE 기반 subtree 조회)가 필요할 수 있다.

## Index Additions
- `Comments(post_id, parent_id, regTime)` 추가
  - 상위 댓글(`parent_id IS NULL`) 조회 + 정렬 패턴 대응
- `Recipe_INFO(ty_NM, row_NUM)`, `Recipe_INFO(nation_NM, row_NUM)`, `Recipe_INFO(irdnt_CODE, row_NUM)` 추가
  - 정확 필터 + `row_NUM ASC` 페이징 정렬 패턴 대응

## Execution Plan Validation
- 실제 DB에서 실행계획 검증용 쿼리셋:
  - `docs/explain-checklist.md`
