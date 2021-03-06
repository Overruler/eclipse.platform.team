/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.filesystem.history;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistoryProvider;

public class FileSystemHistoryProvider extends FileHistoryProvider {

	public IFileHistory getFileHistoryFor(IResource resource, int flags, IProgressMonitor monitor) {
		return null;
	}

	public IFileHistory getFileHistoryFor(IFileStore store, int flags, IProgressMonitor monitor) {
		return null;
	}

	public IFileRevision getWorkspaceFileRevision(IResource resource) {
		return null;
	}

}
