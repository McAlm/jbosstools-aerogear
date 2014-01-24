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

import org.eclipse.ant.launching.IAntLaunchConstants;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.jboss.tools.aerogear.hybrid.android.core.AndroidConstants;
import org.jboss.tools.aerogear.hybrid.android.core.AndroidCore;
import org.jboss.tools.aerogear.hybrid.core.HybridProject;
import org.jboss.tools.aerogear.hybrid.core.platform.AbstractNativeBinaryBuildDelegate;

public class BuildDelegate extends AbstractNativeBinaryBuildDelegate {

	private File binaryDirectory;
	public BuildDelegate() {
	}

	@Override
	public void buildNow(IProgressMonitor monitor) throws CoreException {
		if(monitor.isCanceled())
			return;
		
		//TODO: use extension point to create
		// the generator.
		AndroidProjectGenerator creator = new AndroidProjectGenerator(this.getProject(), getDestination(),"android"); 
           		
		SubProgressMonitor generateMonitor = new SubProgressMonitor(monitor, 1);
		File projectDirectory = creator.generateNow(generateMonitor);
		monitor.worked(1);
		if(monitor.isCanceled() ){
			return;
		}
		buildProject(projectDirectory, monitor);
		monitor.done();
	}
	
	public void buildProject(File projectLocation,IProgressMonitor monitor) throws CoreException{
		doBuildProject(projectLocation, false, monitor);
	}
	
	public void buildLibraryProject(File projectLocation,IProgressMonitor monitor) throws CoreException{
		doBuildProject(projectLocation, true, monitor);
	}
	
	private void doBuildProject(File projectLocation, boolean isLibrary, IProgressMonitor monitor) throws CoreException{
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType antLaunchConfigType = launchManager.getLaunchConfigurationType(IAntLaunchConstants.ID_ANT_LAUNCH_CONFIGURATION_TYPE);
		if(antLaunchConfigType == null ){
			throw new CoreException(new Status(IStatus.ERROR, AndroidCore.PLUGIN_ID, "Ant launch configuration type is not available"));
		}
		ILaunchConfigurationWorkingCopy wc = antLaunchConfigType.newInstance(null, "Android project builder"); //$NON-NLS-1$
		wc.setContainer(null);
		File buildFile = new File(projectLocation, AndroidConstants.FILE_XML_BUILD);
		if(!buildFile.exists()){
			throw new CoreException(new Status(IStatus.ERROR, AndroidCore.PLUGIN_ID, "build.xml does not exist in "+ projectLocation.getPath()));
		}
		wc.setAttribute(IExternalToolConstants.ATTR_LOCATION, buildFile.getPath());
		String target = null;
		if(isLibrary){
			target = "jar";
		}else{
			target = "debug";
			if(isRelease()){
				target = "release";
			}
		}
		wc.setAttribute(IAntLaunchConstants.ATTR_ANT_TARGETS, target);
		wc.setAttribute(IAntLaunchConstants.ATTR_DEFAULT_VM_INSTALL, true);

		wc.setAttribute(IExternalToolConstants.ATTR_LAUNCH_IN_BACKGROUND, false);
		wc.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, true);
		ILaunchConfiguration launchConfig = wc.doSave();
        if (monitor.isCanceled()){
        	return;
        }
		
        launchConfig.launch(ILaunchManager.RUN_MODE, monitor, true, true);
        
        binaryDirectory = new File(projectLocation, AndroidConstants.DIR_BIN);
        if(isLibrary){
        	//no checks for libs
        }else{
        	HybridProject hybridProject = HybridProject.getHybridProject(getProject());
        	if(isRelease()){
        		setBuildArtifact(new File(binaryDirectory,hybridProject.getBuildArtifactAppName()+"-release-unsigned.apk" ));
        	}else{
        		setBuildArtifact(new File(binaryDirectory,hybridProject.getBuildArtifactAppName()+"-debug.apk" ));
        	}
        	if(!getBuildArtifact().exists()){
        		throw new CoreException(new Status(IStatus.ERROR, AndroidCore.PLUGIN_ID, "Build failed... Build artifact does not exist"));
        	}
        }
	}
	

	/**
	 * Returns the directory where build artifacts are stored. 
	 * Will return null if the build is not yet complete or 
	 * {@link #buildNow(IProgressMonitor)} is not called yet for this instance.
	 * @return
	 */
	public File getBinaryDirectory() {
		return binaryDirectory;
	}

}
