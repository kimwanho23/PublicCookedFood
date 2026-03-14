package kwh.PublicCookedFood.board.service.command;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.audit.BoardAuditPublisher;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.service.BoardImageService;
import kwh.PublicCookedFood.board.service.image.BoardImageSyncCommand;
import kwh.PublicCookedFood.board.service.support.BoardContentSanitizer;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.board.service.BoardStatsMutationService;
import kwh.PublicCookedFood.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardCommandServiceUnitTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardImageService boardImageService;

    @Mock
    private BoardSectionCommandService boardSectionCommandService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BoardStatsMutationService boardStatsMutationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private BoardAuditPublisher boardAuditPublisher;

    private BoardCommandService boardCommandService;

    @BeforeEach
    void setUp() {
        boardCommandService = new BoardCommandService(
                boardRepository,
                boardImageService,
                boardSectionCommandService,
                accountRepository,
                new BoardContentSanitizer(),
                boardStatsMutationService,
                notificationService,
                boardAuditPublisher
        );
    }

    @Test
    void create_sanitizesAndPersistsBoard() {
        Account account = Account.builder()
                .id(11L)
                .email("board-write@test.com")
                .name("author")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
        BoardSection section = BoardSection.builder()
                .id(2L)
                .sectionKey("general")
                .sectionName("일반")
                .displayOrder(0)
                .active(true)
                .build();
        BoardSaveCommand request = new BoardSaveCommand(
                "<b>Title</b>",
                "<p><span style=\"font-size: 18px; font-family: 'Noto Serif KR', serif; color: red;\">ok</span></p>"
                        + "<script>alert(1)</script><iframe src=\"https://www.youtube.com/embed/example\"></iframe>",
                2L
        );

        when(accountRepository.findById(11L)).thenReturn(Optional.of(account));
        when(boardSectionCommandService.resolveSectionForWrite(2L)).thenReturn(section);
        when(boardRepository.save(any(Board.class))).thenAnswer(invocation -> {
            Board candidate = invocation.getArgument(0);
            return Board.builder()
                    .id(100L)
                    .title(candidate.getTitle())
                    .contents(candidate.getContents())
                    .account(candidate.getAccount())
                    .section(candidate.getSection())
                    .state(candidate.getState())
                    .hiddenByReport(candidate.isHiddenByReport())
                    .build();
        });

        Board savedBoard = boardCommandService.create(BoardCreateCommand.of(11L, request));

        ArgumentCaptor<Board> boardCaptor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepository).save(boardCaptor.capture());
        Board persisted = boardCaptor.getValue();

        assertThat(savedBoard.getId()).isEqualTo(100L);
        assertThat(persisted.getTitle()).isEqualTo("Title");
        assertThat(persisted.getContents()).contains(">ok</span></p>");
        assertThat(persisted.getContents()).doesNotContain("style=");
        assertThat(persisted.getContents()).doesNotContain("<script>");
        assertThat(persisted.getContents()).contains("<iframe");
        assertThat(persisted.getContents()).contains("youtube.com/embed/example");
        assertThat(persisted.getAccount()).isSameAs(account);
        assertThat(persisted.getSection()).isSameAs(section);
        verify(boardStatsMutationService).initializeBoard(savedBoard);
        verify(boardImageService).syncBoardImages(BoardImageSyncCommand.of(savedBoard, persisted.getContents()));
        verify(notificationService).notifyOnBoardCreated(savedBoard);
        verify(boardAuditPublisher).boardCreate(11L, 100L);
    }

    @Test
    void update_sanitizesAndMutatesExistingBoard() {
        BoardSection currentSection = BoardSection.builder()
                .id(2L)
                .sectionKey("general")
                .sectionName("일반")
                .displayOrder(0)
                .active(true)
                .build();
        BoardSection targetSection = BoardSection.builder()
                .id(3L)
                .sectionKey("tips")
                .sectionName("팁")
                .displayOrder(1)
                .active(true)
                .build();
        Board existingBoard = Board.builder()
                .id(100L)
                .title("before")
                .contents("<p>before</p>")
                .section(currentSection)
                .version(3L)
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(false)
                .build();
        BoardUpdateCommand command = new BoardUpdateCommand(
                100L,
                11L,
                3L,
                "<b>Updated</b>",
                "<p>safe</p><script>alert(1)</script>",
                3L
        );

        when(boardSectionCommandService.resolveSectionForWrite(3L)).thenReturn(targetSection);
        when(boardRepository.findByIdWithAccountAndState(100L, SoftDeleteState.ACTIVE))
                .thenReturn(Optional.of(existingBoard));

        boardCommandService.update(command);

        assertThat(existingBoard.getTitle()).isEqualTo("Updated");
        assertThat(existingBoard.getContents()).contains("<p>safe</p>");
        assertThat(existingBoard.getContents()).doesNotContain("<script>");
        assertThat(existingBoard.getSection()).isSameAs(targetSection);
        verify(boardRepository, never()).save(any(Board.class));
        verify(boardImageService).syncBoardImages(BoardImageSyncCommand.of(existingBoard, existingBoard.getContents()));
        verify(boardAuditPublisher).boardUpdate(11L, 100L);
    }

    @Test
    void update_throwsConflictWhenSubmittedVersionIsStale() {
        BoardSection currentSection = BoardSection.builder()
                .id(2L)
                .sectionKey("general")
                .sectionName("일반")
                .displayOrder(0)
                .active(true)
                .build();
        Board existingBoard = Board.builder()
                .id(100L)
                .title("before")
                .contents("<p>before</p>")
                .section(currentSection)
                .version(5L)
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(false)
                .build();
        BoardUpdateCommand command = new BoardUpdateCommand(
                100L,
                11L,
                4L,
                "Updated",
                "<p>safe</p>",
                2L
        );

        when(boardSectionCommandService.resolveSectionForWrite(2L)).thenReturn(currentSection);
        when(boardRepository.findByIdWithAccountAndState(100L, SoftDeleteState.ACTIVE))
                .thenReturn(Optional.of(existingBoard));

        assertThatThrownBy(() -> boardCommandService.update(command))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.REQUEST_CONFLICT);

        verify(boardImageService, never()).syncBoardImages(any(BoardImageSyncCommand.class));
        verify(boardAuditPublisher, never()).boardUpdate(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void update_translatesOptimisticLockingFailureToConflict() {
        BoardSection currentSection = BoardSection.builder()
                .id(2L)
                .sectionKey("general")
                .sectionName("일반")
                .displayOrder(0)
                .active(true)
                .build();
        Board existingBoard = Board.builder()
                .id(100L)
                .title("before")
                .contents("<p>before</p>")
                .section(currentSection)
                .version(5L)
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(false)
                .build();
        BoardUpdateCommand command = new BoardUpdateCommand(
                100L,
                11L,
                5L,
                "Updated",
                "<p>safe</p>",
                2L
        );

        when(boardSectionCommandService.resolveSectionForWrite(2L)).thenReturn(currentSection);
        when(boardRepository.findByIdWithAccountAndState(100L, SoftDeleteState.ACTIVE))
                .thenReturn(Optional.of(existingBoard));
        org.mockito.Mockito.doThrow(new OptimisticLockingFailureException("stale state"))
                .when(boardRepository)
                .flush();

        assertThatThrownBy(() -> boardCommandService.update(command))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.REQUEST_CONFLICT);

        verify(boardImageService, never()).syncBoardImages(any(BoardImageSyncCommand.class));
        verify(boardAuditPublisher, never()).boardUpdate(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void delete_throwsAppExceptionWhenBoardDoesNotExist() {
        when(boardRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardCommandService.delete(404L, 11L))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));

        verify(boardImageService, never()).deleteBoardImages(any(Board.class));
        verify(boardAuditPublisher, never()).boardDelete(any(Long.class), any(Long.class));
    }
}
