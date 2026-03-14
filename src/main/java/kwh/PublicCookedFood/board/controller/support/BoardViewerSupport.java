package kwh.PublicCookedFood.board.controller.support;

import kwh.PublicCookedFood.board.facade.BoardViewer;
import org.springframework.stereotype.Component;

@Component
public class BoardViewerSupport {

    private static final String LOGIN_REDIRECT = "redirect:/u/login";

    public boolean requiresLogin(BoardViewer viewer) {
        return viewer == null || !viewer.isAuthenticated();
    }

    public String loginRedirect() {
        return LOGIN_REDIRECT;
    }
}
