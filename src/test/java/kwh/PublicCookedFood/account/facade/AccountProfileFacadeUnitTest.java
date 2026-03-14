package kwh.PublicCookedFood.account.facade;

import kwh.PublicCookedFood.account.application.query.view.AccountProfileCommentItemView;
import kwh.PublicCookedFood.account.audit.AccountAuditPublisher;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.dto.request.AccountUpdateDto;
import kwh.PublicCookedFood.account.error.AccountErrorCode;
import kwh.PublicCookedFood.account.facade.profile.AccountProfileViewPages;
import kwh.PublicCookedFood.account.facade.profile.AccountProfileViewStrategy;
import kwh.PublicCookedFood.account.facade.profile.AccountProfileViewType;
import kwh.PublicCookedFood.account.policy.AccountProfileAccessPolicy;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.account.service.AccountService;
import kwh.PublicCookedFood.board.application.query.BoardCardViewAssembler;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.service.CommentNavigationService;
import kwh.PublicCookedFood.board.service.comment.CommentTargetPath;
import kwh.PublicCookedFood.board.service.comment.CommentTargetPathsQuery;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummary;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummaryResolver;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.storage.ImageLifecycleService;
import kwh.PublicCookedFood.storage.ImageUrls;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountProfileFacadeUnitTest {

    @Mock
    private AccountService accountService;
    @Mock
    private AccountBlockService accountBlockService;
    @Mock
    private AccountAuditPublisher accountAuditPublisher;
    @Mock
    private AccountProfileAccessPolicy accountProfileAccessPolicy;
    @Mock
    private ImageLifecycleService imageLifecycleService;
    @Mock
    private BoardStatsSummaryResolver boardStatsSummaryResolver;
    @Mock
    private CommentNavigationService commentNavigationService;

    private AccountProfileFacade accountProfileFacade;

    @BeforeEach
    void setUp() {
        accountProfileFacade = new AccountProfileFacade(
                accountService,
                accountBlockService,
                accountAuditPublisher,
                accountProfileAccessPolicy,
                List.<AccountProfileViewStrategy>of(),
                imageLifecycleService,
                boardStatsSummaryResolver,
                new BoardCardViewAssembler(),
                commentNavigationService
        );
    }

    @Test
    void updateProfile_attachesNewImageAndCleansUpOldImageWhenChanged() {
        Account loginAccount = accountWithProfileImage(1L, "account@test.com", "/images/old-profile.jpg");
        Account existingAccount = accountWithProfileImage(1L, "account@test.com", "/images/old-profile.jpg");
        when(accountService.findAccountByEmail("account@test.com")).thenReturn(existingAccount);
        when(accountService.save(existingAccount)).thenReturn(existingAccount);

        AccountUpdateDto updateDto = new AccountUpdateDto();
        updateDto.setName("updated");
        updateDto.setPhoneNumber("010-1234-5678");
        updateDto.setBirthDate(LocalDate.of(1999, 1, 2));
        updateDto.setProfileImageUrl("/images/new-profile.jpg");

        AccountProfileFacade.ProfileUpdateResult result = accountProfileFacade.updateProfile(loginAccount, updateDto);

        assertThat(result.success()).isTrue();
        verify(imageLifecycleService).attachImagesIfPresent(ImageUrls.single("/images/new-profile.jpg"));
        verify(imageLifecycleService).cleanupImagesByUrlIfUnlinked(ImageUrls.single("/images/old-profile.jpg"));
        verify(accountAuditPublisher).accountProfileUpdate(1L);
    }

    @Test
    void updateProfile_doesNotCleanupImageWhenProfileImageUnchanged() {
        Account loginAccount = accountWithProfileImage(1L, "account@test.com", "/images/same.jpg");
        Account existingAccount = accountWithProfileImage(1L, "account@test.com", "/images/same.jpg");
        when(accountService.findAccountByEmail("account@test.com")).thenReturn(existingAccount);
        when(accountService.save(existingAccount)).thenReturn(existingAccount);

        AccountUpdateDto updateDto = new AccountUpdateDto();
        updateDto.setName("same");
        updateDto.setProfileImageUrl("/images/same.jpg");

        AccountProfileFacade.ProfileUpdateResult result = accountProfileFacade.updateProfile(loginAccount, updateDto);

        assertThat(result.success()).isTrue();
        verify(imageLifecycleService).attachImagesIfPresent(ImageUrls.single("/images/same.jpg"));
        verify(imageLifecycleService, never()).cleanupImagesByUrlIfUnlinked(ImageUrls.single("/images/same.jpg"));
    }

    @Test
    void updateProfile_whenSaveFails_returnsFailureWithoutSideEffects() {
        Account loginAccount = accountWithProfileImage(1L, "account@test.com", "/images/old.jpg");
        Account existingAccount = accountWithProfileImage(1L, "account@test.com", "/images/old.jpg");
        when(accountService.findAccountByEmail("account@test.com")).thenReturn(existingAccount);
        when(accountService.save(existingAccount)).thenThrow(new AppException(AccountErrorCode.ACCOUNT_NAME_DUPLICATED));

        AccountUpdateDto updateDto = new AccountUpdateDto();
        updateDto.setName("updated");
        updateDto.setProfileImageUrl("/images/new.jpg");

        AccountProfileFacade.ProfileUpdateResult result = accountProfileFacade.updateProfile(loginAccount, updateDto);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo(AccountErrorCode.ACCOUNT_NAME_DUPLICATED.message());
        verify(imageLifecycleService, never()).attachImagesIfPresent(ImageUrls.single("/images/new.jpg"));
        verify(imageLifecycleService, never()).cleanupImagesByUrlIfUnlinked(ImageUrls.single("/images/old.jpg"));
        verify(accountAuditPublisher, never()).accountProfileUpdate(1L);
    }

    @Test
    void loadOtherProfile_mapsBoardPagesToBoardCards() {
        Account loginAccount = accountWithProfileImage(1L, "login@test.com", null);
        Account profileAccount = accountWithProfileImage(2L, "profile@test.com", null);
        Board board = Board.builder()
                .id(10L)
                .title("board")
                .account(profileAccount)
                .build();
        AccountProfileViewStrategy boardsStrategy = new StubProfileViewStrategy(
                AccountProfileViewType.BOARDS,
                AccountProfileViewPages.boards(new PageImpl<>(List.of(board), PageRequest.of(0, 10), 1))
        );
        AccountProfileFacade facade = new AccountProfileFacade(
                accountService,
                accountBlockService,
                accountAuditPublisher,
                accountProfileAccessPolicy,
                List.of(
                        boardsStrategy,
                        new StubProfileViewStrategy(AccountProfileViewType.COMMENTS, AccountProfileViewPages.comments(Page.empty())),
                        new StubProfileViewStrategy(AccountProfileViewType.SCRAPS, AccountProfileViewPages.scraps(Page.empty()))
                ),
                imageLifecycleService,
                boardStatsSummaryResolver,
                new BoardCardViewAssembler(),
                commentNavigationService
        );
        facade.initializeProfileViewStrategies();

        when(accountService.findById(2L)).thenReturn(Optional.of(profileAccount));
        when(accountBlockService.getViewRestrictedAccountIds(1L)).thenReturn(Set.of());
        when(accountProfileAccessPolicy.isProfileViewRestricted(loginAccount, 2L)).thenReturn(false);
        when(accountProfileAccessPolicy.hasBlockedProfileAccount(loginAccount, 2L)).thenReturn(false);
        when(boardStatsSummaryResolver.resolve(Set.of(board))).thenReturn(java.util.Map.of(10L, new BoardStatsSummary(7L, 5L, 3L)));

        AccountProfileFacade.OtherProfileViewData result =
                facade.loadOtherProfile(2L, null, loginAccount, PageRequest.of(0, 10));

        assertThat(result.boardPage()).isNotNull();
        assertThat(result.boardPage().getContent()).hasSize(1);
        assertThat(result.boardPage().getContent().get(0).boardId()).isEqualTo(10L);
        assertThat(result.boardPage().getContent().get(0).views()).isEqualTo(7L);
        assertThat(result.boardPage().getContent().get(0).likes()).isEqualTo(5L);
        assertThat(result.boardPage().getContent().get(0).commentsCount()).isEqualTo(3L);
    }

    @Test
    void loadOtherProfile_mapsCommentsToReadModelWithTargetPath() {
        Account loginAccount = accountWithProfileImage(1L, "login@test.com", null);
        Account profileAccount = accountWithProfileImage(2L, "profile@test.com", null);
        Board board = Board.builder()
                .id(10L)
                .title("board title")
                .account(profileAccount)
                .build();
        Comments comment = Comments.builder()
                .id(55L)
                .board(board)
                .account(profileAccount)
                .contents("comment body")
                .build();
        AccountProfileViewStrategy commentStrategy = new StubProfileViewStrategy(
                AccountProfileViewType.COMMENTS,
                AccountProfileViewPages.comments(new PageImpl<>(List.of(comment), PageRequest.of(0, 10), 1))
        );
        AccountProfileFacade facade = new AccountProfileFacade(
                accountService,
                accountBlockService,
                accountAuditPublisher,
                accountProfileAccessPolicy,
                List.of(
                        new StubProfileViewStrategy(AccountProfileViewType.BOARDS, AccountProfileViewPages.boards(Page.empty())),
                        commentStrategy,
                        new StubProfileViewStrategy(AccountProfileViewType.SCRAPS, AccountProfileViewPages.scraps(Page.empty()))
                ),
                imageLifecycleService,
                boardStatsSummaryResolver,
                new BoardCardViewAssembler(),
                commentNavigationService
        );
        facade.initializeProfileViewStrategies();

        when(accountService.findById(2L)).thenReturn(Optional.of(profileAccount));
        when(accountBlockService.getViewRestrictedAccountIds(1L)).thenReturn(Set.of());
        when(accountProfileAccessPolicy.isProfileViewRestricted(loginAccount, 2L)).thenReturn(false);
        when(accountProfileAccessPolicy.hasBlockedProfileAccount(loginAccount, 2L)).thenReturn(false);
        when(commentNavigationService.buildCommentTargetPaths(argThat(query ->
                query.boardId() == 10L && query.commentIds().equals(Set.of(55L)))))
                .thenReturn(Map.of(55L, CommentTargetPath.of("/boards/10?commentPage=0&commentSize=50#comment-55")));

        AccountProfileFacade.OtherProfileViewData result =
                facade.loadOtherProfile(2L, "comments", loginAccount, PageRequest.of(0, 10));

        assertThat(result.commentPage()).isNotNull();
        assertThat(result.commentPage().getContent()).hasSize(1);
        AccountProfileCommentItemView item = result.commentPage().getContent().get(0);
        assertThat(item.commentId()).isEqualTo(55L);
        assertThat(item.boardId()).isEqualTo(10L);
        assertThat(item.boardTitle()).isEqualTo("board title");
        assertThat(item.targetPath()).isEqualTo("/boards/10?commentPage=0&commentSize=50#comment-55");
    }

    private Account accountWithProfileImage(Long id, String email, String profileImageUrl) {
        return Account.builder()
                .id(id)
                .email(email)
                .name("tester")
                .authority(Role.USER)
                .loginMethod("Current")
                .profileImageUrl(profileImageUrl)
                .build();
    }

    private record StubProfileViewStrategy(AccountProfileViewType viewType,
                                           AccountProfileViewPages pages) implements AccountProfileViewStrategy {

        @Override
        public AccountProfileViewPages load(Long profileAccountId,
                                            org.springframework.data.domain.Pageable pageable,
                                            Set<Long> blockedAccountIds) {
            return pages;
        }
    }
}
