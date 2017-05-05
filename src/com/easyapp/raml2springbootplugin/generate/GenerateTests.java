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
					.map(uriParam -> generator.getJavaType(GeneratorUtil.getMemberType(uriParam),
							CodeGenerator.DEFAULT_TRANSPORT_PACKAGE, false) + " "
							+ GeneratorUtil.getMemberName(uriParam))
					.collect(Collectors.toList());
		}
	}

	private List<String> getRequestParameters(final Method method) {
		if (method.queryParameters().isEmpty()) {
			return new ArrayList<>();
		} else {
			return method.queryParameters().stream()
					.map(queryParam -> generator.getJavaType(GeneratorUtil.getMemberType(queryParam),
							CodeGenerator.DEFAULT_TRANSPORT_PACKAGE, false) + " "
							+ GeneratorUtil.getMemberName(queryParam))
					.collect(Collectors.toList());
		}
	}

	private Map<String, List<String>> getVariables(final Method method) {
		final Map<String, List<String>> variables = new HashMap<>();
		final List<String> headerVariables = getPathVariables(GeneratorUtil.getHeaders(method));
		final List<String> pathVariables = getPathVariables(GeneratorUtil.getURIParameters(method.resource()));

		final Map<String, List<String>> requestParams = new HashMap<>();
		requestParams.put("query", getRequestParameters(method));

		if (!headerVariables.isEmpty()) {
			variables.put("header", headerVariables);
		}

		if (!pathVariables.isEmpty()) {
			variables.put("uri", pathVariables);
		}

		if (("post").equals(method.method()) || ("put").equals(method.method()) || ("patch").equals(method.method())) {
			variables.put("body",
					Arrays.asList(generator.getJavaType(
							method.body().isEmpty() ? "string" : GeneratorUtil.getMemberType(method.body().get(0)),
							CodeGenerator.DEFAULT_TRANSPORT_PACKAGE, false) + " "
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
		final String apiTitle = api.title().value().replaceAll(" ", "");
		generator = new CodeGenerator(codeGenConfig, null,
				Arrays.asList("@RunWith(SpringRunner.class)",
						"@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)"),
				false, apiTitle + "Tests", null, null, true);
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
	}

	private void createResourceMethods(final Resource resource) {
		final String resourceEndPointVariable = resource.displayName().value() + "EndPoint";

		members.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT1).append("private final String ")
				.append(resourceEndPointVariable).append(" = \"").append(resource.resourcePath()).append("\";");

		resource.methods().stream().forEach(method -> {
			final Map<String, List<String>> methodVariables = getVariables(method);
			final List<String> headerVariables = !methodVariables.containsKey("header")
					|| methodVariables.get("header").isEmpty() ? null
							: methodVariables.get("header").stream().map(variable -> variable.split(" ")[1])
									.collect(Collectors.toList());
			final String uriVariables = !methodVariables.containsKey("uri") || methodVariables.get("uri").isEmpty()
					? null
					: methodVariables.get("uri").stream().map(variable -> variable.split(" ")[1])
							.collect(Collectors.joining(", "));
			final String bodyVariable = !methodVariables.containsKey("body") || methodVariables.get("body").isEmpty()
					? null : methodVariables.get("body").get(0).split(" ")[1];
			final boolean pageType = method.is().stream().anyMatch(trait -> trait.name().equals("Paginated"));
			final String responseType = generator.getJavaType(method.responses().stream()
					.filter(response -> response.code().value().startsWith("2"))
					.map(response -> GeneratorUtil.getMemberType(response.body().get(0))).findFirst().orElse("string"),
					CodeGenerator.DEFAULT_TRANSPORT_PACKAGE, pageType);

			method.responses().stream().forEach(response -> {
				final String methodName = "test" + GeneratorUtil.getTitleCaseFromCamelCase(method.method())
						+ GeneratorUtil.getTitleCaseFromCamelCase(resource.displayName().value())
						+ response.code().value();

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

				if (headerVariables != null) {
					methods.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT2)
							.append("final HttpHeaders headers = new HttpHeaders();");
					generator.addImport("org.springframework.http.HttpHeaders");

					headerVariables.forEach(header -> {
						methods.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT2).append("headers.set(\"")
								.append(header).append("\", ").append(header).append(");");
					});

					methods.append(CodeGenerator.NEWLINE);
				}

				methods.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT2).append("final ResponseEntity<")
						.append(responseType)
						.append("> response = restTemplate.exchange(uriBuilder.build().encode().toUriString(), ")
						.append(getHttpMethod(method.method())).append(", ");

				if (bodyVariable == null && headerVariables == null) {
					methods.append("null, ");
				} else {
					if (headerVariables == null) {
						methods.append("new HttpEntity<>(").append(bodyVariable).append("), ");
					} else if (bodyVariable == null) {
						methods.append("new HttpEntity<>(headers), ");
					} else {
						methods.append("new HttpEntity<>(").append(bodyVariable).append(", headers), ");
					}
					
					generator.addImport("org.springframework.http.HttpEntity");
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
