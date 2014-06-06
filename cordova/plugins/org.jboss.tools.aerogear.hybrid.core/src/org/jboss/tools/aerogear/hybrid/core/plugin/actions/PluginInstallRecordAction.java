/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.aerogear.hybrid.core.plugin.actions;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.aerogear.hybrid.core.HybridProject;
import org.jboss.tools.aerogear.hybrid.core.config.Feature;
import org.jboss.tools.aerogear.hybrid.core.config.Widget;
import org.jboss.tools.aerogear.hybrid.core.config.WidgetModel;
import org.jboss.tools.aerogear.hybrid.core.platform.IPluginInstallationAction;

public class PluginInstallRecordAction implements IPluginInstallationAction{
	
	private final HybridProject project;
	private final String id;
	private final String version;
	private final String pluginName;
	
	public PluginInstallRecordAction(HybridProject project, String pluginName, String id, String version) {
		this.project = project;
		this.id = id;
		this.version = version;
		this.pluginName = pluginName;
	}
	
	@Override
	public void install() throws CoreException {
		WidgetModel widgetModel = WidgetModel.getModel(project);
		Widget widget = widgetModel.getWidgetForEdit();
		
		Feature feature = getExistingFeature(widget);
		if(feature == null ){
			feature = widgetModel.createFeature(widget);
			feature.setName(pluginName);
			widget.addFeature(feature);
		}
		String existingId= feature.getParams().get("id");
		if(existingId == null){
			feature.addParam("id",id);
		}
		//replace the new version number
		feature.removeParam("version");
		if(version != null && !version.isEmpty()){
			feature.addParam("version", version);
		}
		widgetModel.save();
	}
	
	private Feature getExistingFeature(Widget widget) {
		List<Feature> features = widget.getFeatures();
		if(features == null ) return null;
		for (Feature feature : features) {
			if(feature.getName().equals(pluginName)){
				return feature;
			}
		}
		return null;
	}

	@Override
	public String[] filesToOverwrite() {
		return null;
	}

	@Override
	public void unInstall() throws CoreException {
		//leave uninstall to the feature tag? 
	}
}
