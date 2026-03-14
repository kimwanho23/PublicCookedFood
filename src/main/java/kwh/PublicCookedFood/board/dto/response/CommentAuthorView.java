package kwh.PublicCookedFood.board.dto.response;

import kwh.PublicCookedFood.account.domain.Account;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class CommentAuthorView {

    private static final CommentAuthorView ANONYMOUS = new CommentAuthorView(null, null, null);

    private final Long accountId;
    private final String name;
    private final String profileImageUrl;

    public CommentAuthorView(Long accountId, String name, String profileImageUrl) {
        this.accountId = accountId;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    public static CommentAuthorView anonymous() {
        return ANONYMOUS;
    }

    public static CommentAuthorView from(Account account) {
        if (account == null) {
            return anonymous();
        }
        return new CommentAuthorView(
                account.getId(),
                account.getName(),
                account.getProfileImageUrl()
        );
    }

    public Long accountId() {
        return accountId;
    }

    public String name() {
        return name;
    }

    public String profileImageUrl() {
        return profileImageUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CommentAuthorView)) {
            return false;
        }
        CommentAuthorView other = (CommentAuthorView) obj;
        return Objects.equals(accountId, other.accountId)
                && Objects.equals(name, other.name)
                && Objects.equals(profileImageUrl, other.profileImageUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, name, profileImageUrl);
    }
}
