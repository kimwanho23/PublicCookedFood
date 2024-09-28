package kwh.PublicCookedFood.user.controller;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
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

import java.util.Objects;


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

    @PostMapping("/profile")
    public String update(@Valid UserUpdateDto userUpdateDto, BindingResult bindingResult, Model model){
        if(bindingResult.hasErrors()){
            return "/user/profile";
        }
        try{
            Users currentUser = (Users) httpSession.getAttribute("user");
            log.info(currentUser.toString());
            String email = currentUser.getEmail();
            if(email != null){
                Users existingUser = userService.findUserByEmail(email);

                Users updatedUser = Users.builder()
                        .id(existingUser.getId())
                        .email(existingUser.getEmail()) // 이메일은 변경하지 않음
                        .name(!Objects.equals(userUpdateDto.getName(), "") ? userUpdateDto.getName() : existingUser.getName())
                        .password(existingUser.getPassword())
                        .loginMethod(existingUser.getLoginMethod())
                        .authority(existingUser.getAuthority())
                        .build();

                userService.save(updatedUser);
                httpSession.setAttribute("user", updatedUser);
                return "/user/profile";
            }
            else {
                log.info("email null");
            }

        } catch (IllegalStateException e){
            model.addAttribute("errorMessage", e.getMessage());
            return "/user/profile";
        }
        return "/user/profile";
    }

    //로그인 페이지 출력
    @GetMapping("/login")
    public String login( @ModelAttribute("loginDto") LoginDto loginDto,HttpServletRequest request) {
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
