package kwh.PublicCookedFood.food.repository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("Manual benchmark. Temporarily remove @Disabled to execute.")
class RecipeSearchBenchmarkTest {

    private static final int DATASET_SIZE = 80_000;
    private static final int WARMUP_ROUNDS = 15;
    private static final int MEASURE_ROUNDS = 80;
    private static final String KEYWORD = "한식";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    void prepareDataset() {
        jdbcTemplate.update("DELETE FROM Recipe_INFO");

        String insertSql = "INSERT INTO Recipe_INFO (" +
                "row_NUM, recipe_ID, recipe_NM_KO, sumry, nation_CODE, nation_NM, " +
                "ty_CODE, ty_NM, cooking_TIME, calorie, qnt, level_NM, irdnt_CODE, pc_NM, img_URL" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<String> typeValues = List.of("한식", "중식", "일식", "양식", "퓨전", "한식풍");
        List<String> nationValues = List.of("한국", "중국", "일본", "이탈리아", "태국", "대한민국");
        List<String> ingredientValues = List.of("돼지고기", "소고기", "밀가루", "두부", "해산물", "한식재료");

        jdbcTemplate.batchUpdate(insertSql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                String type = typeValues.get(random.nextInt(typeValues.size()));
                String nation = nationValues.get(random.nextInt(nationValues.size()));
                String ingredient = ingredientValues.get(random.nextInt(ingredientValues.size()));

                ps.setLong(1, i + 1L);
                ps.setLong(2, 1_000_000L + i);
                ps.setString(3, "레시피-" + i + "-" + (i % 10 == 0 ? "고기" : "일반"));
                ps.setString(4, "요약-" + i);
                ps.setString(5, "N" + (i % 20));
                ps.setString(6, nation);
                ps.setString(7, "T" + (i % 20));
                ps.setString(8, type);
                ps.setString(9, "30분");
                ps.setString(10, "250");
                ps.setString(11, "2인분");
                ps.setString(12, "보통");
                ps.setString(13, ingredient);
                ps.setString(14, "중");
                ps.setString(15, "https://example.com/img/" + i + ".jpg");
            }

            @Override
            public int getBatchSize() {
                return DATASET_SIZE;
            }
        });
    }

    @Test
    void compareLegacyKeywordLikeAndOptimizedKeywordEq() {
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            runLegacyKeywordQuery(KEYWORD);
            runOptimizedKeywordQuery(KEYWORD);
        }

        List<Long> legacyTimes = new ArrayList<>();
        List<Long> optimizedTimes = new ArrayList<>();
        for (int i = 0; i < MEASURE_ROUNDS; i++) {
            legacyTimes.add(measureNanos(() -> runLegacyKeywordQuery(KEYWORD)));
            optimizedTimes.add(measureNanos(() -> runOptimizedKeywordQuery(KEYWORD)));
        }

        BenchmarkStats legacyStats = BenchmarkStats.from("legacy-like", legacyTimes);
        BenchmarkStats optimizedStats = BenchmarkStats.from("optimized-eq", optimizedTimes);

        double avgSpeedup = safeRatio(legacyStats.avgMillis(), optimizedStats.avgMillis());
        double p50Speedup = safeRatio(legacyStats.p50Millis(), optimizedStats.p50Millis());
        double p90Speedup = safeRatio(legacyStats.p90Millis(), optimizedStats.p90Millis());
        double p99Speedup = safeRatio(legacyStats.p99Millis(), optimizedStats.p99Millis());

        System.out.printf(
                Locale.ROOT,
                "[RecipeSearchBenchmark] rows=%d, rounds=%d, " +
                        "legacy(avg=%.3fms,p50=%.3fms,p90=%.3fms,p95=%.3fms,p99=%.3fms), " +
                        "optimized(avg=%.3fms,p50=%.3fms,p90=%.3fms,p95=%.3fms,p99=%.3fms), " +
                        "speedup(avg=%.2fx,p50=%.2fx,p90=%.2fx,p99=%.2fx)%n",
                DATASET_SIZE,
                MEASURE_ROUNDS,
                legacyStats.avgMillis(),
                legacyStats.p50Millis(),
                legacyStats.p90Millis(),
                legacyStats.p95Millis(),
                legacyStats.p99Millis(),
                optimizedStats.avgMillis(),
                optimizedStats.p50Millis(),
                optimizedStats.p90Millis(),
                optimizedStats.p95Millis(),
                optimizedStats.p99Millis(),
                avgSpeedup,
                p50Speedup,
                p90Speedup,
                p99Speedup
        );

        assertThat(runLegacyKeywordQuery(KEYWORD)).isNotEmpty();
        assertThat(runOptimizedKeywordQuery(KEYWORD)).isNotEmpty();
    }

    private List<Long> runLegacyKeywordQuery(String keyword) {
        String sql = "SELECT row_NUM FROM Recipe_INFO " +
                "WHERE LOWER(ty_NM) LIKE CONCAT('%', LOWER(?), '%') " +
                "   OR LOWER(nation_NM) LIKE CONCAT('%', LOWER(?), '%') " +
                "   OR LOWER(irdnt_CODE) LIKE CONCAT('%', LOWER(?), '%') " +
                "ORDER BY row_NUM ASC " +
                "LIMIT 15";
        return jdbcTemplate.queryForList(sql, Long.class, keyword, keyword, keyword);
    }

    private List<Long> runOptimizedKeywordQuery(String keyword) {
        String sql = "SELECT row_NUM FROM Recipe_INFO " +
                "WHERE ty_NM = ? OR nation_NM = ? OR irdnt_CODE = ? " +
                "ORDER BY row_NUM ASC " +
                "LIMIT 15";
        return jdbcTemplate.queryForList(sql, Long.class, keyword, keyword, keyword);
    }

    private long measureNanos(Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        return System.nanoTime() - start;
    }

    private double safeRatio(double numerator, double denominator) {
        if (denominator <= 0.0d) {
            return 0.0d;
        }
        return numerator / denominator;
    }

    private record BenchmarkStats(double avgMillis,
                                  double p50Millis,
                                  double p90Millis,
                                  double p95Millis,
                                  double p99Millis) {
        private static BenchmarkStats from(String name, List<Long> nanos) {
            if (nanos == null || nanos.isEmpty()) {
                return new BenchmarkStats(0.0d, 0.0d, 0.0d, 0.0d, 0.0d);
            }
            List<Long> sorted = nanos.stream().sorted().toList();
            double avg = nanos.stream().mapToDouble(value -> value / 1_000_000.0d).average().orElse(0.0d);
            double p50 = percentileMillis(sorted, 0.50d);
            double p90 = percentileMillis(sorted, 0.90d);
            double p95 = percentileMillis(sorted, 0.95d);
            double p99 = percentileMillis(sorted, 0.99d);
            System.out.printf(
                    Locale.ROOT,
                    "[RecipeSearchBenchmark/%s] avg=%.3fms p50=%.3fms p90=%.3fms p95=%.3fms p99=%.3fms samples=%d%n",
                    name,
                    avg,
                    p50,
                    p90,
                    p95,
                    p99,
                    sorted.size()
            );
            return new BenchmarkStats(avg, p50, p90, p95, p99);
        }

        private static double percentileMillis(List<Long> sortedNanos, double percentile) {
            int index = (int) Math.ceil(sortedNanos.size() * percentile) - 1;
            index = Math.max(0, Math.min(index, sortedNanos.size() - 1));
            return sortedNanos.get(index) / 1_000_000.0d;
        }
    }
}
