package xyz.ruraeru.gettoknow;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api")
class AuthController {

	record SignupRequest(@Email @NotBlank String email, @Size(min = 8, max = 72) String password,
			@NotBlank @Size(max = 20) String nickname) {
	}

	record LoginRequest(@NotBlank String email, @NotBlank String password) {
	}

	record LockRequest(@NotBlank @Size(max = 200) String lockQuestion, @NotBlank @Size(max = 100) String lockAnswer) {
	}

	record TokenResponse(String token) {
	}

	/** lockQuestion is null until set; the share link stays closed until then. */
	record MeResponse(String nickname, String shareToken, String lockQuestion) {
	}

	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;

	AuthController(UserRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
	}

	@PostMapping("/auth/signup")
	TokenResponse signup(@Valid @RequestBody SignupRequest req) {
		if (users.findByEmail(req.email()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일이에요");
		}
		var user = new User();
		user.email = req.email();
		user.password = passwordEncoder.encode(req.password());
		user.nickname = req.nickname();
		user.shareToken = newShareToken();
		return token(users.save(user));
	}

	@PostMapping("/auth/login")
	TokenResponse login(@Valid @RequestBody LoginRequest req) {
		return users.findByEmail(req.email())
				.filter(u -> passwordEncoder.matches(req.password(), u.password))
				.map(this::token)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 틀렸어요"));
	}

	@GetMapping("/me")
	MeResponse me(@AuthenticationPrincipal Jwt jwt) {
		var me = users.current(jwt);
		return new MeResponse(me.nickname, me.shareToken, me.lockQuestion);
	}

	@PutMapping("/me/lock")
	@Transactional
	MeResponse lock(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LockRequest req) {
		var me = users.current(jwt);
		me.lockQuestion = req.lockQuestion().trim();
		me.lockAnswer = passwordEncoder.encode(ShareController.normalize(req.lockAnswer()));
		return new MeResponse(me.nickname, me.shareToken, me.lockQuestion);
	}

	private TokenResponse token(User user) {
		var now = Instant.now();
		var claims = JwtClaimsSet.builder()
				.subject(user.id.toString())
				.issuedAt(now)
				.expiresAt(now.plus(Duration.ofDays(30)))
				.build();
		var header = JwsHeader.with(MacAlgorithm.HS256).build();
		return new TokenResponse(jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue());
	}

	/** 12 random bytes as 16 URL-safe chars. */
	private static String newShareToken() {
		var bytes = new byte[12];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
