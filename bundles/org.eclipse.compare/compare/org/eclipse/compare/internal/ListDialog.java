/*
 * Copyright (c) 2000, 2003 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.compare.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;

import org.eclipse.ui.dialogs.SelectionDialog;

public class ListDialog extends SelectionDialog {

	private IStructuredContentProvider fContentProvider;
	private ILabelProvider fLabelProvider;
	private Object fInput;
	private TableViewer fTableViewer;
	private boolean fAddCancelButton;
	
	public ListDialog(Shell parent) {
		super(parent);
		fAddCancelButton= false;
	}

	public void setInput(Object input) {
		fInput= input;
	}
	
	public void setContentProvider(IStructuredContentProvider sp){
		fContentProvider= sp;
	}
	
	public void setLabelProvider(ILabelProvider lp){
		fLabelProvider= lp;
	}

	public void setAddCancelButton(boolean addCancelButton) {
		fAddCancelButton= addCancelButton;
	}
	
	public TableViewer getTableViewer(){
		return fTableViewer;
	}
			
	public boolean hasFilters(){
		return fTableViewer.getFilters() != null && fTableViewer.getFilters().length != 0;
	}
	
	public void create() {
		setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
		super.create();
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		if (! fAddCancelButton)
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		else
			super.createButtonsForButtonBar(parent);	
	}	
	
	protected Control createDialogArea(Composite container) {
		Composite parent= (Composite) super.createDialogArea(container);
		createMessageArea(parent);
		fTableViewer= new TableViewer(parent, getTableStyle());
		fTableViewer.setContentProvider(fContentProvider);
		Table table= fTableViewer.getTable();
		fTableViewer.setLabelProvider(fLabelProvider);
		fTableViewer.setInput(fInput);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(15);
		gd.widthHint= convertWidthInCharsToPixels(55);
		table.setLayoutData(gd);
		return parent;
	}
	
	protected int getTableStyle() {
		return SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER;
	}
}
