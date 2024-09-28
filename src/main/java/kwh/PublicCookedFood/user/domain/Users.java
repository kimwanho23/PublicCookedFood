package kwh.PublicCookedFood.user.domain;

import jakarta.persistence.*;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.domain.Likes;
import kwh.PublicCookedFood.user.dto.UserSaveDto;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "user")
public class Users {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email; //이메일

    @Column
    private String password; //비밀번호

    @Column(nullable = false)
    private String name; //별명

    @Enumerated(EnumType.STRING)
    private Role authority; // 권한

    @Column(nullable = false)
    private String loginMethod;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    private List<Board> boards = new ArrayList<>(); // 사용자가 작성한 글

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    private List<Comments> comments = new ArrayList<>(); // 사용자가 작성한 댓글

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    private List<Likes> likes = new ArrayList<>(); //  내가 찍은 좋아요

    public Users() {
    }

    @Builder
    public Users(Long id, String email, String password, String name, Role authority, String loginMethod, List<Board> boards, List<Comments> comments, List<Likes> likes) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.authority = authority;
        this.loginMethod = loginMethod;
        this.boards = boards;
        this.comments = comments;
        this.likes = likes;
    }

    public Users update(String name) {
        this.name = name;
        return this;
    }

    public String getRoleKey() {
        return this.authority.getKey();
    }

    public static Users createUser(UserSaveDto userSaveDto,
                                   PasswordEncoder passwordEncoder) {
        return Users.builder()
                .name(userSaveDto.getName())
                .email(userSaveDto.getEmail())
                .password(passwordEncoder.encode(userSaveDto.getPassword()))
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

}
