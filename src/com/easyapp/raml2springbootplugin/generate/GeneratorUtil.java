package com.easyapp.raml2springbootplugin.generate;

import java.util.ArrayList;
import java.util.List;

import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.resources.Resource;
import org.springframework.http.HttpStatus;

public class GeneratorUtil {
	private static void addURIParameters(final Resource resource, final List<TypeDeclaration> uriParameters) {
		if (resource != null) {
			addURIParameters(resource.parentResource(), uriParameters);
			uriParameters.addAll(resource.uriParameters());
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
