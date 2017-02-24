package com.easyapp.raml2springbootplugin.generate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.datamodel.ArrayTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.resources.Resource;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;

public class GenerateTransport {
	final Api api;
	final CodeGenConfig codeGenConfig;
	final Map<String, Set<TypeDeclaration>> transportTypes = new HashMap<>();

	private void addToMap(final TypeDeclaration body, final String responseType) {
		final String key = responseType == null || "200".equals(responseType) ? CodeGenerator.DEFAULT_TRANSPORT_PACKAGE
				: CodeGenerator.ERROR_TRANSPORT_PACKAGE;

		Set<TypeDeclaration> types = transportTypes.get(key);

		if (types == null) {
			types = new HashSet<>();
		}

		if (!types.stream().anyMatch(type -> type.type().equals(body.type()))) {
			types.add(body);
			transportTypes.put(key, types);
		}
	}

	private void generateTransport(final String transportPackageName, final ObjectTypeDeclaration objectType) {
		final CodeGenerator generator = new CodeGenerator(codeGenConfig.getSourceDirectory(),
				codeGenConfig.getBasePackage() + "." + transportPackageName, null, false,
				"object".equals(objectType.type()) ? objectType.name() : objectType.type(), null,
				Arrays.asList("Serializable"), codeGenConfig.getExternalConfig().overwriteFiles());
		generator.addImport("java.io.Serializable");

		final StringBuffer blocks = new StringBuffer();
		blocks.append(CodeGenerator.INDENT1).append("private static final long serialVersionUID = 1L;")
				.append(CodeGenerator.NEWLINE);
		generator.addCodeBlock(blocks.toString());

		final Set<TypeDeclaration> members = new HashSet<>();

		objectType.properties().stream().forEach(property -> {
			ObjectTypeDeclaration propertyType = null;

			if (ArrayTypeDeclaration.class.isAssignableFrom(property.getClass())) {
				final ArrayTypeDeclaration arrayType = (ArrayTypeDeclaration) property;
				propertyType = (ObjectTypeDeclaration) arrayType.items();
			} else if (ObjectTypeDeclaration.class.isAssignableFrom(property.getClass())) {
				propertyType = (ObjectTypeDeclaration) property;
			}

			if (propertyType != null) {
				generateTransport(transportPackageName, propertyType);
			}

			members.add(property);
		});

		generator.addMembers(members, transportPackageName);

		generator.writeCode();
	}

	private void getTransportTypes(final Resource resource) {
		resource.methods().stream().forEach(method -> {
			method.body().stream().filter(body -> !body.type().contains("-")).forEach(body -> addToMap(body, null));

			method.responses().stream().forEach(response -> {
				response.body().stream().filter(body -> !body.type().contains("-"))
						.forEach(body -> addToMap(body, response.code().value()));
			});
		});

		resource.resources().stream().forEach(subResource -> getTransportTypes(subResource));
	}

	public GenerateTransport(final Api api, final CodeGenConfig codeGenConfig) {
		this.api = api;
		this.codeGenConfig = codeGenConfig;
	}

	public void create() {
		api.resources().stream().forEach(resource -> getTransportTypes(resource));

		transportTypes.entrySet().stream().forEach(transportType -> {
			transportType.getValue().stream().forEach(type -> {
				ObjectTypeDeclaration objectType = null;

				if (ArrayTypeDeclaration.class.isAssignableFrom(type.getClass())) {
					final ArrayTypeDeclaration arrayType = (ArrayTypeDeclaration) type;
					objectType = (ObjectTypeDeclaration) arrayType.items();
				} else if (ObjectTypeDeclaration.class.isAssignableFrom(type.getClass())) {
					objectType = (ObjectTypeDeclaration) type;
				}

				generateTransport(transportType.getKey(), objectType);
			});

		});
	}
}
