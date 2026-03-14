package kwh.PublicCookedFood.board.dto.response;

import kwh.PublicCookedFood.account.domain.Account;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class BoardAuthorView {

    private static final BoardAuthorView ANONYMOUS = new BoardAuthorView(null, null, null);

    private final Long accountId;
    private final String accountName;
    private final String accountProfileImageUrl;

    public BoardAuthorView(Long accountId, String accountName, String accountProfileImageUrl) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.accountProfileImageUrl = accountProfileImageUrl;
    }

    public static BoardAuthorView anonymous() {
        return ANONYMOUS;
    }

    public static BoardAuthorView from(Account account) {
        if (account == null) {
            return anonymous();
        }
        return new BoardAuthorView(
                account.getId(),
                account.getName(),
                account.getProfileImageUrl()
        );
    }

    public Long accountId() {
        return accountId;
    }

    public String accountName() {
        return accountName;
    }

    public String accountProfileImageUrl() {
        return accountProfileImageUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BoardAuthorView)) {
            return false;
        }
        BoardAuthorView other = (BoardAuthorView) obj;
        return Objects.equals(accountId, other.accountId)
                && Objects.equals(accountName, other.accountName)
                && Objects.equals(accountProfileImageUrl, other.accountProfileImageUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, accountName, accountProfileImageUrl);
    }
}
