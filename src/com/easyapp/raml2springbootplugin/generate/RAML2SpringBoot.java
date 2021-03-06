package com.easyapp.raml2springbootplugin.generate;

import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.raml.v2.api.RamlModelBuilder;
import org.raml.v2.api.RamlModelResult;
import org.raml.v2.api.model.v10.api.Api;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;
import com.easyapp.raml2springbootplugin.generate.util.GeneratorUtil;

public class RAML2SpringBoot {
	public static void generate(final CodeGenConfig codeGenConfig) throws Exception {
		final RamlModelResult ramlModelResult = new RamlModelBuilder()
				.buildApi(Paths.get(codeGenConfig.getRamlFilePath()).toFile());

		if (ramlModelResult.hasErrors()) {
			throw new Exception(ramlModelResult.getValidationResults().stream().map(result -> result.getMessage())
					.collect(Collectors.joining("\n")));
		}

		final Api api = ramlModelResult.getApiV10();

		GeneratorUtil.initialize();
		GeneratorUtil.validateAndUpdateMavenDependency(codeGenConfig);

		if (codeGenConfig.getExternalConfig().generateHealthCheck()) {
			GeneratorUtil.addMavenDependency(codeGenConfig, "org.springframework.boot", "spring-boot-starter-actuator",
					null, null);
		}

		if (codeGenConfig.getExternalConfig().dockerize()) {
			GenerateDocker docker = new GenerateDocker(codeGenConfig);
			docker.create();
		}

		GenerateExceptions exceptions = new GenerateExceptions(api, codeGenConfig);
		exceptions.create();

		GenerateTransport transport = new GenerateTransport(api, codeGenConfig);
		transport.create();

		if (codeGenConfig.getExternalConfig().hasJpaConfig()) {
			GenerateJPA jpa = new GenerateJPA(api, codeGenConfig);
			jpa.create();
		}

		GenerateService service = new GenerateService(api, codeGenConfig);
		service.create();

		GenerateRestController restController = new GenerateRestController(api, codeGenConfig);
		restController.create();

		if (codeGenConfig.getExternalConfig().generateTests()) {
			GenerateTests tests = new GenerateTests(api, codeGenConfig);
			tests.create();
		}
	}
}
