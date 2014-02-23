/*******************************************************************************
* Copyright (c) 2013,2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.aerogear.hybrid.ui.internal.engine;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.aerogear.hybrid.core.HybridCore;
import org.jboss.tools.aerogear.hybrid.core.engine.HybridMobileEngine;
import org.jboss.tools.aerogear.hybrid.core.engine.HybridMobileEngineLocator;
import org.jboss.tools.aerogear.hybrid.core.engine.HybridMobileEngineLocator.EngineSearchListener;
import org.jboss.tools.aerogear.hybrid.core.engine.PlatformLibrary;
import org.jboss.tools.aerogear.hybrid.core.extensions.PlatformSupport;
import org.jboss.tools.aerogear.hybrid.core.platform.PlatformConstants;
import org.jboss.tools.aerogear.hybrid.engine.internal.cordova.CordovaEngineProvider;
import org.jboss.tools.aerogear.hybrid.ui.HybridUI;
import org.jboss.tools.aerogear.hybrid.ui.PlatformImage;
import org.jboss.tools.aerogear.hybrid.ui.internal.status.StatusManager;

import com.github.zafarkhaja.semver.Version;

public class AvailableCordovaEnginesSection implements ISelectionProvider{
	

	private static final int TABLE_HEIGHT = 250;
	private static final int TABLE_WIDTH = 350;
	
	private ListenerList selectionListeners;
	private CheckboxTableViewer engineList;
	private ISelection prevSelection = new StructuredSelection();
	
	private class EngineTooltip extends ToolTip{

		private static final int WIDTH_HINT = 340;

		public EngineTooltip(Control control) {
			super(control, SWT.NONE, true);
		}

		@Override
		protected Composite createToolTipContentArea(Event event,
				Composite parent) {
			GridLayoutFactory.fillDefaults().applyTo(parent);

 			IStructuredSelection selection = (IStructuredSelection) engineList.getSelection();
			HybridMobileEngine selectedEngine = (HybridMobileEngine) selection.getFirstElement();
			
			Composite container = new Composite(parent, SWT.NONE);
			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			GridDataFactory.fillDefaults().grab(true, true).hint(WIDTH_HINT, SWT.DEFAULT).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).spacing(3, 0).applyTo(container);
			List<PlatformLibrary> libs = selectedEngine.getPlatformLibs();
			for (PlatformLibrary platformLibrary : libs) {
				Label imageLabel = new Label(container, SWT.NONE);
				PlatformSupport ps = HybridCore.getPlatformSupport(platformLibrary.getPlatformId());
				imageLabel.setImage(PlatformImage.getImageFor(PlatformImage.ATTR_PLATFORM_SUPPORT, ps.getID()));
				Label path = new Label(container, SWT.WRAP);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(false, false).applyTo(path);
				path.setSize(WIDTH_HINT - 25, 0); //set a width size for shortenText
				path.setText(Dialog.shortenText(platformLibrary.getLocation().toString(), path));
			}
			return container;
		}
		
	}
	
	private class CordovaEnginesContentProvider implements IStructuredContentProvider{
		private List<HybridMobileEngine> engines;
		
		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			this.engines = (List<HybridMobileEngine>) newInput;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if(engines == null ) return null;
			return engines.toArray();
		}
		
	}
	
	private class CordovaEngineLabelProvider extends LabelProvider implements ITableLabelProvider,IFontProvider{
		
		private Font boldFont;

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			switch (columnIndex) {
			case 2:
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK);
			default:
				return null;
			}
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			Assert.isTrue(element instanceof HybridMobileEngine);
			HybridMobileEngine engine = (HybridMobileEngine)element;
			switch (columnIndex) {
			case 0:
				String bind = "{0} [{1}]";
				if(engine.getId().equals(CordovaEngineProvider.CUSTOM_CORDOVA_ENGINE_ID)){
					bind += "*";
				}
				return NLS.bind(bind, new String[]{engine.getName(), engine.getVersion()});
			case 1:
				 List<PlatformLibrary> platforms =  engine.getPlatformLibs();
				 String platformString = "";
				 for (PlatformLibrary lib : platforms) {
					platformString += lib.getPlatformId() +" ";
				}
				return platformString;
			case 2: return null;
			default:
				Assert.isTrue(false);
			}
			return null;
		}

		@Override
		public Font getFont(Object element) {
			if(!engineList.getChecked(element))
				return null;
			if(boldFont == null ){
				FontDescriptor fontDescriptor = JFaceResources.getDialogFontDescriptor();
                fontDescriptor = fontDescriptor.setStyle(SWT.BOLD);
                boldFont = fontDescriptor.createFont(Display.getCurrent());
			}
			return boldFont;
		}
		
	}
	
	private class EngineVersionComparator extends ViewerComparator{
		private boolean descending = true;

		
	public EngineVersionComparator(boolean isDescending) {
		descending = isDescending;
	}


	@Override
		public int compare(Viewer viewer, Object o1, Object o2) {
			HybridMobileEngine e1 = (HybridMobileEngine) o1;
			HybridMobileEngine e2 = (HybridMobileEngine) o2;
			Version version1 = Version.valueOf(e1.getVersion());
			Version version2 = Version.valueOf(e2.getVersion());
			if(descending){
				return version2.compareTo(version1);
			}
			return version1.compareTo(version2);

		}
		
	}
	
	public AvailableCordovaEnginesSection() {
		this.selectionListeners = new ListenerList();
	}

	public void createControl(final Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
		
		Label tableLbl = new Label(composite, SWT.NULL);
		tableLbl.setText("Available Engines: ");
		GridDataFactory.generate(tableLbl, 2, 1);
		
		final Table table= new Table(composite, SWT.CHECK | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		GridDataFactory.fillDefaults().hint(new Point(TABLE_WIDTH, TABLE_HEIGHT)).applyTo(table); 
		
		table.setHeaderVisible(true);
		table.setLinesVisible(true);	
		
			

		TableColumn col = new TableColumn(table, SWT.NONE);

		col.setWidth(TABLE_WIDTH/2);
		col.setText("Name");
		col.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				EngineVersionComparator comp = (EngineVersionComparator) engineList.getComparator();
				engineList.setComparator(new EngineVersionComparator(!comp.descending));
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
		
		col = new TableColumn(table, SWT.NONE);
		col.setWidth(TABLE_WIDTH/2 -25);
		col.setText("Platforms");
		
		col = new TableColumn(table, SWT.NONE);
		col.setWidth(25);
		table.addListener(SWT.MouseUp, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				Point p = new Point(event.x, event.y);
				TableItem item = table.getItem(p);
				if(item != null){
				Rectangle rect = item.getBounds(2);
					if(rect.contains(p)){
						EngineTooltip tooltip = new EngineTooltip(table);
						tooltip.show(p);
					}
				}
			}
		});
		
		
		
		engineList = new CheckboxTableViewer(table);			
		engineList.setContentProvider(new CordovaEnginesContentProvider());
		engineList.setLabelProvider(new CordovaEngineLabelProvider());
		engineList.setComparator(new EngineVersionComparator(true));
		engineList.setUseHashlookup(true);
	
		engineList.addCheckStateListener(new ICheckStateListener(){
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getChecked()) {
					setSelection(new StructuredSelection(event.getElement()));
				} else {
					setSelection(new StructuredSelection());
				}
			}
		});
		
		engineList.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
				
			}
		});
		
		Composite buttonsContainer = new Composite(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL ).applyTo(buttonsContainer);
		GridLayoutFactory.fillDefaults().spacing(0, 3).numColumns(1).applyTo(buttonsContainer);

		Button downloadBtn = new Button(buttonsContainer, SWT.PUSH);
		downloadBtn.setText("Download...");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(downloadBtn);;
		downloadBtn.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				EngineDownloadDialog downloadDialog = new EngineDownloadDialog(engineList.getControl().getShell());
				int status = downloadDialog.open();
				if(status == Window.OK){
					updateAvailableEngines();
				}
				
			}
		});
		
		Button searchBtn = new Button(buttonsContainer, SWT.PUSH);
		searchBtn.setText("Search...");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(searchBtn);
		searchBtn.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				handleSearch(parent);
			}
		});
		
		
		
		updateAvailableEngines();
//		updateButtons();
		
	}

	private void updateButtons() {
		// TODO Auto-generated method stub
		
	}

	private void updateAvailableEngines() {
		CordovaEngineProvider provider = new CordovaEngineProvider();
		final List<HybridMobileEngine> engines = provider.getAvailableEngines();
		Job preCompileJob = new Job("Hybrid Mobile Engine Library pre-compilation") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				for (HybridMobileEngine hybridMobileEngine : engines) {
					try{
						hybridMobileEngine.preCompile(monitor);
					}
					catch(CoreException e ){
						return e.getStatus();
					}
				}
				return Status.OK_STATUS;
			}
		};
		preCompileJob.schedule();
		engineList.setInput(engines);
		
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionListeners.add(listener);
	}

	@Override
	public ISelection getSelection() {
		return new StructuredSelection(engineList.getCheckedElements());
	}

	@Override
	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		selectionListeners.remove(selectionListeners);
	}
	

	@Override
	public void setSelection(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			if (!selection.equals(prevSelection)) {
				prevSelection = selection;
				Object engine = ((IStructuredSelection)selection).getFirstElement();
				if (engine == null) {
					engineList.setCheckedElements(new Object[0]);
				} else {
					engineList.getTable().getItem(0).setChecked(true);
					engineList.setCheckedElements(new Object[]{engine});
					engineList.reveal(engine);
				}
				engineList.refresh(true);
				fireSelectionChanged();
			}
		}	
	}
	
	private void fireSelectionChanged() {
		SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());
		Object[] listeners = selectionListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			ISelectionChangedListener listener = (ISelectionChangedListener)listeners[i];
			listener.selectionChanged(event);
		}	
	}

	private void handleSearch(final Composite parent) {
		DirectoryDialog directoryDialog = new DirectoryDialog(parent.getShell());
		directoryDialog.setMessage("Select the directory in which to search for hybrid mobile engines");
		directoryDialog.setText("Search for Hybrid Mobile Engines");
		
		String pathStr = directoryDialog.open();
		if (pathStr == null)
			return;
		
		final IPath path = new Path(pathStr);
		final ProgressMonitorDialog dialog = new ProgressMonitorDialog(parent.getShell());
		dialog.setBlockOnOpen(false);
		dialog.setCancelable(true);
		dialog.open();
		final EngineSearchListener listener = new EngineSearchListener() {
			
			@Override
			public void libraryFound(PlatformLibrary library) {
				IPreferenceStore store = HybridUI.getDefault().getPreferenceStore();
				String locs = store.getString(PlatformConstants.PREF_CUSTOM_LIB_LOCS);
				if(locs == null || locs.isEmpty() ){
					locs = library.getLocation().toString();
				}else{
					locs += ","+library.getLocation();
				}
				store.setValue(PlatformConstants.PREF_CUSTOM_LIB_LOCS, locs);
			}
		};
		
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				List<HybridMobileEngineLocator> locators = HybridCore.getEngineLocators();
				for (HybridMobileEngineLocator locator : locators) {
					locator.searchForRuntimes(path, listener, monitor);
				}
				parent.getDisplay().asyncExec(new Runnable() {
					
					@Override
					public void run() {
						updateAvailableEngines();
					}
				});
			}
		};
		
		try {
			dialog.run(true, true, runnable);
		} catch (InvocationTargetException e) {
			if (e.getTargetException() != null) {
				if(e.getTargetException() instanceof CoreException ){
					StatusManager.handle((CoreException) e.getTargetException());
				}else{
					ErrorDialog.openError(parent.getShell(), "Local Engine Search Error",null, 
							new Status(IStatus.ERROR, HybridUI.PLUGIN_ID, "Error when searching for local hybrid mobile engines", e.getTargetException() ));
				}
			}
		} catch (InterruptedException e) {
			HybridUI.log(IStatus.ERROR, "Search for Cordova Engines error", e);
		}
	}

}
