/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *       Red Hat, Inc. - initial API and implementation
 ********************************************v***********************************/
package org.jboss.tools.aerogear.hybrid.core.internal.util;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.aerogear.hybrid.core.HybridCore;

public class HttpUtil {
	
	/**
	 * Set the proxy settings from ProxyService.
	 * This method sets a {@link HttpRoutePlanner} to the client
	 * 
	 * @param client
	 */
	public static void setupProxy(DefaultHttpClient client ){
		client.setRoutePlanner(new HttpRoutePlanner() {
			
			@Override
			public HttpRoute determineRoute(HttpHost target, HttpRequest request,
					HttpContext context) throws HttpException {
				IProxyService proxy =  HybridCore.getDefault().getProxyService();
				HttpHost host =null;
				try {
					IProxyData[] proxyDatas = proxy.select(new URI(target.toURI()));
					for (IProxyData data : proxyDatas) {
						if(data.getType().equals(IProxyData.HTTP_PROXY_TYPE)){
							host = new HttpHost(data.getHost(), data.getPort());
							break;
						}
					}
				} catch (URISyntaxException e) {
					HybridCore.log(IStatus.ERROR, "Incorrect URI", e);
				}
				return new HttpRoute(target, null, host, false);
			}
		});
		
	}

}
