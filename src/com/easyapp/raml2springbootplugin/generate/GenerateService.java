package com.easyapp.raml2springbootplugin.generate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.methods.Method;
import org.raml.v2.api.model.v10.resources.Resource;
import org.springframework.util.StringUtils;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;
import com.easyapp.raml2springbootplugin.config.ExternalConfig.JpaConfig;
import com.easyapp.raml2springbootplugin.config.ExternalConfig.JpaConfig.Table.EntityMapping;
import com.easyapp.raml2springbootplugin.generate.util.GeneratorUtil;

public class GenerateService {
	private final Api api;
	private final JpaConfig jpaConfig;
	private final String basePackage;
	private final CodeGenerator generator;

	private String getPathVariables(final List<TypeDeclaration> uriParameters) {
		if (uriParameters.isEmpty()) {
			return "";
		} else {
			return uriParameters.stream()
					.map(uriParam -> "final "
							+ generator.getJavaType(GeneratorUtil.getMemberType(uriParam),
									CodeGenerator.DEFAULT_TRANSPORT_PACKAGE, false)
							+ " " + GeneratorUtil.getMemberName(uriParam))
					.collect(Collectors.joining(", "));
		}
	}

	private String getRequestParameters(final Method method) {
		if (method.queryParameters().isEmpty()) {
			return "";
		} else {
			return method.queryParameters().stream()
					.map(queryParam -> "final "
							+ generator.getJavaType(GeneratorUtil.getMemberType(queryParam),
									CodeGenerator.DEFAULT_TRANSPORT_PACKAGE, false)
							+ " " + GeneratorUtil.getMemberName(queryParam))
					.collect(Collectors.joining(", "));
		}
	}

	private String getMethodParameters(final Method method) {
		final List<String> variables = new ArrayList<>();
		final String pathVariables = getPathVariables(GeneratorUtil.getURIParameters(method.resource()));
		final String requestParams = getRequestParameters(method);

		if (!("").equals(pathVariables)) {
			variables.add(pathVariables);
		}

		if (("post").equals(method.method()) || ("put").equals(method.method()) || ("patch").equals(method.method())) {
			variables
					.add("final "
							+ generator.getJavaType(
									method.body().isEmpty() ? "string"
											: GeneratorUtil.getMemberType(method.body().get(0)),
									CodeGenerator.DEFAULT_TRANSPORT_PACKAGE, false)
							+ " " + GeneratorUtil.getRequestBodyVariableName(method));
		}

		if (!("").equals(requestParams)) {
			variables.add(requestParams);
		}

		return variables.stream().collect(Collectors.joining(", "));
	}

	private String getUriVariables(final Method method, final String entity) {
		final String uriVariables = GeneratorUtil.getURIParameters(method.resource()).stream()
				.map(GeneratorUtil::getMemberName).collect(Collectors.joining(", "));

		if (uriVariables.contains(",")) {
			return "new " + entity + "Id(" + uriVariables + ")";
		} else {
			return uriVariables;
		}
	}

	private String generateJpaImplementation(final Method method, final String responseType) {
		final StringBuffer impl = new StringBuffer();

		final String transportType = responseType.startsWith("Page<") || responseType.startsWith("List<")
				? responseType.substring(5, responseType.length() - 1) : responseType;

		jpaConfig.getTables().forEach(table -> {
			if (impl.length() > 0) {
				return;
			}

			final String mapperName = GeneratorUtil.getTitleCase(table.getTableName(), "_") + "Mapper";
			final String repositoryName = GeneratorUtil.getCamelCase(table.getTableName(), "_") + "Repository";
			final String entity = GeneratorUtil.getTitleCase(table.getTableName(), "_");
			final String entityName = entity + "Entity";
			final EntityMapping entityMapping = table.getEntityMappings() != null ? table.getEntityMappings().stream()
					.filter(mapping -> transportType
							.equals(GeneratorUtil.getTitleCaseFromCamelCase(mapping.getRamlType()) + "Transport"))
					.findFirst().orElse(null) : null;

			if (entityMapping != null) {
				generator.addImport(basePackage + ".mapper." + mapperName);

				if (method.method().equals("get")) {
					if (responseType.startsWith("Page<")) {
						impl.append(CodeGenerator.INDENT2).append("return ").append(repositoryName)
								.append(".findAll(pageable).map(").append(mapperName).append("::get")
								.append(transportType).append(");").append(CodeGenerator.NEWLINE);
					} else if (responseType.startsWith("List<")) {
						impl.append(CodeGenerator.INDENT2).append("return ").append(repositoryName)
								.append(".findAll().parallelStream().map(").append(mapperName).append("::get")
								.append(transportType).append(").collect(toList());").append(CodeGenerator.NEWLINE);

						generator.addImport("static java.util.stream.Collectors.toList");
					} else if (entityMapping.useForCRUD()) {
						impl.append(CodeGenerator.INDENT2).append("return ").append(mapperName).append(".get")
								.append(transportType).append("(").append(repositoryName).append(".findOne(")
								.append(getUriVariables(method, entity)).append("));").append(CodeGenerator.NEWLINE);
					}
				} else if (entityMapping.useForCRUD()) {
					if (responseType.startsWith("List<") || responseType.startsWith("Page<")) {
						impl.append(CodeGenerator.INDENT2).append("return ").append(repositoryName).append(".save(")
								.append(GeneratorUtil.getRequestBodyVariableName(method))
								.append(".parallelStream().map(").append(mapperName).append("::get").append(entityName)
								.append(").collect(toList())).parallelStream().map(").append(mapperName).append("::get")
								.append(transportType).append(").collect(toList());").append(CodeGenerator.NEWLINE);

						generator.addImport("static java.util.stream.Collectors.toList");
					} else {
						impl.append(CodeGenerator.INDENT2).append("return ").append(mapperName).append(".get")
								.append(transportType).append("(").append(repositoryName).append(".save(")
								.append(mapperName).append(".get").append(entityName).append("(")
								.append(GeneratorUtil.getRequestBodyVariableName(method)).append(")));")
								.append(CodeGenerator.NEWLINE);
					}
				}
			} else if (table.getEntityMappings() != null && transportType.equals("Void")
					&& method.method().equals("delete")) {
				final String crudRepository = table.getEntityMappings().stream()
						.filter(mapping -> mapping.useForCRUD()
								&& GeneratorUtil.getTitleCaseFromCamelCase(mapping.getRamlType())
										.equals(GeneratorUtil
												.getTitleCaseFromCamelCase(method.resource().displayName().value())))
						.map(mapping -> GeneratorUtil.getCamelCaseFromTitleCase(mapping.getRamlType()) + "Repository")
						.findFirst().orElse(null);

				if (crudRepository != null) {
					impl.append(CodeGenerator.INDENT2).append(crudRepository).append(".delete(")
							.append(getUriVariables(method, entity)).append(");").append(CodeGenerator.NEWLINE);
				}
			}
		});

		return impl.toString();
	}

