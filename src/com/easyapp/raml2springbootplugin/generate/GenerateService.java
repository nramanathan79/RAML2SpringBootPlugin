package com.easyapp.raml2springbootplugin.generate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.methods.Method;
import org.raml.v2.api.model.v10.resources.Resource;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;

public class GenerateService {
	private final Api api;
	private final CodeGenerator generator;

	private String getPathVariables(final List<TypeDeclaration> uriParameters) {
		if (uriParameters.isEmpty()) {
			return "";
		} else {
			return uriParameters.stream()
					.map(uriParam -> "final "
							+ generator.getJavaType(uriParam.type(), CodeGenerator.DEFAULT_TRANSPORT_PACKAGE) + " "
							+ uriParam.name())
					.collect(Collectors.joining(", "));
		}
	}

	private String getRequestParameters(final Method method) {
		if (method.queryParameters().isEmpty()) {
			return "";
		} else {
			return method.queryParameters().stream()
					.map(queryParam -> "final "
							+ generator.getJavaType(queryParam.type(), CodeGenerator.DEFAULT_TRANSPORT_PACKAGE) + " "
							+ queryParam.name())
					.collect(Collectors.joining(", "));
		}
	}

	private String getMethodParameters(final Method method) {
		final List<String> variables = new ArrayList<>();
		final String pathVariables = getPathVariables(GeneratorUtil.getURIParameters(method.resource()));
		final String requestParams = getRequestParameters(method);

		if (!("").equals(pathVariables)) {
			variables.add(pathVariables);
		}

		if (("post").equals(method.method()) || ("put").equals(method.method()) || ("patch").equals(method.method())) {
			variables
					.add("final "
							+ generator.getJavaType(
									method.body().isEmpty() ? "string"
											: (method.body().get(0).type().isEmpty() ? "string"
													: method.body().get(0).type()),
									CodeGenerator.DEFAULT_TRANSPORT_PACKAGE)
							+ " " + GeneratorUtil.getRequestBodyVariableName(method));
		}

		if (!("").equals(requestParams)) {
			variables.add(requestParams);
		}

		return variables.stream().collect(Collectors.joining(", "));
	}

	private void createResourceMethods(final Resource resource) {
		resource.methods().stream().forEach(method -> {
			final StringBuffer methods = new StringBuffer();
			final String responseType = generator.getJavaType(
					method.responses().stream().filter(response -> ("200").equals(response.code().value()))
							.map(response -> response.body().get(0).type()).findFirst().orElse("string"),
					CodeGenerator.DEFAULT_TRANSPORT_PACKAGE);

			methods.append(CodeGenerator.INDENT1).append("public ").append(responseType).append(" ")
					.append(method.method()).append(method.resource().displayName().value().replaceAll(" ", ""))
					.append("(").append(getMethodParameters(method)).append(") throws Exception {")
					.append(CodeGenerator.NEWLINE);
			methods.append(CodeGenerator.INDENT2).append("// TODO: Build Business Logic Here")
					.append(CodeGenerator.NEWLINE);
			methods.append(CodeGenerator.INDENT2).append("return null;").append(CodeGenerator.NEWLINE);
			methods.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE);

			generator.addCodeBlock(methods.toString());
		});

		resource.resources().stream().forEach(subResource -> createResourceMethods(subResource));
	}

	public GenerateService(final Api api, final CodeGenConfig codeGenConfig) {
		this.api = api;
		final String apiTitle = api.title().value().replaceAll(" ", "");

		generator = new CodeGenerator(codeGenConfig.getSourceDirectory(), codeGenConfig.getBasePackage() + ".service",
				Arrays.asList("@Service"), false, apiTitle + "Service", null, null, codeGenConfig.getExternalConfig().overwriteFiles());
		generator.addImport("org.springframework.stereotype.Service");
	}

	public void create() {
		api.resources().stream().forEach(resource -> createResourceMethods(resource));
		generator.writeCode();
	}
}
