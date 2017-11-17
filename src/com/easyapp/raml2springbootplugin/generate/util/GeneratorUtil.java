package com.easyapp.raml2springbootplugin.generate.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.raml.v2.api.model.v10.datamodel.ArrayTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.methods.Method;
import org.raml.v2.api.model.v10.resources.Resource;
import org.springframework.http.HttpStatus;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;
import com.easyapp.raml2springbootplugin.generate.CodeGenerator;

public class GeneratorUtil {
	private static final Set<String> attributeConvertersCompleted = new HashSet<>();

	private static void addURIParameters(final Resource resource, final List<TypeDeclaration> uriParameters) {
		if (resource != null) {
			addURIParameters(resource.parentResource(), uriParameters);
			uriParameters.addAll(resource.uriParameters());
		}
	}
	
	public static void initialize() {
		attributeConvertersCompleted.clear();
	}

	public static String getRequestBodyVariableName(final Method method) {
		if (method.body().isEmpty()) {
			return "requestBody";
		} else {
			final TypeDeclaration methodBody = method.body().get(0);
			return (methodBody.displayName() == null || methodBody.displayName().value().isEmpty() ? "requestBody"
					: methodBody.displayName().value());
		}
	}

	public static List<TypeDeclaration> getHeaders(final Method method) {
		List<TypeDeclaration> headers = new ArrayList<>();
		
		if (method != null) {
			headers.addAll(method.headers());
		}

		return headers;
	}

	public static List<TypeDeclaration> getURIParameters(final Resource resource) {
		List<TypeDeclaration> uriParameters = new ArrayList<>();
		addURIParameters(resource, uriParameters);

		return uriParameters;
	}

	public static String getTitleCaseFromCamelCase(final String text) {
		return text != null && text.length() > 0 ? Character.toUpperCase(text.charAt(0)) + text.substring(1) : text;
	}

	public static String getCamelCaseFromTitleCase(final String text) {
		return text != null && text.length() > 0 ? Character.toLowerCase(text.charAt(0)) + text.substring(1) : text;
	}

	public static String getTitleCase(final String text, final String delimiter) {
		String returnValue = "";

		if (text != null) {
			for (final String word : text.trim().split(delimiter)) {
				returnValue += Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
			}
		}

		return returnValue;
	}
	
	public static String getCamelCase(final String text, final String delimiter) {
		return getCamelCaseFromTitleCase(getTitleCase(text, delimiter));
	}

	public static void validateAndUpdateMavenDependency(final CodeGenConfig codeGenConfig) throws Exception {
		final MavenXpp3Reader mavenReader = new MavenXpp3Reader();
		final Model pomModel = mavenReader.read(new FileReader(codeGenConfig.getPomFilePath()));
		final Dependency starterDependency = pomModel.getDependencies().stream()
				.filter(dependency -> dependency.getArtifactId().equals("spring-boot-starter")).findAny().orElse(null);

		if (starterDependency != null) {
			starterDependency.setArtifactId("spring-boot-starter-web");
			final MavenXpp3Writer writer = new MavenXpp3Writer();

			if (codeGenConfig.getExternalConfig().overwriteFiles()) {
				writer.write(new FileWriter(codeGenConfig.getPomFilePath()), pomModel);
			} else {
				writer.write(new FileWriter(codeGenConfig.getPomFilePath() + ".MERGE"), pomModel);
			}
		}
	}

