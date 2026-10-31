package xyz.ruraeru.gettoknow;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);

	Optional<User> findByShareToken(String shareToken);

	default User current(Jwt jwt) {
		return findById(Long.valueOf(jwt.getSubject()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}
}

interface QuestionRepository extends JpaRepository<Question, Long> {
}

interface AnswerRepository extends JpaRepository<Answer, Long> {
	List<Answer> findByUser(User user);

	Optional<Answer> findByUserAndQuestionId(User user, Long questionId);
}
