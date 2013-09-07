/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.tools.aerogear.hybrid.ui.wizard.export;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jboss.tools.aerogear.hybrid.core.HybridCore;
import org.jboss.tools.aerogear.hybrid.core.HybridProject;
import org.jboss.tools.aerogear.hybrid.core.extensions.ProjectGenerator;
import org.jboss.tools.aerogear.hybrid.core.platform.AbstractProjectGeneratorDelegate;
import org.jboss.tools.aerogear.hybrid.ui.HybridUI;

public class NativeProjectExportWizard extends Wizard implements IExportWizard {

	private static final String DIALOG_SETTINGS_KEY = "NativeProjectExportWizard";
	private NativeProjectDestinationPage pageOne;
	private IStructuredSelection initialSelection;
	
	public NativeProjectExportWizard() {
		setWindowTitle("Export Native Platform Project");
		this.setNeedsProgressMonitor(true);
		IDialogSettings workbenchSettings= HybridUI.getDefault().getDialogSettings();
		IDialogSettings section= workbenchSettings.getSection(DIALOG_SETTINGS_KEY);
		setDialogSettings(section);

	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		initialSelection = selection;
	}

	@Override
	public boolean performFinish() {
		List<ProjectGenerator>generators = pageOne.getSelectedPlatforms();
		
		List<HybridProject> projects = pageOne.getSelectedProjects();
		ArrayList<AbstractProjectGeneratorDelegate> delegates = new ArrayList<AbstractProjectGeneratorDelegate>();
		
		//Collect delegates
		for (HybridProject project : projects) {
			for (ProjectGenerator generator: generators) {
				try{
					AbstractProjectGeneratorDelegate dlg = generator.createDelegate(project.getProject(), new File(pageOne.getDestinationDirectory(),project.getProject().getName()));
					delegates.add(dlg);
					
				}catch(CoreException e){
					HybridCore.log(IStatus.ERROR, "Error creating project generator delegate for " +generator.getPlatform(), e);
				}
			}
		}
		NativeProjectExportOperation op = new NativeProjectExportOperation(delegates,pageOne);
		try {
			getContainer().run(true, true, op);
		} catch (InvocationTargetException e) {
			if (e.getTargetException() != null) {
				ErrorDialog.openError(getShell(), "Error exporting native projects",null, 
						new Status(IStatus.ERROR, HybridUI.PLUGIN_ID, "Error while exporting native projects", e.getTargetException() ));
				return false;
			}
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		IStatus status= op.getStatus();
		if (!status.isOK()) {
			ErrorDialog.openError(getShell(), "Native Project Export Error", null, status);
			return !(status.matches(IStatus.ERROR));
		}	
		savePageSettings();
		return true;
	}

	private void savePageSettings() {
		IDialogSettings workbenchSettings= HybridUI.getDefault().getDialogSettings();
		IDialogSettings section= workbenchSettings.getSection(DIALOG_SETTINGS_KEY);
		if(section == null ){
			section = workbenchSettings.addNewSection(DIALOG_SETTINGS_KEY);
		}
		setDialogSettings(section);
		pageOne.saveWidgetValues();
	}

	@Override
	public void addPages() {
		super.addPages();
		pageOne = new NativeProjectDestinationPage(getWindowTitle(), initialSelection);
		pageOne.setTitle("Export Native Platform Project");
		pageOne.setDescription("Exports a platform native project from a hybrid mobile project type");
		addPage( pageOne );
	}
	
}
