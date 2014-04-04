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
package org.jboss.tools.aerogear.hybrid.ui.wizard.project;

import static org.jboss.tools.aerogear.hybrid.core.platform.PlatformConstants.DIR_DOT_CORDOVA;
import static org.jboss.tools.aerogear.hybrid.core.platform.PlatformConstants.DIR_MERGES;
import static org.jboss.tools.aerogear.hybrid.core.platform.PlatformConstants.DIR_PLUGINS;
import static org.jboss.tools.aerogear.hybrid.core.platform.PlatformConstants.DIR_WWW;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.jboss.tools.aerogear.hybrid.core.HybridCore;
import org.jboss.tools.aerogear.hybrid.core.HybridProject;
import org.jboss.tools.aerogear.hybrid.core.config.Widget;
import org.jboss.tools.aerogear.hybrid.core.config.WidgetModel;
import org.jboss.tools.aerogear.hybrid.core.engine.HybridMobileEngine;
import org.jboss.tools.aerogear.hybrid.core.extensions.PlatformSupport;
import org.jboss.tools.aerogear.hybrid.core.internal.libraries.CordovaLibraryJsContainerInitializer;
import org.jboss.tools.aerogear.hybrid.core.internal.util.ConfigJSon;
import org.jboss.tools.aerogear.hybrid.core.internal.util.FileUtils;
import org.jboss.tools.aerogear.hybrid.core.natures.HybridAppNature;
import org.jboss.tools.aerogear.hybrid.ui.HybridUI;
import org.osgi.framework.Bundle;

public class HybridProjectCreator {
	
	private static final String[] COMMON_PATHS={ DIR_DOT_CORDOVA, DIR_MERGES, 
		DIR_PLUGINS, DIR_WWW };
	
	/**
	 * Creates a hybrid project with the given name and location. Location can be null, if location is null 
	 * the default location will be used for creating the project. Uses the basic hello world template 
	 * to populate the initial files.
	 *  
	 * @param projectName
	 * @param location
	 * @param monitor
	 * @throws CoreException
	 */
	public IProject createBasicTemplatedProject( String projectName, URI location, String appName, String appID, HybridMobileEngine engine, IProgressMonitor monitor ) throws CoreException {
		if(monitor == null )
			monitor = new NullProgressMonitor();
		
		IProject project = createProject(projectName, location, appName, appID, engine, monitor);
		addTemplateFiles(project, new SubProgressMonitor(monitor, 5));	
		project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		updateConfig(project, appName, appID, new SubProgressMonitor(monitor, 1));
		project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		return project;
	}
	
	
	/**
	 * Creates a hybrid project with the given name and location. Location can be null, if location is null 
	 * the default location will be used for creating the project. Does not add any files to the project 
	 * including the config.xml file. If location has existing files they are kept.
	 *  
	 * @param projectName
	 * @param location
	 * @param monitor
	 * @throws CoreException
	 */
	public IProject createProject( String projectName, URI location,  String appName, String appID, HybridMobileEngine engine, IProgressMonitor monitor ) throws CoreException {
		if(monitor == null )
			monitor = new NullProgressMonitor();
		IProject project = createHybridMobileProject(projectName, location, new SubProgressMonitor(monitor, 1));
		addCommonPaths(project, monitor);
		addPlatformPaths(project, new SubProgressMonitor( monitor, 1));
		addConfigJSon(project, appName, appID, engine, monitor);
		setUpJavaScriptProject(monitor, project);
		return project;
	}

	private IProject createHybridMobileProject(String projectName,
			URI location, IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(projectName, "Project name is null, can not create a project without a name");
		IProject project = createBasicProject(projectName, location, new SubProgressMonitor(monitor, 5));
		addNature(project, new SubProgressMonitor(monitor, 5));
		return project;
	}
	
	


