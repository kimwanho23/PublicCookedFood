package kwh.PublicCookedFood.board.application.query.view;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public final class BoardScrapListView {

    private final List<BoardCardView> boards;

    public BoardScrapListView(List<BoardCardView> boards) {
        if (boards == null || boards.isEmpty()) {
            this.boards = Collections.emptyList();
            return;
        }
        this.boards = Collections.unmodifiableList(new ArrayList<BoardCardView>(boards));
    }

    public List<BoardCardView> boards() {
        return boards;
    }
}
