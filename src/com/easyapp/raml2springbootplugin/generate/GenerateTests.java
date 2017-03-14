package com.easyapp.raml2springbootplugin.generate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.methods.Method;
import org.raml.v2.api.model.v10.resources.Resource;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;
import com.easyapp.raml2springbootplugin.generate.util.GeneratorUtil;

public class GenerateTests {
	private final Api api;
	private final CodeGenerator generator;
	private final Set<String> memberVariables = new HashSet<>();
	private final StringBuffer members = new StringBuffer();
	private final StringBuffer methods = new StringBuffer();

	private String getHttpMethod(final String method) {
		if ("get".equalsIgnoreCase(method)) {
			return "HttpMethod.GET";
		} else if ("post".equalsIgnoreCase(method)) {
			return "HttpMethod.POST";
		} else if ("put".equalsIgnoreCase(method)) {
			return "HttpMethod.PUT";
		} else if ("patch".equalsIgnoreCase(method)) {
			return "HttpMethod.PATCH";
		} else if ("delete".equalsIgnoreCase(method)) {
			return "HttpMethod.DELETE";
		} else if ("head".equalsIgnoreCase(method)) {
			return "HttpMethod.HEAD";
		} else if ("options".equalsIgnoreCase(method)) {
			return "HttpMethod.OPTIONS";
		} else {
			return "HttpMethod.TRACE";
		}
	}

	private List<String> getPathVariables(final List<TypeDeclaration> uriParameters) {
		if (uriParameters.isEmpty()) {
			return new ArrayList<>();
		} else {
			return uriParameters.stream()
					.map(uriParam -> generator.getJavaType(uriParam.type(), CodeGenerator.DEFAULT_TRANSPORT_PACKAGE)
							+ " " + uriParam.name())
					.collect(Collectors.toList());
		}
	}

	private List<String> getRequestParameters(final Method method) {
		if (method.queryParameters().isEmpty()) {
			return new ArrayList<>();
		} else {
			return method.queryParameters().stream()
					.map(queryParam -> generator.getJavaType(queryParam.type(), CodeGenerator.DEFAULT_TRANSPORT_PACKAGE)
							+ " " + queryParam.name())
					.collect(Collectors.toList());
		}
	}

	private Map<String, List<String>> getVariables(final Method method) {
		final Map<String, List<String>> variables = new HashMap<>();
		final Map<String, List<String>> pathVariables = new HashMap<>();
		pathVariables.put("uri", getPathVariables(GeneratorUtil.getURIParameters(method.resource())));

		final Map<String, List<String>> requestParams = new HashMap<>();
		requestParams.put("query", getRequestParameters(method));

		if (!pathVariables.get("uri").isEmpty()) {
			variables.putAll(pathVariables);
		}

		if (("post").equals(method.method()) || ("put").equals(method.method()) || ("patch").equals(method.method())) {
			variables
					.put("body",
							Arrays.asList(generator.getJavaType(
									method.body().isEmpty() ? "string"
											: (method.body().get(0).type().isEmpty() ? "string"
													: method.body().get(0).type()),
									CodeGenerator.DEFAULT_TRANSPORT_PACKAGE) + " "
									+ GeneratorUtil.getRequestBodyVariableName(method)));
		}

		if (!requestParams.get("query").isEmpty()) {
			variables.putAll(requestParams);
		}

		variables.entrySet().stream().forEach(entry -> {
			memberVariables.addAll(entry.getValue());
		});

		return variables;
	}

	public GenerateTests(final Api api, final CodeGenConfig codeGenConfig) {
		this.api = api;
		generator = new CodeGenerator(codeGenConfig.getTestDirectory(), codeGenConfig.getBasePackage(), null,
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

		members.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT1).append("@LocalServerPort")
				.append(CodeGenerator.NEWLINE);
		members.append(CodeGenerator.INDENT1).append("private int port;").append(CodeGenerator.NEWLINE);
		generator.addImport("org.springframework.boot.context.embedded.LocalServerPort");

		members.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT1)
				.append("@Value(\"${server.context-path:}\")").append(CodeGenerator.NEWLINE);
		members.append(CodeGenerator.INDENT1).append("private String serverContextPath;").append(CodeGenerator.NEWLINE);
		generator.addImport("org.springframework.beans.factory.annotation.Value");

