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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jboss.tools.aerogear.hybrid.core.plugin.registry.CordovaPluginRegistryManager;
import org.jboss.tools.aerogear.hybrid.core.plugin.registry.CordovaRegistryPlugin;
import org.jboss.tools.aerogear.hybrid.core.plugin.registry.CordovaRegistryPluginInfo;
import org.jboss.tools.aerogear.hybrid.core.plugin.registry.CordovaRegistryPluginVersion;
import org.jboss.tools.aerogear.hybrid.ui.HybridUI;

@SuppressWarnings("restriction")
public class RegistryConfirmPage extends WizardPage {

	private CordovaPluginViewer pluginViewer;
	final CordovaPluginRegistryManager client = new CordovaPluginRegistryManager(CordovaPluginRegistryManager.DEFAULT_REGISTRY_URL);
	private static final String PAGE_NAME = "Fetch from Registry";
	private static final String PAGE_TITLE = "Confirm plug-ins to be downloaded from registry";
	private static final String PAGE_DESC = "Confirm the plug-ins to be downloaded and installed from registry or go back to select again.";
	
	
	private class DetailedPluginInfoRetrieveJob extends Job{
		
		private List<String> pluginNames;

		public DetailedPluginInfoRetrieveJob(List<String> pluginNames) {
			super("Retrieve Cordova Plug-in Details");
			this.pluginNames = pluginNames;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final ArrayList<CordovaRegistryPlugin> plugins = new ArrayList<CordovaRegistryPlugin>();
			try {
				for (String plugin : pluginNames) {
					if(monitor.isCanceled())
						return Status.CANCEL_STATUS;
					plugins.add(client.getCordovaPluginInfo(plugin));
				}
			} catch (CoreException e) {
				return new Status(e.getStatus().getSeverity(), HybridUI.PLUGIN_ID, "Problem while getting Cordova plugin details", e);
			}
			pluginViewer.getControl().getDisplay().asyncExec(new Runnable() {
				
				@Override
				public void run() {
					Control c = pluginViewer.getViewer().getControl();
					if(c != null && !c.isDisposed()){
						pluginViewer.getViewer().setInput(plugins);
					}
				}
			});
			return Status.OK_STATUS; 
		}
		
	}

	protected RegistryConfirmPage() {
		super(PAGE_NAME,PAGE_TITLE,HybridUI.getImageDescriptor(HybridUI.PLUGIN_ID, CordovaPluginWizard.IMAGE_WIZBAN));
		setDescription(PAGE_DESC);	
	}

	@Override
	public void createControl(Composite parent) {
		pluginViewer = new CordovaPluginViewer();
		pluginViewer.setHeaderVisible(false);
		pluginViewer.createControl(parent);
		setControl(pluginViewer.getControl());
	}
	
	void setSelectedPlugins(List<CordovaRegistryPluginInfo> selected) {
		List<String> pluginNames = new ArrayList<String>(selected.size());
		for (CordovaRegistryPluginInfo cordovaRegistryPluginInfo : selected) {
			pluginNames.add(cordovaRegistryPluginInfo.getName());
		}
		DetailedPluginInfoRetrieveJob job = new DetailedPluginInfoRetrieveJob(pluginNames);
		job.schedule();
	}
	
	public List<CordovaRegistryPluginVersion> getSelectedPluginVersions(){
			IStructuredSelection selection = (IStructuredSelection) pluginViewer.getSelection();
			if(selection == null || selection.isEmpty())
				return Collections.emptyList();
			return selection.toList();
	}

}
