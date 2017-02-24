package com.easyapp.raml2springbootplugin.generate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;

public class CodeGenerator {
	public static final String NEWLINE = System.getProperty("line.separator");
	public static final String INDENT1 = "\t";
	public static final String INDENT2 = "\t\t";
	public static final String INDENT3 = "\t\t\t";
	public static final String INDENT4 = "\t\t\t\t";
	public static final String DEFAULT_TRANSPORT_PACKAGE = "transport";
	public static final String ERROR_TRANSPORT_PACKAGE = "error";

	private final String packageName;
	private final Path codeFilePath;
	private final Map<String, Set<String>> imports = new HashMap<>();
	private final StringBuffer codeBlock = new StringBuffer();
	private final List<String> codeBlocks = new ArrayList<>();

	private String getJavaPrimitiveType(final String strippedFieldType, final String transportPackageName) {
		if ("string".equals(strippedFieldType) || "any".equals(strippedFieldType)) {
			return "String";
		} else if ("number".equals(strippedFieldType)) {
			return "double";
		} else if ("integer".equals(strippedFieldType)) {
			return "long";
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
			return strippedFieldType;
		} else {
			if (strippedFieldType.contains("-")) {
				addImport(strippedFieldType.replaceAll("-", "."));
				return strippedFieldType.substring(strippedFieldType.lastIndexOf('-') + 1);
			} else {
				addImport(packageName.substring(0, packageName.lastIndexOf('.')) + "." + transportPackageName + "."
						+ strippedFieldType);
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

	public CodeGenerator(final String sourceDirectory, final String packageName, final List<String> classAnnotations,
			final boolean isInterface, final String className, final String extendsFrom,
			final List<String> implementsList, final boolean overwriteFile) {
		this.packageName = packageName;

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
			fields.append(INDENT1).append("private ").append(getJavaType(member.type(), transportPackageName)).append(" ")
					.append(member.name()).append(";").append(NEWLINE);
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

	public void addCodeBlock(final String block) {
		codeBlocks.add(block);
	}
}
