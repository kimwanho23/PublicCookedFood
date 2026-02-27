package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.config.properties.ImageSchemaProperties;
import kwh.PublicCookedFood.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageSchemaService {

    private static final String IMAGE_TABLE_NAME_LOOKUP_SQL = """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND LOWER(table_name) = 'images'
            LIMIT 1
            """;
    private static final String IMAGE_POST_ID_TYPE_LOOKUP_SQL = """
            SELECT column_type
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = ?
              AND column_name = 'post_id'
            LIMIT 1
            """;
    private static final String IMAGE_POST_ID_NULLABLE_LOOKUP_SQL = """
            SELECT is_nullable
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = ?
              AND column_name = 'post_id'
            LIMIT 1
            """;
    private static final String TABLE_NAME_LOOKUP_SQL = """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND LOWER(table_name) = ?
            LIMIT 1
            """;
    private static final String COLUMN_EXISTS_LOOKUP_SQL = """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = ?
              AND column_name = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AtomicBoolean legacySchemaChecked = new AtomicBoolean(false);
    private final AtomicBoolean boardImageSchemaChecked = new AtomicBoolean(false);
    private final ImageSchemaProperties imageSchemaProperties;

    public void ensureLegacyImagesSchemaCompatibleIfEnabled() {
        if (!Boolean.TRUE.equals(imageSchemaProperties.schemaAutoMigrate())) {
            return;
        }
        ensureLegacyImagesSchemaCompatible();
    }

    public void ensureBoardImageSchemaCompatibleIfEnabled() {
        if (!Boolean.TRUE.equals(imageSchemaProperties.schemaAutoMigrate())) {
            return;
        }
        ensureBoardImageSchemaCompatible();
    }

    private void ensureLegacyImagesSchemaCompatible() {
        if (legacySchemaChecked.get()) {
            return;
        }

        synchronized (legacySchemaChecked) {
            if (legacySchemaChecked.get()) {
                return;
            }

            try {
                String imagesTableName = querySingleValue(IMAGE_TABLE_NAME_LOOKUP_SQL);
                if (imagesTableName == null) {
                    legacySchemaChecked.set(true);
                    return;
                }

                String postIdColumnType = querySingleValue(IMAGE_POST_ID_TYPE_LOOKUP_SQL, imagesTableName);
                if (postIdColumnType == null) {
                    legacySchemaChecked.set(true);
                    return;
                }

                String isNullable = querySingleValue(IMAGE_POST_ID_NULLABLE_LOOKUP_SQL, imagesTableName);
                if (!"NO".equalsIgnoreCase(isNullable)) {
                    legacySchemaChecked.set(true);
                    return;
                }

                String alterSql = "ALTER TABLE `" + imagesTableName + "` MODIFY COLUMN post_id " + postIdColumnType + " NULL";
                jdbcTemplate.execute(alterSql);
                log.info("Legacy schema migration applied: {}.post_id is now nullable.", imagesTableName);
                legacySchemaChecked.set(true);
            } catch (RuntimeException e) {
                log.error("Failed to auto-migrate legacy images.post_id schema.", e);
                throw new StorageException("DB 스키마 보정 실패: images.post_id를 NULL 허용으로 변경하지 못했습니다.", e);
            }
        }
    }

    private void ensureBoardImageSchemaCompatible() {
        if (boardImageSchemaChecked.get()) {
            return;
        }

        synchronized (boardImageSchemaChecked) {
            if (boardImageSchemaChecked.get()) {
                return;
            }

            try {
                String boardTableName = findTableName("board");
                String imagesTableName = findTableName("images");
                if (boardTableName == null || imagesTableName == null) {
                    return;
                }

                String boardImageTableName = findTableName("board_image");
                if (boardImageTableName == null) {
                    String createSql = "CREATE TABLE board_image ("
                            + "id BIGINT NOT NULL AUTO_INCREMENT, "
                            + "board_id BIGINT NOT NULL, "
                            + "image_id BIGINT NOT NULL, "
                            + "regTime DATETIME(6) NULL, "
                            + "updateTime DATETIME(6) NULL, "
                            + "PRIMARY KEY (id), "
                            + "CONSTRAINT uk_board_image_board_id_image_id UNIQUE (board_id, image_id), "
                            + "INDEX idx_board_image_board_id (board_id), "
                            + "INDEX idx_board_image_image_id (image_id), "
                            + "CONSTRAINT fk_board_image_board FOREIGN KEY (board_id) REFERENCES "
                            + quoteIdentifier(boardTableName) + " (id) ON DELETE CASCADE, "
                            + "CONSTRAINT fk_board_image_image FOREIGN KEY (image_id) REFERENCES "
                            + quoteIdentifier(imagesTableName) + " (id) ON DELETE CASCADE"
                            + ")";
                    jdbcTemplate.execute(createSql);
                    boardImageTableName = "board_image";
                    log.info("Legacy schema migration applied: board_image table created.");
                }

                backfillBoardImageLinks(boardImageTableName, boardTableName, imagesTableName);
                boardImageSchemaChecked.set(true);
            } catch (RuntimeException e) {
                log.error("Failed to auto-migrate board_image schema.", e);
                throw new StorageException("DB 스키마 보정 실패: board_image 테이블을 생성/보정하지 못했습니다.", e);
            }
        }
    }

    private void backfillBoardImageLinks(String boardImageTableName, String boardTableName, String imagesTableName) {
        if (!hasColumn(imagesTableName, "post_id")) {
            return;
        }

        String insertSql = "INSERT INTO " + quoteIdentifier(boardImageTableName)
                + " (board_id, image_id, regTime, updateTime) "
                + "SELECT i.post_id, i.id, COALESCE(i.regTime, NOW()), COALESCE(i.updateTime, NOW()) "
                + "FROM " + quoteIdentifier(imagesTableName) + " i "
                + "JOIN " + quoteIdentifier(boardTableName) + " b ON b.id = i.post_id "
                + "LEFT JOIN " + quoteIdentifier(boardImageTableName) + " bi "
                + "ON bi.board_id = i.post_id AND bi.image_id = i.id "
                + "WHERE i.post_id IS NOT NULL AND bi.id IS NULL";
        jdbcTemplate.execute(insertSql);

        String statusSyncSql = "UPDATE " + quoteIdentifier(imagesTableName) + " i "
                + "JOIN " + quoteIdentifier(boardImageTableName) + " bi ON bi.image_id = i.id "
                + "SET i.status = 'ATTACHED' "
                + "WHERE i.status <> 'DELETED'";
        jdbcTemplate.execute(statusSyncSql);
    }

    private String findTableName(String tableNameLowerCase) {
        return querySingleValue(TABLE_NAME_LOOKUP_SQL, tableNameLowerCase);
    }

    private boolean hasColumn(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(COLUMN_EXISTS_LOOKUP_SQL, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private String querySingleValue(String sql, Object... args) {
        List<String> results = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1), args);
        if (results.isEmpty()) {
            return null;
        }

        String value = results.get(0);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
