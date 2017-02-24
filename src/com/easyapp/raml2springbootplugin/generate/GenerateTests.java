package com.easyapp.raml2springbootplugin.generate;

import java.util.Arrays;

import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.resources.Resource;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;

public class GenerateTests {
	private final Api api;
	private final CodeGenerator generator;
	private final StringBuffer members = new StringBuffer();
	private final StringBuffer methods = new StringBuffer();

	public GenerateTests(final Api api, final CodeGenConfig codeGenConfig) {
		this.api = api;
		generator = new CodeGenerator(codeGenConfig.getTestDirectory(), codeGenConfig.getBasePackage(),
				Arrays.asList("@RunWith(SpringRunner.class)",
						"@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)"),
				false, codeGenConfig.getTestClassName(), null, null,
				codeGenConfig.getExternalConfig().overwriteFiles());
		generator.addImport("org.junit.runner.RunWith");
		generator.addImport("org.springframework.test.context.junit4.SpringRunner");
		generator.addImport("org.springframework.boot.test.context.SpringBootTest");
		generator.addImport("org.springframework.boot.test.context.SpringBootTest.WebEnvironment");

		members.append(CodeGenerator.INDENT1).append("@Autowired").append(CodeGenerator.NEWLINE);
		members.append(CodeGenerator.INDENT1).append("private TestRestTemplate restTemplate;")
				.append(CodeGenerator.NEWLINE);
		generator.addImport("org.springframework.beans.factory.annotation.Autowired");
		generator.addImport("org.springframework.boot.test.web.client.TestRestTemplate");

		members.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT1)
				.append("private String baseURI = \"http://localhost:\";").append(CodeGenerator.NEWLINE);

		members.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT1).append("@LocalServerPort")
				.append(CodeGenerator.NEWLINE);
		members.append(CodeGenerator.INDENT1).append("private int port;").append(CodeGenerator.NEWLINE);
		generator.addImport("org.springframework.boot.context.embedded.LocalServerPort");

		members.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT1)
				.append("@Value(\"${server.context-path:}\")").append(CodeGenerator.NEWLINE);
		members.append(CodeGenerator.INDENT1).append("private String serverContextPath;").append(CodeGenerator.NEWLINE);
		generator.addImport("org.springframework.beans.factory.annotation.Value");

		methods.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT1).append("@Before")
				.append(CodeGenerator.NEWLINE);
		methods.append(CodeGenerator.INDENT1).append("public void setup() {").append(CodeGenerator.NEWLINE);
		methods.append(CodeGenerator.INDENT2).append("baseURI += port + serverContextPath;")
				.append(CodeGenerator.NEWLINE);
		methods.append(CodeGenerator.INDENT2).append("// TODO: Additional Setup").append(CodeGenerator.NEWLINE);
		methods.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE).append(CodeGenerator.NEWLINE);
		generator.addImport("org.junit.Before");

		if (codeGenConfig.getExternalConfig().generateHealthCheck()) {
			members.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT1)
					.append("private final String healthCheckEndPoint = \"/healthCheck\";")
					.append(CodeGenerator.NEWLINE);

			methods.append(CodeGenerator.INDENT1).append("@Test").append(CodeGenerator.NEWLINE);
			methods.append(CodeGenerator.INDENT1).append("public void testHealthCheck() {")
					.append(CodeGenerator.NEWLINE);
			methods.append(CodeGenerator.INDENT2)
					.append("final ResponseEntity<String> response = restTemplate.getForEntity(baseURI + healthCheckEndPoint, String.class);")
					.append(CodeGenerator.NEWLINE);
			methods.append(CodeGenerator.INDENT2).append("assertThat(response.getStatusCode().value(), equalTo(200));")
					.append(CodeGenerator.NEWLINE);
			methods.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE);
			generator.addImport("org.junit.Test");
			generator.addImport("org.springframework.http.ResponseEntity");
			generator.addImport("static org.junit.Assert.assertThat");
			generator.addImport("static org.hamcrest.CoreMatchers.equalTo");
		}
	}

	private void createResourceMethods(final Resource resource) {
		resource.methods().stream().forEach(method -> {
			// TODO: create test methods
		});

		resource.resources().stream().forEach(subResource -> createResourceMethods(subResource));
	}

	public void create() {
		api.resources().stream().forEach(resource -> createResourceMethods(resource));
		generator.addCodeBlock(members.toString() + methods.toString());
		generator.writeCode();
	}
}
