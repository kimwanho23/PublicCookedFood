package kwh.PublicCookedFood.config;

import kwh.PublicCookedFood.config.oauth2.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    private final SaveRequestFilter saveRequestFilter;

    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(CustomLogoutSuccessHandler customLogoutSuccessHandler, CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler, SaveRequestFilter saveRequestFilter, CustomOAuth2UserService customOAuth2UserService) {
        this.customLogoutSuccessHandler = customLogoutSuccessHandler;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.saveRequestFilter = saveRequestFilter;
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/uploadFile"))
                .headers((headers) -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

                .addFilterBefore(saveRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(formLogin -> formLogin
                        .loginPage("/u/login")
                        .defaultSuccessUrl("/foods",true)
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .loginProcessingUrl("/u/login")
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureUrl("/u/login?error=true")
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/u/logout", "POST"))
                        .logoutSuccessHandler(customLogoutSuccessHandler)
                )
                .oauth2Login((oauth2) -> oauth2
                .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                        .userService(customOAuth2UserService))
                .defaultSuccessUrl("/foods", true)
                .failureUrl("/u/login?oauthError=true"))
               .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                .authorizeHttpRequests((authorizeRequests) -> authorizeRequests
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/images/**", "/error/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/u/login", "/u/login/**", "/u/signup", "/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/u/profile", "/u/logout").authenticated()
                        .requestMatchers("/board/write", "/board/update/**", "/board/deleteBoard/**",
                                "/board/deleteComment/**", "/board/comment/**", "/board/likes/**").authenticated()
                        .requestMatchers("/bookmark/**", "/uploadFile").authenticated()
                        .requestMatchers("/crse", "/info", "/irdnt", "/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/", "/main", "/foods", "/foods/**", "/board", "/board/**").permitAll()
                        .anyRequest().authenticated()
                ).
                exceptionHandling((exceptionHandling) -> exceptionHandling
                        .accessDeniedPage("/foods")
                        .authenticationEntryPoint
                                (new CustomAuthenticationEntryPoint()));
        return http.build();
    }


}
