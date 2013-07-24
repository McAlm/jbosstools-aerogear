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
package org.jboss.tools.aerogear.hybrid.core.extensions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.aerogear.hybrid.core.HybridCore;

public abstract class ExtensionPointProxy {
	
	protected IContributor contributor;

	 ExtensionPointProxy(IConfigurationElement element){
		setContributor(element.getContributor());
	}

	protected void setContributor(IContributor contributor) {
		this.contributor = contributor;
	}
	
	public static <T extends ExtensionPointProxy> List<T> getNativeExtensionPointProxy( String extensionPointID, Class<T> clazz){
		IConfigurationElement[] configElements = Platform.getExtensionRegistry().getConfigurationElementsFor(extensionPointID);
		List<T> proxies  = new ArrayList<T>();
		for (int i = 0; i < configElements.length; i++) {
			T proxy;
			try {
				proxy = clazz.getDeclaredConstructor(IConfigurationElement.class).newInstance(configElements[i]);
				proxies.add(proxy);
			} catch (Exception e) {
				HybridCore.log(IStatus.ERROR, "Error instantiating ExtensionPointProxy object", e);
			}
		}
		return proxies;
	}
	
}
