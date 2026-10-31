package xyz.ruraeru.gettoknow;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/** Public: anyone with the share link can see the lock question and try to open it. */
@RestController
@RequestMapping("/api/share/{token}")
class ShareController {

	record ShareInfo(String nickname, String lockQuestion) {
	}

	record UnlockRequest(@NotBlank String guess) {
	}

	record SharedAnswer(String question, String answer) {
	}

	record Unlocked(String nickname, List<SharedAnswer> answers) {
	}

	private final UserRepository users;
	private final AnswerRepository answers;
	private final PasswordEncoder passwordEncoder;

	ShareController(UserRepository users, AnswerRepository answers, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.answers = answers;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping
	ShareInfo info(@PathVariable String token) {
		var owner = owner(token);
		return new ShareInfo(owner.nickname, owner.lockQuestion);
	}

	// shortcut: unlimited guesses, so a short lock answer can be brute-forced by anyone holding the link; add a retry limit if that matters.
	@PostMapping("/unlock")
	@Transactional(readOnly = true)
	Unlocked unlock(@PathVariable String token, @Valid @RequestBody UnlockRequest req) {
		var owner = owner(token);
		if (!passwordEncoder.matches(normalize(req.guess()), owner.lockAnswer)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "틀렸어요. 다시 생각해봐요!");
		}
		var list = answers.findByUser(owner).stream()
				.sorted(Comparator.comparing(a -> a.question.id))
				.map(a -> new SharedAnswer(a.question.content, a.content))
				.toList();
		return new Unlocked(owner.nickname, list);
	}

	/** A link stays closed until its owner sets a lock. */
	private User owner(String token) {
		return users.findByShareToken(token)
				.filter(u -> u.lockAnswer != null)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "열 수 없는 링크예요"));
	}

	/** "강남 역" and "강남역", "Blue" and "blue" count as the same answer. */
	static String normalize(String s) {
		return s.replaceAll("\\s+", "").toLowerCase();
	}
}
