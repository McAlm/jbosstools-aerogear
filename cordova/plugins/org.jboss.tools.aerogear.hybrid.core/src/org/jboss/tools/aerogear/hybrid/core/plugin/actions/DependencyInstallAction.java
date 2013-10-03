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
package org.jboss.tools.aerogear.hybrid.core.plugin.actions;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jboss.tools.aerogear.hybrid.core.HybridProject;
import org.jboss.tools.aerogear.hybrid.core.platform.IPluginInstallationAction;
import org.jboss.tools.aerogear.hybrid.core.plugin.CordovaPluginManager;
import org.jboss.tools.aerogear.hybrid.core.plugin.FileOverwriteCallback;
import org.jboss.tools.aerogear.hybrid.core.plugin.registry.CordovaPluginRegistryManager;
import org.jboss.tools.aerogear.hybrid.core.plugin.registry.CordovaRegistryPlugin;
import org.jboss.tools.aerogear.hybrid.core.plugin.registry.CordovaRegistryPluginVersion;

public class DependencyInstallAction implements IPluginInstallationAction {

	private final HybridProject project;
	private final String dependencyPluginId;
	private final URI uri;
	private final String commit;
	private final String  subdir;
	private final FileOverwriteCallback overwriteCallback;
	
	
	public DependencyInstallAction(String dependencyId, URI uri,
			String commit, String subdir, HybridProject project, FileOverwriteCallback overwrite) {
		this.project = project;
		this.uri = uri;
		this.dependencyPluginId = dependencyId;
		this.commit = commit;
		this.subdir = subdir;
		this.overwriteCallback = overwrite;
	}

	@Override
	public void install() throws CoreException {
		CordovaPluginManager pluginManager = project.getPluginManager();
		if(!pluginManager.isPluginInstalled(dependencyPluginId)){
			if( uri != null){
				pluginManager.installPlugin(uri,commit,subdir, overwriteCallback, new NullProgressMonitor());
			}else{//install from registry
				CordovaPluginRegistryManager manager = new CordovaPluginRegistryManager(CordovaPluginRegistryManager.DEFAULT_REGISTRY_URL);
				CordovaRegistryPlugin plugin = manager.getCordovaPluginInfo(dependencyPluginId);
				List<CordovaRegistryPluginVersion> versions = plugin.getVersions();
				for (CordovaRegistryPluginVersion version : versions) {
					if(plugin.getLatestVersion().equals(version.getVersionNumber())){
						File f =manager.getInstallationDirectory(version, new NullProgressMonitor());
						pluginManager.installPlugin(f, this.overwriteCallback, new NullProgressMonitor());
					}
				}
			}
			
		}
	}

	@Override
	public void unInstall() throws CoreException {
		// Let user uninstall them manually.
//		CordovaPluginManager pluginManager = project.getPluginManager();
//		pluginManager.unInstallPlugin(dependencyPluginId, new NullProgressMonitor());
	}

	@Override
	public String[] filesToOverwrite() {
		// no need to report here.
		return null;
	}

}
