package kwh.PublicCookedFood.user.controller;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.LoginDto;
import kwh.PublicCookedFood.user.dto.UserSaveDto;
import kwh.PublicCookedFood.user.dto.UserUpdateDto;

import kwh.PublicCookedFood.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/u")
@Slf4j
public class UserController {
    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    private final HttpSession httpSession;

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

    @GetMapping(value="/profile")
    public String profileForm(@ModelAttribute("userUpdateDto") UserUpdateDto userUpdateDto){
        return "/user/profile";
    }

    @PutMapping("/profile")
    public String update(@LoginUser Users user, @Valid UserUpdateDto userUpdateDto, BindingResult bindingResult, Model model){
        if (user == null) {
            return "redirect:/u/login";
        }

        if(bindingResult.hasErrors()){
            return "/user/profile";
        }

        try{
            Users existingUser = userService.findUserByEmail(user.getEmail());
            if (existingUser == null) {
                model.addAttribute("errorMessage", "사용자 정보를 찾을 수 없습니다.");
                return "/user/profile";
            }

            if (userUpdateDto.getName() != null && !userUpdateDto.getName().isBlank()) {
                existingUser.update(userUpdateDto.getName());
            }

            Users savedUser = userService.save(existingUser);
            httpSession.setAttribute("user", savedUser);
            return "redirect:/u/profile";
        } catch (IllegalStateException e){
            model.addAttribute("errorMessage", e.getMessage());
            return "/user/profile";
        }
    }

    //로그인 페이지 출력
    @GetMapping("/login")
    public String login(@ModelAttribute("loginDto") LoginDto loginDto,
                        @RequestParam(required = false) Boolean error,
                        @RequestParam(required = false) Boolean oauthError,
                        HttpServletRequest request,
                        Model model) {
        String uri = request.getHeader("Referer");

        if (uri != null && !uri.contains("/u/login")) {
            request.getSession().setAttribute("prevPage", uri);
        }

        if (Boolean.TRUE.equals(error)) {
            model.addAttribute("loginErrorMsg", "아이디 또는 비밀번호를 확인해주세요.");
        }
        if (Boolean.TRUE.equals(oauthError)) {
            model.addAttribute("loginErrorMsg", "소셜 로그인에 실패했습니다. OAuth 클라이언트 설정(redirect URI, client id/secret)을 확인해주세요.");
        }
        return "user/loginForm";
    }


    @GetMapping(value = "/login/error")
    public String loginError(Model model){
        model.addAttribute("loginErrorMsg", "아이디 또는 비밀번호를 확인해주세요");
        return "/user/loginForm";
    }

}
