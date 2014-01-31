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
package org.jboss.tools.aerogear.hybrid.android.core.adt;

import static org.jboss.tools.aerogear.hybrid.android.core.AndroidConstants.DIR_ASSETS;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.aerogear.hybrid.android.core.AndroidCore;
import org.jboss.tools.aerogear.hybrid.core.platform.PlatformConstants;

public class AndroidProjectUtils {
	
	private static final int REQUIRED_MIN_API_LEVEL = 17;

	
	public static File getPlatformWWWDirectory(File projectDirectory) {
		Assert.isNotNull(projectDirectory);
		return new File(projectDirectory, DIR_ASSETS + File.separator +PlatformConstants.DIR_WWW);
	}
	
	/**
	 * Returns the most suitable target defined on the system to be used with the projects.
	 * It returns the highest api level target that is better than minimum requirement.
	 * 
	 * @return android SDK 
 	 * @throws CoreException
 	 * 	<ul>
 	 * 		<li>If there are no targets defined</li>
 	 * 		<li>If no target has a higher than or equal to minimum required API level.</li>
 	 * ,</ul>
	 */
	public static AndroidSDK selectBestValidTarget() throws CoreException {
		AndroidSDKManager sdkManager = AndroidSDKManager.getManager();
		List<AndroidSDK> targets = sdkManager.listTargets();
		if(targets == null || targets.isEmpty() ){
			throw new CoreException(new Status(IStatus.ERROR, AndroidCore.PLUGIN_ID, "No Android targets were found, Please create a target"));
		}
		AndroidSDK target = null;
		for (AndroidSDK androidSDK : targets) {
			if(androidSDK.getApiLevel() >= REQUIRED_MIN_API_LEVEL &&
					(target == null || androidSDK.getApiLevel() > target.getApiLevel())){
				target = androidSDK;
			}
		}
		if( target == null ){
			throw new CoreException(new Status(IStatus.ERROR, AndroidCore.PLUGIN_ID, 
					"Please install Android API " +REQUIRED_MIN_API_LEVEL +" or later. Use the Android SDK Manager to install or upgrade any missing SDKs to tools."));
		}
		return target;
	}

}
