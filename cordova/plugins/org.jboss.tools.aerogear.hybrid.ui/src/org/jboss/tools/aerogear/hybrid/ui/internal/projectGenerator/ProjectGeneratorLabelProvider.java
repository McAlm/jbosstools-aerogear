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
package org.jboss.tools.aerogear.hybrid.ui.internal.projectGenerator;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.aerogear.hybrid.core.extensions.PlatformSupport;
import org.jboss.tools.aerogear.hybrid.ui.PlatformImage;

public class ProjectGeneratorLabelProvider extends BaseLabelProvider implements ILabelProvider{
	@Override
	public Image getImage(Object element) {
		PlatformSupport platform = (PlatformSupport)element;
		return PlatformImage.getImageFor(PlatformImage.ATTR_PLATFORM_SUPPORT, platform.getID());
	}

	@Override
	public String getText(Object element) {
		PlatformSupport generator = (PlatformSupport)element;
		return generator.getPlatform();
	}

}
