package xyz.ruraeru.gettoknow;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/questions")
class QuestionController {

	/** answer is null when the user has not answered yet. */
	record QuestionView(Long id, String content, String answer) {
	}

	record AnswerRequest(@NotBlank @Size(max = 500) String content) {
	}

	private final UserRepository users;
	private final QuestionRepository questions;
	private final AnswerRepository answers;

	QuestionController(UserRepository users, QuestionRepository questions, AnswerRepository answers) {
		this.users = users;
		this.questions = questions;
		this.answers = answers;
	}

	@GetMapping
	@Transactional(readOnly = true)
	List<QuestionView> list(@AuthenticationPrincipal Jwt jwt) {
		var mine = answers.findByUser(users.current(jwt)).stream()
				.collect(Collectors.toMap(a -> a.question.id, Function.identity()));
		return questions.findAll(Sort.by("id")).stream().map(q -> {
			var a = mine.get(q.id);
			return new QuestionView(q.id, q.content, a == null ? null : a.content);
		}).toList();
	}

	@PutMapping("/{id}/answer")
	@Transactional
	void answer(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody AnswerRequest req) {
		var me = users.current(jwt);
		var answer = answers.findByUserAndQuestionId(me, id).orElseGet(() -> {
			var a = new Answer();
			a.user = me;
			a.question = questions.findById(id)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
			return a;
		});
		answer.content = req.content().trim();
		answers.save(answer);
	}
}
