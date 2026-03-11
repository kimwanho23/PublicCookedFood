package kwh.PublicCookedFood.notification.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationVisibilityCriteriaUnitTest {

    @Test
    void of_usesSentinelWhenRestrictedAccountsAreAbsent() {
        NotificationVisibilityCriteria criteria = NotificationVisibilityCriteria.of(1L, Set.of(), false);

        assertThat(criteria.excludeRestricted()).isFalse();
        assertThat(criteria.restrictedAccountIdsOrSentinel()).containsExactly(-1L);
    }

    @Test
    void of_preservesRestrictedAccountsWhenPresent() {
        NotificationVisibilityCriteria criteria = NotificationVisibilityCriteria.of(1L, Set.of(3L, 2L), true);

        assertThat(criteria.excludeRestricted()).isTrue();
        assertThat(criteria.unreadOnly()).isTrue();
        assertThat(criteria.restrictedAccountIdsOrSentinel()).containsExactlyInAnyOrder(2L, 3L);
    }
}
