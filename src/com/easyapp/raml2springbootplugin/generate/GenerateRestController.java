package com.easyapp.raml2springbootplugin.generate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.bodies.MimeType;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.methods.Method;
import org.raml.v2.api.model.v10.resources.Resource;

public class GenerateRestController {
	private final Api api;
	private final String basePackage;
	private final String apiTitle;
	private final String apiTitleSvc;
	private final CodeGenerator generator;
	private final Map<String, String> exceptionMap = new HashMap<>();

	private String getRequestMethod(final Method method) {
		if ("get".equals(method.method())) {
			return "RequestMethod.GET";
		} else if ("post".equals(method.method())) {
			return "RequestMethod.POST";
		} else if ("put".equals(method.method())) {
			return "RequestMethod.PUT";
		} else if ("delete".equals(method.method())) {
			return "RequestMethod.DELETE";
		} else if ("patch".equals(method.method())) {
			return "RequestMethod.PATCH";
		} else if ("head".equals(method.method())) {
			return "RequestMethod.HEAD";
		} else if ("options".equals(method.method())) {
			return "RequestMethod.OPTIONS";
		} else {
			return "RequestMethod.TRACE";
		}
	}

	private String getMediaType(final Method method, final List<MimeType> mimeTypes) {
		String mediaType = mimeTypes.stream().map(mimeType -> "\"" + mimeType.value() + "\"")
				.collect(Collectors.joining(", "));

		if (mimeTypes.size() > 1) {
			mediaType = "{" + mediaType + "}";
		}

		if ("get".equals(method.method())) {
			return ", produces = " + mediaType;
		} else if ("post".equals(method.method()) || "put".equals(method.method()) || "patch".equals(method.method())) {
			return ", produces = " + mediaType + ", consumes = " + mediaType;
		} else {
			return "";
		}
	}

	private String getPathVariables(final List<TypeDeclaration> uriParameters) {
		if (uriParameters.isEmpty()) {
			return "";
		} else {
			generator.addImport("org.springframework.web.bind.annotation.PathVariable");
			return uriParameters.stream()
					.map(uriParam -> "@PathVariable(name = \"" + uriParam.name() + "\", required = "
							+ String.valueOf(uriParam.required()) + ") final "
							+ generator.getJavaType(uriParam.type(), CodeGenerator.DEFAULT_TRANSPORT_PACKAGE) + " "
							+ uriParam.name())
					.collect(Collectors.joining(", "));
		}
	}

	private String getRequestParameters(final Method method) {
		if (method.queryParameters().isEmpty()) {
			return "";
		} else {
			generator.addImport("org.springframework.web.bind.annotation.RequestParam");
			return method.queryParameters().stream()
					.map(queryParam -> "@RequestParam(name = \"" + queryParam.name() + "\", required = "
							+ String.valueOf(queryParam.required())
							+ (queryParam.defaultValue() != null && queryParam.defaultValue().trim().length() > 0
									? ", defaultValue = \"" + queryParam.defaultValue() + "\"" : "")
							+ ") final "
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
			generator.addImport("org.springframework.web.bind.annotation.RequestBody");
			variables
					.add("@RequestBody final "
							+ generator.getJavaType(
									method.body().isEmpty() ? "String"
											: (method.body().get(0).type().isEmpty() ? "String"
													: method.body().get(0).type()),
									CodeGenerator.DEFAULT_TRANSPORT_PACKAGE)
							+ " requestBody");
		}

		if (!("").equals(requestParams)) {
			variables.add(requestParams);
		}

		return variables.stream().collect(Collectors.joining(", "));
	}

	private String getMethodVariables(final Method method) {
		final List<String> variables = new ArrayList<>();

		final String pathVariables = GeneratorUtil.getURIParameters(method.resource()).stream()
				.map(uriParam -> uriParam.name()).collect(Collectors.joining(", "));

		if (!("").equals(pathVariables)) {
			variables.add(pathVariables);
		}

		if (("post").equals(method.method()) || ("put").equals(method.method()) || ("patch").equals(method.method())) {
			variables.add("requestBody");
		}

		variables.add(method.queryParameters().stream().map(queryParam -> queryParam.name())
				.collect(Collectors.joining(", ")));

		return variables.stream().collect(Collectors.joining(", "));
	}

	private void createResourceMethods(final Resource resource) {
		resource.methods().stream().forEach(method -> {
			final StringBuffer methods = new StringBuffer();

			methods.append(CodeGenerator.INDENT1).append("@RequestMapping(path = \"").append(resource.resourcePath())
					.append("\", method = ").append(getRequestMethod(method))
					.append(getMediaType(method, api.mediaType())).append(")").append(CodeGenerator.NEWLINE);

			generator.addImport("org.springframework.web.bind.annotation.RequestMapping");
			generator.addImport("org.springframework.web.bind.annotation.RequestMethod");

			final String methodName = method.method() + resource.displayName().value().replaceAll(" ", "");

			final String responseType = generator.getJavaType(
					method.responses().stream().filter(response -> ("200").equals(response.code().value()))
							.map(response -> response.body().get(0).type()).findFirst().orElse("string"),
					CodeGenerator.DEFAULT_TRANSPORT_PACKAGE);

			final String throwsClause = " throws " + method.responses().stream()
					.filter(response -> !("200").equals(response.code().value())).map(response -> {
						final String exceptionClassName = GeneratorUtil.getExceptionClassName(response.code().value());
						final String errorReturnType = generator.getJavaType(response.body().get(0).type(),
								CodeGenerator.ERROR_TRANSPORT_PACKAGE);

						exceptionMap.put(response.code().value(), exceptionClassName + "~" + errorReturnType);
						generator.addImport(basePackage + ".exception." + exceptionClassName);
						generator.addImport(basePackage + ".error." + errorReturnType);

						return exceptionClassName;
					}).collect(Collectors.joining(", "));

			methods.append(CodeGenerator.INDENT1).append("public ResponseEntity<").append(responseType).append("> ")
					.append(methodName).append("(").append(getMethodParameters(method)).append(")")
					.append((" throws ".equals(throwsClause) ? "" : throwsClause)).append(" {")
					.append(CodeGenerator.NEWLINE);

			generator.addImport("org.springframework.http.ResponseEntity");

			methods.append(CodeGenerator.INDENT2).append("return new ResponseEntity<>(").append(apiTitleSvc).append(".")
					.append(methodName).append("(").append(getMethodVariables(method)).append("), HttpStatus.OK);")
					.append(CodeGenerator.NEWLINE);

			generator.addImport("org.springframework.http.HttpStatus");

			methods.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE);

			generator.addCodeBlock(methods.toString());
		});

		resource.resources().stream().forEach(subResource -> createResourceMethods(subResource));
	}

