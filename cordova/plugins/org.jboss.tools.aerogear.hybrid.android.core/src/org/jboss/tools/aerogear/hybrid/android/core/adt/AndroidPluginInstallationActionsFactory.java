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
package org.jboss.tools.aerogear.hybrid.android.core.adt;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.aerogear.hybrid.android.core.AndroidCore;
import org.jboss.tools.aerogear.hybrid.core.platform.AbstractPluginInstallationActionsFactory;
import org.jboss.tools.aerogear.hybrid.core.platform.IPluginInstallationAction;
import org.jboss.tools.aerogear.hybrid.core.platform.PlatformConstants;
import org.jboss.tools.aerogear.hybrid.core.plugin.actions.CopyFileAction;
import org.jboss.tools.aerogear.hybrid.core.plugin.actions.CreateFileAction;
import org.jboss.tools.aerogear.hybrid.core.plugin.actions.XMLConfigFileAction;

public class AndroidPluginInstallationActionsFactory extends AbstractPluginInstallationActionsFactory
{
	//same as copy only removes empty directories. 
	public static class AndroidSourceFileAction extends CopyFileAction{
		private File target;

		public AndroidSourceFileAction(File source, File target) {
			super(source, target);
			this.target = target;
		}
		
		@Override
		public void unInstall() throws CoreException {
			super.unInstall();
			
			//Remove the empty package directory structure
			File dir = findHighestEmptyParent(target);
			try {
				if( dir.isDirectory() )
					FileUtils.deleteDirectory(dir);
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, AndroidCore.PLUGIN_ID, "Error deleting package name structure",e));
			}
			
		}

		private File findHighestEmptyParent(File file) {
			File parent = file.getParentFile();
			if(parent == null ){
				return file;
			}
			File[] children = parent.listFiles();
			if(children.length>1){//count the empty dir as 1
				return file;
			}
			return findHighestEmptyParent(parent);
		}
		
	}

	@Override
	public IPluginInstallationAction getSourceFileAction(String src,
			String targetDir, String framework, String pluginId, String compilerFlags) {
		File source = new File(getPluginDirectory(), src);
		File target = new File(getProjectDirectory(), targetDir);
		return new AndroidSourceFileAction(source, target);
	}

	@Override
	public IPluginInstallationAction getResourceFileAction(String src) {
		throw new UnsupportedOperationException("Not relevant for Android");
	}

	@Override
	public IPluginInstallationAction getHeaderFileAction(String src, String targetDir, String pluginId) {
		throw new UnsupportedOperationException("Not relevant for Android");
	}

	@Override
	public IPluginInstallationAction getAssetAction(String src, String target) {
		File source = new File(getPluginDirectory(),src);
		File targetDir = new File(AndroidProjectUtils.getPlatformWWWDirectory(getProjectDirectory()),target);
		return new CopyFileAction(source, targetDir);
	}

	@Override
	public IPluginInstallationAction getConfigFileAction(String target,
			String parent, String value) {
		
		File configFile = new File(getProjectDirectory(),target);
		if(target.endsWith(PlatformConstants.FILE_XML_CONFIG)){
			//TODO: return the global config.xml action
			return null; 
		}
		return new XMLConfigFileAction(configFile, parent, value);
	}

	@Override
	public IPluginInstallationAction getLibFileAction(String src, String arch) {
		File source  = new File(getPluginDirectory(),src);
		File target = new File (getProjectDirectory(), "libs");
		if( !target.isDirectory()){//it should be created during project generation but just in case.
			target.mkdir();
		}
		CopyFileAction action = new CopyFileAction(source, target);
		return action;
	}

	@Override
	public IPluginInstallationAction getFrameworkAction(String src, String weak) {
		throw new UnsupportedOperationException("Not relevant for Android");
	}

	@Override
	public IPluginInstallationAction getJSModuleAction(String src,
			String pluginId) {
		File source = new File(getPluginDirectory(), src);
		File target = new File(AndroidProjectUtils.getPlatformWWWDirectory(getProjectDirectory()), 
				PlatformConstants.DIR_PLUGINS+File.separator+pluginId+File.separator+src );
		if(!target.getParentFile().isDirectory()){// create the target directory
			target.getParentFile().mkdirs();
		}
		CopyFileAction action = new CopyFileAction(source, target);
		return action;
	}

	@Override
	public IPluginInstallationAction getCreatePluginJSAction(String content) {
		File pluginJs = new File(AndroidProjectUtils.getPlatformWWWDirectory(getProjectDirectory()), PlatformConstants.FILE_JS_CORDOVA_PLUGIN);
		CreateFileAction action = new CreateFileAction(content, pluginJs);
		return action;
	}


}
