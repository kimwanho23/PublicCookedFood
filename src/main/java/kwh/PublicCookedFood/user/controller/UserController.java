package kwh.PublicCookedFood.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.LoginDto;
import kwh.PublicCookedFood.user.dto.UserSaveDto;
import kwh.PublicCookedFood.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/u")
public class UserController {

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    @GetMapping(value="/signup")
    public String saveForm(@ModelAttribute("userSaveDto") UserSaveDto userSaveDto, HttpServletRequest request){
        String uri = request.getHeader("Referer");
        if (uri != null && !uri.contains("/u/signup")) {
            request.getSession().setAttribute("prevPage", uri);
        }

        return "/user/signUpForm";
    }

    @PostMapping(value = "/signup")
    public String save(@Valid UserSaveDto userSaveDto, BindingResult bindingResult, Model model){
        if(bindingResult.hasErrors()){
            return "/user/signUpForm";
        }
        try {
            Users users = Users.createUser(userSaveDto, passwordEncoder);
            userService.save(users);
        } catch (IllegalStateException e){
            model.addAttribute("errorMessage", e.getMessage());
            return "/user/signUpForm";
        }
        return "redirect:/foods";
    }

    //로그인 페이지 출력
    @GetMapping("/login")
    public String login(@ModelAttribute("loginDto") LoginDto loginDto, HttpServletRequest request) {
        String uri = request.getHeader("Referer");

        if (uri != null && !uri.contains("/u/login")) {
            request.getSession().setAttribute("prevPage", uri);
        }
        return "user/loginForm";
    }


    @GetMapping(value = "/login/error")
    public String loginError(Model model){
        model.addAttribute("loginErrorMsg", "아이디 또는 비밀번호를 확인해주세요");
        return "/user/loginForm";
    }


}
