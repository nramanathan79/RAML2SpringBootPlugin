package com.easyapp.raml2springbootplugin.generate;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.resources.Resource;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;

public class GenerateExceptions {
	private final Api api;
	private final CodeGenConfig codeGenConfig;
	private final Set<String> responseCodes = new HashSet<>();

	private void getResourceErrorResponses(final Resource resource) {
		resource.methods().stream().forEach(method -> {
			responseCodes.addAll(method.responses().stream().map(response -> response.code().value())
					.filter(responseCode -> !responseCode.equals("200")).collect(Collectors.toSet()));
		});

		resource.resources().stream().forEach(subResource -> getResourceErrorResponses(subResource));
	}

	public GenerateExceptions(final Api api, final CodeGenConfig codeGenConfig) {
		this.api = api;
		this.codeGenConfig = codeGenConfig;
	}

	public void create() {
		api.resources().stream().forEach(resource -> getResourceErrorResponses(resource));

		responseCodes.stream().forEach(responseCode -> {
			String exceptionClassName = GeneratorUtil.getExceptionClassName(responseCode);
			CodeGenerator generator = new CodeGenerator(codeGenConfig.getSourceDirectory(),
					codeGenConfig.getBasePackage() + ".exception", null, false, exceptionClassName, "Exception", null);

			final StringBuffer block = new StringBuffer();
			block.append(CodeGenerator.INDENT1).append("private static final long serialVersionUID = 1L;")
					.append(CodeGenerator.NEWLINE).append(CodeGenerator.NEWLINE);

			block.append(CodeGenerator.INDENT1).append("public ").append(exceptionClassName).append("() {")
					.append(CodeGenerator.NEWLINE);
			block.append(CodeGenerator.INDENT2).append("super();").append(CodeGenerator.NEWLINE);
			block.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE).append(CodeGenerator.NEWLINE);

			block.append(CodeGenerator.INDENT1).append("public ").append(exceptionClassName)
					.append("(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {")
					.append(CodeGenerator.NEWLINE);
			block.append(CodeGenerator.INDENT2).append("super(message, cause, enableSuppression, writableStackTrace);")
					.append(CodeGenerator.NEWLINE);
			block.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE).append(CodeGenerator.NEWLINE);

			block.append(CodeGenerator.INDENT1).append("public ").append(exceptionClassName)
					.append("(final String message, final Throwable cause) {").append(CodeGenerator.NEWLINE);
			block.append(CodeGenerator.INDENT2).append("super(message, cause);").append(CodeGenerator.NEWLINE);
			block.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE).append(CodeGenerator.NEWLINE);

			block.append(CodeGenerator.INDENT1).append("public ").append(exceptionClassName)
					.append("(final String message) {").append(CodeGenerator.NEWLINE);
			block.append(CodeGenerator.INDENT2).append("super(message);").append(CodeGenerator.NEWLINE);
			block.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE).append(CodeGenerator.NEWLINE);

			block.append(CodeGenerator.INDENT1).append("public ").append(exceptionClassName)
					.append("(final Throwable cause) {").append(CodeGenerator.NEWLINE);
			block.append(CodeGenerator.INDENT2).append("super(cause);").append(CodeGenerator.NEWLINE);
			block.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE);

			generator.addCodeBlock(block.toString());
			generator.writeCode();
		});
	}
}
