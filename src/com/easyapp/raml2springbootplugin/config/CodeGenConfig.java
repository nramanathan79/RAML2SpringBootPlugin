package com.easyapp.raml2springbootplugin.config;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CodeGenConfig {
	private String ramlFilePath = null;
	private String projectDirectory = null;
	private String sourceDirectory = null;
	private String basePackage = null;

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

	public CodeGenConfig(final String absoluteRamlFilePath, final String relativeRamlFilePath) {
		this.ramlFilePath = absoluteRamlFilePath;
		this.projectDirectory = absoluteRamlFilePath.substring(0,
				absoluteRamlFilePath.indexOf(relativeRamlFilePath) - 1);
		this.sourceDirectory = this.projectDirectory + "/src/main/java";
		getBasePackage(this.sourceDirectory);
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

		if (basePackage == null) {
			return "Base Package for the Application is missing";
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
}
