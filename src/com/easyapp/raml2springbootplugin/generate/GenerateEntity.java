package com.easyapp.raml2springbootplugin.generate;

import java.util.Arrays;
import java.util.stream.Stream;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;
import com.easyapp.raml2springbootplugin.config.ExternalConfig.JpaConfig.Table;
import com.easyapp.raml2springbootplugin.generate.util.ColumnDefinition;
import com.easyapp.raml2springbootplugin.generate.util.GeneratorUtil;

public class GenerateEntity {
	private final CodeGenConfig codeGenConfig;

	public GenerateEntity(final CodeGenConfig codeGenConfig) {
		this.codeGenConfig = codeGenConfig;
	}

	public void create(final Stream<ColumnDefinition> columns, final Table table) throws Exception {
		final CodeGenerator generator = new CodeGenerator(codeGenConfig.getSourceDirectory(),
				codeGenConfig.getBasePackage(), "entity",
				Arrays.asList("@Entity", "@Table(name = \"" + table.getTableName().toUpperCase() + "\")"), false,
				GeneratorUtil.getTitleCase(table.getTableName(), "_"), null, Arrays.asList("Serializable"),
				codeGenConfig.getExternalConfig().overwriteFiles());

		generator.addImport("javax.persistence.Entity");
		generator.addImport("javax.persistence.Table");
		generator.addImport("java.io.Serializable");

		final StringBuffer block = new StringBuffer();
		block.append(CodeGenerator.INDENT1).append("private static final long serialVersionUID = 1L;");
		generator.addCodeBlock(block.toString());
		
		generator.addMembers(columns, table);
		generator.writeCode();
	}
}