	public static void addMavenDependency(final CodeGenConfig codeGenConfig, final String groupId,
			final String artifactId, final String version, final String scope) throws Exception {
		final MavenXpp3Reader mavenReader = new MavenXpp3Reader();
		final Model pomModel = mavenReader.read(new FileReader(codeGenConfig.getPomFilePath()));

		if (pomModel.getDependencies().stream().filter(
				dependency -> dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId))
				.findAny().orElse(null) == null) {
			final Dependency dependency = new Dependency();

			dependency.setGroupId(groupId);
			dependency.setArtifactId(artifactId);

			if (version != null) {
				dependency.setVersion(version);
			}

			if (scope != null) {
				dependency.setScope(scope);
			}

			pomModel.addDependency(dependency);

			final MavenXpp3Writer writer = new MavenXpp3Writer();

			if (codeGenConfig.getExternalConfig().overwriteFiles()) {
				writer.write(new FileWriter(codeGenConfig.getPomFilePath()), pomModel);
			} else {
				writer.write(new FileWriter(codeGenConfig.getPomFilePath() + ".MERGE"), pomModel);
			}
		}
	}

	public static String getHttpStatusPhrase(final String httpCode) {
		return getHttpStatus(httpCode).getReasonPhrase();
	}

	public static HttpStatus getHttpStatus(final String httpCode) {
		return HttpStatus.valueOf(Integer.valueOf(httpCode));
	}

	public static String getExceptionClassName(final String httpCode) {
		return getTitleCase(getHttpStatus(httpCode).name(), "_") + "Exception";
	}

	public static String getJavaPrimitiveType(final String strippedFieldType) {
		if ("string".equals(strippedFieldType) || "any".equals(strippedFieldType)) {
			return "String";
		} else if ("number".equals(strippedFieldType)) {
			return "Double";
		} else if ("integer".equals(strippedFieldType)) {
			return "Long";
		} else if ("date-only".equals(strippedFieldType)) {
			return "LocalDate";
		} else if ("time-only".equals(strippedFieldType)) {
			return "LocalTime";
		} else if ("datetime-only".equals(strippedFieldType)) {
			return "LocalDateTime";
		} else if ("datetime".equals(strippedFieldType)) {
			return "OffsetDateTime";
		} else if ("boolean".equals(strippedFieldType)) {
			return "Boolean";
		} else if ("nil".equals(strippedFieldType)) {
			return "Void";
		} else if (strippedFieldType.contains("-")) {
			return strippedFieldType.substring(strippedFieldType.lastIndexOf('-') + 1);
		} else {
			return strippedFieldType;
		}
	}

	public static String getJavaDataType(final JDBCType dataType) {
		if (dataType == JDBCType.CHAR || dataType == JDBCType.VARCHAR || dataType == JDBCType.NCHAR
				|| dataType == JDBCType.NVARCHAR || dataType == JDBCType.LONGVARCHAR
				|| dataType == JDBCType.LONGNVARCHAR || dataType == JDBCType.ROWID || dataType == JDBCType.SQLXML
				|| dataType == JDBCType.STRUCT) {
			return "String";
		} else if (dataType == JDBCType.BIGINT || dataType == JDBCType.INTEGER || dataType == JDBCType.SMALLINT
				|| dataType == JDBCType.TINYINT) {
			return "Long";
		} else if (dataType == JDBCType.DECIMAL || dataType == JDBCType.DOUBLE || dataType == JDBCType.FLOAT
				|| dataType == JDBCType.NUMERIC || dataType == JDBCType.REAL) {
			return "Double";
		} else if (dataType == JDBCType.BOOLEAN || dataType == JDBCType.BIT) {
			return "Boolean";
		} else if (dataType == JDBCType.DATE) {
			return "LocalDate";
		} else if (dataType == JDBCType.TIME) {
			return "LocalTime";
		} else if (dataType == JDBCType.TIME_WITH_TIMEZONE) {
			return "OffsetTime";
		} else if (dataType == JDBCType.TIMESTAMP) {
			return "LocalDateTime";
		} else if (dataType == JDBCType.TIMESTAMP_WITH_TIMEZONE) {
			return "OffsetDateTime";
		} else {
			return "Unknown";
		}
	}

	public static void createAttributeConverter(final String sourceDirectory, final String packageName,
			final String entityKeyClassName, final boolean overwriteFiles) throws Exception {
		if (attributeConvertersCompleted.contains(entityKeyClassName)) {
			return;
		}

		final File directory = new File(sourceDirectory + File.separator + packageName.replace(".", File.separator));

		if (!directory.exists()) {
			directory.mkdirs();
		}

		String attributeConverterFilePath = directory + File.separator + entityKeyClassName + "AttributeConverter.java";

		if (Files.exists(Paths.get(attributeConverterFilePath)) && !overwriteFiles) {
			attributeConverterFilePath += ".MERGE";
		}

		final FileWriter attributeConverterFileWriter = new FileWriter(attributeConverterFilePath);
		attributeConverterFileWriter
				.write("package " + packageName + ";" + CodeGenerator.NEWLINE + CodeGenerator.NEWLINE);

		final BufferedReader attributeConverterFileReader = new BufferedReader(new InputStreamReader(
				new URL("platform:/plugin/RAML2SpringBootPlugin/" + entityKeyClassName + "AttributeConverter")
						.openStream()));
		String line = null;

		while ((line = attributeConverterFileReader.readLine()) != null) {
			attributeConverterFileWriter.write(line + CodeGenerator.NEWLINE);
		}

		attributeConverterFileWriter.flush();
		attributeConverterFileWriter.close();
		attributeConverterFileReader.close();

		attributeConvertersCompleted.add(entityKeyClassName);
	}

	public static boolean isScalarRAMLType(final String type) {
		return ("string".equals(type) || "boolean".equals(type) || "number".equals(type) || "integer".equals(type)
				|| "date-only".equals(type) || "time-only".equals(type) || "datetime-only".equals(type)
				|| "datetime".equals(type) || "null".equals(type) || "file".equals(type));
	}

	public static String getMemberName(final TypeDeclaration member) {
		if (member.name().endsWith("?")) {
			return member.name().substring(0, member.name().length() - 1);
		} else {
			return member.name();
		}
	}

	public static String getMemberType(final TypeDeclaration member) {
		if (member.type() == null || member.type().isEmpty()) {
			return "string";
		} else if (!isScalarRAMLType(member.type()) && isScalarRAMLType(member.parentTypes().get(0).type())) {
			return member.parentTypes().get(0).type();
		} else {
			return member.type();
		}
	}

	private static void recursivelyAddTypes(final List<TransportDefinition> transportTypes, final String packageName,
			final ObjectTypeDeclaration objectType, final boolean topLevel) {
		final String className = topLevel
				? (objectType.name().contains("/") ? objectType.type() : GeneratorUtil.getMemberName(objectType))
				: ("object".equals(objectType.type()) ? GeneratorUtil.getMemberName(objectType) : objectType.type());

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
						recursivelyAddTypes(transportTypes, packageName, propertyType, false);
					}
				});
			}
		}
	}

	public static void addToMap(final List<TransportDefinition> transportTypes, final TypeDeclaration body,
			final String responseCode) {
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
			recursivelyAddTypes(transportTypes, packageName, objectType, true);
		}
	}
}
