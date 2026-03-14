package kwh.PublicCookedFood.board.policy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class BoardViewPolicy {

    private static final String SKIP_NEXT_DETAIL_VIEWS_SESSION_KEY =
            BoardViewPolicy.class.getName() + ".skipNextDetailViews";

    public void markSkipNextDetailView(HttpServletRequest request, Long boardId) {
        if (request == null || boardId == null || boardId <= 0) {
            return;
        }
        HttpSession session = request.getSession(true);
        Set<Long> boardIds = readSkipBoardIds(session);
        boardIds.add(boardId);
        writeSkipBoardIds(session, boardIds);
    }

    public boolean shouldIncreaseDetailView(HttpServletRequest request, Long boardId) {
        if (request == null || boardId == null || boardId <= 0) {
            return true;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return true;
        }

        Set<Long> boardIds = readSkipBoardIds(session);
        boolean skipNextView = boardIds.remove(boardId);
        if (skipNextView) {
            writeSkipBoardIds(session, boardIds);
            return false;
        }
        return true;
    }

    private Set<Long> readSkipBoardIds(HttpSession session) {
        if (session == null) {
            return new LinkedHashSet<>();
        }
        Object sessionValue = session.getAttribute(SKIP_NEXT_DETAIL_VIEWS_SESSION_KEY);
        if (!(sessionValue instanceof Set<?> rawSet)) {
            return new LinkedHashSet<>();
        }

        LinkedHashSet<Long> boardIds = new LinkedHashSet<>();
        for (Object value : rawSet) {
            if (value instanceof Long boardId && boardId > 0) {
                boardIds.add(boardId);
            }
        }
        return boardIds;
    }

    private void writeSkipBoardIds(HttpSession session, Set<Long> boardIds) {
        if (session == null) {
            return;
        }
        if (boardIds == null || boardIds.isEmpty()) {
            session.removeAttribute(SKIP_NEXT_DETAIL_VIEWS_SESSION_KEY);
            return;
        }
        session.setAttribute(SKIP_NEXT_DETAIL_VIEWS_SESSION_KEY, new LinkedHashSet<>(boardIds));
    }
}
