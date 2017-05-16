package com.easyapp.raml2springbootplugin.generate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.resources.Resource;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;
import com.easyapp.raml2springbootplugin.config.ExternalConfig.JpaConfig.Table;
import com.easyapp.raml2springbootplugin.generate.util.ColumnDefinition;
import com.easyapp.raml2springbootplugin.generate.util.DatabaseUtil;
import com.easyapp.raml2springbootplugin.generate.util.GeneratorUtil;
import com.easyapp.raml2springbootplugin.generate.util.TableDefinition;
import com.easyapp.raml2springbootplugin.generate.util.TransportDefinition;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

public class GenerateJPA {
	private final List<TransportDefinition> transportTypes = new ArrayList<>();
	private final CodeGenConfig codeGenConfig;

	private void generateEmbeddable(final List<ColumnDefinition> columns, final Table table) throws Exception {
		final String className = GeneratorUtil.getTitleCase(table.getTableName(), "_") + "Id";
		final CodeGenerator generator = new CodeGenerator(codeGenConfig, "entity", Arrays.asList("@Embeddable"), false,
				className, null, Arrays.asList("Serializable"), false);

		generator.addImport("javax.persistence.Embeddable");
		generator.addImport("java.io.Serializable");

		generator.addCodeBlock(CodeGenerator.INDENT1 + "private static final long serialVersionUID = 1L;");
		generator.addMembers(columns, new Table(table.getTableName()));

		final StringBuffer constructors = new StringBuffer();
		constructors.append(CodeGenerator.INDENT1).append("public ").append(className).append("() {")
				.append(CodeGenerator.NEWLINE);
		constructors.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE)
				.append(CodeGenerator.NEWLINE);

		constructors.append(CodeGenerator.INDENT1).append("public ").append(className).append("(final String key) {")
				.append(CodeGenerator.NEWLINE);
		constructors.append(CodeGenerator.INDENT2).append("try {").append(CodeGenerator.NEWLINE);
		constructors.append(CodeGenerator.INDENT3)
				.append("final String[] tokens = URLDecoder.decode(key, \"UTF-8\").split(\"~\");")
				.append(CodeGenerator.NEWLINE);
		generator.addImport("java.net.URLDecoder");

		final AtomicInteger index = new AtomicInteger(0);
		columns.forEach(column -> {
			constructors.append(CodeGenerator.INDENT3).append("this.")
					.append(GeneratorUtil.getCamelCase(column.getColumnName(), "_")).append(" = new ")
					.append(GeneratorUtil.getJavaDataType(column.getDataType())).append("(URLDecoder.decode(tokens[")
					.append(index.getAndAdd(1)).append("], \"UTF-8\"));").append(CodeGenerator.NEWLINE);
		});

