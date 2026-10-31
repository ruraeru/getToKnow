package xyz.ruraeru.gettoknow;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

	private final SecretKey key;

	SecurityConfig(@Value("${app.jwt-secret}") String secret) {
		this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(c -> c.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(a -> a
						.requestMatchers("/api/auth/**", "/api/share/**", "/error").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
				.build();
	}

	@Bean
	JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withSecretKey(key).build();
	}

	@Bean
	JwtEncoder jwtEncoder() {
		return NimbusJwtEncoder.withSecretKey(key).build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origin}") String origin) {
		var config = new CorsConfiguration();
		config.setAllowedOrigins(List.of(origin));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
