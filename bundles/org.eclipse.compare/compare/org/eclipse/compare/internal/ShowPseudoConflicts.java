/*
 * Copyright (c) 2000, 2003 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.compare.internal;

import java.util.ResourceBundle;

import org.eclipse.compare.*;

/**
 * Toggles the <code>ICompareConfiguration.SHOW_PSEUDO_CONFLICTS</code> property of an
 * <code>ICompareConfiguration</code>.
 */
public class ShowPseudoConflicts extends ChangePropertyAction {

	public ShowPseudoConflicts(ResourceBundle bundle, CompareConfiguration cc) {
		super(bundle, cc, "action.ShowPseudoConflicts.", CompareConfiguration.SHOW_PSEUDO_CONFLICTS); //$NON-NLS-1$
	}
}
