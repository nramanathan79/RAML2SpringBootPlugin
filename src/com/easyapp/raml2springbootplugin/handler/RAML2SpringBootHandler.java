package com.easyapp.raml2springbootplugin.handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.easyapp.raml2springbootplugin.generate.RAML2SpringBoot;

public class RAML2SpringBootHandler extends AbstractHandler {
	private String ramlFilePath = null;
	private String sourceDirectory = null;
	private String errorMessage = null;
	private String basePackage = null;
	private IProject project = null;

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
			} catch (IOException ex) {
				errorMessage = "the Project does not contain src/main/java directory";
			}
		}
	}

	private void getProjectDetails(final ISelection selection) {
		if (selection != null && selection instanceof IStructuredSelection) {
			final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			final Object selectedObj = structuredSelection.getFirstElement();

			if (selectedObj != null && selectedObj instanceof IFile) {
				final IFile selectedFile = (IFile) selectedObj;
				final String relativePath = selectedFile.getProjectRelativePath().toString();
				project = selectedFile.getProject();

				if (!relativePath.startsWith("src/main/resources")) {
					errorMessage = "the RAML file is NOT in the <Project>/src/main/resources directory";
				} else {
					ramlFilePath = selectedFile.getRawLocation().toString();
					sourceDirectory = ramlFilePath.substring(0, ramlFilePath.indexOf(relativePath)) + "src/main/java";
					getBasePackage(sourceDirectory);
				}
			}
		}
	}

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final Shell shell = HandlerUtil.getActiveShell(event);

		getProjectDetails(HandlerUtil.getActiveMenuSelection(event));

		if (ramlFilePath != null && sourceDirectory != null && basePackage != null) {
			try {
				RAML2SpringBoot.generate(ramlFilePath, sourceDirectory, basePackage);

				MessageDialog.openInformation(shell, "RAML2SpringBootPlugin",
						"Successfully executed RAML to Spring Boot");

				if (project != null) {
					try {
						project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
					} catch (final CoreException e) {
						e.printStackTrace();
					}
				}
			} catch (final Throwable e) {
				final StringWriter writer = new StringWriter();
				e.printStackTrace(new PrintWriter(writer));

				MessageDialog.openInformation(shell, "RAML2SpringBootPlugin",
						"Error encountered while generating Spring Boot code for RAML: " + ramlFilePath
								+ ", Error Message: " + writer.toString());
			}
		} else if (errorMessage != null) {
			MessageDialog.openInformation(shell, "RAML2SpringBootPlugin",
					"RAML to Spring Boot was NOT executed for " + ramlFilePath + " because " + errorMessage);
		}

		return null;
	}
}
