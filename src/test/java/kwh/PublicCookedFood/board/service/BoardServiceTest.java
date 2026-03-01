package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.dto.request.BoardSaveRequest;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.user.domain.Role;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private CommentsRepository commentsRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private BoardSectionService boardSectionService;

    @Mock
    private BoardPolicyService boardPolicyService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BoardService boardService;

    @Test
    void write() {
        Users user = Users.builder()
                .id(11L)
                .email("board-write@test.com")
                .name("테스터")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
        BoardSection section = BoardSection.builder()
                .id(2L)
                .sectionKey("general")
                .sectionName("자유")
                .displayOrder(0)
                .active(true)
                .build();
        BoardSaveRequest request = BoardSaveRequest.builder()
                .title("<b>Title</b>")
                .contents("<p>ok</p><script>alert(1)</script><iframe src=\"https://www.youtube.com/embed/example\"></iframe>")
                .userId(11L)
                .sectionId(2L)
                .views(0L)
                .likesCount(0L)
                .commentsCount(0L)
                .state(SoftDeleteState.ACTIVE)
                .build();

        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(boardSectionService.resolveSectionForWrite(2L)).thenReturn(section);
        when(boardRepository.save(any(Board.class))).thenAnswer(invocation -> {
            Board candidate = invocation.getArgument(0);
            return Board.builder()
                    .id(100L)
                    .title(candidate.getTitle())
                    .contents(candidate.getContents())
                    .user(candidate.getUser())
                    .section(candidate.getSection())
                    .views(candidate.getViews())
                    .likeCount(candidate.getLikeCount())
                    .commentCount(candidate.getCommentCount())
                    .state(candidate.getState())
                    .hiddenByReport(candidate.isHiddenByReport())
                    .build();
        });

        Board savedBoard = boardService.save(request);

        ArgumentCaptor<Board> boardCaptor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepository).save(boardCaptor.capture());
        Board persisted = boardCaptor.getValue();

        assertThat(savedBoard.getId()).isEqualTo(100L);
        assertThat(persisted.getTitle()).isEqualTo("Title");
        assertThat(persisted.getContents()).contains("<p>ok</p>");
        assertThat(persisted.getContents()).doesNotContain("<script>");
        assertThat(persisted.getContents()).contains("<iframe");
        assertThat(persisted.getContents()).contains("youtube.com/embed/example");
        assertThat(persisted.getUser()).isSameAs(user);
        assertThat(persisted.getSection()).isSameAs(section);
        verify(imageService).syncBoardImages(savedBoard, persisted.getContents());
    }

    @Test
    void likeTest() {
        boardService.updateLikes(25L);

        verify(boardRepository).updateLikes(25L);
    }
}
