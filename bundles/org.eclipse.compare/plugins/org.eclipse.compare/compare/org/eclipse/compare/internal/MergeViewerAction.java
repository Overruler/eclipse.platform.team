/*
 * Copyright (c) 2000, 2003 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.compare.internal;

import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.jface.action.Action;


public abstract class MergeViewerAction extends Action implements IUpdate {
	
	private boolean fMutable;
	private boolean fSelection;
	private boolean fContent;
	
	public MergeViewerAction(boolean mutable, boolean selection, boolean content) {
		fMutable= mutable;
		fSelection= selection;
		fContent= content;
	}

	public boolean isSelectionDependent() {
		return fSelection;
	}
	
	public boolean isContentDependent() {
		return fContent;
	}
	
	public boolean isEditableDependent() {
		return fMutable;
	}
	
	public void update() {
	}
}
