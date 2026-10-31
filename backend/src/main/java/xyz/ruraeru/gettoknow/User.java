package xyz.ruraeru.gettoknow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	@Column(nullable = false, unique = true)
	String email;

	@Column(nullable = false)
	String password;

	@Column(nullable = false)
	String nickname;

	/** Random id in the share link; unguessable so only link holders can try the lock. */
	@Column(nullable = false, unique = true, length = 16)
	String shareToken;

	/** Question a link viewer must answer before seeing this user's answers. Null until set. */
	String lockQuestion;

	/** BCrypt hash of the normalized lock answer. */
	String lockAnswer;
}
