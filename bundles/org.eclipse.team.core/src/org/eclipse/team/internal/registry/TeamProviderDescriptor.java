/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.registry;

import org.eclipse.core.runtime.*;

public class TeamProviderDescriptor {
	public  static final String ATT_ID = "id"; //$NON-NLS-1$
		public  static final String ATT_NAME = "name"; //$NON-NLS-1$
		public  static final String ATT_ICON = "icon"; //$NON-NLS-1$
		public  static final String ATT_CLASS = "class"; //$NON-NLS-1$
		private static final String ATT_TYPE = "type"; //$NON-NLS-1$	
		private static final String TYPE_STATIC = "static"; //$NON-NLS-1$
	
		private String label;
		private String className;
		private String type;
		private String id;
		private String description;
	
		private IConfigurationElement configElement;

		/**
		 * Create a new ViewDescriptor for an extension.
		 */
		public TeamProviderDescriptor(IConfigurationElement e, String desc) throws CoreException {
			configElement = e;
			description = desc;
			loadFromExtension();
		}


		public IConfigurationElement getConfigurationElement() {
			return configElement;
		}

		/**
		 * Returns this view's description. This is the value of its <code>"description"</code>
		 * attribute.
		 * 
		 * @return the description
		 */
		public String getDescription() {
			return description;
		}
	
		public String getId() {
			return id;
		}

		public String getType() {
			return type;
		}
		
		public String getLabel() {
			return label;
		}

		public boolean isStatic() {
			if(type == null) return true;
			return type.equals(TYPE_STATIC);
		}
	
		/**
		 * load a view descriptor from the registry.
		 */
		private void loadFromExtension() throws CoreException {
			String identifier = configElement.getAttribute(ATT_ID);
			label = configElement.getAttribute(ATT_NAME);
			className = configElement.getAttribute(ATT_CLASS);
			type = configElement.getAttribute(ATT_TYPE);

			// Sanity check.
			if ((label == null) || (className == null) || (identifier == null)) {
				throw new CoreException(new Status(IStatus.ERROR, configElement.getDeclaringExtension().getDeclaringPluginDescriptor().getUniqueIdentifier(), 0, "Invalid extension (missing label or class name): " + id, //$NON-NLS-1$
						null));
			}
		
			id = identifier;
		}

		/**
		 * Returns a string representation of this descriptor. For debugging
		 * purposes only.
		 */
		public String toString() {
			return "Synchronize Participant(" + getId() + ")"; //$NON-NLS-2$//$NON-NLS-1$
		}
}
