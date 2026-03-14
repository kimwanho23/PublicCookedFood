package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.account.domain.Account;

import java.util.Objects;
import java.util.Optional;

public interface BoardViewer {

    static BoardViewer from(Account account) {
        if (account == null || account.getId() == null) {
            return anonymous();
        }
        return authenticated(account.getId());
    }

    static BoardViewer anonymous() {
        return Anonymous.INSTANCE;
    }

    static Authenticated authenticated(long accountId) {
        return new Authenticated(accountId);
    }

    default boolean isAuthenticated() {
        return this instanceof Authenticated;
    }

    default Optional<Long> maybeAccountId() {
        if (this instanceof Authenticated authenticated) {
            return Optional.of(authenticated.accountId());
        }
        return Optional.empty();
    }

    default Authenticated requireAuthenticated() {
        if (this instanceof Authenticated) {
            return (Authenticated) this;
        }
        throw new IllegalStateException("authenticated viewer is required");
    }

    final class Anonymous implements BoardViewer {
        private static final Anonymous INSTANCE = new Anonymous();

        private Anonymous() {
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Anonymous;
        }

        @Override
        public int hashCode() {
            return Anonymous.class.hashCode();
        }
    }

    final class Authenticated implements BoardViewer {

        private final long accountId;

        public Authenticated(long accountId) {
            if (accountId <= 0) {
                throw new IllegalArgumentException("viewer accountId must be positive");
            }
            this.accountId = accountId;
        }

        public long accountId() {
            return accountId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Authenticated other)) {
                return false;
            }
            return accountId == other.accountId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(accountId);
        }

        @Override
        public String toString() {
            return "Authenticated[accountId=" + accountId + "]";
        }
    }
}