	public GenerateRestController(final Api api, final String sourceDirectory, final String basePackage) {
		this.api = api;
		this.basePackage = basePackage;
		apiTitle = api.title().value().replaceAll(" ", "");
		apiTitleSvc = Character.toLowerCase(apiTitle.charAt(0)) + apiTitle.substring(1) + "Svc";
		generator = new CodeGenerator(sourceDirectory, basePackage + ".restcontroller",
				Arrays.asList("@RestController"), false, apiTitle + "RestController", null, null);
		generator.addImport("org.springframework.web.bind.annotation.RestController");
	}

	public void create() {
		final StringBuffer members = new StringBuffer();
		members.append(CodeGenerator.INDENT1).append("@Autowired").append(CodeGenerator.NEWLINE)
				.append(CodeGenerator.INDENT1).append("private ").append(apiTitle + "Service ").append(apiTitleSvc)
				.append(";").append(CodeGenerator.NEWLINE);
		generator.addCodeBlock(members.toString());
		generator.addImport(basePackage + ".service." + apiTitle + "Service");
		generator.addImport("org.springframework.beans.factory.annotation.Autowired");

		api.resources().stream().forEach(resource -> createResourceMethods(resource));

		exceptionMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(exception -> {
			final StringBuffer methods = new StringBuffer();
			final String[] exceptionValues = exception.getValue().split("~");

			methods.append(CodeGenerator.INDENT1).append("@ExceptionHandler(").append(exception.getValue())
					.append(".class)").append(CodeGenerator.NEWLINE);
			generator.addImport("org.springframework.web.bind.annotation.ExceptionHandler");

			methods.append(CodeGenerator.INDENT1).append("public ResponseEntity<").append(exceptionValues[1])
					.append("> when").append(exceptionValues[0]).append("(final ").append(exceptionValues[0])
					.append(" exception) {").append(CodeGenerator.NEWLINE);
			generator.addImport("org.springframework.http.ResponseEntity");

			methods.append(CodeGenerator.INDENT2).append("final ").append(exceptionValues[1])
					.append(" errorResponse = new ").append(exceptionValues[1]).append("();")
					.append(CodeGenerator.NEWLINE);
			methods.append(CodeGenerator.INDENT2).append("// TODO: Set Error Response.").append(CodeGenerator.NEWLINE)
					.append(CodeGenerator.NEWLINE);

			methods.append(CodeGenerator.INDENT2).append("return new ResponseEntity<>(errorResponse, HttpStatus.")
					.append(GeneratorUtil.getHttpStatus(exception.getKey()).name()).append(");")
					.append(CodeGenerator.NEWLINE);
			generator.addImport("org.springframework.http.HttpStatus");

			methods.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE);

			generator.addCodeBlock(methods.toString());
		});

		generator.writeCode();
	}
}
