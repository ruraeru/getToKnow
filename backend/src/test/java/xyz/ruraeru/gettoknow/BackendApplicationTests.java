package xyz.ruraeru.gettoknow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class BackendApplicationTests {

	@Autowired
	MockMvc mvc;

	@Test
	void answersAreSharedBehindLock() throws Exception {
		var a = signup("a@test.com", "에이");
		String link = JsonPath.read(
				mvc.perform(auth(get("/api/me"), a)).andReturn().getResponse().getContentAsString(), "$.shareToken");

		mvc.perform(json(put("/api/questions/2/answer"), a, "{\"content\":\"파랑\"}")).andExpect(status().isOk());
		mvc.perform(auth(get("/api/questions"), a)).andExpect(jsonPath("$[1].answer").value("파랑"));

		// closed until a lock is set
		mvc.perform(get("/api/share/" + link)).andExpect(status().isNotFound());
		mvc.perform(json(put("/api/me/lock"), a, "{\"lockQuestion\":\"우리 처음 만난 곳?\",\"lockAnswer\":\"강남 역\"}"))
				.andExpect(status().isOk());

		// viewer needs no account
		mvc.perform(get("/api/share/" + link))
				.andExpect(jsonPath("$.nickname").value("에이"))
				.andExpect(jsonPath("$.lockQuestion").value("우리 처음 만난 곳?"))
				.andExpect(jsonPath("$.answers").doesNotExist());
		mvc.perform(post("/api/share/" + link + "/unlock").contentType(MediaType.APPLICATION_JSON)
				.content("{\"guess\":\"홍대\"}"))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/api/share/" + link + "/unlock").contentType(MediaType.APPLICATION_JSON)
				.content("{\"guess\":\"강남역\"}"))
				.andExpect(jsonPath("$.answers[0].question").value("좋아하는 색깔은?"))
				.andExpect(jsonPath("$.answers[0].answer").value("파랑"));

		mvc.perform(get("/api/questions")).andExpect(status().isUnauthorized());
	}

	private String signup(String email, String nickname) throws Exception {
		var body = "{\"email\":\"" + email + "\",\"password\":\"password1\",\"nickname\":\"" + nickname + "\"}";
		var res = mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk()).andReturn();
		return JsonPath.read(res.getResponse().getContentAsString(), "$.token");
	}

	private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, String token) {
		return req.header("Authorization", "Bearer " + token);
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String token, String body) {
		return auth(req, token).contentType(MediaType.APPLICATION_JSON).content(body);
	}
}
