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

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;
import com.easyapp.raml2springbootplugin.generate.util.GeneratorUtil;

public class GenerateRestController {
	private final Api api;
	private final CodeGenConfig codeGenConfig;
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
					.map(uriParam -> "@PathVariable(name = \"" + GeneratorUtil.getMemberName(uriParam)
							+ "\", required = " + String.valueOf(uriParam.required()) + ") final "
							+ generator.getJavaType(GeneratorUtil.getMemberType(uriParam),
									CodeGenerator.DEFAULT_TRANSPORT_PACKAGE, false)
							+ " " + GeneratorUtil.getMemberName(uriParam))
					.collect(Collectors.joining(", "));
		}
	}

	private String getRequestParameters(final Method method) {
		if (method.queryParameters().isEmpty()) {
			return "";
		} else {
			return method.queryParameters().stream().map(queryParam -> {
				final String queryParamType = generator.getJavaType(GeneratorUtil.getMemberType(queryParam),
						CodeGenerator.DEFAULT_TRANSPORT_PACKAGE, false);

				if (queryParamType.equals("Pageable")) {
					return "final Pageable " + GeneratorUtil.getMemberName(queryParam);
				} else {
					generator.addImport("org.springframework.web.bind.annotation.RequestParam");

					final String queryParamName = GeneratorUtil.getMemberName(queryParam);

					return "@RequestParam(name = \"" + queryParamName + "\", required = "
							+ String.valueOf(queryParam.required())
							+ (queryParam.defaultValue() != null && queryParam.defaultValue().trim().length() > 0
									? ", defaultValue = \"" + queryParam.defaultValue() + "\"" : "")
							+ ") final " + queryParamType + " " + queryParamName;
				}
			}).collect(Collectors.joining(", "));
		}
	}

	private String getRequestBodyVariable(final Method method) {
		if (("post").equals(method.method()) || ("put").equals(method.method()) || ("patch").equals(method.method())) {
			generator.addImport("org.springframework.web.bind.annotation.RequestBody");
			if (method.body().isEmpty()) {
				return "@RequestBody final String requestBody";
			} else {
				return "@RequestBody final "
						+ generator.getJavaType(GeneratorUtil.getMemberType(method.body().get(0)),
								CodeGenerator.DEFAULT_TRANSPORT_PACKAGE, false)
						+ " " + GeneratorUtil.getRequestBodyVariableName(method);
			}
		}

		return "";
	}

	private String getMethodParameters(final Method method) {
		final List<String> variables = new ArrayList<>();
		final String pathVariables = getPathVariables(GeneratorUtil.getURIParameters(method.resource()));
		final String requestBodyVariable = getRequestBodyVariable(method);
		final String requestParams = getRequestParameters(method);

		if (!("").equals(pathVariables)) {
			variables.add(pathVariables);
		}

		if (!("").equals(requestBodyVariable)) {
			variables.add(requestBodyVariable);
		}

		if (!("").equals(requestParams)) {
			variables.add(requestParams);
		}

		return variables.stream().collect(Collectors.joining(", "));
	}

	private String getMethodVariables(final Method method) {
		final List<String> variables = new ArrayList<>();

		final String pathVariables = GeneratorUtil.getURIParameters(method.resource()).stream()
				.map(GeneratorUtil::getMemberName).collect(Collectors.joining(", "));

		if (!("").equals(pathVariables)) {
			variables.add(pathVariables);
		}

		if (("post").equals(method.method()) || ("put").equals(method.method()) || ("patch").equals(method.method())) {
			variables.add(GeneratorUtil.getRequestBodyVariableName(method));
		}

		if (!method.queryParameters().isEmpty()) {
			variables.add(method.queryParameters().stream().map(GeneratorUtil::getMemberName)
					.collect(Collectors.joining(", ")));
		}

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

			final String methodName = method.method() + GeneratorUtil.getTitleCase(resource.displayName().value(), " ");
			final boolean pageType = method.is().stream().anyMatch(trait -> trait.name().equals("Paginated"));

			final String responseType = generator.getJavaType(method.responses().stream()
					.filter(response -> response.code().value().startsWith("2"))
					.map(response -> GeneratorUtil.getMemberType(response.body().get(0))).findFirst().orElse("string"),
					CodeGenerator.DEFAULT_TRANSPORT_PACKAGE, pageType);

			// Get the exceptions
			method.responses().stream().filter(response -> !response.code().value().startsWith("2"))
					.forEach(response -> {
						final String exceptionClassName = GeneratorUtil.getExceptionClassName(response.code().value());
						final String errorReturnType = generator.getJavaType(
								GeneratorUtil.getMemberType(response.body().get(0)),
								CodeGenerator.ERROR_TRANSPORT_PACKAGE, false);

						exceptionMap.put(response.code().value(), exceptionClassName + "~" + errorReturnType);
						generator.addImport(codeGenConfig.getBasePackage() + ".exception." + exceptionClassName);
					});

			methods.append(CodeGenerator.INDENT1).append("public ResponseEntity<").append(responseType).append("> ")
					.append(methodName).append("(").append(getMethodParameters(method)).append(") throws Exception {")
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

	public GenerateRestController(final Api api, final CodeGenConfig codeGenConfig) {
		this.api = api;
		this.codeGenConfig = codeGenConfig;
		apiTitle = api.title().value().replaceAll(" ", "");
		apiTitleSvc = Character.toLowerCase(apiTitle.charAt(0)) + apiTitle.substring(1) + "Svc";
		generator = new CodeGenerator(codeGenConfig, "restcontroller", Arrays.asList("@RestController"), false,
				apiTitle + "RestController", null, null, false);
		generator.addImport("org.springframework.web.bind.annotation.RestController");
	}

	public void create() {
		final StringBuffer members = new StringBuffer();
		members.append(CodeGenerator.INDENT1).append("@Autowired").append(CodeGenerator.NEWLINE)
				.append(CodeGenerator.INDENT1).append("private ").append(apiTitle + "Service ").append(apiTitleSvc)
				.append(";").append(CodeGenerator.NEWLINE);
		generator.addCodeBlock(members.toString());
		generator.addImport(codeGenConfig.getBasePackage() + ".service." + apiTitle + "Service");
		generator.addImport("org.springframework.beans.factory.annotation.Autowired");

		if (codeGenConfig.getExternalConfig().generateHealthCheck()) {
			final StringBuffer healthCheckMethod = new StringBuffer();
			healthCheckMethod.append(CodeGenerator.INDENT1)
					.append("@RequestMapping(path = \"/healthCheck\", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)")
					.append(CodeGenerator.NEWLINE);
			healthCheckMethod.append(CodeGenerator.INDENT1)
					.append("public @ResponseBody String getHealthCheck() throws Exception {")
					.append(CodeGenerator.NEWLINE);
			healthCheckMethod.append(CodeGenerator.INDENT2).append("return \"GOOD\";").append(CodeGenerator.NEWLINE);
			healthCheckMethod.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE);
			generator.addCodeBlock(healthCheckMethod.toString());
			generator.addImport("org.springframework.web.bind.annotation.RequestMapping");
			generator.addImport("org.springframework.web.bind.annotation.RequestMethod");
			generator.addImport("org.springframework.http.MediaType");
			generator.addImport("org.springframework.web.bind.annotation.ResponseBody");
		}

		api.resources().stream().forEach(resource -> createResourceMethods(resource));

		exceptionMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(exception -> {
			final StringBuffer methods = new StringBuffer();
			final String[] exceptionValues = exception.getValue().split("~");

			methods.append(CodeGenerator.INDENT1).append("@ExceptionHandler(").append(exceptionValues[0])
					.append(".class)").append(CodeGenerator.NEWLINE);
			generator.addImport("org.springframework.web.bind.annotation.ExceptionHandler");

			methods.append(CodeGenerator.INDENT1).append("public ResponseEntity<").append(exceptionValues[1])
					.append("> when").append(exceptionValues[0]).append("(final ").append(exceptionValues[0])
					.append(" exception) {").append(CodeGenerator.NEWLINE);
			generator.addImport("org.springframework.http.ResponseEntity");

			methods.append(CodeGenerator.INDENT2).append("final ").append(exceptionValues[1])
					.append(" errorResponse = new ").append(exceptionValues[1]).append("();")
					.append(CodeGenerator.NEWLINE).append(CodeGenerator.NEWLINE);

			methods.append(CodeGenerator.INDENT2).append("// TODO: Set Error Response.").append(CodeGenerator.NEWLINE)
					.append(CodeGenerator.NEWLINE);

			methods.append(CodeGenerator.INDENT2).append("return new ResponseEntity<>(errorResponse, HttpStatus.")
					.append(GeneratorUtil.getHttpStatus(exception.getKey()).name()).append(");")
					.append(CodeGenerator.NEWLINE);
			generator.addImport("org.springframework.http.HttpStatus");

			methods.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE);

			generator.addCodeBlock(methods.toString());
		});

		final StringBuffer genericExceptionBlock = new StringBuffer();
		genericExceptionBlock.append(CodeGenerator.INDENT1).append("@ExceptionHandler(Exception.class)")
				.append(CodeGenerator.NEWLINE);
		generator.addImport("org.springframework.web.bind.annotation.ExceptionHandler");

		genericExceptionBlock.append(CodeGenerator.INDENT1)
				.append("public ResponseEntity<Error> whenException(final Exception exception) {")
				.append(CodeGenerator.NEWLINE);
		generator.addImport("org.springframework.http.ResponseEntity");

		genericExceptionBlock.append(CodeGenerator.INDENT2)
				.append("return new ResponseEntity<>(new Error(exception), HttpStatus.INTERNAL_SERVER_ERROR);")
				.append(CodeGenerator.NEWLINE);
		generator.addImport("org.springframework.http.HttpStatus");

		genericExceptionBlock.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE);

		generator.addCodeBlock(genericExceptionBlock.toString());

		generator.writeCode();
	}
}
