package org.eclipse.team.internal.ccvs.core.client;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;

/**
 * @author Administrator
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class NotifiedHandler extends ResponseHandler {

	/**
	 * @see org.eclipse.team.internal.ccvs.core.client.ResponseHandler#getResponseID()
	 */
	public String getResponseID() {
		return "Notified"; //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.core.client.ResponseHandler#handle(Session, String, IProgressMonitor)
	 */
	public void handle(
		Session session,
		String localDir,
		IProgressMonitor monitor)
		throws CVSException {
			
		// read additional data for the response 
		// (which is the full repository path of the file)
		String repositoryFilePath = session.readLine();

		// clear the notify info for the file
		ICVSFolder folder = session.getLocalRoot().getFolder(localDir);
		ICVSFile file = folder.getFile(new Path(repositoryFilePath).lastSegment());
		file.notificationCompleted();
	}

}
