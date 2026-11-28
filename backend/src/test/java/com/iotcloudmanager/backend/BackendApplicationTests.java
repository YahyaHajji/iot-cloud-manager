package com.iotcloudmanager.backend;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void swaggerAndOpenApiPathsDoNotRequireAuthentication() throws Exception {
		for (String path : new String[] {
				"/swagger-ui.html",
				"/swagger-ui/index.html",
				"/v3/api-docs",
				"/v3/api-docs/swagger-config"
		}) {
			int status = mockMvc.perform(get(path))
					.andReturn()
					.getResponse()
					.getStatus();

			assertNotEquals(401, status, path + " must not require authentication");
			assertNotEquals(403, status, path + " must not be forbidden");
		}
	}

}
