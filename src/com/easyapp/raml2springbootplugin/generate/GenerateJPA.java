package com.easyapp.raml2springbootplugin.generate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;
import com.easyapp.raml2springbootplugin.config.ExternalConfig.JpaConfig.Table;
import com.easyapp.raml2springbootplugin.generate.util.ColumnDefinition;
import com.easyapp.raml2springbootplugin.generate.util.DatabaseUtil;
import com.easyapp.raml2springbootplugin.generate.util.GeneratorUtil;
import com.easyapp.raml2springbootplugin.generate.util.TableDefinition;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

public class GenerateJPA {
	private final CodeGenConfig codeGenConfig;

	private void generateEmbeddable(final List<ColumnDefinition> columns, final Table table) throws Exception {
		final String className = GeneratorUtil.getTitleCase(table.getTableName(), "_") + "Id";
		final CodeGenerator generator = new CodeGenerator(codeGenConfig.getSourceDirectory(),
				codeGenConfig.getBasePackage(), "entity", Arrays.asList("@Embeddable"), false, className, null,
				Arrays.asList("Serializable"), codeGenConfig.getExternalConfig().overwriteFiles());

		generator.addImport("javax.persistence.Embeddable");
		generator.addImport("java.io.Serializable");

		generator.addCodeBlock(CodeGenerator.INDENT1 + "private static final long serialVersionUID = 1L;");
		generator.addMembers(columns, table);

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

		final AtomicInteger index = new AtomicInteger(1);
		columns.forEach(column -> {
			constructors.append(CodeGenerator.INDENT3).append("this.")
					.append(GeneratorUtil.getCamelCase(column.getColumnName(), "_")).append(" = new ")
					.append(generator.getJavaDataType(column.getDataType(), table.getTableName()))
					.append("(URLDecoder.decode(tokens[").append(index.getAndAdd(1)).append("], \"UTF-8\"));")
					.append(CodeGenerator.NEWLINE);
		});

		constructors.append(CodeGenerator.INDENT2).append("} catch (Exception e) {").append(CodeGenerator.NEWLINE);
		constructors.append(CodeGenerator.INDENT3).append("e.printStackTrace();").append(CodeGenerator.NEWLINE);
		constructors.append(CodeGenerator.INDENT2).append("}").append(CodeGenerator.NEWLINE);
		constructors.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE)
				.append(CodeGenerator.NEWLINE);

		constructors.append(CodeGenerator.INDENT1).append("public ").append(className).append("(")
				.append(columns.stream()
						.map(column -> "final " + generator.getJavaDataType(column.getDataType(), table.getTableName())
								+ " " + GeneratorUtil.getCamelCase(column.getColumnName(), "_"))
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
		overrides.append(CodeGenerator.INDENT2).append("} catch(Exception e) {").append(CodeGenerator.NEWLINE);
		overrides.append(CodeGenerator.INDENT3).append("return \"\";").append(CodeGenerator.NEWLINE);
		overrides.append(CodeGenerator.INDENT2).append("}").append(CodeGenerator.NEWLINE);
		overrides.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE);

		generator.addImport("java.net.URLEncoder");
		generator.addCodeBlock(overrides.toString());
		generator.writeCode();
	}

	private void generateEntity(final List<ColumnDefinition> columns, final Table table) throws Exception {
		final CodeGenerator generator = new CodeGenerator(codeGenConfig.getSourceDirectory(),
				codeGenConfig.getBasePackage(), "entity",
				Arrays.asList("@Entity", "@Table(name = \"" + table.getTableName().toUpperCase() + "\")"), false,
				GeneratorUtil.getTitleCase(table.getTableName(), "_"), null, Arrays.asList("Serializable"),
				codeGenConfig.getExternalConfig().overwriteFiles());

		generator.addImport("javax.persistence.Entity");
		generator.addImport("javax.persistence.Table");
		generator.addImport("java.io.Serializable");

		generator.addCodeBlock(CodeGenerator.INDENT1 + "private static final long serialVersionUID = 1L;");
		generator.addMembers(columns, table);
		generator.writeCode();
	}

	public GenerateJPA(final CodeGenConfig codeGenConfig) {
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

			if (tableDefinition.hasCompositeKey()) {
				generateEmbeddable(tableDefinition.getKeyColumns(), table);
				generateEntity(tableDefinition.getNonKeyColumns(), table);
			} else {
				generateEntity(tableDefinition.getColumns(), table);
			}
		}
	}
}