	private void setUpJavaScriptProject(IProgressMonitor monitor,
			IProject project) throws JavaScriptModelException {
		IIncludePathEntry entry = JavaScriptCore.newContainerEntry(new Path(CordovaLibraryJsContainerInitializer.CONTAINER_ID));
		IJavaScriptProject javascriptProject = JavaScriptCore.create(project);
		IIncludePathEntry[] entries = javascriptProject.getRawIncludepath();
		IIncludePathEntry[] newEntries = Arrays.copyOf(entries, entries.length +1);
		newEntries[newEntries.length -1 ] = entry;
		javascriptProject.setRawIncludepath(newEntries, monitor);
	}

	
	private void updateConfig(IProject project, String appName, String appID, IProgressMonitor  monitor) throws CoreException{
		HybridProject hybridProject = HybridProject.getHybridProject(project);
		try {
			WidgetModel model = WidgetModel.getModel(hybridProject);
			Widget w = model.getWidgetForEdit();
			w.setId(appID);
			w.setName(appName);
			model.save();
		} catch (CoreException e) {
			HybridCore.log(IStatus.ERROR, "Error updating application name and id to config.xml", e);
		}
		
	}


	private void addTemplateFiles(IProject project, IProgressMonitor monitor) throws CoreException{
		Bundle bundle = HybridUI.getDefault().getBundle();
	    URL source = bundle.getEntry("/templates/www");
	    IFolder folder = project.getFolder(DIR_WWW);
	    if (!folder.exists()){
	    	folder.create(true, true, monitor);
	    }
	    
		try {
			FileUtils.directoryCopy(source, FileUtils.toURL(folder.getLocation().toFile()));
			monitor.done();
		} catch (MalformedURLException e) {
			throw new CoreException(new Status(IStatus.ERROR, HybridUI.PLUGIN_ID, "Error adding template files", e));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, HybridUI.PLUGIN_ID, "Error adding template files", e));
		}
	}


	private void addConfigJSon(IProject project, String applicationName, String applicationID, HybridMobileEngine engine, IProgressMonitor monitor) throws CoreException{
		ConfigJSon configJson = new ConfigJSon();
		configJson.setId(applicationID);
		configJson.setName(applicationName);
		configJson.setEngineInfo(engine);
		configJson.persist(project);
	}


	private void addPlatformPaths(IProject project, IProgressMonitor monitor) throws CoreException{
		List<PlatformSupport> platforms = HybridCore.getPlatformSupports();
		IPath merges = new Path(DIR_MERGES);
		for (PlatformSupport platform : platforms) {
			IFolder folder = project.getFolder(merges.append(platform.getPlatformId()));
			if(!folder.exists()){
				createFolder(folder, monitor);
			}
		}
		monitor.done();
	}


	private void addCommonPaths(IProject project, IProgressMonitor monitor) throws CoreException {
		SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, COMMON_PATHS.length);
		for (String path : COMMON_PATHS) {
			IFolder folder = project.getFolder(path);
			if( !folder.exists()){
				createFolder(folder,subMonitor);
			}
			subMonitor.worked(1);
		}
		subMonitor.done();
	}

	private void createFolder(IFolder folder, IProgressMonitor monitor) throws CoreException {
		IContainer parent = folder.getParent();
		IFolder parentFolder = (IFolder)parent.getAdapter(IFolder.class);
		if ( parentFolder != null ) {
			createFolder(parentFolder, monitor);
		}
		if ( !folder.exists() ) {
			folder.create(false, true, monitor);
		}
	}

	private void addNature(IProject project, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = project.getDescription();
	    String[] oldNatures = description.getNatureIds();
	    List<String> natureList =  new ArrayList<String>();
	    natureList.addAll(Arrays.asList(oldNatures));
	    
		if( !project.hasNature(HybridAppNature.NATURE_ID ) ){
			natureList.add(HybridAppNature.NATURE_ID);
		}
		
		if( !project.hasNature( JavaScriptCore.NATURE_ID )){
			natureList.add(JavaScriptCore.NATURE_ID);
		}
		
	    description.setNatureIds(natureList.toArray(new String[natureList.size()]));
	    project.setDescription(description, monitor);
	    
	}
	


	private IProject createBasicProject( String name, URI location, IProgressMonitor monitor ) throws CoreException {
		
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject newProject = workspaceRoot.getProject(name);
		
		if ( !newProject.exists() ){
			IProjectDescription description = newProject.getWorkspace().newProjectDescription(name);
			if( location != null ){
				description.setLocationURI(location);
			}
			
			newProject.create(description, monitor);
			if( !newProject.isOpen() ){
				newProject.open(monitor);
			}
		}
		
		return newProject;
	}
	
}
