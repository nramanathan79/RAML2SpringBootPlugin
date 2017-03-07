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

	private final String sourceDirectory;
	private final String basePackageName;
	private final String packageName;
	private final boolean overwriteFile;
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

	public String getJavaDataType(final JDBCType dataType, final String tableName) throws Exception {
		String javaDataType = GeneratorUtil.getJavaDataType(dataType);

		if (javaDataType.endsWith("Date") || javaDataType.endsWith("Time")) {
			addImport("java.time." + javaDataType);
			GeneratorUtil.createAttributeConverter(sourceDirectory, basePackageName + ".repository", javaDataType,
					overwriteFile);
		} else if (javaDataType.equals("Unknown") && dataType == JDBCType.JAVA_OBJECT) {
			javaDataType = GeneratorUtil.getTitleCase(tableName, "_") + "Id";
			addImport(basePackageName + ".entity." + javaDataType);
		}

		return javaDataType;
	}

	public CodeGenerator(final String sourceDirectory, final String basePackageName, final String packageNameSuffix,
			final List<String> classAnnotations, final boolean isInterface, final String className,
			final String extendsFrom, final List<String> implementsList, final boolean overwriteFile) {
		this.sourceDirectory = sourceDirectory;
		this.basePackageName = basePackageName;
		this.packageName = StringUtils.isEmpty(packageNameSuffix) ? basePackageName
				: basePackageName + "." + packageNameSuffix;
		this.overwriteFile = overwriteFile;

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

	public void addMembers(final List<ColumnDefinition> columns, final Table table) throws Exception {
		final StringBuffer fields = new StringBuffer();
		final StringBuffer methods = new StringBuffer();

		if (columns != null) {
			addImport("javax.persistence.Column");

			for (final ColumnDefinition column : columns) {
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
							.append("\")");
				}

				fields.append(NEWLINE).append(INDENT1).append("private ").append(memberType).append(" ")
						.append(memberName).append(";").append(NEWLINE);

				methods.append(NEWLINE).append(INDENT1).append("public ").append(memberType).append(" get")
						.append(memberTitleCase).append("() {").append(NEWLINE);
				methods.append(INDENT2).append("return ").append(memberName).append(";").append(NEWLINE);
				methods.append(INDENT1).append("}").append(NEWLINE).append(NEWLINE);

				methods.append(INDENT1).append("public void set").append(memberTitleCase).append("(final ")
						.append(memberType).append(" ").append(memberName).append(") {").append(NEWLINE);
				methods.append(INDENT2).append("this.").append(memberName).append(" = ").append(memberName).append(";")
						.append(NEWLINE);
				methods.append(INDENT1).append("}").append(NEWLINE);
			}
		}

		if (table.getRelationships() != null) {
			table.getRelationships().stream().forEach(relationship -> {
				String objectType = relationship.getObjectType();

				if ("OneToOne".equals(relationship.getRelationshipType())
						|| "OneToMany".equals(relationship.getRelationshipType())) {
					objectType = "List<" + objectType + ">";

					fields.append(NEWLINE).append(INDENT1).append("@").append(relationship.getRelationshipType())
							.append("(mappedBy = \"").append(relationship.getMappedBy()).append("\", fetch = ")
							.append(relationship.getFetchType()).append(", cascade = ")
							.append(relationship.getCascadeType()).append(")").append(NEWLINE);
					fields.append(INDENT1).append("private ").append(objectType).append(" ")
							.append(relationship.getObjectName()).append(";").append(NEWLINE);
					addImport("java.util.List");
				} else {
					fields.append(NEWLINE).append(INDENT1).append("@").append(relationship.getRelationshipType())
							.append("(fetch = ").append(relationship.getFetchType()).append(", cascade = ")
							.append(relationship.getCascadeType()).append(")").append(NEWLINE);

					if (relationship.getJoins().size() > 1) {
						fields.append(INDENT1).append("@JoinColumns({")
								.append(relationship.getJoins().stream()
										.map(join -> "@JoinColumn(name = \"" + join.getColumnName()
												+ "\", referencedColumnName = \"" + join.getReferencedColumnName()
												+ "\", nullable = true, updatable = false, insertable = false)")
										.collect(Collectors.joining(", ")))
								.append("})").append(NEWLINE);
						addImport("javax.persistence.JoinColumns");
					} else {
						fields.append(INDENT1).append("@JoinColumn(name = \"")
								.append(relationship.getJoins().get(0).getColumnName())
								.append("\", referencedColumnName = \"")
								.append(relationship.getJoins().get(0).getReferencedColumnName())
								.append("\", nullable = true, updatable = false, insertable = false)").append(NEWLINE);
					}

					fields.append(INDENT1).append("private ").append(objectType).append(" ")
							.append(relationship.getObjectName()).append(";").append(NEWLINE);
					addImport("javax.persistence.JoinColumn");
				}

				addImport("javax.persistence." + relationship.getRelationshipType());
				addImport("javax.persistence.FetchType");
				addImport("javax.persistence.CascadeType");

				final String memberName = relationship.getObjectName();
				final String memberTitleCase = Character.toUpperCase(memberName.charAt(0)) + memberName.substring(1);

				methods.append(NEWLINE).append(INDENT1).append("public ").append(objectType).append(" get")
						.append(memberTitleCase).append("() {").append(NEWLINE);
				methods.append(INDENT2).append("return ").append(memberName).append(";").append(NEWLINE);
				methods.append(INDENT1).append("}").append(NEWLINE).append(NEWLINE);

				methods.append(INDENT1).append("public void set").append(memberTitleCase).append("(final ")
						.append(objectType).append(" ").append(memberName).append(") {").append(NEWLINE);
				methods.append(INDENT2).append("this.").append(memberName).append(" = ").append(memberName).append(";")
						.append(NEWLINE);
				methods.append(INDENT1).append("}").append(NEWLINE);
			});
		}

		codeBlocks.add(fields.toString() + methods.toString());
	}

	public void addCodeBlock(final String block) {
		codeBlocks.add(block);
	}
}
