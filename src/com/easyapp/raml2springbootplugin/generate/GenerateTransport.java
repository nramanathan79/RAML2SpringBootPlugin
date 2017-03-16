package com.easyapp.raml2springbootplugin.generate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.datamodel.ArrayTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.resources.Resource;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;
import com.easyapp.raml2springbootplugin.generate.util.GeneratorUtil;
import com.easyapp.raml2springbootplugin.generate.util.TransportDefinition;

public class GenerateTransport {
	final Api api;
	final CodeGenConfig codeGenConfig;
	final List<TransportDefinition> transportTypes = new ArrayList<>();

	private void recursivelyAddTypes(final String packageName, final ObjectTypeDeclaration objectType,
			final boolean topLevel) {
		final String className = topLevel ? (objectType.name().contains("/") ? objectType.type() : objectType.name())
				: ("object".equals(objectType.type()) ? objectType.name() : objectType.type());

		if (transportTypes.stream().noneMatch(transportType -> className.equals(transportType.getClassName()))) {
			final String extendsFrom = className.equals(objectType.type()) ? null
					: ("object".equals(objectType.type()) ? null : objectType.type());
			transportTypes.add(new TransportDefinition(packageName, className, extendsFrom, objectType));

			if (objectType.properties() != null) {
				objectType.properties().forEach(property -> {
					ObjectTypeDeclaration propertyType = null;

					if (property instanceof ArrayTypeDeclaration) {
						final ArrayTypeDeclaration arrayType = (ArrayTypeDeclaration) property;
						propertyType = (ObjectTypeDeclaration) arrayType.items();
					} else if (property instanceof ObjectTypeDeclaration) {
						propertyType = (ObjectTypeDeclaration) property;
					}

					if (propertyType != null) {
						recursivelyAddTypes(packageName, propertyType, false);
					}
				});
			}
		}
	}

	private void addToMap(final TypeDeclaration body, final String responseCode) {
		final String packageName = responseCode == null || responseCode.startsWith("2")
				? CodeGenerator.DEFAULT_TRANSPORT_PACKAGE : CodeGenerator.ERROR_TRANSPORT_PACKAGE;

		ObjectTypeDeclaration objectType = null;

		if (body instanceof ArrayTypeDeclaration) {
			final ArrayTypeDeclaration arrayType = (ArrayTypeDeclaration) body;
			objectType = (ObjectTypeDeclaration) arrayType.items();
		} else if (body instanceof ObjectTypeDeclaration) {
			objectType = (ObjectTypeDeclaration) body;
		}

		if (objectType != null) {
			recursivelyAddTypes(packageName, objectType, true);
		}
	}

	private void getTransportTypes(final Resource resource) {
		resource.methods().stream().forEach(method -> {
			method.body().stream().filter(body -> !body.type().contains("-"))
					.filter(body -> !GeneratorUtil.isScalarRAMLType(body.type())).forEach(body -> addToMap(body, null));

			method.responses().stream().forEach(response -> {
				response.body().stream().filter(body -> !body.type().contains("-"))
						.filter(body -> !GeneratorUtil.isScalarRAMLType(body.type()))
						.forEach(body -> addToMap(body, response.code().value()));
			});
		});

		resource.resources().stream().forEach(subResource -> getTransportTypes(subResource));
	}

	private void generateTransport() {
		transportTypes.forEach(transportType -> {
			final CodeGenerator generator = new CodeGenerator(codeGenConfig, transportType.getPackageName(), null,
					false, transportType.getClassName(), transportType.getExtendsFrom(), Arrays.asList("Serializable"),
					false);
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

	public void create() {
		api.resources().stream().forEach(resource -> getTransportTypes(resource));
		generateTransport();
	}
}
