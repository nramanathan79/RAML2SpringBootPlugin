package com.easyapp.raml2springbootplugin.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CodeGenConfig {
	private String ramlFilePath = null;
	private String projectDirectory = null;
	private String sourceDirectory = null;
	private String testDirectory = null;
	private String testFilePath = null;
	private String pomFilePath = null;
	private String testClassName = null;
	private String basePackage = null;
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

	private void getTestClass(final String directoryPath) {
		try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directoryPath))) {
			directoryStream.forEach(path -> {
				if (path.toString().endsWith("ApplicationTests.java")) {
					final String testFileName = path.getFileName().toString();

					testFilePath = path.toString();
					testClassName = testFileName.substring(0, testFileName.indexOf(".java"));
				} else {
					getTestClass(path.toString());
				}
			});
		} catch (IOException ioe) {
			// Do nothing
		}
	}

	private void getExternalConfig(final String configFilePath) {
		if (Files.exists(Paths.get(configFilePath)) && Files.isReadable(Paths.get(configFilePath))) {
			try {
				final ObjectMapper mapper = new ObjectMapper();
				externalConfig = mapper.readValue(new File(configFilePath), ExternalConfig.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public CodeGenConfig(final String absoluteRamlFilePath, final String relativeRamlFilePath) {
		this.ramlFilePath = absoluteRamlFilePath;
		this.projectDirectory = absoluteRamlFilePath.substring(0,
				absoluteRamlFilePath.indexOf(relativeRamlFilePath) - 1);
		this.sourceDirectory = this.projectDirectory + "/src/main/java";
		this.testDirectory = this.projectDirectory + "/src/test/java";
		this.pomFilePath = this.projectDirectory + "/pom.xml";
		getBasePackage(this.sourceDirectory);
		getTestClass(this.testDirectory);
		getExternalConfig(this.projectDirectory + "/src/main/resources/config.json");
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

		if (testFilePath == null) {
			return "Test file is missing";
		}

		if (!Files.exists(Paths.get(testFilePath))) {
			return "Test file: " + testFilePath + " does NOT exist";
		}

		if (!Files.isWritable(Paths.get(testFilePath))) {
			return "Test file: " + testFilePath + " is not writable";
		}

		if (!Files.exists(Paths.get(pomFilePath))) {
			return "Maven POM file: " + pomFilePath + " does NOT exist";
		}

		if (testClassName == null) {
			return "Test class is missing";
		}

		if (basePackage == null) {
			return "Base Package for the Application is missing";
		}

		if (Files.exists(Paths.get(this.projectDirectory + "/src/main/resources/config.json"))
				&& externalConfig == null) {
			return "config.json file invalid";
		}

		if (externalConfig.dockerize()) {
			if (externalConfig.getDockerConfig() == null) {
				return "Docker Configuration is missing";
			}

			if (StringUtils.isEmpty(externalConfig.getDockerConfig().getDockerBaseImageName())) {
				return "Docker Base Image is missing";
			}

			if (!externalConfig.getDockerConfig().getDockerBaseImageName()
					.equals(externalConfig.getDockerConfig().getDockerBaseImageName().toLowerCase())) {
				return "Docker Base Image " + externalConfig.getDockerConfig().getDockerBaseImageName()
						+ " is invalid (Docker Images should be all lower case)";
			}

			if (StringUtils.isEmpty(externalConfig.getDockerConfig().getDockerImageName())) {
				return "Docker Image is missing";
			}

			if (!externalConfig.getDockerConfig().getDockerImageName()
					.equals(externalConfig.getDockerConfig().getDockerImageName().toLowerCase())) {
				return "Docker Image " + externalConfig.getDockerConfig().getDockerImageName()
						+ " is invalid (Docker Images should be all lower case)";
			}
		}

		return null;
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

	public String getTestFilePath() {
		return testFilePath;
	}

	public String getPomFilePath() {
		return pomFilePath;
	}

	public void setPomFilePath(final String pomFilePath) {
		this.pomFilePath = pomFilePath;
	}

	public String getTestClassName() {
		return testClassName;
	}

	public ExternalConfig getExternalConfig() {
		return externalConfig;
	}
}
