/*
 * Copyright (c) 2000, 2003 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.compare.structuremergeviewer;

/**
 * Listener that gets informed if one (or more)
 * of the three sides of an <code>ICompareInput</code> object changes its value.
 * <p>
 * For example when accepting an incoming addition
 * the (non-null) left side of an <code>ICompareInput</code>
 * is copied to the right side (which was <code>null</code>).
 * This triggers a call to <code>compareInputChanged</code> of registered
 * <code>ICompareInputChangeListener</code>. 
 * <p>
 * Note however, that listener are not informed if the content of one of the sides changes.
 * <p>
 * Clients may implement this interface. It is also implemented by viewers that take 
 * an <code>ICompareInput</code> as input.
 * </p>
 */
public interface ICompareInputChangeListener {
	
	/**
	 * Called whenever the value (not the content) of one or more of the three sides 
 	 * of a <code>ICompareInput</code> has changed.
	 *
	 * @param source the <code>ICompareInput</code> that has changed
	 */
	void compareInputChanged(ICompareInput source);
}
