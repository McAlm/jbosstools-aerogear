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
package org.jboss.tools.aerogear.hybrid.ios.core.pbxproject;


public class PBXProjectException extends Exception {

	private static final long serialVersionUID = -6269682300249537220L;
	
	public PBXProjectException(Throwable throwable){
		super(throwable);
	}
}