		constructors.append(CodeGenerator.INDENT2).append("} catch (Exception e) {").append(CodeGenerator.NEWLINE);
		constructors.append(CodeGenerator.INDENT3).append("e.printStackTrace();").append(CodeGenerator.NEWLINE);
		constructors.append(CodeGenerator.INDENT2).append("}").append(CodeGenerator.NEWLINE);
		constructors.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE)
				.append(CodeGenerator.NEWLINE);

		constructors.append(CodeGenerator.INDENT1).append("public ").append(className).append("(")
				.append(columns.stream()
						.map(column -> "final " + GeneratorUtil.getJavaDataType(column.getDataType()) + " "
								+ GeneratorUtil.getCamelCase(column.getColumnName(), "_"))
						.collect(Collectors.joining(", ")))
				.append(") {").append(CodeGenerator.NEWLINE);

		columns.forEach(column -> {
			final String memberName = GeneratorUtil.getCamelCase(column.getColumnName(), "_");
			constructors.append(CodeGenerator.INDENT2).append("this.").append(memberName).append(" = ")
					.append(memberName).append(";").append(CodeGenerator.NEWLINE);
		});

		constructors.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE);

		generator.addCodeBlock(constructors.toString());

		final StringBuffer overrides = new StringBuffer();
		overrides.append(CodeGenerator.INDENT1).append("@Override").append(CodeGenerator.NEWLINE);
		overrides.append(CodeGenerator.INDENT1).append("public int hashCode() {").append(CodeGenerator.NEWLINE);
		overrides.append(CodeGenerator.INDENT2).append("return toString().hashCode();").append(CodeGenerator.NEWLINE);
		overrides.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE).append(CodeGenerator.NEWLINE);

		overrides.append(CodeGenerator.INDENT1).append("@Override").append(CodeGenerator.NEWLINE);
		overrides.append(CodeGenerator.INDENT1).append("public boolean equals(final Object target) {")
				.append(CodeGenerator.NEWLINE);
		overrides.append(CodeGenerator.INDENT2).append("return toString().equals(target.toString());")
				.append(CodeGenerator.NEWLINE);
		overrides.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE).append(CodeGenerator.NEWLINE);

		overrides.append(CodeGenerator.INDENT1).append("@Override").append(CodeGenerator.NEWLINE);
		overrides.append(CodeGenerator.INDENT1).append("public String toString() {").append(CodeGenerator.NEWLINE);
		overrides.append(CodeGenerator.INDENT2).append("try {").append(CodeGenerator.NEWLINE);
		overrides.append(CodeGenerator.INDENT3).append("return ")
				.append(columns.stream()
						.map(column -> "URLEncoder.encode(" + GeneratorUtil.getCamelCase(column.getColumnName(), "_")
								+ ".toString(), \"UTF-8\")")
						.collect(Collectors.joining(" + \"~\" + ")))
				.append(";").append(CodeGenerator.NEWLINE);
		overrides.append(CodeGenerator.INDENT2).append("} catch (Exception e) {").append(CodeGenerator.NEWLINE);
		overrides.append(CodeGenerator.INDENT3).append("return \"\";").append(CodeGenerator.NEWLINE);
		overrides.append(CodeGenerator.INDENT2).append("}").append(CodeGenerator.NEWLINE);
		overrides.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE);

		generator.addImport("java.net.URLEncoder");
		generator.addCodeBlock(overrides.toString());
		generator.writeCode();
	}

	private void generateEntity(final List<ColumnDefinition> columns, final Table table) throws Exception {
		final CodeGenerator generator = new CodeGenerator(codeGenConfig, "entity",
				Arrays.asList("@Entity", "@Table(name = \"" + table.getTableName().toUpperCase() + "\")", "@Data"), false,
				GeneratorUtil.getTitleCase(table.getTableName(), "_"), null, Arrays.asList("Serializable"), false);

		generator.addImport("javax.persistence.Entity");
		generator.addImport("javax.persistence.Table");
		generator.addImport("lombok.Data");
		generator.addImport("java.io.Serializable");

		generator.addCodeBlock(CodeGenerator.INDENT1 + "private static final long serialVersionUID = 1L;");
		generator.addMembers(columns, table);
		generator.writeCode();
	}

	private void generateRepository(final String entityClassName, final String entityKeyClassName) throws Exception {
		final CodeGenerator generator = new CodeGenerator(codeGenConfig, "repository", Arrays.asList("@Repository"),
				true, entityClassName + "Repository",
				"JpaRepository<" + entityClassName + ", " + entityKeyClassName + ">", null, false);
		generator.addImport("org.springframework.stereotype.Repository");
		generator.addImport("org.springframework.data.jpa.repository.JpaRepository");
		GeneratorUtil.addMavenDependency(codeGenConfig, "org.springframework.boot", "spring-boot-starter-data-jpa",
				null, null);

		if (entityKeyClassName.endsWith("Date") || entityKeyClassName.endsWith("Time")) {
			generator.addImport("java.time." + entityKeyClassName);
		}

		generator.addImport(codeGenConfig.getBasePackage() + ".entity." + entityClassName);

		if (entityKeyClassName.startsWith(entityClassName)) {
			generator.addImport(codeGenConfig.getBasePackage() + ".entity." + entityKeyClassName);
		}

		generator.writeCode();
	}

	private String getTransformedTransport(final TableDefinition tableDefinition,
			final TransportDefinition transportType, final String entityObjectName, final String columnName,
			final String fieldName) {
		final String fieldDataType = transportType.getObjectType().properties().stream()
				.filter(property -> property.name().equals(fieldName))
				.map(property -> GeneratorUtil.getJavaPrimitiveType(property.type())).findFirst().orElse(null);

		if (fieldDataType == null) {
			throw new RuntimeException(
					"Field " + fieldName + " is not a valid RAML field for column mappings for table "
							+ tableDefinition.getTableName() + " in JPA Config");
		}

		final String columnDataType = tableDefinition.getColumns().stream()
				.filter(column -> column.getColumnName().equalsIgnoreCase(columnName))
				.map(column -> GeneratorUtil.getJavaDataType(column.getDataType())).findFirst().orElse(null);

		final boolean isFieldObjectType = transportType.getObjectType().properties().stream()
				.filter(property -> property.name().equals(fieldName))
				.map(property -> !GeneratorUtil.isScalarRAMLType(property.type())).findFirst().orElse(true);

		final List<String> fields = Arrays.asList(columnName.split("\\."));

		final String getFunctionName = entityObjectName + "." + fields.stream().map(field -> {
			if (field.equals("ARRAY_FIRST_ITEM")) {
				return "stream().findFirst().orElse(null)";
			} else {
				return "get" + GeneratorUtil.getTitleCase(field, "_") + "()";
			}
		}).collect(Collectors.joining("."));

		if (columnDataType == null) {
			final String referenceTableName = GeneratorUtil.getTitleCase(fields.get(0), "_");

			return isFieldObjectType
					? referenceTableName + "Mapper.get" + referenceTableName + "Transport(" + getFunctionName + ")"
					: getFunctionName;
		} else if (columnDataType.equals(fieldDataType)) {
			return entityObjectName + ".get" + GeneratorUtil.getTitleCase(columnName, "_") + "()";
		} else {
			return fieldDataType + ".valueOf(" + getFunctionName + ")";
		}
	}

	private String getTransformedEntity(final TableDefinition tableDefinition, final TransportDefinition transportType,
			final String transportObjectName, final String columnName, final String fieldName) {
		final String fieldDataType = transportType.getObjectType().properties().stream()
				.filter(property -> property.name().equals(fieldName))
				.map(property -> GeneratorUtil.getJavaPrimitiveType(property.type())).findFirst().orElse(null);

		final String columnDataType = tableDefinition.getColumns().stream()
				.filter(column -> column.getColumnName().equalsIgnoreCase(columnName))
				.map(column -> GeneratorUtil.getJavaDataType(column.getDataType())).findFirst().orElse(null);

		if (fieldDataType == null || columnDataType == null) {
			throw new RuntimeException("Field " + fieldName + " OR Column " + columnName
					+ " is not a valid RAML field for column mappings for table " + tableDefinition.getTableName()
					+ " in JPA Config");
		}

		final String getFunctionName = transportObjectName + ".get" + GeneratorUtil.getTitleCase(columnName, "_")
				+ "()";

		if (columnDataType.equals(fieldDataType)) {
			return getFunctionName;
		} else {
			return columnDataType + ".valueOf(" + getFunctionName + ")";
		}
	}

	private void generateEntityMappings(final Table table, final TableDefinition tableDefinition) {
		final String entityClassName = GeneratorUtil.getTitleCase(table.getTableName(), "_");
		final String entityObjectName = GeneratorUtil.getCamelCase(entityClassName, "_");

		final CodeGenerator generator = new CodeGenerator(codeGenConfig, "mapper", null, false,
				entityClassName + "Mapper", null, null, false);
		generator.addImport(codeGenConfig.getBasePackage() + ".entity." + entityClassName);

		table.getEntityMappings().forEach(entityMapping -> {
			final TransportDefinition transportType = transportTypes.stream()
					.filter(transport -> transport.getClassName().equals(entityMapping.getRamlType() + "Transport"))
					.findFirst().orElse(null);

			if (transportType == null) {
				throw new RuntimeException("Invalid RAML Type " + entityMapping.getRamlType() + " for table "
						+ table.getTableName() + " in JPA Config");
			}

			final String transportClassName = GeneratorUtil.getTitleCaseFromCamelCase(entityMapping.getRamlType())
					+ "Transport";
			final String transportObjectName = GeneratorUtil.getCamelCaseFromTitleCase(transportClassName);
			final StringBuffer method = new StringBuffer();

			generator.addImport(codeGenConfig.getBasePackage() + ".transport." + transportClassName);

			method.append(CodeGenerator.INDENT1).append("public static ").append(transportClassName).append(" get")
					.append(transportClassName).append("(final ").append(entityClassName).append(" ")
					.append(entityObjectName).append(") {").append(CodeGenerator.NEWLINE);
			method.append(CodeGenerator.INDENT2).append("final ").append(transportClassName).append(" ")
					.append(transportObjectName).append(" = new ").append(transportClassName).append("();")
					.append(CodeGenerator.NEWLINE).append(CodeGenerator.NEWLINE);

			entityMapping.getColumnMappings().entrySet().forEach(columnMapping -> {
				final String setFunctionName = "set"
						+ GeneratorUtil.getTitleCaseFromCamelCase(columnMapping.getValue());
				final String getFunctionName = getTransformedTransport(tableDefinition, transportType, entityObjectName,
						columnMapping.getKey(), columnMapping.getValue());

				method.append(CodeGenerator.INDENT2).append(transportObjectName).append(".").append(setFunctionName)
						.append("(").append(getFunctionName).append(");").append(CodeGenerator.NEWLINE);
			});

			method.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT2).append("return ")
					.append(transportObjectName).append(";").append(CodeGenerator.NEWLINE);

			method.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE);

			if (entityMapping.useForCRUD()) {
				method.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT1).append("public static ")
						.append(entityClassName).append(" get").append(entityClassName).append("Entity(final ")
						.append(transportClassName).append(" ").append(transportObjectName).append(") {")
						.append(CodeGenerator.NEWLINE);
				method.append(CodeGenerator.INDENT2).append("final ").append(entityClassName).append(" ")
						.append(entityObjectName).append(" = new ").append(entityClassName).append("();")
						.append(CodeGenerator.NEWLINE).append(CodeGenerator.NEWLINE);

				entityMapping.getColumnMappings().entrySet().forEach(columnMapping -> {
					final String setFunctionName = "set" + GeneratorUtil.getTitleCase(columnMapping.getKey(), "_");
					final String getFunctionName = getTransformedEntity(tableDefinition, transportType,
							transportObjectName, columnMapping.getKey(), columnMapping.getValue());

					method.append(CodeGenerator.INDENT2).append(entityObjectName).append(".").append(setFunctionName)
							.append("(").append(getFunctionName).append(");").append(CodeGenerator.NEWLINE);
				});

				method.append(CodeGenerator.NEWLINE).append(CodeGenerator.INDENT2).append("return ")
						.append(entityObjectName).append(";").append(CodeGenerator.NEWLINE);

				method.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE);
			}

			generator.addCodeBlock(method.toString());
		});

		generator.writeCode();
	}

	private void getRAMLTypes(final Resource resource) {
		resource.methods().forEach(method -> {
			method.responses().stream().filter(response -> response.code().value().startsWith("2"))
					.forEach(response -> {
						response.body().stream().filter(body -> !body.type().contains("-"))
								.filter(body -> !GeneratorUtil.isScalarRAMLType(body.type()))
								.forEach(body -> GeneratorUtil.addToMap(transportTypes, body, response.code().value()));
					});
		});

		resource.resources().forEach(subResource -> getRAMLTypes(subResource));
	}

	public GenerateJPA(final Api api, final CodeGenConfig codeGenConfig) {
		api.resources().forEach(resource -> getRAMLTypes(resource));
		this.codeGenConfig = codeGenConfig;
	}

	public void create() throws Exception {
		final String driverClassName = codeGenConfig.getApplicationProperty("spring.datasource.driver-class-name");
		final String jdbcUrl = codeGenConfig.getApplicationProperty("spring.datasource.url");
		final String userName = codeGenConfig.getApplicationProperty("spring.datasource.username");
		final String password = codeGenConfig.getApplicationProperty("spring.datasource.password");

		final DatabaseUtil databaseUtil = DatabaseUtil.getInstance(driverClassName, jdbcUrl, userName, password);

		for (final Table table : codeGenConfig.getExternalConfig().getJpaConfig().getTables()) {
			final TableDefinition tableDefinition = databaseUtil.getTableDefinition(table.getTableName());
			final String entityClassName = GeneratorUtil.getTitleCase(table.getTableName(), "_");
			String entityKeyClassName = GeneratorUtil.getTitleCase(table.getTableName(), "_") + "Id";

			if (tableDefinition.hasCompositeKey()) {
				generateEmbeddable(tableDefinition.getKeyColumns(), table);
				generateEntity(tableDefinition.getNonKeyColumns(), table);
			} else {
				generateEntity(tableDefinition.getColumns(), table);
				entityKeyClassName = GeneratorUtil
						.getJavaDataType(tableDefinition.getKeyColumns().get(0).getDataType());
			}

			generateRepository(entityClassName, entityKeyClassName);

			if (table.getEntityMappings() != null && !table.getEntityMappings().isEmpty()) {
				generateEntityMappings(table, tableDefinition);
			}
		}
	}
}
