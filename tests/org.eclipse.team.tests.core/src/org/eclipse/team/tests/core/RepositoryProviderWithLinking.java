/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;

/**
 * This class is used to text resource linking
 */
public class RepositoryProviderWithLinking extends RepositoryProvider {
	
	final public static String TYPE_ID = "org.eclipse.team.tests.core.linking";
	
	private static boolean canHandleLinking = false;

	private static boolean canHandleLinkedURI;
	
	/**
	 * @see org.eclipse.team.core.RepositoryProvider#configureProject()
	 */
	public void configureProject() throws CoreException {
	}
	/**
	 * @see org.eclipse.team.core.RepositoryProvider#getID()
	 */
	public String getID() {
		return TYPE_ID;
	}
	/**
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {
	}

	public static void setCanHandleLinking(boolean canHandleLinking) {
		RepositoryProviderWithLinking.canHandleLinking = canHandleLinking;
	}

	/**
	 * @see org.eclipse.team.core.RepositoryProvider#canHandleLinkedResources()
	 */
	public boolean canHandleLinkedResources() {
		return canHandleLinking;
	}
	
	public static void setCanHandleLinkedURI(boolean canHandleLinkedURI) {
		RepositoryProviderWithLinking.canHandleLinkedURI = canHandleLinkedURI;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.RepositoryProvider#canHandleLinkedResourceURI()
	 */
	public boolean canHandleLinkedResourceURI() {
		return RepositoryProviderWithLinking.canHandleLinkedURI;
	}

}
