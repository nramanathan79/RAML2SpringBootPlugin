package com.easyapp.raml2springbootplugin.generate.util;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.methods.Method;
import org.raml.v2.api.model.v10.resources.Resource;
import org.springframework.http.HttpStatus;

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;

public class GeneratorUtil {
	private static void addURIParameters(final Resource resource, final List<TypeDeclaration> uriParameters) {
		if (resource != null) {
			addURIParameters(resource.parentResource(), uriParameters);
			uriParameters.addAll(resource.uriParameters());
		}
	}

	public static String getRequestBodyVariableName(final Method method) {
		if (method.body().isEmpty()) {
			return "requestBody";
		} else {
			final TypeDeclaration methodBody = method.body().get(0);
			return (methodBody.displayName() == null || methodBody.displayName().value().isEmpty() ? "requestBody"
					: methodBody.displayName().value());
		}
	}

	public static List<TypeDeclaration> getURIParameters(final Resource resource) {
		List<TypeDeclaration> uriParameters = new ArrayList<>();
		addURIParameters(resource, uriParameters);

		return uriParameters;
	}

	public static String getTitleCase(final String text, final String delimiter) {
		String returnValue = "";

		if (text != null) {
			String[] words = text.trim().split(delimiter);

			for (String word : words) {
				returnValue += Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
			}
		}

		return returnValue;
	}

	public static String getCamelCase(final String text, final String delimiter) {
		String returnValue = getTitleCase(text, delimiter);

		if (returnValue.length() > 0) {
			returnValue = Character.toLowerCase(returnValue.charAt(0)) + returnValue.substring(1);
		}

		return returnValue;
	}

	public static void validateAndUpdateMavenDependency(final CodeGenConfig codeGenConfig) throws Exception {
		final MavenXpp3Reader mavenReader = new MavenXpp3Reader();
		final Model pomModel = mavenReader.read(new FileReader(codeGenConfig.getPomFilePath()));
		final Dependency starterDependency = pomModel.getDependencies().stream()
				.filter(dependency -> dependency.getArtifactId().equals("spring-boot-starter")).findAny().orElse(null);

		if (starterDependency != null) {
			starterDependency.setArtifactId("spring-boot-starter-web");
			final MavenXpp3Writer writer = new MavenXpp3Writer();

			if (codeGenConfig.getExternalConfig().overwriteFiles()) {
				writer.write(new FileWriter(codeGenConfig.getPomFilePath()), pomModel);
			} else {
				writer.write(new FileWriter(codeGenConfig.getPomFilePath() + ".MERGE"), pomModel);
			}
		}
	}

	public static String getHttpStatusPhrase(final String httpCode) {
		return getHttpStatus(httpCode).getReasonPhrase();
	}

	public static HttpStatus getHttpStatus(final String httpCode) {
		return HttpStatus.valueOf(Integer.valueOf(httpCode));
	}

	public static String getExceptionClassName(final String httpCode) {
		return getTitleCase(getHttpStatus(httpCode).name(), "_") + "Exception";
	}
}
