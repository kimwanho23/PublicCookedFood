package kwh.PublicCookedFood.board.service.support;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Getter
public final class BoardVisibilityCriteria {

    private static final long NO_RESTRICTED_ACCOUNT_SENTINEL = -1L;

    private final Set<Long> restrictedAccountIds;

    public BoardVisibilityCriteria(Set<Long> restrictedAccountIds) {
        LinkedHashSet<Long> normalizedIds = new LinkedHashSet<Long>();
        if (restrictedAccountIds != null) {
            for (Long id : restrictedAccountIds) {
                if (id != null && id > 0L) {
                    normalizedIds.add(id);
                }
            }
        }
        if (normalizedIds.isEmpty()) {
            this.restrictedAccountIds = Collections.emptySet();
            return;
        }
        this.restrictedAccountIds = Collections.unmodifiableSet(new LinkedHashSet<Long>(normalizedIds));
    }

    public static BoardVisibilityCriteria of(Collection<Long> restrictedAccountIds) {
        if (restrictedAccountIds == null || restrictedAccountIds.isEmpty()) {
            return new BoardVisibilityCriteria(Collections.<Long>emptySet());
        }
        return new BoardVisibilityCriteria(new LinkedHashSet<Long>(restrictedAccountIds));
    }

    public Set<Long> restrictedAccountIds() {
        return restrictedAccountIds;
    }

    public boolean excludeRestricted() {
        return !restrictedAccountIds.isEmpty();
    }

    public List<Long> restrictedAccountIdsOrSentinel() {
        if (restrictedAccountIds.isEmpty()) {
            return Collections.singletonList(NO_RESTRICTED_ACCOUNT_SENTINEL);
        }
        return List.copyOf(restrictedAccountIds);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BoardVisibilityCriteria other)) {
            return false;
        }
        return Objects.equals(restrictedAccountIds, other.restrictedAccountIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(restrictedAccountIds);
    }
}
