/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

/**
 * A synchronization context that allows clients to cache operation
 * state for the duration of the operation. When the context is disposed,
 * the cache will be cleared.
 * <p>
 * This interface is not intended to be implemented by clients.
 * 
 * @since 3.2
 */
public interface ISynchronizeOperationContext extends ISynchronizationContext {

	/**
	 * Cache the given property with this context.
	 * @param name the property name that uniquely identifies the property
	 * @param value the value to be cached.
	 */
	void addProperty(String name, Object value);
	
	/**
	 * Retrieve a property that has been cached with the context
	 * @param name the name of the property
	 * @return the object associated with the property name or <code>null</code>
	 */
	Object getProperty(String name);
	
	/**
	 * Remove the named property from the context
	 * @param name the property name
	 */
	void removeProperty(String name);
	
	/**
	 * Add a listener to the context that will receive notification
	 * when the context is disposed. Adding a listener that has already
	 * been added has no effect.
	 * @param listener the listener to add
	 */
	void addDisposeListener(IDisposeListener listener);
	
	/**
	 * Remove the listener. Removing a listener that is not registered
	 * has no effect.
	 * @param listener the listener to remove
	 */
	void removeDisposeListener(IDisposeListener listener);
}
