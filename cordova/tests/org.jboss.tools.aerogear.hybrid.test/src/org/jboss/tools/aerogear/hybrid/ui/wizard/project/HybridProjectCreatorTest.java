/*******************************************************************************
 * Copyright (c) 2013, 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *       Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.aerogear.hybrid.ui.wizard.project;

import static org.jboss.tools.aerogear.hybrid.core.platform.PlatformConstants.DIR_MERGES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStreamReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.jboss.tools.aerogear.hybrid.core.HybridCore;
import org.jboss.tools.aerogear.hybrid.core.extensions.PlatformSupport;
import org.jboss.tools.aerogear.hybrid.core.natures.HybridAppNature;
import org.jboss.tools.aerogear.hybrid.core.platform.PlatformConstants;
import org.jboss.tools.aerogear.hybrid.engine.internal.cordova.CordovaEngineProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class HybridProjectCreatorTest {
	private static final String PROJECT_NAME = "TestProject";
	private static final String APP_NAME = "Test App";
	private static final String APP_ID = "Test.id";
	private static final String CORDOVA_ENGINE_VER = "3.1.0";


	private IProject getTheProject() {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject theProject = workspaceRoot.getProject(PROJECT_NAME);
		return theProject;
	}

	@BeforeClass
	public static void createTestProject() throws CoreException{
		HybridProjectCreator creator = new HybridProjectCreator();
		CordovaEngineProvider engineProvider = new CordovaEngineProvider();
		creator.createBasicTemplatedProject(PROJECT_NAME, null, APP_NAME, APP_ID, 
				engineProvider.createEngine(CordovaEngineProvider.CORDOVA_ENGINE_ID,  CORDOVA_ENGINE_VER),new NullProgressMonitor());
	}

	@Test
	public void createProjectTest() {
		IProject theProject = getTheProject();
		assertTrue(theProject.exists());
	}

	@Test
	public void projectNatureTest() throws CoreException{
		IProject theProject = getTheProject();
		assertTrue(theProject.hasNature(HybridAppNature.NATURE_ID));
	}
	
	@Test
	public void directoryStructureTest(){
		IProject theProject = getTheProject();
		
		String[] paths={ ".cordova", PlatformConstants.DIR_MERGES, "plugins", PlatformConstants.DIR_WWW };//Copied from HybridProjectCreator
		for (int i = 0; i < paths.length; i++) {
			IFolder folder = theProject.getFolder( paths[i]);
			assertTrue(paths[i]+ " is not created. ", folder.exists());
		}
		List<PlatformSupport> platforms = HybridCore.getPlatformSupports();
		IPath merges = new Path(PlatformConstants.DIR_MERGES);
		for (PlatformSupport platform : platforms) {
			IPath platformDir = merges.append(platform.getPlatformId());
			IFolder folder = theProject.getFolder(platformDir);
			assertTrue(platformDir+ " is not created. ", folder.exists());
		}
	}
	
	@Test
	public void configFileTest() throws JsonIOException, JsonParseException, CoreException{
		IProject prj = getTheProject();
		IFile f = prj.getFile(PlatformConstants.DIR_DOT_CORDOVA+"/config.json");
		assertNotNull(f);
		assertTrue(f.exists());
		JsonParser parser = new JsonParser();
		JsonElement el = parser.parse(new InputStreamReader(f.getContents()));
		assertTrue(el.isJsonObject());
		JsonObject object = el.getAsJsonObject();
		assertEquals(APP_ID, object.get("id").getAsString());
		assertEquals(APP_NAME, object.get("name").getAsString());
		JsonElement engineEl = object.get("engine");
		assertNotNull(engineEl);
		assertTrue(engineEl.isJsonObject());
		JsonObject engineObj = engineEl.getAsJsonObject();
		assertEquals(CORDOVA_ENGINE_VER, engineObj.get("ver").getAsString());
		assertEquals(CordovaEngineProvider.CORDOVA_ENGINE_ID, engineObj.get("id").getAsString());
	}
	
	@Test
	public void essentialFilesTest(){
		IProject theProject = getTheProject();
		IFile file = theProject.getFile("/www/config.xml");
		assertTrue(file.exists());
		file= theProject.getFile("/www/index.html");
		assertTrue(file.exists());
	}
	
	private  Document loadConfigXML() throws Exception {
	    DocumentBuilder db;
		DocumentBuilderFactory dbf =DocumentBuilderFactory.newInstance();
	
	    	db = dbf.newDocumentBuilder();
	    	IFile file =  getTheProject().getFile("/www/config.xml");
	    	if(file == null )
	    		return null;
	    	return db.parse(file.getContents()); 
	}
	
	@Test
	public void configUpdatesTest() throws Exception{
		Document doc;
		doc = loadConfigXML();
		
		String id = doc.getDocumentElement().getAttribute("id");
		assertEquals(APP_ID, id);
		NodeList nodes = doc.getDocumentElement().getElementsByTagName("name");
		assertTrue(nodes.getLength()> 0);
		String name = nodes.item(0).getTextContent();
		assertEquals(APP_NAME, name);
	}
}
