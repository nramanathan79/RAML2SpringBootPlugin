package com.easyapp.raml2springbootplugin.config;

public class ExternalConfig {
	private boolean generateTests = true;
	private boolean overwriteFiles = true;
	private boolean generateHealthCheck = true;
	private boolean dockerize = false;
	private DockerConfig dockerConfig;
	
	public class DockerConfig {
		private String dockerBaseImageName = "java:8";
		private String dockerImageName;
		
		public String getDockerBaseImageName() {
			return dockerBaseImageName;
		}
		
		public void setDockerBaseImageName(final String dockerBaseImageName) {
			this.dockerBaseImageName = dockerBaseImageName;
		}
		
		public String getDockerImageName() {
			return dockerImageName;
		}
		
		public void setDockerImageName(final String dockerImageName) {
			this.dockerImageName = dockerImageName;
		}
	}
	
	public ExternalConfig() {
		
	}

	public boolean generateTests() {
		return generateTests;
	}

	public void setGenerateTests(boolean generateTests) {
		this.generateTests = generateTests;
	}

	public boolean overwriteFiles() {
		return overwriteFiles;
	}

	public void setOverwriteFiles(final boolean overwriteFiles) {
		this.overwriteFiles = overwriteFiles;
	}

	public boolean generateHealthCheck() {
		return generateHealthCheck;
	}

	public void setGenerateHealthCheck(final boolean generateHealthCheck) {
		this.generateHealthCheck = generateHealthCheck;
	}

	public boolean dockerize() {
		return dockerize;
	}

	public void setDockerize(final boolean dockerize) {
		this.dockerize = dockerize;
	}

	public DockerConfig getDockerConfig() {
		return dockerConfig;
	}

	public void setDockerConfig(final DockerConfig dockerConfig) {
		this.dockerConfig = dockerConfig;
	}
}
