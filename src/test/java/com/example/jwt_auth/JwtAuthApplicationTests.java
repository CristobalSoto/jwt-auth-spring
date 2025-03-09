package com.example.jwt_auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = JwtAuthApplication.class)
@TestPropertySource(locations = "classpath:application.properties")
@ActiveProfiles("test")
class JwtAuthApplicationTests {

	@Test
	void contextLoads() {
		// This test verifies that the Spring context loads successfully
	}

}
