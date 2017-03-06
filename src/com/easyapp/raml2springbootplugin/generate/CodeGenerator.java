package com.easyapp.raml2springbootplugin.generate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.springframework.util.StringUtils;

import com.easyapp.raml2springbootplugin.config.ExternalConfig.JpaConfig.Table;
import com.easyapp.raml2springbootplugin.generate.util.ColumnDefinition;
import com.easyapp.raml2springbootplugin.generate.util.GeneratorUtil;

public class CodeGenerator {
	public static final String NEWLINE = System.getProperty("line.separator");
	public static final String INDENT1 = "\t";
	public static final String INDENT2 = "\t\t";
	public static final String INDENT3 = "\t\t\t";
	public static final String INDENT4 = "\t\t\t\t";
	public static final String DEFAULT_TRANSPORT_PACKAGE = "transport";
	public static final String ERROR_TRANSPORT_PACKAGE = "error";

	private final String basePackageName;
	private final String packageName;
	private final Path codeFilePath;
	private final Map<String, Set<String>> imports = new HashMap<>();
	private final StringBuffer codeBlock = new StringBuffer();
	private final List<String> codeBlocks = new ArrayList<>();

	private String getJavaPrimitiveType(final String strippedFieldType, final String transportPackageName) {
		if ("string".equals(strippedFieldType) || "any".equals(strippedFieldType)) {
			return "String";
		} else if ("number".equals(strippedFieldType)) {
			return "Double";
		} else if ("integer".equals(strippedFieldType)) {
			return "Long";
		} else if ("date-only".equals(strippedFieldType)) {
			addImport("java.time.LocalDate");
			return "LocalDate";
		} else if ("time-only".equals(strippedFieldType)) {
			addImport("java.time.LocalTime");
			return "LocalTime";
		} else if ("datetime-only".equals(strippedFieldType) || "datetime".equals(strippedFieldType)) {
			addImport("java.time.LocalDateTime");
			return "LocalDateTime";
		} else if ("boolean".equals(strippedFieldType)) {
			return "Boolean";
		} else {
			if (strippedFieldType.contains("-")) {
				addImport(strippedFieldType.replaceAll("-", "."));
				return strippedFieldType.substring(strippedFieldType.lastIndexOf('-') + 1);
			} else {
				addImport(basePackageName + "." + transportPackageName + "." + strippedFieldType);
				return strippedFieldType;
			}
		}
	}

	public String getJavaType(final String fieldType, final String transportPackageName) {
		final String strippedFieldType = fieldType == null ? "" : fieldType.replaceAll("\\[\\]", "");

		if (strippedFieldType.equals(fieldType)) {
			return getJavaPrimitiveType(strippedFieldType, transportPackageName);
		} else {
			addImport("java.util.List");
			return "List<" + getJavaPrimitiveType(strippedFieldType, transportPackageName) + ">";
		}
	}

