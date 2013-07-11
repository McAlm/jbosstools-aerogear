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
package org.jboss.tools.aerogear.hybrid.ios.ui;

import static org.jboss.tools.aerogear.hybrid.core.HybridProjectLaunchConfigConstants.ATTR_BUILD_SCOPE;
import static org.jboss.tools.aerogear.hybrid.ios.core.simulator.IOSSimulatorLaunchConstants.ATTR_DEVICE_FAMILY;
import static org.jboss.tools.aerogear.hybrid.ios.core.simulator.IOSSimulatorLaunchConstants.ATTR_SIMULATOR_SDK_DESCRIPTION;
import static org.jboss.tools.aerogear.hybrid.ios.core.simulator.IOSSimulatorLaunchConstants.ATTR_USE_RETINA;
import static org.jboss.tools.aerogear.hybrid.ios.core.simulator.IOSSimulatorLaunchConstants.ATTR_USE_TALL;
import static org.jboss.tools.aerogear.hybrid.ios.core.simulator.IOSSimulatorLaunchConstants.VAL_DEVICE_FAMILY_IPAD;
import static org.jboss.tools.aerogear.hybrid.ios.core.simulator.IOSSimulatorLaunchConstants.VAL_DEVICE_FAMILY_IPHONE;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.jboss.tools.aerogear.hybrid.core.HybridCore;
import org.jboss.tools.aerogear.hybrid.core.HybridProject;
import org.jboss.tools.aerogear.hybrid.ios.core.xcode.XCodeBuild;
import org.jboss.tools.aerogear.hybrid.ios.core.xcode.XCodeSDK;

public class IOSSimOptionsTab extends AbstractLaunchConfigurationTab {
	private static final String TXT_SDK_VER_DEFAULT = "Default";
	private Text textProject;
	private Combo comboDeviceFamily;
	private Listener dirtyFlagListener;
	private Button btnCheckRetina;
	private Button btnTall;
	private Combo comboSDKVer;
	
	private class DirtyListener implements Listener{
		@Override
		public void handleEvent(Event event) {
			setDirty(true);
			updateLaunchConfigurationDialog();
		}
	}
	
	public IOSSimOptionsTab() {
		this.dirtyFlagListener = new DirtyListener();
	}
	
	/**
	 * @wbp.parser.entryPoint
	 */
	@Override
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		comp.setLayout(new GridLayout(1, false));
		
		Group grpProject = new Group(comp, SWT.NONE);
		grpProject.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		grpProject.setText("Project");
		grpProject.setLayout(new GridLayout(3, false));
		
		Label lblProject = new Label(grpProject, SWT.NONE);
		lblProject.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblProject.setText("Project:");
		
		textProject = new Text(grpProject, SWT.BORDER);
		textProject.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		textProject.addListener(SWT.Modify, dirtyFlagListener);
		
		
		Button btnProjectBrowse = new Button(grpProject, SWT.NONE);
		btnProjectBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ElementListSelectionDialog es = new ElementListSelectionDialog(getShell(), WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
				es.setElements(HybridCore.getHybridProjects().toArray());
				es.setTitle("Project Selection");
				es.setMessage("Select a project to run");
				if (es.open() == Window.OK) {			
					HybridProject project = (HybridProject) es.getFirstResult();
					textProject.setText(project.getProject().getName());
				}		
			}
		});
		btnProjectBrowse.setText("Browse...");
		
		Group grpSimulator = new Group(comp, SWT.NONE);
		grpSimulator.setLayout(new GridLayout(2, false));
		grpSimulator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		grpSimulator.setText("Simulator");
		
		Label lblSdkVersion = new Label(grpSimulator, SWT.NONE);
		lblSdkVersion.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblSdkVersion.setText("SDK Version:");
		
		comboSDKVer = new Combo(grpSimulator, SWT.NONE);
		comboSDKVer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		comboSDKVer.addListener(SWT.Selection, dirtyFlagListener);
		comboSDKVer.add(TXT_SDK_VER_DEFAULT, 0);
		
		Label lblDeviceFamily = new Label(grpSimulator, SWT.NONE);
		lblDeviceFamily.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblDeviceFamily.setText("Device Family:");
		
		comboDeviceFamily = new Combo(grpSimulator, SWT.NONE);
		comboDeviceFamily.setItems(new String[] {VAL_DEVICE_FAMILY_IPHONE, VAL_DEVICE_FAMILY_IPAD});
		comboDeviceFamily.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		comboDeviceFamily.select(0);
		comboDeviceFamily.addListener(SWT.Selection, dirtyFlagListener);
		
		btnCheckRetina = new Button(grpSimulator, SWT.CHECK);
		btnCheckRetina.setText("Retina");
		btnCheckRetina.addListener(SWT.Selection, dirtyFlagListener);
		
		btnTall = new Button(grpSimulator, SWT.CHECK);
		btnTall.setText("Tall");
		btnTall.addListener(SWT.Selection, dirtyFlagListener);
		XCodeBuild build = new XCodeBuild();
		try{
			List<XCodeSDK> list = build.showSdks();
			for (XCodeSDK sdk : list) {
				if(sdk.isSimulator()){
					comboSDKVer.add(sdk.getDescription());
				}
			}
		}
		catch (CoreException e) {
				comboSDKVer.add("error: defaults to latest");
			}
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(ATTR_DEVICE_FAMILY, VAL_DEVICE_FAMILY_IPHONE);
		configuration.setAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, true);
	}

	/**
	 * @wbp.parser.entryPoint
	 */
	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		
		String projectName =null;
		try {
			projectName = configuration.getAttribute(ATTR_BUILD_SCOPE, (String)null);
		} catch (CoreException e) {
			// not handled
		}
		if(projectName != null){
			textProject.setText(projectName);
		}
		
		
		try{ 
			String sdkVer = configuration.getAttribute(ATTR_SIMULATOR_SDK_DESCRIPTION, TXT_SDK_VER_DEFAULT);
			int index = comboSDKVer.indexOf(sdkVer);
			if(index <0 )//it is possible that the selected SDK version is no longer available
				index=0; // it can be either uninstalled or the launch config is shared. fall back to default
			comboSDKVer.select(index);
			
		}catch(CoreException ce){
			
		}
		
		try {
			String devFamily = configuration.getAttribute(ATTR_DEVICE_FAMILY, VAL_DEVICE_FAMILY_IPHONE);
			int index= comboDeviceFamily.indexOf(devFamily);
			Assert.isTrue(index >-1 , "LaunchConfiguration should never return a device family that is not on the combo");
			comboDeviceFamily.select(index);
			
		} catch (CoreException e) {
			
		}
		
		try{
			btnTall.setSelection( configuration.getAttribute(ATTR_USE_TALL, false));
		}catch(CoreException e){
			
		}
		
		try{
			btnCheckRetina.setSelection(configuration.getAttribute(ATTR_USE_RETINA, false));
		}catch(CoreException e){
			
		}
			
		setDirty(false);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(ATTR_BUILD_SCOPE, textProject.getText());
		configuration.setAttribute(ATTR_DEVICE_FAMILY, comboDeviceFamily.getText());
		configuration.setAttribute(ATTR_USE_RETINA, btnCheckRetina.getSelection() );
		configuration.setAttribute(ATTR_USE_TALL, btnTall.getSelection());
		if(!TXT_SDK_VER_DEFAULT.equals(comboSDKVer.getText())){
			configuration.setAttribute(ATTR_SIMULATOR_SDK_DESCRIPTION, comboSDKVer.getText());
		}
	}

	@Override
	public String getName() {
		return "Simulator";
	}
}
