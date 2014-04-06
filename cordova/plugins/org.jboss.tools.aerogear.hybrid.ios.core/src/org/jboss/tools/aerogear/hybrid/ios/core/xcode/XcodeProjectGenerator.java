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
package org.jboss.tools.aerogear.hybrid.ios.core.xcode;

import static org.jboss.tools.aerogear.hybrid.core.engine.HybridMobileLibraryResolver.VAR_APP_NAME;
import static org.jboss.tools.aerogear.hybrid.core.internal.util.FileUtils.directoryCopy;
import static org.jboss.tools.aerogear.hybrid.core.internal.util.FileUtils.fileCopy;
import static org.jboss.tools.aerogear.hybrid.core.internal.util.FileUtils.templatedFileCopy;
import static org.jboss.tools.aerogear.hybrid.core.internal.util.FileUtils.toURL;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.aerogear.hybrid.core.HybridProject;
import org.jboss.tools.aerogear.hybrid.core.config.Widget;
import org.jboss.tools.aerogear.hybrid.core.config.WidgetModel;
import org.jboss.tools.aerogear.hybrid.core.engine.HybridMobileLibraryResolver;
import org.jboss.tools.aerogear.hybrid.core.platform.AbstractProjectGeneratorDelegate;
import org.jboss.tools.aerogear.hybrid.core.platform.PlatformConstants;
import org.jboss.tools.aerogear.hybrid.ios.core.IOSCore;

import com.dd.plist.ASCIIPropertyListParser;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListParser;

public class XcodeProjectGenerator extends AbstractProjectGeneratorDelegate{
	
	public XcodeProjectGenerator(){
		super();
	}
	
	public XcodeProjectGenerator(IProject project, File generationFolder, String platform) {
		init(project, generationFolder, platform);
	}
	
	@Override
	protected void generateNativeFiles(HybridMobileLibraryResolver resolver) throws CoreException{
		
		try{
			HybridProject hybridProject = HybridProject.getHybridProject(getProject());
			if(hybridProject == null ){
				throw new CoreException(new Status(IStatus.ERROR, IOSCore.PLUGIN_ID, "Not a hybrid mobile project, can not generate files"));
			}

			File destinationDir = getDestination();
			Path destinationPath = new Path(destinationDir.toString());
			
			String name = hybridProject.getBuildArtifactAppName();
			IPath prjPath = destinationPath.append(name);
			Widget widgetModel = WidgetModel.getModel(hybridProject).getWidgetForRead();
			String packageName = widgetModel.getId();
			
			File prjdir = prjPath.toFile();
			if( !prjdir.exists() ){//create the project directory
				prjdir.mkdirs();
			}
			
			// /${project_name}
			directoryCopy(resolver.getTemplateFile(new Path(VAR_APP_NAME)),toURL(prjdir));		
			
			// cordova
			IPath cordovaScriptPath = destinationPath.append("cordova");
			directoryCopy(resolver.getTemplateFile(cordovaScriptPath.makeRelativeTo(destinationPath)), 
					toURL(cordovaScriptPath.toFile()));
			
			File wwwwCopyScript = cordovaScriptPath.append("lib").append("copy-www-build-step.sh").toFile();
			if(wwwwCopyScript.exists()){
				wwwwCopyScript.setExecutable(true);
			}
			
			HashMap<String, String > values = new HashMap<String, String>();
			values.put("__TESTING__", name);
			values.put("__PROJECT_NAME__", name); // replaced __TESTING__ after 3.4.0
			values.put("--ID--", packageName);
			
			// /${project_name}/${project_name}-Info.plist
			IPath templatePath = prjPath.append(name+"-Info.plist");
			templatedFileCopy(resolver.getTemplateFile(new Path(VAR_APP_NAME+"/"+VAR_APP_NAME+"-Info.plist")),
					toURL(templatePath.toFile()), 
					values);
			// /${project_name}/${project_name}-Prefix.pch
			templatePath = prjPath.append(name+"-Prefix.pch");
			templatedFileCopy(resolver.getTemplateFile(new Path(VAR_APP_NAME+"/"+VAR_APP_NAME+"-Prefix.pch")),
					toURL(templatePath.toFile()),
					values);
			
			// /${project_name}.xcodeproj/project.pbxproj
			IPath xcodeprojDirPath = destinationPath.append(name+".xcodeproj");
			File xcodeDir = xcodeprojDirPath.toFile();//create the xcodeproj folder first
			if(!xcodeDir.exists()){
				xcodeDir.mkdir();
			}
			IPath xcodeprojectFilePath = xcodeprojDirPath.append("project.pbxproj");
			File xcodeprojectFile = xcodeprojectFilePath.toFile();
			templatedFileCopy(resolver.getTemplateFile(new Path(VAR_APP_NAME+".xcodeproj/project.pbxproj")),
					toURL(xcodeprojectFile), 
					values);
			
			// /${project_name}/Classes/AppDelegate.h
			IPath classesPath = prjPath.append("Classes");
			templatePath = classesPath.append("AppDelegate.h");
			templatedFileCopy(resolver.getTemplateFile(new Path(VAR_APP_NAME+"/Classes/AppDelegate.h")),
					toURL(templatePath.toFile()),
					values);
			// /${project_name}/Classes/AppDelegate.m
			templatePath = classesPath.append("AppDelegate.m");
			templatedFileCopy(resolver.getTemplateFile(new Path(VAR_APP_NAME+"/Classes/AppDelegate.m")),
					toURL(templatePath.toFile()),
					values);
			// /${project_name}/Classes/MainViewController.h
			templatePath = classesPath.append("MainViewController.h");
			templatedFileCopy(resolver.getTemplateFile(new Path(VAR_APP_NAME+"/Classes/MainViewController.h")),
					toURL(templatePath.toFile()),
					values);

			// /${project_name}/Classes/MainViewController.h
			templatePath = classesPath.append("MainViewController.m");
			templatedFileCopy(resolver.getTemplateFile(new Path(VAR_APP_NAME+"/Classes/MainViewController.m")),
					toURL(templatePath.toFile()),
					values);
			// /${project_name}/main.m
			templatePath = prjPath.append("main.m");
			templatedFileCopy(resolver.getTemplateFile(new Path(VAR_APP_NAME+"/main.m")),
					toURL(templatePath.toFile()),
					values);
			
			//CordovaLib	
			IPath cordovaLibDirectory = getCordovaLibPath();
			directoryCopy(resolver.getTemplateFile(new Path("CordovaLib")), toURL(cordovaLibDirectory.toFile()));

			updateCordovaSubProjectPath(xcodeprojectFile, "CordovaLib/CordovaLib.xcodeproj", "<group>");
		
			//iOS config.xml needs to be copied outside www to be used
			File configxml = hybridProject.getConfigFile().getLocation().toFile();
			fileCopy(toURL(configxml),toURL(new File(prjdir, "/"+PlatformConstants.FILE_XML_CONFIG)));
		}
		catch(IOException e ){
			throw new CoreException(new Status(IStatus.ERROR,IOSCore.PLUGIN_ID,"Error generating the native iOS project", e));
		}
		
	}
	