	private void createResourceMethods(final Resource resource) {
		resource.methods().stream().forEach(method -> {
			final StringBuffer methods = new StringBuffer();
			final boolean pageType = method.is().stream().anyMatch(trait -> trait.name().equals("Paginated"));

			final String responseType = generator.getJavaType(method.responses().stream()
					.filter(response -> response.code().value().startsWith("2"))
					.map(response -> GeneratorUtil.getMemberType(response.body().get(0))).findFirst().orElse("string"),
					CodeGenerator.DEFAULT_TRANSPORT_PACKAGE, pageType);

			final String methodName = method.method()
					+ GeneratorUtil.getTitleCaseFromCamelCase(resource.displayName().value());

			methods.append(CodeGenerator.INDENT1).append("public ")
					.append(responseType.equals("Void") ? "void" : responseType).append(" ").append(methodName)
					.append("(").append(getMethodParameters(method)).append(") throws Exception {")
					.append(CodeGenerator.NEWLINE);

			final String jpaImplementation = (jpaConfig != null && jpaConfig.getTables() != null)
					? generateJpaImplementation(method, responseType) : null;

			if (StringUtils.isEmpty(jpaImplementation)) {
				methods.append(CodeGenerator.INDENT2).append("// TODO: Build Business Logic Here")
						.append(CodeGenerator.NEWLINE);
				methods.append(CodeGenerator.INDENT2).append("return null;").append(CodeGenerator.NEWLINE);
			} else {
				methods.append(jpaImplementation);
			}

			methods.append(CodeGenerator.INDENT1).append("}").append(CodeGenerator.NEWLINE);

			generator.addCodeBlock(methods.toString());
		});

		resource.resources().stream().forEach(subResource -> createResourceMethods(subResource));
	}

	public GenerateService(final Api api, final CodeGenConfig codeGenConfig) {
		this.api = api;
		this.jpaConfig = codeGenConfig.getExternalConfig().getJpaConfig();
		this.basePackage = codeGenConfig.getBasePackage();
		final String apiTitle = api.title().value().replaceAll(" ", "");

		generator = new CodeGenerator(codeGenConfig, "service", Arrays.asList("@Service"), false, apiTitle + "Service",
				null, null, false);
		generator.addImport("org.springframework.stereotype.Service");

		if (jpaConfig != null && jpaConfig.getTables() != null) {
			codeGenConfig.getExternalConfig().getJpaConfig().getTables().stream().forEach(table -> {
				final StringBuffer autowire = new StringBuffer();
				final String repository = GeneratorUtil.getTitleCase(table.getTableName(), "_") + "Repository";

				autowire.append(CodeGenerator.INDENT1).append("@Autowired").append(CodeGenerator.NEWLINE);
				autowire.append(CodeGenerator.INDENT1).append("private ").append(repository).append(" ")
						.append(GeneratorUtil.getCamelCase(table.getTableName(), "_")).append("Repository;")
						.append(CodeGenerator.NEWLINE);

				generator.addCodeBlock(autowire.toString());
				generator.addImport("org.springframework.beans.factory.annotation.Autowired");
				generator.addImport(codeGenConfig.getBasePackage() + ".repository." + repository);
			});
		}

	}

	public void create() {
		api.resources().stream().forEach(resource -> createResourceMethods(resource));
		generator.writeCode();
	}
}
