package kwh.PublicCookedFood.config;

import kwh.PublicCookedFood.config.oauth2.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
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
                .csrf(AbstractHttpConfigurer::disable)
                .headers((headers) -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

                .addFilterBefore(saveRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(formLogin -> formLogin
                        .loginPage("/u/login")
                        .defaultSuccessUrl("/",true)
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .loginProcessingUrl("/u/login")
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureUrl("/u/login?error=true")
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/u/logout"))
                        .logoutSuccessHandler(customLogoutSuccessHandler)
                )
                .oauth2Login((oauth2) -> oauth2
                .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                        .userService(customOAuth2UserService))
                .defaultSuccessUrl("/foods", true))

                .authorizeHttpRequests((authorizeRequests) -> authorizeRequests
                        .requestMatchers("/u/login", "/u/signup").not().fullyAuthenticated()
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/foods/**", "/fragments/**").permitAll()
                        .requestMatchers("/", "/**", "/u/**", "/foods/**", "/error/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                       .anyRequest().authenticated()
                ).
                exceptionHandling((exceptionHandling) -> exceptionHandling
                        .accessDeniedPage("/foods")
                        .authenticationEntryPoint
                                (new CustomAuthenticationEntryPoint()));
        return http.build();
    }


}
