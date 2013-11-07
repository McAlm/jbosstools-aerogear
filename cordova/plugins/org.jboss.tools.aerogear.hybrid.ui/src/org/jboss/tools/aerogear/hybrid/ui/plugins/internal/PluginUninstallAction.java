/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *       Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.aerogear.hybrid.ui.plugins.internal;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.aerogear.hybrid.core.HybridProject;
import org.jboss.tools.aerogear.hybrid.core.plugin.CordovaPlugin;
import org.jboss.tools.aerogear.hybrid.ui.HybridUI;

public class PluginUninstallAction extends Action{
	
	private CordovaPlugin plugin;
	public PluginUninstallAction() {
		super("Remove Cordova Plug-in");
	}
	
	public PluginUninstallAction(CordovaPlugin cordovaPlugin) {
		this();
		this.plugin = cordovaPlugin;
	}

	@Override
	public void run() {
		if(this.plugin == null ){
			IStructuredSelection selection = getSelection();
			if(selection.isEmpty())
				return;
			Object o = selection.getFirstElement();
			if(o instanceof CordovaPlugin ){
				this.plugin = (CordovaPlugin)o;
			}else{
				return;
			}
		}
		
		if(!MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
				"Uninstall Cordova Plugin",
				"Uninstall "+this.plugin.getName() +" ("+plugin.getId()+") plug-in?")){
			return;
		}
		
		final HybridProject project = HybridProject.getHybridProject(plugin.getFolder().getProject());
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
				InterruptedException {
					try {
						project.getPluginManager().unInstallPlugin(plugin.getId(), new NullProgressMonitor());
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}

				}
			});
		} catch (InvocationTargetException e) {
			Throwable t = e;
			if(e.getTargetException() != null ){
				t =e.getTargetException();
			}
			ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Error removing Cordova plug-in",null, 
					new Status(IStatus.ERROR, HybridUI.PLUGIN_ID, "Error when removing the Cordova plug-in", t ));
		} catch (InterruptedException e) {
			HybridUI.log(IStatus.ERROR, "Error while removing a Cordova plugin " ,e);
		}
	}
	
	private IStructuredSelection getSelection(){
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if(window != null ){
			ISelection selection = window.getSelectionService().getSelection();
			if(selection instanceof IStructuredSelection)
				return (IStructuredSelection)selection;
		}
		return StructuredSelection.EMPTY;
	}
	
}