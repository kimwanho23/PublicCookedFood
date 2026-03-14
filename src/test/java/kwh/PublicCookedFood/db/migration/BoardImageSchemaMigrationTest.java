package kwh.PublicCookedFood.db.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class BoardImageSchemaMigrationTest {

    private static final int APPLIED_V4_CHECKSUM = 1093982291;

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.4")
            .withDatabaseName("public_cooked_food")
            .withUsername("test")
            .withPassword("test");

    @Test
    void v4MigratesLegacyImageSchemaToBoardImageLinks() {
        DataSource dataSource = new DriverManagerDataSource(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        );
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        createLegacySchema(jdbcTemplate);
        seedLegacyData(jdbcTemplate);

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("3")
                .load()
                .migrate();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT checksum FROM flyway_schema_history WHERE version = '4'",
                Integer.class
        )).isEqualTo(APPLIED_V4_CHECKSUM);

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND LOWER(table_name) = 'board_image'
                """,
                Integer.class
        )).isEqualTo(1);

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'images'
                  AND column_name = 'post_id'
                """,
                String.class
        )).isEqualTo("YES");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM board_image",
                Integer.class
        )).isEqualTo(2);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM images WHERE id = 10",
                String.class
        )).isEqualTo("ATTACHED");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM images WHERE id = 11",
                String.class
        )).isEqualTo("DELETED");

        jdbcTemplate.update(
                """
                INSERT INTO images (id, imgUrl, post_id, regTime, updateTime, status)
                VALUES (12, '/images/new.png', NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'TEMP')
                """
        );

        assertThat(jdbcTemplate.queryForObject(
                "SELECT post_id FROM images WHERE id = 12",
                Long.class
        )).isNull();
    }

    private void createLegacySchema(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute(
                """
                CREATE TABLE board (
                    id BIGINT NOT NULL,
                    PRIMARY KEY (id)
                )
                """
        );
        jdbcTemplate.execute(
                """
                CREATE TABLE images (
                    id BIGINT NOT NULL,
                    imgUrl VARCHAR(500) NOT NULL,
                    post_id BIGINT NOT NULL,
                    regTime DATETIME(6) NULL,
                    updateTime DATETIME(6) NULL,
                    status VARCHAR(20) NULL,
                    PRIMARY KEY (id),
                    CONSTRAINT fk_images_board FOREIGN KEY (post_id) REFERENCES board (id)
                )
                """
        );
    }

    private void seedLegacyData(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("INSERT INTO board (id) VALUES (1)");
        jdbcTemplate.update("INSERT INTO board (id) VALUES (2)");
        jdbcTemplate.update(
                """
                INSERT INTO images (id, imgUrl, post_id, regTime, updateTime, status)
                VALUES (10, '/images/first.png', 1, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'TEMP')
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO images (id, imgUrl, post_id, regTime, updateTime, status)
                VALUES (11, '/images/second.png', 2, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'DELETED')
                """
        );
    }
}
