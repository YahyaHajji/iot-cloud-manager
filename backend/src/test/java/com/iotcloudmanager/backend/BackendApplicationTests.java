package com.iotcloudmanager.backend;

import com.iotcloudmanager.backend.security.JwtService;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:backend-test;NON_KEYWORDS=VALUE"
})
@AutoConfigureMockMvc
class BackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

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

	@Test
	void swaggerUiAndOpenApiDocumentAreAvailable() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.openapi").exists());

		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath(
						"$.components.securitySchemes.bearerAuth.type")
						.value("http"))
				.andExpect(jsonPath(
						"$.components.securitySchemes.bearerAuth.scheme")
						.value("bearer"))
				.andExpect(jsonPath(
						"$.components.securitySchemes.bearerAuth.bearerFormat")
						.value("JWT"))
				.andExpect(jsonPath(
						"$.paths['/api/measurements/device/{deviceId}/export']"
								+ ".get.security[0].bearerAuth")
						.exists())
				.andExpect(jsonPath(
						"$.paths['/api/auth/login'].post.security")
						.doesNotExist());

		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}

	@Test
	void measurementExportRequiresAuthentication() throws Exception {
		mockMvc.perform(get(
						"/api/measurements/device/sensor-room-01/export"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanDownloadMeasurementCsv() throws Exception {
		String token = jwtService.generateToken("test-admin", "ADMIN");

		mockMvc.perform(get(
						"/api/measurements/device/sensor-room-01/export")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/csv"))
				.andExpect(content().string(
						"measuredAt,deviceId,type,value,unit\r\n"));
	}

}