		members.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT1)
				.append("private String baseURI = \"http://localhost:\";");

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
					.append("private final String healthCheckEndPoint = \"/healthCheck\";");

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
		final String resourceEndPointVariable = resource.resourcePath().split("/")[1] + "EndPoint";

		members.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT1).append("private final String ")
				.append(resourceEndPointVariable).append(" = \"").append(resource.resourcePath()).append("\";");

		resource.methods().stream().forEach(method -> {
			final Map<String, List<String>> methodVariables = getVariables(method);
			final String uriVariables = !methodVariables.containsKey("uri") || methodVariables.get("uri").isEmpty()
					? null
					: methodVariables.get("uri").stream().map(variable -> variable.split(" ")[1])
							.collect(Collectors.joining(", "));
			final String bodyVariable = !methodVariables.containsKey("body") || methodVariables.get("body").isEmpty()
					? null : methodVariables.get("body").get(0).split(" ")[1];
			final String responseType = generator.getJavaType(
					method.responses().stream().filter(response -> response.code().value().startsWith("2"))
							.map(response -> response.body().get(0).type()).findFirst().orElse("string"),
					CodeGenerator.DEFAULT_TRANSPORT_PACKAGE);

			method.responses().stream().forEach(response -> {
				final String methodName = "test" + GeneratorUtil.getTitleCase(method.method(), "-")
						+ resource.displayName().value().replaceAll(" ", "") + response.code().value();

				methods.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT1).append("@Test")
						.append(CodeGenerator.NEWLINE);
				methods.append(CodeGenerator.INDENT1).append("public void ").append(methodName).append("() {")
						.append(CodeGenerator.NEWLINE);
				methods.append(CodeGenerator.INDENT2)
						.append("final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseURI + ")
						.append(resourceEndPointVariable).append(");").append(CodeGenerator.NEWLINE);

				if (methodVariables.containsKey("query")) {
					methodVariables.get("query").stream().map(variable -> variable.split(" ")[1])
							.forEach(queryParam -> {
								methods.append(CodeGenerator.INDENT2).append("uriBuilder.queryParam(\"")
										.append(queryParam).append("\", ").append(queryParam).append(");")
										.append(CodeGenerator.NEWLINE);
							});
				}

				methods.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT2).append("final ResponseEntity<")
						.append(responseType)
						.append("> response = restTemplate.exchange(uriBuilder.build().encode().toUriString(), ")
						.append(getHttpMethod(method.method())).append(", ");

				if (bodyVariable == null) {
					methods.append("null, ");
				} else {
					methods.append("new HttpEntity<>(").append(bodyVariable).append("), ");
				}

				if (responseType.contains("<")) {
					methods.append("new ParameterizedTypeReference<").append(responseType).append(">() { }");
					generator.addImport("org.springframework.core.ParameterizedTypeReference");
				} else {
					methods.append(responseType).append(".class");
				}

				if (uriVariables != null) {
					methods.append(", ").append(uriVariables);
				}

				methods.append(");").append(CodeGenerator.NEWLINE);

				methods.append(CodeGenerator.INDENT2).append("assertThat(response.getStatusCode().value(), equalTo(")
						.append(response.code().value()).append("));").append(CodeGenerator.NEWLINE);
				methods.append(CodeGenerator.INDENT2).append("// TODO: Additional Tests").append(CodeGenerator.NEWLINE);
				methods.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE);

				generator.addImport("org.junit.Test");
				generator.addImport("org.springframework.web.util.UriComponentsBuilder");
				generator.addImport("org.springframework.http.ResponseEntity");
				generator.addImport("org.springframework.http.HttpMethod");
				generator.addImport("org.springframework.http.HttpEntity");
				generator.addImport("static org.junit.Assert.assertThat");
				generator.addImport("static org.hamcrest.CoreMatchers.equalTo");
			});
		});

		resource.resources().stream().forEach(subResource -> createResourceMethods(subResource));
	}

	public void create() {
		api.resources().stream().forEach(resource -> createResourceMethods(resource));

		memberVariables.stream().forEach(memberVariable -> {
			members.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT1).append("private ")
					.append(memberVariable).append(";");
		});

		generator.addCodeBlock(members.toString());
		generator.addCodeBlock(methods.toString());
		generator.writeCode();
	}
}
