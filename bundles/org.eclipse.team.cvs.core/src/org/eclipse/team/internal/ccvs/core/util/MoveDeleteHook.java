/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.team.*;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;

/**
 * This hook exists to ensure that folders deletions will be recorded so that outgoing file
 * deletions can be properly communicated to the server.
 */
public class MoveDeleteHook implements IMoveDeleteHook {
	
	/**
	 * @see IMoveDeleteHook#deleteFile(IResourceTree, IFile, int, IProgressMonitor)
	 */
	public boolean deleteFile(
		final IResourceTree tree,
		final IFile file,
		final int updateFlags,
		IProgressMonitor monitor) {
		
		try {
			monitor.beginTask(null, 100);

			// No special handling required for team-private members
			if (file.isTeamPrivateMember()) return false;

			// If the file is ignored by CVS then we can just delete it.
			ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor(file);
			if (cvsFile.isIgnored()) return false;

			// If we can't check out the files, return.
			if (!checkOutFiles(tree, new IFile[] {file}, Policy.subMonitorFor(monitor, 30))) {
				// Return that the delete was handled because the checkout
				// will have reported the error to the IResourceTree
				return true;
			}

			// Otherwise, we need to prepare properly for the delete
			EclipseSynchronizer.getInstance().performMoveDelete(new ICVSRunnable() {
				public void run(IProgressMonitor monitor) throws CVSException {
					try {
						monitor.beginTask(null, 100);		
						EclipseSynchronizer.getInstance().prepareForDeletion(file, Policy.subMonitorFor(monitor, 40));
						tree.standardDeleteFile(file, updateFlags, Policy.subMonitorFor(monitor, 60));
					} finally {
						monitor.done();
					}
				}
			}, Policy.subMonitorFor(monitor, 70));
		} catch (CVSException e) {
			tree.failed(e.getStatus());
		} finally {
			monitor.done();
		}
		return true;
	}
	
	/**
	 * @see IMoveDeleteHook#deleteFolder(IResourceTree, IFolder, int, IProgressMonitor)
	 */
	public boolean deleteFolder(
		final IResourceTree tree,
		final IFolder folder,
		final int updateFlags,
		IProgressMonitor monitor) {
		
		// No special handling required for team-private members
		if (folder.isTeamPrivateMember()) return false;
		monitor.beginTask(null, 100);
		try {
			final ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(folder);
			if (cvsFolder.isCVSFolder() && ensureCheckedOut(new IFolder[] {folder}, tree, Policy.subMonitorFor(monitor, 30))) {
				EclipseSynchronizer.getInstance().performMoveDelete(new ICVSRunnable() {
					public void run(IProgressMonitor monitor) throws CVSException {
						try {
							monitor.beginTask(null, 100);
							EclipseSynchronizer.getInstance().prepareForDeletion(folder, Policy.subMonitorFor(monitor, 20));
							tree.standardDeleteFolder(folder, updateFlags, Policy.subMonitorFor(monitor, 50));
						} finally {
							monitor.done();
						}
					}
				}, Policy.subMonitorFor(monitor, 70));
				return true;
			} else if (!cvsFolder.isIgnored()) {
				EclipseSynchronizer.getInstance().prepareForDeletion(cvsFolder.getIResource(), Policy.subMonitorFor(monitor, 70));
			}
		} catch (CVSException e) {
			tree.failed(e.getStatus());
		} finally {
			monitor.done();
		}
		return false;
	}

	/**
	 * @see IMoveDeleteHook#deleteProject(IResourceTree, IProject, int, IProgressMonitor)
	 */
	public boolean deleteProject(
		IResourceTree tree,
		IProject project,
		int updateFlags,
		IProgressMonitor monitor) {
			
		// We need to flush any remembered folder deletions for the deleted project.
		// All other sync info is stored in session and persistant properties, which
		// are deleted when the associated resources are deleted
		try {
			EclipseSynchronizer.getInstance().prepareForDeletion(project, monitor);
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
		}
		// todo: Perform a "cvs release" if there are any edits on the project
		return false;
	}

	/**
	 * @see IMoveDeleteHook#moveFile(IResourceTree, IFile, IFile, int, IProgressMonitor)
	 */
	public boolean moveFile(
			final IResourceTree tree,
			final IFile source,
			final IFile destination,
			final int updateFlags,
			IProgressMonitor monitor) {
		
		try {
			monitor.beginTask(null, 100);

			// ensure we can write to both the source and the destination
			IFile[] filesToCheckOut;
			if (destination.exists()) {
				filesToCheckOut = new IFile[] {source, destination};
			} else {
				filesToCheckOut = new IFile[] {source};
			}
			if (!checkOutFiles(tree, filesToCheckOut, Policy.subMonitorFor(monitor, 30))) {
				// Return that the move was handled because the checkout
				// will have reported the error to the IResourceTree
				return true;
			}

			// Perform the move
			EclipseSynchronizer.getInstance().performMoveDelete(new ICVSRunnable() {
				public void run(IProgressMonitor monitor) throws CVSException {
					try {
						monitor.beginTask(null, 100);
						EclipseSynchronizer.getInstance().prepareForDeletion(source, Policy.subMonitorFor(monitor, 40));
						if (destination.exists()) {
							EclipseSynchronizer.getInstance().prepareForDeletion(destination, Policy.subMonitorFor(monitor, 20));
						}
						tree.standardMoveFile(source, destination, updateFlags, Policy.subMonitorFor(monitor, 40));
						EclipseSynchronizer.getInstance().postMove(destination);
					} finally {
						monitor.done();
					}
				}
			}, Policy.subMonitorFor(monitor, 70));
		} catch (CVSException e) {
			tree.failed(e.getStatus());
		} finally {
			monitor.done();
		}
		return true;
	}

