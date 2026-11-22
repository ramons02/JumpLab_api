package br.com.jumplab.api.core.seguranca;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;

/**
 * API sem estado e sem cookie: CSRF desligado, só {@code /api/auth/**} é público e todo o resto exige o
 * token de acesso no cabeçalho {@code Authorization: Bearer}.
 */
@Configuration
@EnableConfigurationProperties(PropriedadesJwt.class)
public class ConfiguracaoSeguranca {

    public static final String PAPEL_ADMIN = "ROLE_ADMIN";

    @Bean
    public SecurityFilterChain filtrosDeSeguranca(HttpSecurity http, JwtDecoder decodificador,
                                                  TratadorAcessoNegado tratador) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(rotas -> rotas
                .requestMatchers("/api/auth/**", "/error").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(servidor -> servidor
                .jwt(jwt -> jwt.decoder(decodificador).jwtAuthenticationConverter(conversorDePapeis()))
                .authenticationEntryPoint(tratador)
                .accessDeniedHandler(tratador))
            .exceptionHandling(erros -> erros
                .authenticationEntryPoint(tratador)
                .accessDeniedHandler(tratador));
        return http.build();
    }

    @Bean
    public JwtDecoder decodificadorJwt(JwtService jwtService) {
        return jwtService.getDecodificador();
    }

    @Bean
    public PasswordEncoder codificadorDeSenha() {
        return new BCryptPasswordEncoder();
    }

    private static JwtAuthenticationConverter conversorDePapeis() {
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(papeisDoToken());
        return conversor;
    }

    private static Converter<Jwt, Collection<GrantedAuthority>> papeisDoToken() {
        return jwt -> Boolean.TRUE.equals(jwt.getClaimAsBoolean(JwtService.CLAIM_ADMIN))
            ? List.of(new SimpleGrantedAuthority(PAPEL_ADMIN))
            : List.of();
    }
}
