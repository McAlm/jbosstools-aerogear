/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.jboss.tools.aerogear.hybrid.core.HybridProject;
import org.jboss.tools.aerogear.hybrid.core.plugin.RestorableCordovaPlugin;
import org.jboss.tools.aerogear.hybrid.core.plugin.registry.CordovaPluginRegistryManager;
import org.jboss.tools.aerogear.hybrid.core.plugin.registry.CordovaRegistryPlugin;
import org.jboss.tools.aerogear.hybrid.core.plugin.registry.CordovaRegistryPluginVersion;
import org.jboss.tools.aerogear.hybrid.ui.HybridUI;
import org.jboss.tools.aerogear.hybrid.ui.internal.status.StatusManager;

public class PluginRestorePage extends WizardPage {
	
	private class RestorablePluginsContentProvider implements IStructuredContentProvider{
		private CordovaRegistryPluginVersion[] vers;
		@Override
		public void dispose() {
		}
		
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if(newInput == null ){
				vers = null;
			}else{
				@SuppressWarnings("unchecked")
				List<CordovaRegistryPluginVersion> versionList = (List<CordovaRegistryPluginVersion>) newInput;
				vers = versionList.toArray(new CordovaRegistryPluginVersion[versionList.size()]);
			}
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return vers;
		}
	}
	
	private class RestorablePluginLabelProvider extends LabelProvider implements ITableLabelProvider{

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			
			CordovaRegistryPluginVersion pv = (CordovaRegistryPluginVersion) element;
			switch (columnIndex) {
			case 0:
				return pv.getName();
			case 1: 
				String ver = pv.getVersionNumber();
				return ver;
			case 2:
				return "registry";
			}
			return "";
		}
		
	}
	
	private static final int TABLE_HEIGHT = 250;
	private static final int TABLE_WIDTH = 350;
	private CheckboxTableViewer restorableList;
	private HybridProject project;

	protected PluginRestorePage(HybridProject project) {
		super("Restore Plugins");
		setTitle("Restore Cordova plug-ins");
		setDescription("Select config.xml referenced Cordova plug-ins to be restored");
		this.project = project;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(composite);
		
		Label tableLbl = new Label(composite, SWT.NULL);
		tableLbl.setText("Discovered the following Cordova plug-ins on config.xml and will attempt to restore the selected plug-in(s)");
		GridDataFactory.generate(tableLbl, 2, 1);
		
		final Table table= new Table(composite, SWT.CHECK | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).minSize(new Point(TABLE_WIDTH, TABLE_HEIGHT)).applyTo(table); 
		
		table.setHeaderVisible(true);
		table.setLinesVisible(true);		
		
		TableColumn col = new TableColumn(table, SWT.NONE);

		col.setWidth(TABLE_WIDTH/2);
		col.setText("ID");
		
		col = new TableColumn(table, SWT.NULL);
		col.setText("Version");
		col.setWidth(TABLE_WIDTH/4);
		
		col = new TableColumn(table, SWT.NULL);
		col.setText("Source");
		col.setWidth(TABLE_WIDTH/4);

		restorableList = new CheckboxTableViewer(table);			
		restorableList.setContentProvider(new RestorablePluginsContentProvider());
		restorableList.setLabelProvider(new RestorablePluginLabelProvider());
		
		setControl(composite);
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		getControl().getDisplay().asyncExec(new Runnable() {
			
			@Override
			public void run() {
				populateRestorables();
				
			}
		});
		
		
	}
	
	private void populateRestorables(){
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try{
						List<RestorableCordovaPlugin> restorables = project.getPluginManager().getRestorablePlugins(monitor);
						final List<CordovaRegistryPluginVersion> restoreVersion = new ArrayList<CordovaRegistryPluginVersion>();
						CordovaPluginRegistryManager regMng = new CordovaPluginRegistryManager(CordovaPluginRegistryManager.DEFAULT_REGISTRY_URL);
						for (RestorableCordovaPlugin restorable : restorables) {
							if(monitor.isCanceled()){
								throw new OperationCanceledException();
							}
							CordovaRegistryPlugin plugin = regMng.getCordovaPluginInfo(restorable.getId());
							
							String version = restorable.getVersion();
							if(version == null){
								version = plugin.getLatestVersion();
							}
							restoreVersion.add(plugin.getVersion(version));
						}
						
						getControl().getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
									@Override
									public void run() {
										restorableList.setInput(restoreVersion);
										restorableList.setAllChecked(true);
									}
								});
							}
						});
					}
					catch(CoreException e){
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			if (e.getTargetException() != null) {
				if(e.getTargetException() instanceof CoreException ){
					StatusManager.handle((CoreException) e.getTargetException());
				}else{
					ErrorDialog.openError(getShell(), "Restorable Cordova Plug-ins error",null, 
							new Status(IStatus.ERROR, HybridUI.PLUGIN_ID, "Error while generating the list of restorable Cordova plug-ins", 
									e.getTargetException() ));
				}
			}

		} catch (InterruptedException e) {
			throw new OperationCanceledException(e.getMessage());
		}
	}
	
	public CordovaRegistryPluginVersion[] getSelectedRestorables(){
		Object[] checked = restorableList.getCheckedElements();
		return Arrays.copyOf(checked, checked.length, CordovaRegistryPluginVersion[].class);
 	}
	

}
