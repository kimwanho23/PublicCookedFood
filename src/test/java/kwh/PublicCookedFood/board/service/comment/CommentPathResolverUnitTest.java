package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.board.domain.Comments;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommentPathResolverUnitTest {

    private final CommentPathResolver commentPathResolver = new CommentPathResolver();

    @Test
    void buildPath_returnsSingleSegmentForRootComment() {
        String path = commentPathResolver.buildPath(7L, ResolvedCommentParent.root());

        assertThat(path).isEqualTo("0000000000000000007");
    }

    @Test
    void buildPath_appendsSegmentToResolvedParentPathForReply() {
        Comments parent = Comments.builder()
                .id(5L)
                .contents("parent")
                .build();

        String path = commentPathResolver.buildPath(7L, ResolvedCommentParent.reply(parent));

        assertThat(path).isEqualTo("0000000000000000005/0000000000000000007");
    }
}