	public String getJavaDataType(final JDBCType dataType, final String tableName) {
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
			addImport("java.time.LocalDate");
			return "LocalDate";
		} else if (dataType == JDBCType.TIME) {
			addImport("java.time.LocalTime");
			return "LocalTime";
		} else if (dataType == JDBCType.TIME_WITH_TIMEZONE) {
			addImport("java.time.OffsetTime");
			return "OffsetTime";
		} else if (dataType == JDBCType.TIMESTAMP) {
			addImport("java.time.LocalDateTime");
			return "LocalDateTime";
		} else if (dataType == JDBCType.TIMESTAMP_WITH_TIMEZONE) {
			addImport("java.time.OffsetDateTime");
			return "OffsetDateTime";
		} else if (dataType == JDBCType.JAVA_OBJECT) {
			final String embeddableClassName = GeneratorUtil.getTitleCase(tableName, "_") + "Id";
			addImport(basePackageName + ".entity." + embeddableClassName);

			return embeddableClassName;
		} else {
			return "String";
		}
	}

	public CodeGenerator(final String sourceDirectory, final String basePackageName, final String packageNameSuffix,
			final List<String> classAnnotations, final boolean isInterface, final String className,
			final String extendsFrom, final List<String> implementsList, final boolean overwriteFile) {
		this.basePackageName = basePackageName;
		this.packageName = StringUtils.isEmpty(packageNameSuffix) ? basePackageName
				: basePackageName + "." + packageNameSuffix;

		final File directory = new File(sourceDirectory + File.separator + packageName.replace(".", File.separator));

		if (!directory.exists()) {
			directory.mkdirs();
		}

		String filePath = directory + File.separator + className + ".java";

		if (Files.exists(Paths.get(filePath)) && !overwriteFile) {
			filePath += ".MERGE";
		}

		this.codeFilePath = Paths.get(filePath);
		this.codeBlock
				.append(classAnnotations == null ? "" : classAnnotations.stream().collect(Collectors.joining(NEWLINE)))
				.append(classAnnotations == null ? "" : NEWLINE).append("public ")
				.append(isInterface ? "interface " : "class ").append(className)
				.append(extendsFrom == null ? "" : " extends " + extendsFrom)
				.append(implementsList == null ? ""
						: " implements " + implementsList.stream().collect(Collectors.joining(", ")))
				.append(" {" + NEWLINE);
	}

	public final void addImport(final String importResource) {
		final String org = importResource.substring(0, importResource.indexOf('.'));

		if (imports.containsKey(org)) {
			imports.get(org).add(importResource);
		} else {
			final Set<String> importSet = new HashSet<>();
			importSet.add(importResource);
			imports.put(org, importSet);
		}
	}

	public final void writeCode() {
		codeBlock.append(codeBlocks.stream().collect(Collectors.joining(NEWLINE))).append("}").append(NEWLINE);

		try {
			final BufferedWriter writer = Files.newBufferedWriter(codeFilePath);

			writer.write("package " + packageName + ";");
			writer.newLine();
			writer.newLine();

			if (imports.size() > 0) {
				imports.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(org -> {
					try {
						writer.write(
								org.getValue().stream().sorted().map(importResource -> "import " + importResource + ";")
										.collect(Collectors.joining(NEWLINE)));
						writer.newLine();
						writer.newLine();
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}

			writer.write(codeBlock.toString());

			writer.flush();
			writer.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public void addMembers(final Set<TypeDeclaration> members, final String transportPackageName) {
		final Comparator<TypeDeclaration> byName = (e1, e2) -> e1.name().compareTo(e2.name());
		final StringBuffer fields = new StringBuffer();

		members.stream().sorted(byName).forEach(member -> {
			fields.append(INDENT1).append("private ").append(getJavaType(member.type(), transportPackageName))
					.append(" ").append(member.name()).append(";").append(NEWLINE);
		});

		codeBlocks.add(fields.toString());

		members.stream().sorted(byName).forEach(member -> {
			final StringBuffer methods = new StringBuffer();
			final String memberType = getJavaType(member.type(), transportPackageName);
			final String functionName = Character.toUpperCase(member.name().charAt(0)) + member.name().substring(1);

			methods.append(INDENT1).append("public ").append(memberType).append(" get").append(functionName)
					.append("() {").append(NEWLINE);
			methods.append(INDENT2).append("return ").append(member.name()).append(";").append(NEWLINE);
			methods.append(INDENT1).append("}").append(NEWLINE).append(NEWLINE);

			methods.append(INDENT1).append("public void set").append(functionName).append("(final ").append(memberType)
					.append(" ").append(member.name()).append(") {").append(NEWLINE);
			methods.append(INDENT2).append("this.").append(member.name()).append(" = ").append(member.name())
					.append(";").append(NEWLINE);
			methods.append(INDENT1).append("}").append(NEWLINE);

			if (memberType.startsWith("List<")) {
				methods.append(NEWLINE).append(INDENT1).append("public void add").append(functionName).append("(final ")
						.append(memberType.substring(5, memberType.length() - 1)).append(" ").append(member.name())
						.append(") {").append(NEWLINE);
				methods.append(INDENT2).append("this.").append(member.name()).append(".add(").append(member.name())
						.append(");").append(NEWLINE);
				methods.append(INDENT1).append("}").append(NEWLINE);
			}

			codeBlocks.add(methods.toString());
		});
	}

	public void addMembers(final List<ColumnDefinition> columns, final Table table) {
		final StringBuffer fields = new StringBuffer();
		final StringBuffer methods = new StringBuffer();

		if (columns != null) {
			addImport("javax.persistence.Column");

			columns.forEach(column -> {
				final String memberName = GeneratorUtil.getCamelCase(column.getColumnName(), "_");
				final String memberType = getJavaDataType(column.getDataType(), table.getTableName());
				final String memberTitleCase = GeneratorUtil.getTitleCase(column.getColumnName(), "_");

				if (column.isInPrimaryKey()) {
					if (column.getDataType() == JDBCType.JAVA_OBJECT) {
						fields.append(NEWLINE).append(INDENT1).append("@EmbeddedId");
						addImport("javax.persistence.EmbeddedId");
					} else {
						fields.append(NEWLINE).append(INDENT1).append("@Id");
						addImport("javax.persistence.Id");

						if (table.getSequenceName() != null) {
							final String sequenceName = GeneratorUtil.getTitleCase(table.getTableName(), "_") + "Seq";
							fields.append(NEWLINE).append(INDENT1).append("@GeneratedValue(generator = \"")
									.append(sequenceName).append("\")");
							fields.append(NEWLINE).append(INDENT1).append("@SequenceGenerator(name = \"")
									.append(sequenceName).append("\", sequenceName = \"")
									.append(table.getSequenceName()).append("\", allocationSize = ")
									.append(table.getSequenceIncrement()).append(")");
							addImport("javax.persistence.GeneratedValue");
							addImport("javax.persistence.SequenceGenerator");
						}
					}
				}

				if (column.getDataType() != JDBCType.JAVA_OBJECT) {
					fields.append(NEWLINE).append(INDENT1).append("@Column(name = \"").append(column.getColumnName())
							.append("\")").append(NEWLINE);
				}

				fields.append(INDENT1).append("private ").append(memberType).append(" ").append(memberName).append(";")
						.append(NEWLINE);

				methods.append(NEWLINE).append(INDENT1).append("public ").append(memberType).append(" get")
						.append(memberTitleCase).append("() {").append(NEWLINE);
				methods.append(INDENT2).append("return ").append(memberName).append(";").append(NEWLINE);
				methods.append(INDENT1).append("}").append(NEWLINE).append(NEWLINE);

				methods.append(INDENT1).append("public void set").append(memberTitleCase).append("(final ")
						.append(memberType).append(" ").append(memberName).append(") {").append(NEWLINE);
				methods.append(INDENT2).append("this.").append(memberName).append(" = ").append(memberName).append(";")
						.append(NEWLINE);
				methods.append(INDENT1).append("}").append(NEWLINE);
			});
		}

		if (table.getRelationships() != null) {
			table.getRelationships().stream().forEach(relationship -> {
				// TODO: Add relationships
			});
		}

		codeBlocks.add(fields.toString() + methods.toString());
	}

	public void addCodeBlock(final String block) {
		codeBlocks.add(block);
	}
}