	/**
	 * @see IMoveDeleteHook#moveFolder(IResourceTree, IFolder, IFolder, int, IProgressMonitor)
	 */
	public boolean moveFolder(
		final IResourceTree tree,
		final IFolder source,
		final IFolder destination,
		final int updateFlags,
		IProgressMonitor monitor) {
		
		monitor.beginTask(null, 100);
		try {
			final ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(source);
			if (cvsFolder.isManaged()) {
				if (!ensureCheckedOut(new IFolder[] {source, destination}, tree, Policy.subMonitorFor(monitor, 20))) return true;
				EclipseSynchronizer.getInstance().performMoveDelete(new ICVSRunnable() {
					public void run(IProgressMonitor monitor) throws CVSException {
						EclipseSynchronizer.getInstance().prepareForDeletion(source, Policy.subMonitorFor(monitor, 20));
						if (destination.exists()) {
							EclipseSynchronizer.getInstance().prepareForDeletion(destination, Policy.subMonitorFor(monitor, 20));
						}
						tree.standardMoveFolder(source, destination, updateFlags, Policy.subMonitorFor(monitor, 30));
						purgeCVSFolders(destination, Policy.subMonitorFor(monitor, 20));
						EclipseSynchronizer.getInstance().postMove(destination);
					}
					private void purgeCVSFolders(IFolder destination, final IProgressMonitor monitor) throws CVSException {
						// Delete any CVS folders
						try {
							destination.accept(new IResourceVisitor() {
								public boolean visit(IResource resource) throws CoreException {
									if (resource.getType() == IResource.FOLDER && resource.getName().equals(SyncFileWriter.CVS_DIRNAME)) {
										tree.standardDeleteFolder((IFolder)resource, updateFlags, monitor);
										return false;
									}
									return true;
								}
							}, IResource.DEPTH_INFINITE, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
						} catch (CoreException e) {
							throw CVSException.wrapException(e);
						}
					}
				}, Policy.subMonitorFor(monitor, 60));
				return true;
			} else if (!cvsFolder.isIgnored()) {
				EclipseSynchronizer.getInstance().prepareForDeletion(cvsFolder.getIResource(), Policy.subMonitorFor(monitor, 60));
			}
		} catch (CVSException e) {
			tree.failed(e.getStatus());
			return true;
		} finally {
			monitor.done();
		}
			
		return false;
	}

	/**
	 * @see IMoveDeleteHook#moveProject(IResourceTree, IProject, IProjectDescription, int, IProgressMonitor)
	 */
	public boolean moveProject(
		IResourceTree tree,
		IProject source,
		IProjectDescription description,
		int updateFlags,
		IProgressMonitor monitor) {
			
		// We need to move (or flush) any remembered folder deletions for the deleted project
		// XXX We flush for now. This means that deleting a managed folder and then moving the
		// project will mean that the file deletions will be lost. It also means that phantom
		// folders are lost.
		try {
			EclipseSynchronizer.getInstance().prepareForDeletion(source, monitor);
		} catch (CVSException e) {
			CVSProviderPlugin.log(e);
		}
		return false;
	}
	
	/**
	 * Ensure that the given file is checked out (i.e. not read only). Return
	 * true if it is OK to procede and false otherwise.
	 * 
	 * @param tree
	 * @param file
	 * @return boolean
	 */
	/* private */ boolean checkOutFiles(IResourceTree tree, IFile[] files, IProgressMonitor monitor) {
		// Ensure that the file is "checked out" (i.e. not read-only
		IFileModificationValidator validator = getFileModificationValidator(files);
		if (validator instanceof ICVSFileModificationValidator) {
			IStatus status = ((ICVSFileModificationValidator)validator).validateMoveDelete(files, monitor);
			if (status.isOK()) {
				return true;
			} else {
				tree.failed(status);
				return false;
			}
		}
		return true;
	}

	private boolean ensureCheckedOut(IFolder[] folders, IResourceTree tree, IProgressMonitor monitor) {
		final List readOnlyFiles = new ArrayList();
		try {
			// Find any read-only files
			for (int i = 0; i < folders.length; i++) {
				IFolder folder = folders[i];
				if (folder.exists()) {
					folder.accept(new IResourceVisitor() {
						public boolean visit(IResource resource) throws CoreException {
							if (resource.getType() == IResource.FILE) {
								IFile file = (IFile) resource;
								if (file.isReadOnly()) {
									readOnlyFiles.add(file);
								}
							}
							return true;
						}
					});
				}
			}
			if (readOnlyFiles.isEmpty()) return true;
			// Ensure read-only files are checked out
			return checkOutFiles(tree, (IFile[]) readOnlyFiles.toArray(new IFile[readOnlyFiles.size()]), monitor);
		} catch (CoreException e) {
			tree.failed(e.getStatus());
			return false;
		}
	}

	private FileModificationValidator getFileModificationValidator(IFile[] files) {
		return getProvider(files).getFileModificationValidator2();
	}

	private CVSTeamProvider getProvider(IFile[] files) {
		CVSTeamProvider provider = (CVSTeamProvider)RepositoryProvider.getProvider(files[0].getProject(), CVSProviderPlugin.getTypeId());
		return provider;
	}
}
