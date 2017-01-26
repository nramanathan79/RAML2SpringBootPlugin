package com.easyapp.raml2springbootplugin.generate;

import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.raml.v2.api.RamlModelBuilder;
import org.raml.v2.api.RamlModelResult;
import org.raml.v2.api.model.v10.api.Api;

public class RAML2SpringBoot {
	public static void generate(final String ramlFilePath, final String sourceDirectory, final String basePackage)
			throws Exception {
		final RamlModelResult ramlModelResult = new RamlModelBuilder().buildApi(Paths.get(ramlFilePath).toFile());

		if (ramlModelResult.hasErrors()) {
			throw new Exception(ramlModelResult.getValidationResults().stream().map(result -> result.getMessage())
					.collect(Collectors.joining("\n")));
		}

		final Api api = ramlModelResult.getApiV10();

		GenerateExceptions exceptions = new GenerateExceptions(api, sourceDirectory, basePackage);
		exceptions.create();

		GenerateTransport transport = new GenerateTransport(api, sourceDirectory, basePackage);
		transport.create();

		GenerateService service = new GenerateService(api, sourceDirectory, basePackage);
		service.create();

		GenerateServiceImpl serviceImpl = new GenerateServiceImpl(api, sourceDirectory, basePackage);
		serviceImpl.create();

		GenerateRestController restController = new GenerateRestController(api, sourceDirectory, basePackage);
		restController.create();
	}
}
