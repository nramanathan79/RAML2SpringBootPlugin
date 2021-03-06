package com.easyapp.raml2springbootplugin.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class CodeGenConfig {
	private String ramlFilePath = null;
	private String projectDirectory = null;
	private String sourceDirectory = null;
	private String testDirectory = null;
	private String pomFilePath = null;
	private String basePackage = null;
	private Properties applicationProperties = new Properties();
	private ExternalConfig externalConfig = null;

	private void getBasePackage(final String directoryPath) {
		if (basePackage == null) {
			try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directoryPath))) {
				for (final Path path : directoryStream) {
					if (path.toString().endsWith("Application.java")) {
						final String relativePath = directoryPath.substring(sourceDirectory.length() + 1);
						basePackage = relativePath.replaceAll("\\\\", ".").replaceAll("/", ".");
					} else {
						getBasePackage(path.toString());
					}
				}
			} catch (IOException ioe) {
				// Do nothing
			}
		}
	}

	private void getExternalConfigYaml(final String configFilePath) throws Exception {
		if (Files.exists(Paths.get(configFilePath)) && Files.isReadable(Paths.get(configFilePath))) {
			final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			externalConfig = mapper.readValue(new File(configFilePath), ExternalConfig.class);
		}
	}

	private void getExternalConfigJson(final String configFilePath) throws Exception {
		if (Files.exists(Paths.get(configFilePath)) && Files.isReadable(Paths.get(configFilePath))) {
			final ObjectMapper mapper = new ObjectMapper();
			externalConfig = mapper.readValue(new File(configFilePath), ExternalConfig.class);
		}
	}

	private void getApplicationProperties(final String applicationPropertiesFilePath) throws Exception {
		if (Files.exists(Paths.get(applicationPropertiesFilePath))
				&& Files.isReadable(Paths.get(applicationPropertiesFilePath))) {
			applicationProperties.load(new FileReader(applicationPropertiesFilePath));
		}
	}

	public CodeGenConfig(final String absoluteRamlFilePath, final String relativeRamlFilePath) throws Exception {
		this.ramlFilePath = absoluteRamlFilePath;
		this.projectDirectory = absoluteRamlFilePath.substring(0,
				absoluteRamlFilePath.indexOf(relativeRamlFilePath) - 1);
		this.sourceDirectory = this.projectDirectory + "/src/main/java";
		this.testDirectory = this.projectDirectory + "/src/test/java";
		this.pomFilePath = this.projectDirectory + "/pom.xml";
		getBasePackage(this.sourceDirectory);
		getApplicationProperties(this.projectDirectory + "/src/main/resources/application.properties");
		getExternalConfigYaml(this.projectDirectory + "/src/main/resources/config.yaml");

		if (externalConfig == null) {
			getExternalConfigJson(this.projectDirectory + "/src/main/resources/config.json");
		}
		
		if (externalConfig == null) {
			externalConfig = new ExternalConfig();
		}
	}

	public String getConfigError() {
		if (ramlFilePath == null) {
			return "RAML file path is missing";
		}

		if (!Files.exists(Paths.get(ramlFilePath))) {
			return "RAML file: " + ramlFilePath + " does NOT exist";
		}

		if (!Files.isReadable(Paths.get(ramlFilePath))) {
			return "RAML file: " + ramlFilePath + " is unreadable";
		}

		if (projectDirectory == null) {
			return "Project Directory is missing";
		}

		if (!Files.isDirectory(Paths.get(projectDirectory))) {
			return "Project Directory: " + projectDirectory + " does NOT exist";
		}

		if (sourceDirectory == null) {
			return "Source Directory is missing";
		}

		if (!Files.isDirectory(Paths.get(sourceDirectory))) {
			return "Source Directory: " + sourceDirectory + " does NOT exist";
		}

		if (testDirectory == null) {
			return "Test Directory is missing";
		}

		if (!Files.isDirectory(Paths.get(testDirectory))) {
			return "Test Directory: " + testDirectory + " does NOT exist";
		}

		if (!Files.exists(Paths.get(pomFilePath))) {
			return "Maven POM file: " + pomFilePath + " does NOT exist";
		}

		if (basePackage == null) {
			return "Base Package for the Application is missing";
		}

		if (externalConfig.hasJpaConfig()) {
			if (StringUtils.isEmpty(getApplicationProperty("spring.datasource.driver-class-name"))) {
				return "Datasource property spring.datasource.driver-class-name is missing in application.properties";
			}

			if (StringUtils.isEmpty(getApplicationProperty("spring.datasource.url"))) {
				return "Datasource property spring.datasource.url is missing in application.properties";
			}

			if (StringUtils.isEmpty(getApplicationProperty("spring.datasource.username"))) {
				return "Datasource property spring.datasource.username is missing in application.properties";
			}

			if (StringUtils.isEmpty(getApplicationProperty("spring.datasource.password"))) {
				return "Datasource property spring.datasource.password is missing in application.properties";
			}
		}

		return externalConfig.getConfigError();
	}

	public String getRamlFilePath() {
		return ramlFilePath;
	}

	public String getProjectDirectory() {
		return projectDirectory;
	}

	public String getSourceDirectory() {
		return sourceDirectory;
	}

	public String getBasePackage() {
		return basePackage;
	}

	public String getTestDirectory() {
		return testDirectory;
	}

	public String getPomFilePath() {
		return pomFilePath;
	}

	public ExternalConfig getExternalConfig() {
		return externalConfig;
	}

	public String getApplicationProperty(final String propertyName) {
		return applicationProperties.getProperty(propertyName);
	}
}
