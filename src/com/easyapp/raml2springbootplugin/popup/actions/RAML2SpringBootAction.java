package com.easyapp.raml2springbootplugin.popup.actions;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.easyapp.raml2springbootplugin.generate.RAML2SpringBoot;

public class RAML2SpringBootAction implements IObjectActionDelegate {

	private Shell shell = null;
	private IProject project = null;
	private String ramlFilePath = null;
	private String sourceDirectory = null;
	private String errorMessage = null;
	private String basePackage = null;

	private void getBasePackage(final String directoryPath) {
		try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directoryPath))) {
			for (final Path path : directoryStream) {
				final String pathString = path.toString();

				if (pathString.endsWith("Application.java")) {
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

	/**
	 * Constructor for Action1.
	 */
	public RAML2SpringBootAction() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(final IAction action, final IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(final IAction action) {
		if (errorMessage == null) {
			getBasePackage(sourceDirectory);

			if (!"".equals(basePackage)) {
				try {
					RAML2SpringBoot.generate(ramlFilePath, sourceDirectory, basePackage);
					MessageDialog.openInformation(shell, "RAML2SpringBootPlugin",
							"Successfully executed RAML to Spring Boot");
				} catch (final Throwable e) {
					StringWriter writer = new StringWriter();
					e.printStackTrace(new PrintWriter(writer));
					MessageDialog.openInformation(shell, "RAML2SpringBootPlugin",
							"Error encountered while generating Spring Boot code for RAML: " + ramlFilePath
									+ ", Error Message: " + writer.toString());
				} finally {
					try {
						project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
					} catch (final CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}

		if (errorMessage != null) {
			MessageDialog.openInformation(shell, "RAML2SpringBootPlugin",
					"RAML to Spring Boot was NOT executed for " + ramlFilePath + " because " + errorMessage);
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(final IAction action, final ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			final Object selectedObj = structuredSelection.getFirstElement();

			if (selectedObj instanceof IFile) {
				final IFile selectedFile = (IFile) selectedObj;
				final String relativePath = selectedFile.getProjectRelativePath().toString();
				project = selectedFile.getProject();

				if (!relativePath.startsWith("src/main/resources")) {
					errorMessage = "the RAML file is NOT in <Project>/src/main/resources";
				} else {
					ramlFilePath = selectedFile.getRawLocation().toString();
					sourceDirectory = ramlFilePath.substring(0, ramlFilePath.indexOf(relativePath)) + "src/main/java";
				}
			}
		}
	}
}
