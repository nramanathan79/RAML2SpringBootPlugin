package com.easyapp.raml2springbootplugin.generate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.resources.Resource;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;
import com.easyapp.raml2springbootplugin.generate.util.GeneratorUtil;
import com.easyapp.raml2springbootplugin.generate.util.TransportDefinition;

public class GenerateTransport {
	final Api api;
	final CodeGenConfig codeGenConfig;
	final List<TransportDefinition> transportTypes = new ArrayList<>();

	private void getTransportTypes(final Resource resource) {
		resource.methods().stream().forEach(method -> {
			method.body().stream().filter(body -> !body.type().contains("-"))
					.filter(body -> !GeneratorUtil.isScalarRAMLType(body.type()))
					.forEach(body -> GeneratorUtil.addToMap(transportTypes, body, null));

			method.responses().stream().forEach(response -> {
				response.body().stream().filter(body -> !body.type().contains("-"))
						.filter(body -> !GeneratorUtil.isScalarRAMLType(body.type()))
						.forEach(body -> GeneratorUtil.addToMap(transportTypes, body, response.code().value()));
			});
		});

		resource.resources().stream().forEach(subResource -> getTransportTypes(subResource));
	}

	private void generateTransport() {
		transportTypes.forEach(transportType -> {
			final CodeGenerator generator = new CodeGenerator(codeGenConfig, transportType.getPackageName(),
					Arrays.asList("@Data"), false, transportType.getClassName(), transportType.getExtendsFrom(),
					Arrays.asList("Serializable"), false);
			generator.addImport("lombok.Data");
			generator.addImport("java.io.Serializable");

			final StringBuffer blocks = new StringBuffer();
			blocks.append(CodeGenerator.INDENT1).append("private static final long serialVersionUID = 1L;")
					.append(CodeGenerator.NEWLINE);
			generator.addCodeBlock(blocks.toString());

			generator.addMembers(transportType.getDeclaredProperties(transportTypes), transportType.getPackageName());
			generator.writeCode();
		});
	}

	public GenerateTransport(final Api api, final CodeGenConfig codeGenConfig) {
		this.api = api;
		this.codeGenConfig = codeGenConfig;
	}

	public void create() throws Exception {
		GeneratorUtil.addMavenDependency(codeGenConfig, "org.projectlombok", "lombok", null, "provided");

		api.resources().stream().forEach(resource -> getTransportTypes(resource));
		generateTransport();
	}
}
