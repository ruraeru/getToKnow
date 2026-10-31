package xyz.ruraeru.gettoknow;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BackendApplication {

	static final List<String> QUESTIONS = List.of(
			"키는 몇이야?",
			"좋아하는 색깔은?",
			"싫어하는 음식은?",
			"가장 좋아하는 음식은?",
			"신발 사이즈는?",
			"혈액형은?",
			"MBTI는?",
			"제일 무서워하는 건?",
			"스트레스 받을 때 하는 일은?",
			"어릴 때 꿈은?",
			"가장 좋아하는 계절은?",
			"인생 영화는?");

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	/** Adds any new built-in question; existing ones are left alone. */
	@Bean
	CommandLineRunner seedQuestions(QuestionRepository questions) {
		return args -> {
			var existing = questions.findAll().stream().map(q -> q.content).toList();
			for (var content : QUESTIONS) {
				if (!existing.contains(content)) {
					var q = new Question();
					q.content = content;
					questions.save(q);
				}
			}
		};
	}

}
