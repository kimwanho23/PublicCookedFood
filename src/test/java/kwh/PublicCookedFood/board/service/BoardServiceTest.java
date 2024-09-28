package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.dto.BoardDto;
import kwh.PublicCookedFood.board.dto.CommentsDto;
import kwh.PublicCookedFood.food.service.RecipeService;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;

@SpringBootTest
class BoardServiceTest {
    private static final Logger log = LoggerFactory.getLogger(BoardServiceTest.class);

    @Autowired
    private UserService userService;


    @Autowired
    private BoardService boardService;

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private CommentsService commentsService;


    @Commit
    @Test
    public void write() {
        Users user = userService.findUserByEmail("test@gmail.com");

        BoardDto saveDto = BoardDto.builder()
                .title("Title")
                .contents("testContents")
                .userId(user)
                .build();
        boardService.save(saveDto);
    }

    @Commit
    @Test
    public void likeTest() {
        Users user = userService.findUserByEmail("test@gmail.com");

        likeService.saveLikes(5L, user.getId());
        boardService.updateLikes(5L);
    }

/*
    @Commit
    @Test
    public void commentTest() {
        Users user = userService.findUserByEmail("test@gmail.com");
        Board board = boardService.getBoard(5L);
        CommentsDto commentsDto = CommentsDto.builder()
                .userId(user)
                .postId(board)
                .contents("re-ReComment testContent")
                .parentIdx(3L)
                .build();

        commentsService.save(commentsDto);

    }

*/



}