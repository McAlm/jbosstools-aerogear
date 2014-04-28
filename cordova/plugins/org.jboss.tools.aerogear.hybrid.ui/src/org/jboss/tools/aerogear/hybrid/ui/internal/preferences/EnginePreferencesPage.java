/*******************************************************************************
 * Copyright (c) 2013,2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *       Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.aerogear.hybrid.ui.internal.preferences;

import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.tools.aerogear.hybrid.core.engine.HybridMobileEngine;
import org.jboss.tools.aerogear.hybrid.core.engine.HybridMobileEngineManager;
import org.jboss.tools.aerogear.hybrid.core.platform.PlatformConstants;
import org.jboss.tools.aerogear.hybrid.engine.internal.cordova.CordovaEngineProvider;
import org.jboss.tools.aerogear.hybrid.ui.HybridUI;
import org.jboss.tools.aerogear.hybrid.ui.internal.engine.AvailableCordovaEnginesSection;
import org.jboss.tools.aerogear.hybrid.ui.internal.engine.AvailableCordovaEnginesSection.EngineListChangeListener;

public class EnginePreferencesPage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private AvailableCordovaEnginesSection engineSection;
	
	public EnginePreferencesPage(){
		super();
		setDescription("Add or remove Hybrid Mobile Engines. By default, checked engine is used for the newly created Hybrid Mobile projects.");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(control);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(control);
		
		engineSection = new AvailableCordovaEnginesSection();
		engineSection.createControl(control);
		
		engineSection.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				setValid(validate());
			}
		});
		engineSection.addEngineListChangeListener(new EngineListChangeListener() {
			@Override
			public void listChanged() {
				initDefaultEngine();
			}
		});
		noDefaultAndApplyButton();
		initDefaultEngine();
		setValid(validate());
		return control;
	}

	private void initDefaultEngine() {
		HybridMobileEngine defaultEngine = HybridMobileEngineManager.getDefaultEngine();
		if(defaultEngine != null ){
			engineSection.setSelection(new StructuredSelection(defaultEngine));
		}else{
			List<HybridMobileEngine> engines = engineSection.getListedEngines();
			if(engines != null && engines.size() ==1){
				engineSection.setSelection(new StructuredSelection(engines.get(0)));
			}
		}
	}
	
	private boolean validate() {
		if(engineSection.getSelection().isEmpty()){
			setErrorMessage("Default engine is not selected");
			return false;
		}
		setMessage(null);
		setErrorMessage(null);
		return true;
	}
	
	@Override
	public boolean performOk() {
		IStructuredSelection sel = (IStructuredSelection) engineSection.getSelection();
		HybridMobileEngine engine =(HybridMobileEngine) sel.getFirstElement();
		getPreferenceStore().setValue(PlatformConstants.PREF_DEFAULT_ENGINE, engine.getId()+":"+engine.getVersion());
		return true;
	}
	
	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return HybridUI.getDefault().getPreferenceStore();
	}

}
