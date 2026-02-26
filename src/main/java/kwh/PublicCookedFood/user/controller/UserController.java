package kwh.PublicCookedFood.user.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.service.BoardScrapService;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.board.service.CommentsService;
import kwh.PublicCookedFood.common.Paging;
import kwh.PublicCookedFood.config.AuthThrottleService;
import kwh.PublicCookedFood.config.oauth2.LoginUser;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.dto.CustomUserDetails;
import kwh.PublicCookedFood.user.dto.request.LoginDto;
import kwh.PublicCookedFood.user.dto.request.UserSaveDto;
import kwh.PublicCookedFood.user.dto.request.UserUpdateDto;

import kwh.PublicCookedFood.user.service.AccountRecoveryVerificationService;
import kwh.PublicCookedFood.user.service.UserBlockService;
import kwh.PublicCookedFood.user.service.UserActivityLogService;
import kwh.PublicCookedFood.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
@RequestMapping("/u")
@Slf4j
@Hidden
public class UserController {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final UserService userService;

    private final UserBlockService userBlockService;

    private final UserActivityLogService userActivityLogService;

    private final PasswordEncoder passwordEncoder;

    private final BoardService boardService;

    private final CommentsService commentsService;

    private final BoardScrapService boardScrapService;

    private final AuthThrottleService authThrottleService;

