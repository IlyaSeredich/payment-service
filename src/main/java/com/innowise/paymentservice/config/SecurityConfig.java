package com.innowise.paymentservice.config;

import com.innowise.paymentservice.converter.RoleConverter;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {
    private final RoleConverter roleConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(roleConverter);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests ->
                        requests
                                .requestMatchers(HttpMethod.GET, "/api/payments/my")
                                    .hasAnyRole("user", "admin")
                                .requestMatchers(HttpMethod.GET, "/api/payments/users")
                                    .hasRole("admin")
                                .requestMatchers(HttpMethod.GET, "/api/payments/orders")
                                .hasRole("admin")
                                .requestMatchers(HttpMethod.GET, "/api/payments/statuses")
                                .hasRole("admin")
                                .requestMatchers(HttpMethod.GET, "/api/payments/my/total")
                                .hasAnyRole("user", "admin")
                                .requestMatchers(HttpMethod.GET, "/api/payments/total")
                                .hasRole("admin")
                                .requestMatchers(HttpMethod.POST, "/api/payments")
                                .hasRole( "admin")

                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .oauth2ResourceServer(oauth2 -> {
                            oauth2.jwt(jwt ->
                                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter));
                        }
                );

        return http.build();
    }

}
