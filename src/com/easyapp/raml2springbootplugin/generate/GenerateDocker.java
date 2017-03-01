package com.easyapp.raml2springbootplugin.generate;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;

public class GenerateDocker {
	private final CodeGenConfig codeGenConfig;

	public GenerateDocker(final CodeGenConfig codeGenConfig) {
		this.codeGenConfig = codeGenConfig;
	}

	public void create() throws Exception {
		final Path dockerDirectoryPath = Paths.get(codeGenConfig.getProjectDirectory() + "/docker");

		if (!Files.exists(dockerDirectoryPath)) {
			Files.createDirectory(dockerDirectoryPath);
		}

		final FileWriter writer = new FileWriter(dockerDirectoryPath.toString() + "/Dockerfile");
		final BufferedReader reader = new BufferedReader(
				new InputStreamReader(new URL("platform:/plugin/RAML2SpringBootPlugin/Dockerfile").openStream()));
		String line = null;

		while ((line = reader.readLine()) != null) {
			writer.write(line.replaceAll("DOCKER_BASE_IMAGE_NAME",
					codeGenConfig.getExternalConfig().getDockerConfig().getDockerBaseImageName())
					+ CodeGenerator.NEWLINE);
		}

		writer.close();
		reader.close();
	}
}