    private final AccountRecoveryVerificationService accountRecoveryVerificationService;

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
            Users savedUser = userService.save(users);
            userActivityLogService.record(savedUser.getId(), "USER_SIGNUP", "email=" + savedUser.getEmail());
        } catch (IllegalStateException e){
            model.addAttribute("errorMessage", e.getMessage());
            return "/user/signUpForm";
        }
        return "redirect:/recipes";
    }

    @GetMapping(value="/profile")
    public String profileForm(@LoginUser Users user,
                              @ModelAttribute("userUpdateDto") UserUpdateDto userUpdateDto,
                              Model model){
        if (user == null) {
            return "redirect:/u/login";
        }
        populateUserUpdateDto(userUpdateDto, user);
        model.addAttribute("user", user);
        return "/user/profile";
    }

    @GetMapping("/settings")
    public String settingsForm(@LoginUser Users user, Model model) {
        if (user == null) {
            return "redirect:/u/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("blockedUsers", userBlockService.getBlockedUsers(user.getId()));
        return "/user/settings";
    }

    @GetMapping({"/{userId:[0-9]+}", "/{userId:[0-9]+}/{view:comments|scraps}"})
    public String otherProfile(@PathVariable Long userId,
                               @PathVariable(name = "view", required = false) String view,
                               @LoginUser Users loginUser,
                               @PageableDefault(page = 0, size = 10, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable,
                               Model model) {
        Users profileUser = userService.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자 정보를 찾을 수 없습니다."));

        if (loginUser != null
                && loginUser.getId() != null
                && !loginUser.getId().equals(userId)
                && userBlockService.isEitherBlocked(loginUser.getId(), userId)) {
            throw new IllegalStateException("차단 관계인 사용자의 프로필은 조회할 수 없습니다.");
        }

        Set<Long> blockedUserIds = userBlockService.getViewRestrictedUserIds(loginUser == null ? null : loginUser.getId());
        String profileView = normalizeProfileView(view);
        model.addAttribute("profileView", profileView);

        switch (profileView) {
            case "comments" -> {
                Page<Comments> commentPage = commentsService.getUserCommentPage(userId, pageable, blockedUserIds);
                Paging.addPagingAttributes(model, commentPage, pageable);
                model.addAttribute("commentPage", commentPage);
            }
            case "scraps" -> {
                Page<Board> scrapPage = boardScrapService.getScrappedBoardsPage(userId, pageable, blockedUserIds);
                Paging.addPagingAttributes(model, scrapPage, pageable);
                model.addAttribute("scrapPage", scrapPage);
            }
            default -> {
                Page<Board> boardPage = boardService.getBoardList(pageable, null, userId, blockedUserIds);
                Paging.addPagingAttributes(model, boardPage, pageable);
                model.addAttribute("boardPage", boardPage);
            }
        }
        boolean myBlockedProfileUser = loginUser != null
                && loginUser.getId() != null
                && !loginUser.getId().equals(userId)
                && userBlockService.isBlocked(loginUser.getId(), userId);

        model.addAttribute("profileUser", profileUser);
        model.addAttribute("currentUserId", loginUser == null ? null : loginUser.getId());
        model.addAttribute("myBlockedProfileUser", myBlockedProfileUser);
        return "/user/otherProfile";
    }

    @PutMapping("/profile")
    public String update(@LoginUser Users user, @Valid UserUpdateDto userUpdateDto, BindingResult bindingResult, Model model){
        if (user == null) {
            log.warn("action=user.profile_update result=failed reason=unauthenticated");
            return "redirect:/u/login";
        }
        model.addAttribute("user", user);

        if(bindingResult.hasErrors()){
            log.warn("action=user.profile_update result=failed reason=validation_error userId={}", user.getId());
            return "/user/profile";
        }

        try{
            Users existingUser = userService.findUserByEmail(user.getEmail());
            if (existingUser == null) {
                log.warn("action=user.profile_update result=failed reason=user_not_found userId={}", user.getId());
                model.addAttribute("errorMessage", "사용자 정보를 찾을 수 없습니다.");
                return "/user/profile";
            }

            existingUser.updateProfile(
                    userUpdateDto.getName(),
                    userUpdateDto.getPhoneNumber(),
                    userUpdateDto.getBirthDate(),
                    userUpdateDto.getGender(),
                    userUpdateDto.getAddress(),
                    userUpdateDto.getAddressDetail(),
                    userUpdateDto.getProfileImageUrl()
            );

            Users savedUser = userService.save(existingUser);
            model.addAttribute("user", savedUser);
            refreshAuthenticationPrincipal(savedUser);
            log.info("action=user.profile_update result=success userId={}", savedUser.getId());
            userActivityLogService.record(savedUser.getId(), "USER_PROFILE_UPDATE", null);
            return "redirect:/u/profile";
        } catch (IllegalStateException e){
            log.warn("action=user.profile_update result=failed userId={} reason={}", user.getId(), e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "/user/profile";
        }
    }

    //로그인 페이지 출력
    @GetMapping("/login")
    public String login(@ModelAttribute("loginDto") LoginDto loginDto,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) Long retryAfter,
                        @RequestParam(required = false) Boolean oauthError,
                        HttpServletRequest request,
                        Model model) {
        String uri = request.getHeader("Referer");

        if (uri != null && !uri.contains("/u/login")) {
            request.getSession().setAttribute("prevPage", uri);
        }

        if (error != null) {
            if ("locked".equalsIgnoreCase(error)) {
                long seconds = retryAfter == null ? 0L : retryAfter;
                model.addAttribute("loginErrorMsg",
                        seconds > 0
                                ? "로그인 시도가 너무 많습니다. 약 " + seconds + "초 후 다시 시도해주세요."
                                : "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
            } else {
                model.addAttribute("loginErrorMsg", "아이디 또는 비밀번호를 확인해주세요.");
            }
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

    @GetMapping("/account/recover")
    public String accountRecoverForm(@RequestParam(required = false) String tab, Model model) {
        model.addAttribute("activeTab", "reset-password".equals(tab) ? "reset-password" : "find-email");
        populateRecoveryDefaults(model);
        return "/user/accountRecoverForm";
    }

    @PostMapping("/account/find-email")
    public String findAccountEmail(@RequestParam String name,
                                   @RequestParam String phoneNumber,
                                   HttpServletRequest request,
                                   Model model) {
        model.addAttribute("activeTab", "find-email");
        model.addAttribute("findName", name);
        model.addAttribute("findPhoneNumber", phoneNumber);
        populateRecoveryDefaults(model);

        if (isBlank(name) || isBlank(phoneNumber)) {
            model.addAttribute("recoverError", "이름과 전화번호를 모두 입력해주세요.");
            return "/user/accountRecoverForm";
        }

        String throttleIdentity = (name == null ? "" : name.trim()) + "|" + (phoneNumber == null ? "" : phoneNumber.trim());
        if (!authThrottleService.tryConsumeRecoveryAttempt(request, "find-email", throttleIdentity)) {
            long retryAfter = authThrottleService.getRecoveryRetryAfterSeconds(request, "find-email", throttleIdentity);
            model.addAttribute("recoverError", buildThrottleMessage(retryAfter));
            return "/user/accountRecoverForm";
        }

        return userService.findByNameAndPhoneNumber(name, phoneNumber)
                .map(user -> {
                    model.addAttribute("foundEmail", maskEmail(user.getEmail()));
                    model.addAttribute("recoverSuccess", "가입된 이메일을 확인했습니다.");
                    log.info("action=user.account_recover_email result=success userId={}", user.getId());
                    userActivityLogService.record(user.getId(), "USER_ACCOUNT_FIND_EMAIL", null);
                    return "/user/accountRecoverForm";
                })
                .orElseGet(() -> {
                    log.info("action=user.account_recover_email result=not_found");
                    model.addAttribute("recoverError", "일치하는 가입 정보가 없습니다.");
                    return "/user/accountRecoverForm";
                });
    }

    @PostMapping("/account/reset-password")
    public String resetAccountPassword(@RequestParam String email,
                                       @RequestParam String name,
                                       @RequestParam String phoneNumber,
                                       @RequestParam String verificationCode,
                                       @RequestParam String newPassword,
                                       @RequestParam String confirmPassword,
                                       HttpServletRequest request,
                                       Model model) {
        model.addAttribute("activeTab", "reset-password");
        model.addAttribute("resetEmail", email);
        model.addAttribute("resetName", name);
        model.addAttribute("resetPhoneNumber", phoneNumber);
        model.addAttribute("resetVerificationCode", verificationCode);
        populateRecoveryDefaults(model);

        if (isBlank(email) || isBlank(name) || isBlank(phoneNumber)
                || isBlank(verificationCode)
                || isBlank(newPassword) || isBlank(confirmPassword)) {
            model.addAttribute("recoverError", "모든 항목을 입력해주세요.");
            return "/user/accountRecoverForm";
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            model.addAttribute("recoverError", "이메일 형식이 올바르지 않습니다.");
            return "/user/accountRecoverForm";
        }
        if (newPassword.length() < 8 || newPassword.length() > 50) {
            model.addAttribute("recoverError", "비밀번호는 8자 이상 50자 이하여야 합니다.");
            return "/user/accountRecoverForm";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("recoverError", "새 비밀번호 확인이 일치하지 않습니다.");
            return "/user/accountRecoverForm";
        }
        if (!verificationCode.trim().matches("^\\d{6}$")) {
            model.addAttribute("recoverError", "인증 코드는 6자리 숫자여야 합니다.");
            return "/user/accountRecoverForm";
        }

        if (!authThrottleService.tryConsumeRecoveryAttempt(request, "reset-password-verify", email)) {
            long retryAfter = authThrottleService.getRecoveryRetryAfterSeconds(request, "reset-password-verify", email);
            model.addAttribute("recoverError", buildThrottleMessage(retryAfter));
            return "/user/accountRecoverForm";
        }

        try {
            accountRecoveryVerificationService.verifyPasswordResetCode(
                    request,
                    email,
                    name,
                    phoneNumber,
                    verificationCode
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("recoverError", e.getMessage());
            return "/user/accountRecoverForm";
        }

        if (!authThrottleService.tryConsumeRecoveryAttempt(request, "reset-password", email)) {
            long retryAfter = authThrottleService.getRecoveryRetryAfterSeconds(request, "reset-password", email);
            model.addAttribute("recoverError", buildThrottleMessage(retryAfter));
            return "/user/accountRecoverForm";
        }

        return userService.findByEmailAndNameAndPhoneNumber(email, name, phoneNumber)
                .map(user -> {
                    user.updatePassword(passwordEncoder.encode(newPassword));
                    userService.save(user);
                    authThrottleService.clearLoginFailures(request, email);
                    model.addAttribute("recoverSuccess", "비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해주세요.");
                    log.info("action=user.account_reset_password result=success userId={}", user.getId());
                    userActivityLogService.record(user.getId(), "USER_ACCOUNT_RESET_PASSWORD", null);
                    return "/user/accountRecoverForm";
                })
                .orElseGet(() -> {
                    log.info("action=user.account_reset_password result=not_found");
                    model.addAttribute("recoverError", "일치하는 가입 정보가 없습니다.");
                    return "/user/accountRecoverForm";
                });
    }

    @PostMapping("/account/reset-password/code")
    public String requestPasswordResetCode(@RequestParam String email,
                                           @RequestParam String name,
                                           @RequestParam String phoneNumber,
                                           HttpServletRequest request,
                                           Model model) {
        model.addAttribute("activeTab", "reset-password");
        model.addAttribute("resetEmail", email);
        model.addAttribute("resetName", name);
        model.addAttribute("resetPhoneNumber", phoneNumber);
        populateRecoveryDefaults(model);

        if (isBlank(email) || isBlank(name) || isBlank(phoneNumber)) {
            model.addAttribute("recoverError", "이메일, 이름, 전화번호를 모두 입력해주세요.");
            return "/user/accountRecoverForm";
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            model.addAttribute("recoverError", "이메일 형식이 올바르지 않습니다.");
            return "/user/accountRecoverForm";
        }
        if (!authThrottleService.tryConsumeRecoveryAttempt(request, "reset-password-code", email)) {
            long retryAfter = authThrottleService.getRecoveryRetryAfterSeconds(request, "reset-password-code", email);
            model.addAttribute("recoverError", buildThrottleMessage(retryAfter));
            return "/user/accountRecoverForm";
        }

        boolean issued;
        try {
            issued = accountRecoveryVerificationService.issuePasswordResetCode(request, email, name, phoneNumber);
        } catch (IllegalStateException e) {
            issued = false;
            log.warn("action=user.account_reset_password_code result=mail_unavailable email={}", email, e);
        }
        if (issued) {
            log.info("action=user.account_reset_password_code result=issued email={}", email);
        } else {
            log.info("action=user.account_reset_password_code result=not_found_or_skipped email={}", email);
        }
        model.addAttribute("recoverSuccess", "입력하신 이메일로 인증 코드를 전송했습니다. 코드를 확인 후 비밀번호를 재설정해주세요.");
        return "/user/accountRecoverForm";
    }

    private void populateRecoveryDefaults(Model model) {
        if (!model.containsAttribute("findName")) {
            model.addAttribute("findName", "");
        }
        if (!model.containsAttribute("findPhoneNumber")) {
            model.addAttribute("findPhoneNumber", "");
        }
        if (!model.containsAttribute("resetEmail")) {
            model.addAttribute("resetEmail", "");
        }
        if (!model.containsAttribute("resetName")) {
            model.addAttribute("resetName", "");
        }
        if (!model.containsAttribute("resetPhoneNumber")) {
            model.addAttribute("resetPhoneNumber", "");
        }
        if (!model.containsAttribute("resetVerificationCode")) {
            model.addAttribute("resetVerificationCode", "");
        }
    }

    private String buildThrottleMessage(long retryAfterSeconds) {
        if (retryAfterSeconds <= 0) {
            return "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.";
        }
        return "요청이 너무 많습니다. 약 " + retryAfterSeconds + "초 후 다시 시도해주세요.";
    }

    private void refreshAuthenticationPrincipal(Users savedUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails currentPrincipal)) {
            return;
        }

        CustomUserDetails updatedPrincipal = currentPrincipal.getAttributes() == null
                ? new CustomUserDetails(savedUser)
                : new CustomUserDetails(savedUser, currentPrincipal.getAttributes());

        UsernamePasswordAuthenticationToken updatedAuthentication =
                new UsernamePasswordAuthenticationToken(
                        updatedPrincipal,
                        authentication.getCredentials(),
                        updatedPrincipal.getAuthorities());
        updatedAuthentication.setDetails(authentication.getDetails());
        SecurityContextHolder.getContext().setAuthentication(updatedAuthentication);
    }

    private void populateUserUpdateDto(UserUpdateDto userUpdateDto, Users user) {
        if (userUpdateDto.getEmail() == null) {
            userUpdateDto.setEmail(user.getEmail());
        }
        if (userUpdateDto.getName() == null) {
            userUpdateDto.setName(user.getName());
        }
        if (userUpdateDto.getPhoneNumber() == null) {
            userUpdateDto.setPhoneNumber(user.getPhoneNumber());
        }
        if (userUpdateDto.getBirthDate() == null) {
            userUpdateDto.setBirthDate(user.getBirthDate());
        }
        if (userUpdateDto.getGender() == null) {
            userUpdateDto.setGender(user.getGender());
        }
        if (userUpdateDto.getAddress() == null) {
            userUpdateDto.setAddress(user.getAddress());
        }
        if (userUpdateDto.getAddressDetail() == null) {
            userUpdateDto.setAddressDetail(user.getAddressDetail());
        }
        if (userUpdateDto.getProfileImageUrl() == null) {
            userUpdateDto.setProfileImageUrl(user.getProfileImageUrl());
        }
    }

    private String normalizeProfileView(String view) {
        if (view == null || view.isBlank()) {
            return "boards";
        }
        String normalized = view.trim().toLowerCase();
        if ("comments".equals(normalized) || "scraps".equals(normalized) || "boards".equals(normalized)) {
            return normalized;
        }
        return "boards";
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        String id = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (id.length() <= 2) {
            return id.charAt(0) + "*" + domain;
        }
        return id.substring(0, 2) + "*".repeat(Math.max(1, id.length() - 2)) + domain;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
