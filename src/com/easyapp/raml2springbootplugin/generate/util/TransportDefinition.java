package com.easyapp.raml2springbootplugin.generate.util;

import java.util.ArrayList;
import java.util.List;

import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;

public class TransportDefinition {
	private String packageName;
	private String className;
	private String extendsFrom;
	private ObjectTypeDeclaration objectType;

	public TransportDefinition(final String packageName, final String className, final String extendsFrom,
			final ObjectTypeDeclaration objectType) {
		this.packageName = packageName;
		this.className = className;
		this.extendsFrom = extendsFrom;
		this.objectType = objectType;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getClassName() {
		return className;
	}

	public String getExtendsFrom() {
		return extendsFrom;
	}

	public ObjectTypeDeclaration getObjectType() {
		return objectType;
	}

	public String getType() {
		return objectType == null ? null : objectType.type();
	}

	public List<TypeDeclaration> getDeclaredProperties(final List<TransportDefinition> transportTypes) {
		if (extendsFrom != null && transportTypes != null) {
			final TransportDefinition transport = transportTypes.stream()
					.filter(transportType -> extendsFrom.equals(transportType.getClassName())).findFirst().orElse(null);

			if (transport != null && transport.getObjectType().properties() != null
					&& objectType.properties() != null) {
				final List<TypeDeclaration> declaredProperties = new ArrayList<>();
				declaredProperties.addAll(objectType.properties());

				transport.getObjectType().properties().forEach(parentProperty -> {
					declaredProperties.removeIf(property -> GeneratorUtil.getMemberName(property)
							.equals(GeneratorUtil.getMemberName(parentProperty)));
				});

				return declaredProperties;
			}
		}

		return objectType == null ? null : objectType.properties();
	}

	@Override
	public String toString() {
		return "Package = " + packageName + ", Class = " + className + ", Extends From = " + extendsFrom + ", Type = "
				+ getType();
	}
}
