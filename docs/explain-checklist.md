# EXPLAIN Checklist

## Preconditions
- MySQL 8.x 기준
- 애플리케이션과 동일 DB(`food`)에 접속
- SQL 실행 전/후 `EXPLAIN ANALYZE` 결과를 비교

## 1) Board Detail - Parent Comments Paging
```sql
EXPLAIN ANALYZE
SELECT c.*
FROM Comments c
JOIN user u ON u.id = c.user_id
WHERE c.post_id = :postId
  AND c.parent_id IS NULL
ORDER BY c.regTime ASC
LIMIT :size OFFSET :offset;
```

Expected:
- `idx_comments_post_parent_reg_time` 또는 `idx_comments_post_reg_time` 활용
- `filesort` 최소화

## 2) Board Detail - Replies Loading
```sql
EXPLAIN ANALYZE
SELECT c.*
FROM Comments c
JOIN user u ON u.id = c.user_id
WHERE c.post_id = :postId
  AND c.parent_id IS NOT NULL
ORDER BY c.regTime ASC;
```

Expected:
- `idx_comments_post_reg_time` 활용

## 3) Bookmark List
```sql
EXPLAIN ANALYZE
SELECT b.*
FROM bookmark b
JOIN Recipe_INFO r ON r.row_NUM = b.recipe_ID
WHERE b.user_id = :userId;
```

Expected:
- `bookmark`의 `(user_id, recipe_ID)` unique index 활용

## 4) Recipe Search (Exact Filters + Paging)
```sql
EXPLAIN ANALYZE
SELECT r.*
FROM Recipe_INFO r
WHERE r.ty_NM = :type
  AND r.nation_NM = :nation
  AND r.irdnt_CODE = :ingredient
ORDER BY r.row_NUM ASC
LIMIT :size OFFSET :offset;
```

Expected:
- 상황에 따라 `idx_recipe_info_ty_nm_row_num`, `idx_recipe_info_nation_nm_row_num`,
  `idx_recipe_info_irdnt_code_row_num` 중 하나를 활용

## 5) Recipe Search (Keyword LIKE)
```sql
EXPLAIN ANALYZE
SELECT r.*
FROM Recipe_INFO r
WHERE LOWER(r.recipe_NM_KO) LIKE CONCAT('%', LOWER(:search), '%')
ORDER BY r.row_NUM ASC
LIMIT :size OFFSET :offset;
```

Expected:
- `%keyword%` 패턴은 일반 B-Tree 인덱스 사용이 제한적
- 검색 성능 요구가 높으면 FULLTEXT(또는 별도 검색 엔진) 검토
