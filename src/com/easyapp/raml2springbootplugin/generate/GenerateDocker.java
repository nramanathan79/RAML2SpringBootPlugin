package com.easyapp.raml2springbootplugin.generate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;

public class GenerateDocker {
	private final CodeGenConfig codeGenConfig;
	
	private static Xpp3Dom newElement(final String elementName, final String elementValue) {
		final Xpp3Dom element = new Xpp3Dom(elementName);
		element.setValue(elementValue);
		
		return element;
	}

	public GenerateDocker(final CodeGenConfig codeGenConfig) {
		this.codeGenConfig = codeGenConfig;
	}

	public void create() throws Exception {
		final Path dockerDirectoryPath = Paths.get(codeGenConfig.getProjectDirectory() + "/docker");
		String dockerFile = dockerDirectoryPath.toString() + "/Dockerfile";

		if (!Files.exists(dockerDirectoryPath)) {
			Files.createDirectory(dockerDirectoryPath);
		} else if (Files.exists(Paths.get(dockerFile)) && !codeGenConfig.getExternalConfig().overwriteFiles()) {
			dockerFile += ".MERGE";
		}

		final FileWriter dockerFileWriter = new FileWriter(dockerFile);
		final BufferedReader dockerFileReader = new BufferedReader(
				new InputStreamReader(new URL("platform:/plugin/RAML2SpringBootPlugin/Dockerfile").openStream()));
		String line = null;

		while ((line = dockerFileReader.readLine()) != null) {
			dockerFileWriter.write(line.replaceAll("DOCKER_BASE_IMAGE_NAME",
					codeGenConfig.getExternalConfig().getDockerConfig().getDockerBaseImageName())
					+ CodeGenerator.NEWLINE);
		}

		dockerFileWriter.close();
		dockerFileReader.close();

		final MavenXpp3Reader mavenReader = new MavenXpp3Reader();
		final Model pomModel = mavenReader.read(new FileReader(codeGenConfig.getPomFilePath()));
		final List<Plugin> plugins = pomModel.getBuild().getPlugins();
		
		// Force regeneration
		plugins.removeIf(plugin -> plugin.getKey().equals(Plugin.constructKey("com.spotify", "docker-maven-plugin")));

		final Plugin dockerPlugin = new Plugin();
		dockerPlugin.setGroupId("com.spotify");
		dockerPlugin.setArtifactId("docker-maven-plugin");
		dockerPlugin.setVersion("0.4.13");

		final Xpp3Dom dockerConfig = new Xpp3Dom("configuration");
		dockerConfig.addChild(newElement("imageName", codeGenConfig.getExternalConfig().getDockerConfig().getDockerImageName()));
		dockerConfig.addChild(newElement("dockerHost", codeGenConfig.getExternalConfig().getDockerConfig().getDockerHost()));
		dockerConfig.addChild(newElement("dockerDirectory", "docker"));

		final Xpp3Dom dockerResource = new Xpp3Dom("resource");
		dockerResource.addChild(newElement("targetPath", "/"));
		dockerResource.addChild(newElement("directory", "${project.build.directory}"));
		dockerResource.addChild(newElement("include", "${project.build.finalName}.jar"));

		final Xpp3Dom dockerResources = new Xpp3Dom("resources");
		dockerResources.addChild(dockerResource);

		final Xpp3Dom buildArgs = new Xpp3Dom("buildArgs");
		buildArgs.addChild(newElement("userid", "${user.id}"));

		dockerConfig.addChild(dockerResources);
		dockerConfig.addChild(buildArgs);
		
		dockerPlugin.setConfiguration(dockerConfig);

		plugins.add(dockerPlugin);
		
		if (!pomModel.getProperties().containsKey("user.id")) {
			pomModel.addProperty("user.id", "api");
		}

		final MavenXpp3Writer writer = new MavenXpp3Writer();

		if (codeGenConfig.getExternalConfig().overwriteFiles()) {
			writer.write(new FileWriter(codeGenConfig.getPomFilePath()), pomModel);
		} else {
			writer.write(new FileWriter(codeGenConfig.getPomFilePath() + ".MERGE"), pomModel);
		}
	}
}
