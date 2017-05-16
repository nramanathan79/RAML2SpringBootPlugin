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

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;
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

	private final CodeGenConfig codeGenConfig;
	private final String packageName;
	private final Path codeFilePath;
	private final Map<String, Set<String>> imports = new HashMap<>();
	private final StringBuffer codeBlock = new StringBuffer();
	private final List<String> codeBlocks = new ArrayList<>();

	private String getJavaPrimitiveType(final String strippedFieldType, final String transportPackageName) {
		String javaDataType = GeneratorUtil.getJavaPrimitiveType(strippedFieldType);

		if (javaDataType.endsWith("Date") || javaDataType.endsWith("Time")) {
			addImport("java.time." + javaDataType);
		} else if (strippedFieldType.contains("-")) {
			addImport(strippedFieldType.replaceAll("-", "."));

			if (strippedFieldType.contains("Page")) {
				try {
					GeneratorUtil.addMavenDependency(codeGenConfig, "org.springframework.boot",
							"spring-boot-starter-data-jpa", null, null);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		} else if (strippedFieldType.equals(javaDataType)) {
			if (transportPackageName.equals(DEFAULT_TRANSPORT_PACKAGE)) {
				javaDataType += "Transport";
			}

			addImport(codeGenConfig.getBasePackage() + "." + transportPackageName + "." + javaDataType);
		}

		return javaDataType;
	}

	public String getJavaType(final String fieldType, final String transportPackageName, final boolean pageType) {
		final String strippedFieldType = fieldType == null ? "" : fieldType.replaceAll("\\[\\]", "");

		if (strippedFieldType.equals(fieldType)) {
			return getJavaPrimitiveType(strippedFieldType, transportPackageName);
		} else if (pageType) {
			addImport("org.springframework.data.domain.Page");
			return "Page<" + getJavaPrimitiveType(strippedFieldType, transportPackageName) + ">";
		} else {
			addImport("java.util.List");
			return "List<" + getJavaPrimitiveType(strippedFieldType, transportPackageName) + ">";
		}
	}

	public String getJavaDataType(final JDBCType dataType, final String tableName) {
		String javaDataType = GeneratorUtil.getJavaDataType(dataType);

		if (javaDataType.endsWith("Date") || javaDataType.endsWith("Time")) {
			addImport("java.time." + javaDataType);
			try {
				GeneratorUtil.createAttributeConverter(codeGenConfig.getSourceDirectory(),
						codeGenConfig.getBasePackage() + ".repository", javaDataType,
						codeGenConfig.getExternalConfig().overwriteFiles());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else if (javaDataType.equals("Unknown") && dataType == JDBCType.JAVA_OBJECT) {
			javaDataType = GeneratorUtil.getTitleCase(tableName, "_") + "Id";
			addImport(codeGenConfig.getBasePackage() + ".entity." + javaDataType);
		}

		return javaDataType;
	}

	public CodeGenerator(final CodeGenConfig codeGenConfig, final String packageNameSuffix,
			final List<String> classAnnotations, final boolean isInterface, final String className,
			final String extendsFrom, final List<String> implementsList, final boolean test) {
		this.codeGenConfig = codeGenConfig;
		this.packageName = StringUtils.isEmpty(packageNameSuffix) ? codeGenConfig.getBasePackage()
				: codeGenConfig.getBasePackage() + "." + packageNameSuffix;

		final File directory = new File((test ? codeGenConfig.getTestDirectory() : codeGenConfig.getSourceDirectory())
				+ File.separator + packageName.replace(".", File.separator));

		if (!directory.exists()) {
			directory.mkdirs();
		}

		String filePath = directory + File.separator + className + ".java";

		if (Files.exists(Paths.get(filePath)) && !codeGenConfig.getExternalConfig().overwriteFiles()) {
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

	public void addMembers(final List<TypeDeclaration> members, final String transportPackageName) {
		final Comparator<TypeDeclaration> byName = (e1, e2) -> e1.name().compareTo(e2.name());
		final StringBuffer fields = new StringBuffer();

		members.stream().sorted(byName).forEach(member -> {
			fields.append(INDENT1).append("private ")
					.append(getJavaType(GeneratorUtil.getMemberType(member), transportPackageName, false)).append(" ")
					.append(GeneratorUtil.getMemberName(member)).append(";").append(NEWLINE);
		});

		codeBlocks.add(fields.toString());
	}

	public void addMembers(final List<ColumnDefinition> columns, final Table table) throws Exception {
		final StringBuffer fields = new StringBuffer();

		if (columns != null) {
			addImport("javax.persistence.Column");

			columns.forEach(column -> {
				final String memberName = GeneratorUtil.getCamelCase(column.getColumnName(), "_");
				final String memberType = getJavaDataType(column.getDataType(), table.getTableName());

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
			});
		}

		if (table.getRelationships() != null) {
			table.getRelationships().stream().forEach(relationship -> {
				String memberType = GeneratorUtil.getTitleCase(relationship.getReferencedTableName(), "_");
				final String memberName = GeneratorUtil.getCamelCase(relationship.getReferencedTableName(), "_");

				if ("OneToMany".equals(relationship.getRelationshipType())
						|| ("ManyToMany".equals(relationship.getRelationshipType())
								&& relationship.getJoins() == null)) {
					memberType = "Collection<" + memberType + ">";

					fields.append(NEWLINE).append(INDENT1).append("@").append(relationship.getRelationshipType())
							.append("(mappedBy = \"").append(GeneratorUtil.getCamelCase(table.getTableName(), "_"))
							.append("\", fetch = ").append(relationship.getFetchType()).append(", cascade = ")
							.append(relationship.getCascadeType()).append(")").append(NEWLINE);

					if (!StringUtils.isEmpty(relationship.getWhereClause())) {
						fields.append(INDENT1).append("@Where(clause = \"").append(relationship.getWhereClause())
								.append("\")").append(NEWLINE);
						addImport("org.hibernate.annotations.Where");
					}

					fields.append(INDENT1).append("private ").append(memberType).append(" ").append(memberName)
							.append(";").append(NEWLINE);

					addImport("java.util.Collection");
				} else if ("ManyToMany".equals(relationship.getRelationshipType())) {
					memberType = "Collection<" + memberType + ">";

					fields.append(NEWLINE).append(INDENT1).append("@").append(relationship.getRelationshipType())
							.append("(fetch = ").append(relationship.getFetchType()).append(", cascade = ")
							.append(relationship.getCascadeType()).append(")").append(NEWLINE);

					if (!StringUtils.isEmpty(relationship.getWhereClause())) {
						fields.append(INDENT1).append("@Where(clause = \"").append(relationship.getWhereClause())
								.append("\")").append(NEWLINE);
						addImport("org.hibernate.annotations.Where");
					}

					String joinColumns = relationship.getJoinColumns();
					String inverseJoinColumns = relationship.getInverseJoinColumns();

					if (!joinColumns.startsWith("{")) {
						joinColumns = "{" + joinColumns + "}";
						inverseJoinColumns = "{" + inverseJoinColumns + "}";
					}

					fields.append(INDENT1).append("@JoinTable(name = \"").append(relationship.getReferencedTableName())
							.append("\", joinColumns = ").append(joinColumns).append(", inverseJoinColumns = ")
							.append(inverseJoinColumns).append(")").append(NEWLINE);
					fields.append(INDENT1).append("private ").append(memberType).append(" ").append(memberName)
							.append(";").append(NEWLINE);

					addImport("javax.persistence.JoinTable");
					addImport("javax.persistence.JoinColumn");
					addImport("java.util.Collection");
				} else {
					fields.append(NEWLINE).append(INDENT1).append("@").append(relationship.getRelationshipType())
							.append("(fetch = ").append(relationship.getFetchType()).append(", cascade = ")
							.append(relationship.getCascadeType()).append(")").append(NEWLINE);

					if (!StringUtils.isEmpty(relationship.getWhereClause())) {
						fields.append(INDENT1).append("@Where(clause = \"").append(relationship.getWhereClause())
								.append("\")").append(NEWLINE);
						addImport("org.hibernate.annotations.Where");
					}

					if (relationship.getJoins() != null) {
						String joinColumns = relationship.getJoinColumns();

						if (joinColumns.startsWith("{")) {
							joinColumns = "@JoinColumns(" + joinColumns + ")";
							addImport("javax.persistence.JoinColumns");
						}

						fields.append(INDENT1).append(joinColumns).append(NEWLINE);
						addImport("javax.persistence.JoinColumn");
					}

					fields.append(INDENT1).append("private ").append(memberType).append(" ").append(memberName)
							.append(";").append(NEWLINE);
				}

				addImport("javax.persistence." + relationship.getRelationshipType());
				addImport("javax.persistence.FetchType");
				addImport("javax.persistence.CascadeType");
			});
		}

		codeBlocks.add(fields.toString());
	}

	public void addCodeBlock(final String block) {
		codeBlocks.add(block);
	}
}
