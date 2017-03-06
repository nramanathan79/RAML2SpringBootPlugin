package com.easyapp.raml2springbootplugin.generate;

import java.util.Arrays;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;
import com.easyapp.raml2springbootplugin.config.ExternalConfig.JpaConfig.Table;
import com.easyapp.raml2springbootplugin.generate.util.DatabaseUtil;
import com.easyapp.raml2springbootplugin.generate.util.GeneratorUtil;
import com.easyapp.raml2springbootplugin.generate.util.TableDefinition;

public class GenerateJPA {
	private final CodeGenConfig codeGenConfig;

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
			
			final CodeGenerator generator = new CodeGenerator(codeGenConfig.getSourceDirectory(),
					codeGenConfig.getBasePackage(), "entity", Arrays.asList("@Entity", "@Table(name = \"" + tableDefinition.getTableName() + "\")"), false,
					GeneratorUtil.getTitleCase(tableDefinition.getTableName(), "_"), null,
					Arrays.asList("Serializable"), codeGenConfig.getExternalConfig().overwriteFiles());
			
			generator.addImport("javax.persistence.Entity");
			generator.addImport("javax.persistence.Table");
			generator.addImport("java.io.Serializable");
			
			final StringBuffer block = new StringBuffer();
			block.append(CodeGenerator.INDENT1).append("private static final long serialVersionUID = 1L;");
			generator.addCodeBlock(block.toString());
			
			if (tableDefinition.hasCompositeKey()) {
				//GenerateEmbeddable embeddable = new GenerateEmbeddable(codeGenConfig);
				//embeddable.create(tableDefinition.getKeyColumnStream(), table);
				
				GenerateEntity entity = new GenerateEntity(codeGenConfig);
				entity.create(tableDefinition.getNonKeyColumnStream(), table);
			} else {
				GenerateEntity entity = new GenerateEntity(codeGenConfig);
				entity.create(tableDefinition.getColumnStream(), table);
			}
			
			generator.writeCode();
		}
	}
}
