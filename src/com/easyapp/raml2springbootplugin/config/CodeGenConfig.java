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
	private String testDirectory = null;
	private String testFilePath = null;
	private String testClassName = null;
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

	private void getTestClass(final String directoryPath) {
		try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directoryPath))) {
			directoryStream.forEach(path -> {
				if (path.toString().endsWith("ApplicationTests.java")) {
					final String testFileName = path.getFileName().toString();

					testFilePath = path.toString();
					testClassName = testFileName.substring(0, testFileName.indexOf(".java"));
					System.out.println("RAMBO TEST CLASS NAME: " + testClassName);
				} else {
					getTestClass(path.toString());
				}
			});
		} catch (IOException ioe) {
			// Do nothing
		}
	}

	public CodeGenConfig(final String absoluteRamlFilePath, final String relativeRamlFilePath) {
		this.ramlFilePath = absoluteRamlFilePath;
		this.projectDirectory = absoluteRamlFilePath.substring(0,
				absoluteRamlFilePath.indexOf(relativeRamlFilePath) - 1);
		this.sourceDirectory = this.projectDirectory + "/src/main/java";
		this.testDirectory = this.projectDirectory + "/src/test/java";
		getBasePackage(this.sourceDirectory);
		getTestClass(this.testDirectory);
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
		
		if (testClassName == null) {
			return "Test class is missing";
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
