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
package org.jboss.tools.aerogear.hybrid.ui.internal.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.aerogear.hybrid.core.HybridMobileStatus;
import org.jboss.tools.aerogear.hybrid.ui.HybridUI;
import org.jboss.tools.aerogear.hybrid.ui.status.AbstractStatusHandler;

public class StatusManager {
	
	
	private static Map<String, AbstractStatusHandler> handlers;

	
	public static void handle(IStatus status){
		if(status.isMultiStatus()){
			IStatus[] childs = status.getChildren();
			for (int i = 0; i < childs.length; i++) {
				if(!childs[i].isOK()){
					AbstractStatusHandler handler =  getStatusHandler(childs[i]);
					handler.handle(childs[i]);
					break;
				}
			}
		}else{
			AbstractStatusHandler handler = getStatusHandler(status);
			handler.handle(status);
		}
	}

	public static void handle(CoreException exception){
		AbstractStatusHandler handler = getStatusHandler(exception);
		handler.handle(exception);
	}		
	
	
	private static AbstractStatusHandler getStatusHandler(IStatus status){
		if(status instanceof HybridMobileStatus ){
			return getHybridMobileStatusHandler((HybridMobileStatus) status);
		}
		return new DefaultStatusHandler();
	}
	
	private static AbstractStatusHandler getStatusHandler(CoreException exception){
		IStatus status = exception.getStatus();
		if(status instanceof HybridMobileStatus){
			return getHybridMobileStatusHandler((HybridMobileStatus) status);
		}
		return getStatusHandler(status);
	}
	
	private static AbstractStatusHandler getHybridMobileStatusHandler(HybridMobileStatus status){
		initHandlers();
		String key = makeHandlerKey(status.getPlugin(), status.getCode());
		if(handlers.containsKey(key)){
			return handlers.get(key);
		}
        return new DefaultStatusHandler();
	}
	
	private static void initHandlers(){
		if(handlers != null ) return;
		handlers = new HashMap<String, AbstractStatusHandler>();
		List<HybridMobileStatusExtension> extensions = HybridUI.getHybridMobileStatusExtensions();
		for (HybridMobileStatusExtension extension : extensions) {
			String key = makeHandlerKey(extension.getPluginID() ,extension.getCode());
			handlers.put(key, extension.getHandler());
		}
	}
	
	private static String makeHandlerKey(String pluginID, int code){
		return  NLS.bind("{0}_{1}",new String[]{pluginID, Integer.toString(code)});
	}
	
	
}
