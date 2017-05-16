package com.easyapp.raml2springbootplugin.handler;

import java.io.PrintWriter;
import java.io.StringWriter;

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

import com.easyapp.raml2springbootplugin.config.CodeGenConfig;
import com.easyapp.raml2springbootplugin.generate.RAML2SpringBoot;

public class RAML2SpringBootHandler extends AbstractHandler {
	private CodeGenConfig codeGenConfig = null;
	private IProject project = null;

	private String getCodeGenConfig(final ISelection selection) throws Exception {
		if (selection != null && selection instanceof IStructuredSelection) {
			final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			final Object selectedObj = structuredSelection.getFirstElement();

			if (selectedObj != null && selectedObj instanceof IFile) {
				final IFile selectedFile = (IFile) selectedObj;
				final String relativePath = selectedFile.getProjectRelativePath().toString();
				project = selectedFile.getProject();

				if (!relativePath.startsWith("src/main/resources")) {
					return "RAML file is not in the <Project>/src/main/resources directory";
				} else {
					codeGenConfig = new CodeGenConfig(selectedFile.getRawLocation().toString(), relativePath);
				}
			}
		}

		if (codeGenConfig == null) {
			return "Could not get all the configuration for the RAML file";
		} else {
			return codeGenConfig.getConfigError();
		}
	}

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final Shell shell = HandlerUtil.getActiveShell(event);
		final MessageDialog dialog = new MessageDialog(shell, "RAML2SpringBootPlugin", null,
						"Operation in progress. Please wait...", MessageDialog.INFORMATION, new String[] { "Ok" }, 0);
		dialog.setBlockOnOpen(false);
		dialog.open();

		try {
			final String errorMessage = getCodeGenConfig(HandlerUtil.getActiveMenuSelection(event));

			if (errorMessage == null) {
				RAML2SpringBoot.generate(codeGenConfig);

				dialog.close();
				MessageDialog.openInformation(shell, "RAML2SpringBootPlugin",
						"Successfully executed RAML to Spring Boot");

				if (project != null) {
					try {
						project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
					} catch (final CoreException e) {
						e.printStackTrace();
					}
				}
			} else {
				dialog.close();
				MessageDialog.openInformation(shell, "RAML2SpringBootPlugin",
						"RAML to Spring Boot was NOT executed because " + errorMessage);
			}
		} catch (final Throwable e) {
			final StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			e.printStackTrace();

			dialog.close();
			MessageDialog.openInformation(shell, "RAML2SpringBootPlugin",
					"Error encountered while generating Spring Boot code, Error Message: " + writer.toString());
		}

		return null;
	}
}