	private void updateCordovaSubProjectPath(File pbxprojfile, String path,
			String sourcetree) throws CoreException{
		try {
			NSDictionary dict = (NSDictionary)ASCIIPropertyListParser.parse(pbxprojfile);
			NSDictionary objects = (NSDictionary)dict.objectForKey("objects");
			HashMap<String, NSObject> hashmap =  objects.getHashMap();	
			Collection<NSObject> values = hashmap.values();
			for (NSObject nsObject : values) {
				NSDictionary obj = (NSDictionary) nsObject;
				NSString isa = (NSString) obj.objectForKey("isa");
				NSString pathObj = (NSString)obj.objectForKey("path");
				if(isa != null && isa.getContent().equals("PBXFileReference") && path != null && pathObj.getContent().contains("CordovaLib.xcodeproj")){
					obj.remove("path");
					obj.put("path", path);
					obj.remove("sourceTree");
					obj.put("sourceTree", sourcetree);
					if(!obj.containsKey("name")){
						obj.put("name","CordovaLib.xcodeproj");
					}
					PropertyListParser.saveAsASCII(dict, pbxprojfile);
					break;
				}
			}

		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, IOSCore.PLUGIN_ID, "Error updatign CordovaLib subproject", e));
		}
	}
	
	private IPath getCordovaLibPath(){
		return new Path(getDestination().toString()).append("CordovaLib");
	}

	@Override
	protected void replaceCordovaPlatformFiles(HybridMobileLibraryResolver resolver) throws IOException{
		fileCopy(resolver.getTemplateFile(HybridMobileLibraryResolver.PATH_CORDOVA_JS), 
				toURL(new File(getPlatformWWWDirectory(), PlatformConstants.FILE_JS_CORDOVA)));
	}

	@Override
	protected File getPlatformWWWDirectory() {
		return XCodeProjectUtils.getPlatformWWWDirectory(getDestination());
	}

}
