package com.easyapp.raml2springbootplugin.generate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.methods.Method;
import org.raml.v2.api.model.v10.resources.Resource;

public class GenerateService {
	private final Api api;
	private final String apiTitle;
	private final CodeGenerator generator;

	private String getPathVariables(final Resource resource) {
		if (resource.uriParameters().isEmpty()) {
			return "";
		} else {
			return resource.uriParameters().stream()
					.map(uriParam -> "final " + generator.getJavaType(uriParam.type()) + " " + uriParam.name())
					.collect(Collectors.joining(", "));
		}
	}

	private String getRequestParameters(final Method method) {
		if (method.queryParameters().isEmpty()) {
			return "";
		} else {
			return method.queryParameters().stream()
					.map(queryParam -> "final " + generator.getJavaType(queryParam.type()) + " " + queryParam.name())
					.collect(Collectors.joining(", "));
		}
	}

	private String getMethodParameters(final Method method) {
		final List<String> variables = new ArrayList<>();
		final String pathVariables = getPathVariables(method.resource());
		final String requestParams = getRequestParameters(method);

		if (!("").equals(pathVariables)) {
			variables.add(pathVariables);
		}

		if (("post").equals(method.method()) || ("put").equals(method.method()) || ("patch").equals(method.method())) {
			variables.add("final "
					+ generator.getJavaType(method.body().isEmpty() ? "String"
							: (method.body().get(0).type().isEmpty() ? "String" : method.body().get(0).type()))
					+ " requestBody");
		}

		if (!("").equals(requestParams)) {
			variables.add(requestParams);
		}

		return variables.stream().collect(Collectors.joining(", "));
	}

	public GenerateService(final Api api, final String sourceDirectory, final String basePackage) {
		this.api = api;
		apiTitle = api.title().value().replaceAll(" ", "");
		generator = new CodeGenerator(sourceDirectory, basePackage + ".service", null, true, apiTitle + "Service", null, null);
	}

	public void create() {
		api.resources().stream().forEach(resource -> {
			resource.methods().stream().forEach(method -> {
				final StringBuffer methods = new StringBuffer();
				
				final String responseType = generator.getJavaType(method.responses().stream()
						.filter(response -> ("200").equals(response.code().value()))
						.map(response -> response.body().get(0).type()).findFirst().orElse(apiTitle + "Response"));

				methods.append(CodeGenerator.INDENT1).append("public ").append(responseType).append(" ")
						.append(method.method()).append(resource.displayName().value().replaceAll(" ", "")).append("(")
						.append(getMethodParameters(method)).append(");").append(CodeGenerator.NEWLINE);

				generator.addCodeBlock(methods.toString());
			});
		});

		generator.writeCode();
	}
}
