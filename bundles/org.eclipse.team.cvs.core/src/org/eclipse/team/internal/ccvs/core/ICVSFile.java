/*******************************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.team.internal.ccvs.core;

import java.io.InputStream;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.syncinfo.NotifyInfo;

/**
 * The CVS analog of a file. CVS files have access to synchronization information
 * that describes their association with the CVS repository. CVS files also provide 
 * mechanisms for sending and receiving content.
 * 
 * @see ICVSResource
 */
public interface ICVSFile extends ICVSResource {
	
	// Constants used to indicate the type of updated response from the server
	public static final int UPDATED = 1;
	public static final int MERGED = 2;
	public static final int UPDATE_EXISTING = 3;
	public static final int CREATED = 4;
	
	// Constants used to indicate temporary watches
	public static final int NO_NOTIFICATION = 0;
	public static final int NOTIFY_ON_EDIT = 1;
	public static final int NOTIFY_ON_UNEDIT = 2;
	public static final int NOTIFY_ON_COMMIT = 4;
	public static final int NOTIFY_ON_ALL = NOTIFY_ON_EDIT | NOTIFY_ON_UNEDIT | NOTIFY_ON_COMMIT;
	
	/**
	 * Answers the size of the file. 
	 */
	long getSize();
	
 	/**
	 * Gets an input stream for reading from the file.
	 * It is the responsibility of the caller to close the stream when finished.
 	 */
	InputStream getContents() throws CVSException;
	
	/**
	 * Set the contents of the file to the contents of the provided input stream.
	 * 
	 * This method is used by the command framework and should not be used by other clients.
	 * Other clients should set the contents of the underlying <code>IFile</code> which
	 * can be obtained using <code>getIResource()</code>.
	 * 
	 * @param responseType the type of reponse that was received from the server
	 * 
	 *    UPDATED - could be a new file or an existing file
	 *    MERGED - merging remote changes with local changes. Failure could result in loss of local changes
	 *    CREATED - contents for a file that doesn't exist locally
	 *    UPDATE_EXISTING - Replacing a local file with no local changes with remote changes.
	 */
	public void setContents(InputStream stream, int responseType, boolean keepLocalHistory, IProgressMonitor monitor) throws CVSException;

	/**
	 * Sets the file to read-only (<code>true</code>) or writable (<code>false</code>).
	 * 
	 * This method is used by the command framework and should not be used by other clients.
	 * Other clients should use <code>checkout</code> and <code>uncheckout</code> instead as they
	 * will report the change to the server if appropriate.
	 */
	void setReadOnly(boolean readOnly) throws CVSException;
	
	/**
	 * Answers whether the file is read-only or not.
	 * 
	 * This method is used by the command framework and should not be used by other clients.
	 * Other clients should use <code>isCheckedOut</code> instead.
	 */
	boolean isReadOnly() throws CVSException;
	
	/**
	 * Copy the resource to another file in the same directory
	 * 
	 * This method is used by the command framework and should not be used by other clients.
	 */
	void copyTo(String filename) throws CVSException;
	
	/**
	 * Answers the current timestamp for this file with second precision.
	 * 
	 * This method is used by the command framework and should not be used by other clients.
	 */
	Date getTimeStamp();

	/**
	 * If the date is <code>null</code> then the current time is used.
	 * 
	 * This method is used by the command framework and should not be used by other clients.
	 */
	void setTimeStamp(Date date) throws CVSException;
	
	/**
	 * Answers <code>true</code> if the file has changed since it was last updated
	 * from the repository, if the file does not exist, or is not managed. And <code>false</code> 
	 * if it has not changed.
	 */
	boolean isModified() throws CVSException;
	
	/**
	 * Answers the revision history for this file. This is similar to the
	 * output of the log command.
	 */
	public ILogEntry[] getLogEntries(IProgressMonitor monitor) throws TeamException;
	
	/**
	 * Indicate whether a fiel has been checked out for local editing. A file is checked out
	 * for local editing if it's read-only bit is false.
	 */
	public boolean isCheckedOut() throws CVSException;
	
	/**
	 * Mark the file as checked out to allow local editing (analogous to "cvs edit"). 
	 * If this method is invoked when <code>isCheckedOut()</code> returns <code>false</code>, 
	 * a notification message that will be sent to the server on the next connection
	 * If <code>isCheckedOut()</code> returns <code>true</code> then nothing is done.
	 * 
	 * @param notifications the set of operations for which the local user would like notification
	 * while the local file is being edited.
	 */
	public void checkout(int notifications) throws CVSException;

	/**
	 * Undo a checkout of the file (analogous to "cvs unedit").
	 * If this method is invoked when <code>isCheckedOut()</code> returns <code>true</code>, 
	 * a notification message that will be sent to the server on the next connection
	 * If <code>isCheckedOut()</code> returns <code>false</code> then nothing is done.
	 */
	public void uncheckout() throws CVSException;
	
	/**
	 * Answer any pending notification information associated with the receiver.
	 * 
	 * This method is used by the command framework and should not be used by other clients.
	 */
	public NotifyInfo getPendingNotification() throws CVSException;
	
	/**
	 * Indicate to the file that the pending notification was successfully communicated to the server.
	 * 
	 * This method is used by the command framework and should not be used by other clients.
	 */
	public void notificationCompleted() throws CVSException;

}