package kwh.PublicCookedFood.account.facade;

import jakarta.annotation.PostConstruct;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.service.ImageService;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.dto.request.AccountUpdateDto;
import kwh.PublicCookedFood.account.audit.AccountAuditPublisher;
import kwh.PublicCookedFood.account.facade.profile.AccountProfileViewPages;
import kwh.PublicCookedFood.account.facade.profile.AccountProfileViewStrategy;
import kwh.PublicCookedFood.account.facade.profile.AccountProfileViewType;
import kwh.PublicCookedFood.account.policy.AccountProfileAccessPolicy;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccountProfileFacade {

    private final AccountService accountService;
    private final AccountBlockService accountBlockService;
    private final AccountAuditPublisher accountAuditPublisher;
    private final AccountProfileAccessPolicy accountProfileAccessPolicy;
    private final List<AccountProfileViewStrategy> profileViewStrategies;
    private final ImageService imageService;

    private Map<AccountProfileViewType, AccountProfileViewStrategy> profileViewStrategyMap = Map.of();

    @PostConstruct
    void initializeProfileViewStrategies() {
        EnumMap<AccountProfileViewType, AccountProfileViewStrategy> strategyMap = new EnumMap<>(AccountProfileViewType.class);
        for (AccountProfileViewStrategy strategy : profileViewStrategies) {
            AccountProfileViewType viewType = strategy.viewType();
            AccountProfileViewStrategy existing = strategyMap.putIfAbsent(viewType, strategy);
            if (existing != null) {
                throw new IllegalStateException("중복 프로필 뷰 전략이 등록되어 있습니다: " + viewType.key());
            }
        }

        for (AccountProfileViewType viewType : AccountProfileViewType.values()) {
            if (!strategyMap.containsKey(viewType)) {
                throw new IllegalStateException("필수 프로필 뷰 전략이 누락되었습니다: " + viewType.key());
            }
        }

        profileViewStrategyMap = Map.copyOf(strategyMap);
    }

    public void populateAccountUpdateDto(AccountUpdateDto accountUpdateDto, Account account) {
        if (accountUpdateDto.getEmail() == null) {
            accountUpdateDto.setEmail(account.getEmail());
        }
        if (accountUpdateDto.getName() == null) {
            accountUpdateDto.setName(account.getName());
        }
        if (accountUpdateDto.getPhoneNumber() == null) {
            accountUpdateDto.setPhoneNumber(account.getPhoneNumber());
        }
        if (accountUpdateDto.getBirthDate() == null) {
            accountUpdateDto.setBirthDate(account.getBirthDate());
        }
        if (accountUpdateDto.getGender() == null) {
            accountUpdateDto.setGender(account.getGender());
        }
        if (accountUpdateDto.getAddress() == null) {
            accountUpdateDto.setAddress(account.getAddress());
        }
        if (accountUpdateDto.getAddressDetail() == null) {
            accountUpdateDto.setAddressDetail(account.getAddressDetail());
        }
        if (accountUpdateDto.getProfileImageUrl() == null) {
            accountUpdateDto.setProfileImageUrl(account.getProfileImageUrl());
        }
    }

    public List<Account> getBlockedAccounts(Long accountId) {
        return accountBlockService.getBlockedAccounts(accountId);
    }

    public OtherProfileViewData loadOtherProfile(Long accountId,
                                                 String view,
                                                 Account loginAccount,
                                                 Pageable pageable) {
        Account profileAccount = accountService.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("사용자 정보를 찾을 수 없습니다."));

        if (accountProfileAccessPolicy.isProfileViewRestricted(loginAccount, accountId)) {
            throw new AppException(CommonErrorCode.ACCESS_DENIED, "차단 관계인 사용자의 프로필은 조회할 수 없습니다.");
        }

        Set<Long> blockedAccountIds = accountBlockService.getViewRestrictedAccountIds(loginAccount == null ? null : loginAccount.getId());
        AccountProfileViewType profileViewType = AccountProfileViewType.from(view);
        AccountProfileViewPages viewPages = resolveProfileViewStrategy(profileViewType)
                .load(accountId, pageable, blockedAccountIds);

        boolean myBlockedProfileAccount = accountProfileAccessPolicy.hasBlockedProfileAccount(loginAccount, accountId);
        Long currentAccountId = loginAccount == null ? null : loginAccount.getId();

        return new OtherProfileViewData(
                profileAccount,
                profileViewType.key(),
                currentAccountId,
                myBlockedProfileAccount,
                viewPages.boardPage(),
                viewPages.commentPage(),
                viewPages.scrapPage()
        );
    }

    @Transactional
    public ProfileUpdateResult updateProfile(Account account, AccountUpdateDto accountUpdateDto) {
        Account existingAccount = accountService.findAccountByEmail(account.getEmail());
        if (existingAccount == null) {
            accountAuditPublisher.accountProfileUpdateFailedAccountNotFound(account.getId());
            return ProfileUpdateResult.failure("사용자 정보를 찾을 수 없습니다.");
        }

        String previousProfileImageUrl = normalizeNullableText(existingAccount.getProfileImageUrl());
        existingAccount.updateProfile(
                accountUpdateDto.getName(),
                accountUpdateDto.getPhoneNumber(),
                accountUpdateDto.getBirthDate(),
                accountUpdateDto.getGender(),
                accountUpdateDto.getAddress(),
                accountUpdateDto.getAddressDetail(),
                accountUpdateDto.getProfileImageUrl()
        );

        try {
            Account savedAccount = accountService.save(existingAccount);
            imageService.attachProfileImageIfPresent(savedAccount.getProfileImageUrl());
            String currentProfileImageUrl = normalizeNullableText(savedAccount.getProfileImageUrl());
            if (!Objects.equals(previousProfileImageUrl, currentProfileImageUrl)) {
                imageService.cleanupImageByUrlIfUnlinked(previousProfileImageUrl);
            }
            accountAuditPublisher.accountProfileUpdate(savedAccount.getId());
            return ProfileUpdateResult.success(savedAccount);
        } catch (AppException e) {
            return ProfileUpdateResult.failure(e.getMessage());
        }
    }

    private AccountProfileViewStrategy resolveProfileViewStrategy(AccountProfileViewType profileViewType) {
        AccountProfileViewStrategy strategy = profileViewStrategyMap.get(profileViewType);
        if (strategy == null) {
            throw new IllegalStateException("지원하지 않는 프로필 뷰 타입입니다. " + profileViewType.key());
        }
        return strategy;
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record OtherProfileViewData(Account profileAccount,
                                       String profileView,
                                       Long currentAccountId,
                                       boolean myBlockedProfileAccount,
                                       Page<Board> boardPage,
                                       Page<Comments> commentPage,
                                       Page<Board> scrapPage) {

        public Page<?> activePage() {
            AccountProfileViewType viewType = AccountProfileViewType.from(profileView);
            return switch (viewType) {
                case COMMENTS -> commentPage;
                case SCRAPS -> scrapPage;
                case BOARDS -> boardPage;
            };
        }
    }

    public record ProfileUpdateResult(boolean success,
                                      Account savedAccount,
                                      String errorMessage) {

        public static ProfileUpdateResult success(Account savedAccount) {
            return new ProfileUpdateResult(true, savedAccount, null);
        }

        public static ProfileUpdateResult failure(String errorMessage) {
            return new ProfileUpdateResult(false, null, errorMessage);
        }
    }
}


