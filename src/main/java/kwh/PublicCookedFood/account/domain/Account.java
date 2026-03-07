package kwh.PublicCookedFood.account.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.domain.Likes;
import kwh.PublicCookedFood.account.dto.request.AccountSaveDto;
import kwh.PublicCookedFood.account.policy.AccountNicknamePolicy;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Getter
@Table(name = "accounts")
public class Account {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email; //이메일

    @Column
    private String password; //비밀번호

    @Column(nullable = false, unique = true)
    private String name; //별명

    @Column(length = 30)
    private String phoneNumber;

    @Column
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(length = 120)
    private String address;

    @Column(length = 120)
    private String addressDetail;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(nullable = false)
    private boolean notificationEnabled = true;

    @Enumerated(EnumType.STRING)
    private Role authority; // 권한

    @Column(nullable = false)
    private String loginMethod;

    @BatchSize(size = 100)
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "account")
    private List<Bookmark> bookmarks = new ArrayList<>(); // 사용자 북마크

    @BatchSize(size = 100)
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "account")
    private List<Board> boards = new ArrayList<>(); // 사용자가 작성한 글

    @BatchSize(size = 100)
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "account")
    private List<Comments> comments = new ArrayList<>(); // 사용자가 작성한 댓글

    @BatchSize(size = 100)
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "account")
    private List<Likes> likes = new ArrayList<>(); //  내가 찍은 좋아요

    public Account() {
    }

    @Builder
    public Account(Long id, String email, String password, String name, String phoneNumber, LocalDate birthDate,
                 Gender gender, String address, String addressDetail, String profileImageUrl,
                 Boolean notificationEnabled, Role authority, String loginMethod, List<Bookmark> bookmarks,
                 List<Board> boards, List<Comments> comments, List<Likes> likes) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = normalizeRequiredName(name);
        this.phoneNumber = normalizePhoneNumber(phoneNumber);
        this.birthDate = birthDate;
        this.gender = gender;
        this.address = normalizeNullableText(address);
        this.addressDetail = normalizeNullableText(addressDetail);
        this.profileImageUrl = normalizeNullableText(profileImageUrl);
        this.notificationEnabled = notificationEnabled == null || notificationEnabled;
        this.authority = authority;
        this.loginMethod = loginMethod;
        this.bookmarks = bookmarks == null ? new ArrayList<>() : new ArrayList<>(bookmarks);
        this.boards = boards == null ? new ArrayList<>() : new ArrayList<>(boards);
        this.comments = comments == null ? new ArrayList<>() : new ArrayList<>(comments);
        this.likes = likes == null ? new ArrayList<>() : new ArrayList<>(likes);
    }

    public List<Bookmark> getBookmarks() {
        return Collections.unmodifiableList(bookmarks);
    }

    public List<Board> getBoards() {
        return Collections.unmodifiableList(boards);
    }

    public List<Comments> getComments() {
        return Collections.unmodifiableList(comments);
    }

    public List<Likes> getLikes() {
        return Collections.unmodifiableList(likes);
    }

    public Account update(String name) {
        if (name != null && !name.isBlank()) {
            this.name = normalizeRequiredName(name);
        }
        return this;
    }

    public Account updateProfile(String name, String phoneNumber, LocalDate birthDate, Gender gender,
                               String address, String addressDetail, String profileImageUrl) {
        if (name != null && !name.isBlank()) {
            this.name = normalizeRequiredName(name);
        }
        this.phoneNumber = normalizePhoneNumber(phoneNumber);
        this.birthDate = birthDate;
        this.gender = gender;
        this.address = normalizeNullableText(address);
        this.addressDetail = normalizeNullableText(addressDetail);
        this.profileImageUrl = normalizeNullableText(profileImageUrl);
        return this;
    }

    public Account updatePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            return this;
        }
        this.password = encodedPassword;
        return this;
    }

    public Account updateNotificationEnabled(boolean enabled) {
        this.notificationEnabled = enabled;
        return this;
    }

    public String getRoleKey() {
        return this.authority.getKey();
    }

    public static Account createAccount(AccountSaveDto accountSaveDto,
                                   PasswordEncoder passwordEncoder) {
        return Account.builder()
                .name(accountSaveDto.getName())
                .email(accountSaveDto.getEmail())
                .password(passwordEncoder.encode(accountSaveDto.getPassword()))
                .phoneNumber(accountSaveDto.getPhoneNumber())
                .birthDate(accountSaveDto.getBirthDate())
                .gender(accountSaveDto.getGender())
                .address(accountSaveDto.getAddress())
                .addressDetail(accountSaveDto.getAddressDetail())
                .profileImageUrl(accountSaveDto.getProfileImageUrl())
                .notificationEnabled(true)
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

    private static String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        return digits.isBlank() ? null : digits;
    }

    private static String normalizeNullableText(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String trimmed = rawValue.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeRequiredName(String rawName) {
        return AccountNicknamePolicy.normalizeRequiredNickname(rawName);
    }

}
