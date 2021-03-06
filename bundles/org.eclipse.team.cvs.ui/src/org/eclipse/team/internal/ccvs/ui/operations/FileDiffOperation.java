/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import java.io.*;
import java.nio.channels.FileChannel;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.*;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.ui.IWorkbenchPart;

public class FileDiffOperation extends DiffOperation {

	FileOutputStream os;
	PrintStream printStream;
	File file;
	File tempFile;
	
	public FileDiffOperation(IWorkbenchPart part, ResourceMapping[] mappings, LocalOption[] options, File file, boolean isMultiPatch, boolean includeFullPathInformation, IPath patchRoot) {
		super(part, mappings, options, isMultiPatch, includeFullPathInformation, patchRoot, file.getAbsolutePath());
		IPath teamLocation= TeamPlugin.getPlugin().getStateLocation();
		IPath tempFilePath = teamLocation.append(new Path(IPath.SEPARATOR + "tempDiff" + System.currentTimeMillis())); //$NON-NLS-1$
		tempFile = tempFilePath.toFile();
		this.file = file;
	}

	public void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
	    super.execute(monitor);
     
		if (tempFile.length() == 0) {
			tempFile.delete();
			reportEmptyDiff();
			return;
		}	
		
		if (this.isMultiPatch &&
			(!patchHasContents && !patchHasNewFiles)){
			tempFile.delete();
			reportEmptyDiff();
			return;
		}
		
		 copyFile();
	}
	
	protected void copyFile() throws CVSException {
		FileChannel tempFileChannel=null;
		FileChannel fileChannel=null;
		try {
		    tempFileChannel = new FileInputStream(tempFile).getChannel();
		    fileChannel = new FileOutputStream(file).getChannel();
		
		    long size= tempFileChannel.size();
			long bytesTransferred = fileChannel.transferFrom(tempFileChannel, 0, size);
		    while (bytesTransferred != size){
		    	//Transfer from point left off until the end of the file
		    	bytesTransferred += fileChannel.transferFrom(tempFileChannel, bytesTransferred, size);
		    }
		} catch (IOException e) {
			throw CVSException.wrapException(e); 
		} finally {
			if (tempFileChannel!=null)
				try {
					tempFileChannel.close();
				} catch (IOException e) {
					throw CVSException.wrapException(e); 
				}
			
			if (fileChannel!=null)
				try {
					fileChannel.close();
				} catch (IOException e) {
					throw CVSException.wrapException(e); 
				}
				
			if (tempFile != null)
				tempFile.delete();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.DiffOperation#openStream()
	 */
	protected PrintStream openStream() throws CVSException {
		try {
			os = new FileOutputStream(tempFile);
			return new PrintStream(os);
		} catch (FileNotFoundException e) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, CVSUIMessages.GenerateDiffFileOperation_0, e);
			throw new CVSException(status); 
		}
	}

}
