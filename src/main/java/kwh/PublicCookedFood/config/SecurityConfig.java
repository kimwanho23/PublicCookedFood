package kwh.PublicCookedFood.config;

import kwh.PublicCookedFood.config.oauth2.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    private final SaveRequestFilter saveRequestFilter;

    private final LoginThrottleFilter loginThrottleFilter;

    private final CustomOAuth2UserService customOAuth2UserService;

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(CustomLogoutSuccessHandler customLogoutSuccessHandler,
                          CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
                          CustomAuthenticationFailureHandler customAuthenticationFailureHandler,
                          SaveRequestFilter saveRequestFilter,
                          LoginThrottleFilter loginThrottleFilter,
                          CustomOAuth2UserService customOAuth2UserService,
                          CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
                          CustomAccessDeniedHandler customAccessDeniedHandler) {
        this.customLogoutSuccessHandler = customLogoutSuccessHandler;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.customAuthenticationFailureHandler = customAuthenticationFailureHandler;
        this.saveRequestFilter = saveRequestFilter;
        this.loginThrottleFilter = loginThrottleFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))

                .addFilterBefore(loginThrottleFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(saveRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(formLogin -> formLogin
                        .loginPage("/u/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .loginProcessingUrl("/u/login")
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureHandler(customAuthenticationFailureHandler)
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/u/logout", "POST"))
                        .logoutSuccessHandler(customLogoutSuccessHandler)
                )
                .oauth2Login((oauth2) -> oauth2
                .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                        .userService(customOAuth2UserService))
                .successHandler(customAuthenticationSuccessHandler)
                .failureUrl("/u/login?oauthError=true"))
               .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
                )

                .authorizeHttpRequests((authorizeRequests) -> authorizeRequests
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/images/**", "/files/**", "/error/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/u/login", "/u/login/**", "/u/signup", "/u/account/**", "/oauth2/**", "/login/**").permitAll()
                        .requestMatchers(new RegexRequestMatcher("^/u/[0-9]+$", HttpMethod.GET.name())).permitAll()
                        .requestMatchers(new RegexRequestMatcher("^/u/[0-9]+/(comments|scraps)$", HttpMethod.GET.name())).permitAll()
                        .requestMatchers("/u/profile", "/u/settings", "/u/logout", "/u/blocks/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/boards/new", "/boards/*/edit", "/boards/scraps").authenticated()
                        .requestMatchers(HttpMethod.POST, "/recipes/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.POST, "/boards", "/boards/*/comments", "/boards/*/scraps", "/boards/*/reports").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/boards/*", "/boards/*/delete", "/boards/*/comments/*/delete", "/boards/*/scraps/delete").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/boards/*/likes").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/images/original", "/api/images/boards/*/zip").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/ai/recipes/**").permitAll()
                        .requestMatchers("/bookmarks/**", "/api/images").authenticated()
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/", "/main", "/recipes", "/recipes/**", "/boards", "/boards/featured", "/boards/*").permitAll()
                        .anyRequest().authenticated()
                ).
                exceptionHandling((exceptionHandling) -> exceptionHandling
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .authenticationEntryPoint(customAuthenticationEntryPoint));
        return http.build();
    }


}
