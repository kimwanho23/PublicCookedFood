package kwh.PublicCookedFood.board.service.support;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BoardVisibilityCriteriaUnitTest {

    @Test
    void of_returnsSentinelWhenRestrictedAccountsAreEmpty() {
        BoardVisibilityCriteria criteria = BoardVisibilityCriteria.of(List.of());

        assertThat(criteria.excludeRestricted()).isFalse();
        assertThat(criteria.restrictedAccountIdsOrSentinel()).containsExactly(-1L);
    }

    @Test
    void of_filtersNullAndNonPositiveIds() {
        BoardVisibilityCriteria criteria = BoardVisibilityCriteria.of(Arrays.asList(null, -1L, 0L, 2L, 3L));

        assertThat(criteria.excludeRestricted()).isTrue();
        assertThat(criteria.restrictedAccountIds()).containsExactly(2L, 3L);
    }
}
