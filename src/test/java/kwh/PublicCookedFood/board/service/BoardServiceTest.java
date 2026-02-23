package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.dto.BoardDto;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.UserSaveDto;
import kwh.PublicCookedFood.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BoardServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private BoardService boardService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void write() {
        Users user = createUser("board-write-" + System.nanoTime() + "@test.com");

        Board savedBoard = boardService.save(BoardDto.builder()
                .title("Title")
                .contents("testContents")
                .userId(user)
                .views(0L)
                .likesCount(0L)
                .commentsCount(0L)
                .state("1")
                .build());

        assertThat(savedBoard.getId()).isNotNull();
    }

    @Test
    void likeTest() {
        Users user = createUser("board-like-" + System.nanoTime() + "@test.com");
        Board board = boardService.save(BoardDto.builder()
                .title("Like Test")
                .contents("like-content")
                .userId(user)
                .views(0L)
                .likesCount(0L)
                .commentsCount(0L)
                .state("1")
                .build());

        Long likeId = likeService.saveLikes(board.getId(), user.getId());
        boardService.updateLikes(board.getId());

        assertThat(likeId).isNotNull();
        assertThat(likeService.getLike(board.getId())).isEqualTo(1L);
    }

    private Users createUser(String email) {
        UserSaveDto userDto = UserSaveDto.builder()
                .email(email)
                .name("테스터")
                .password("12345678")
                .build();
        return userService.save(Users.createUser(userDto, passwordEncoder));
    }
}
