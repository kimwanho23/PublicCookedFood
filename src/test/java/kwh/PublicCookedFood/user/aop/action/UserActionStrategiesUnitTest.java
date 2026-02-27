package kwh.PublicCookedFood.user.aop.action;

import kwh.PublicCookedFood.user.aop.UserActionAuditType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserActionStrategiesUnitTest {

    @Mock
    private UserActionAuditRecorder recorder;

    private UserProfileAuditStrategy userProfileAuditStrategy;
    private UserBlockAuditStrategy userBlockAuditStrategy;
    private BookmarkAuditStrategy bookmarkAuditStrategy;
    private RecipeReviewAuditStrategy recipeReviewAuditStrategy;
    private BoardCommentAuditStrategy boardCommentAuditStrategy;
    private BoardLifecycleAuditStrategy boardLifecycleAuditStrategy;
    private BoardReportAuditStrategy boardReportAuditStrategy;
    private NotificationAuditStrategy notificationAuditStrategy;

    @BeforeEach
    void setUp() {
        userProfileAuditStrategy = new UserProfileAuditStrategy(recorder);
        userBlockAuditStrategy = new UserBlockAuditStrategy(recorder);
        bookmarkAuditStrategy = new BookmarkAuditStrategy(recorder);
        recipeReviewAuditStrategy = new RecipeReviewAuditStrategy(recorder);
        boardCommentAuditStrategy = new BoardCommentAuditStrategy(recorder);
        boardLifecycleAuditStrategy = new BoardLifecycleAuditStrategy(recorder);
        boardReportAuditStrategy = new BoardReportAuditStrategy(recorder);
        notificationAuditStrategy = new NotificationAuditStrategy(recorder);
    }

    @Test
    void userProfileStrategy_signup_recordsActivity() {
        userProfileAuditStrategy.handle(UserActionAuditType.USER_SIGNUP,
                new UserActionAuditArgs(new Object[]{1L, "tester@example.com"}));

        verify(recorder).record(1L, "USER_SIGNUP", "email=tester@example.com");
    }

    @Test
    void userProfileStrategy_profileUpdateFailure_doesNotRecordActivity() {
        userProfileAuditStrategy.handle(UserActionAuditType.USER_PROFILE_UPDATE_FAILED_UNAUTHENTICATED,
                new UserActionAuditArgs(new Object[]{}));

        verifyNoInteractions(recorder);
    }

    @Test
    void userBlockStrategy_blockSuccess_recordsActivity() {
        userBlockAuditStrategy.handle(UserActionAuditType.USER_BLOCK_ADD,
                new UserActionAuditArgs(new Object[]{3L, 9L}));

        verify(recorder).record(3L, "USER_BLOCK_ADD", "targetUserId=9");
    }

    @Test
    void userBlockStrategy_blockFailure_doesNotRecordActivity() {
        userBlockAuditStrategy.handle(UserActionAuditType.USER_BLOCK_FAILED,
                new UserActionAuditArgs(new Object[]{3L, 9L, "duplicate", new IllegalStateException("boom")}));

        verifyNoInteractions(recorder);
    }

    @Test
    void bookmarkStrategy_addSuccess_recordsActivity() {
        bookmarkAuditStrategy.handle(UserActionAuditType.BOOKMARK_ADD,
                new UserActionAuditArgs(new Object[]{5L, 13L}));

        verify(recorder).record(5L, "BOOKMARK_ADD", "recipeId=13");
    }

    @Test
    void bookmarkStrategy_addFailure_doesNotRecordActivity() {
        bookmarkAuditStrategy.handle(UserActionAuditType.BOOKMARK_ADD_FAILED,
                new UserActionAuditArgs(new Object[]{5L, 13L, "already-bookmarked"}));

        verifyNoInteractions(recorder);
    }

    @Test
    void recipeReviewStrategy_upsertSuccess_recordsActivity() {
        recipeReviewAuditStrategy.handle(UserActionAuditType.RECIPE_REVIEW_UPSERT,
                new UserActionAuditArgs(new Object[]{7L, 21L, 4}));

        verify(recorder).record(7L, "RECIPE_REVIEW_UPSERT", "recipeId=21,rating=4");
    }

    @Test
    void recipeReviewStrategy_upsertFailure_doesNotRecordActivity() {
        recipeReviewAuditStrategy.handle(UserActionAuditType.RECIPE_REVIEW_UPSERT_FAILED,
                new UserActionAuditArgs(new Object[]{7L, 21L, "validation-failed"}));

        verifyNoInteractions(recorder);
    }

    @Test
    void boardCommentStrategy_commentCreateSuccess_recordsActivityWithParentIdFallback() {
        boardCommentAuditStrategy.handle(UserActionAuditType.BOARD_COMMENT_CREATE,
                new UserActionAuditArgs(new Object[]{11L, 22L, 33L, null}));

        verify(recorder).record(11L, "BOARD_COMMENT_CREATE", "boardId=22,commentId=33,parentId=-");
    }

    @Test
    void boardLifecycleStrategy_createSuccess_recordsActivity() {
        boardLifecycleAuditStrategy.handle(UserActionAuditType.BOARD_CREATE,
                new UserActionAuditArgs(new Object[]{11L, 77L}));

        verify(recorder).record(11L, "BOARD_CREATE", "boardId=77");
    }

    @Test
    void boardReportStrategy_reportCreateFailure_doesNotRecordActivity() {
        boardReportAuditStrategy.handle(UserActionAuditType.BOARD_REPORT_CREATE_FAILED,
                new UserActionAuditArgs(new Object[]{11L, 22L, "validation-failed"}));

        verifyNoInteractions(recorder);
    }

    @Test
    void notificationStrategy_settingUpdate_recordsActivityWithSafeBooleanFallback() {
        notificationAuditStrategy.handle(UserActionAuditType.NOTIFICATION_SETTING_UPDATE,
                new UserActionAuditArgs(new Object[]{15L, null}));

        verify(recorder).record(15L, "NOTIFICATION_SETTING_UPDATE", "enabled=-");
    }
}
