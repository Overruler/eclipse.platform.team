package org.eclipse.team.internal.ui.jobs;

import java.util.*;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.RefreshCompleteDialog;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.team.ui.synchronize.subscriber.*;
import org.eclipse.team.ui.synchronize.viewers.SyncInfoCompareInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

/**
 * This class manages the notification and setup that occurs after a refresh is completed.
 * 
 * 
 */
public class RefreshUserNotificationPolicy implements IRefreshSubscriberListener {

	private SubscriberParticipant participant;

	public RefreshUserNotificationPolicy(SubscriberParticipant participant) {
		this.participant = participant;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.jobs.IRefreshSubscriberListener#refreshStarted(org.eclipse.team.internal.ui.jobs.IRefreshEvent)
	 */
	public void refreshStarted(IRefreshEvent event) {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.jobs.IRefreshSubscriberListener#refreshDone(org.eclipse.team.internal.ui.jobs.IRefreshEvent)
	 */
	public void refreshDone(final IRefreshEvent event) {
		if (event.getSubscriber() != participant.getSubscriberSyncInfoCollector().getSubscriber())
			return;
		int type = event.getRefreshType();
		boolean promptWithChanges = TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_PROMPT_WITH_CHANGES);
		boolean promptWhenNoChanges = TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_PROMPT_WHEN_NO_CHANGES);
		boolean promptWithChangesBkg = TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_BKG_PROMPT_WITH_CHANGES);
		boolean promptWhenNoChangesBkg = TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_VIEW_BKG_PROMPT_WHEN_NO_CHANGES);
		boolean shouldPrompt = false;
		final SyncInfo[] infos = event.getChanges();
		if (type == IRefreshEvent.USER_REFRESH) {
			if (promptWhenNoChanges && infos.length == 0) {
				shouldPrompt = true;
			} else if (promptWithChanges && infos.length > 0) {
				shouldPrompt = true;
			}
		} else {
			if (promptWhenNoChangesBkg && infos.length == 0) {
				shouldPrompt = true;
			} else if (promptWithChangesBkg && infos.length > 0) {
				shouldPrompt = true;
			}
		}
		// If there are interesting changes, ensure the sync view is showing
		// them
		// Also, select them in the sync view
		if (infos.length > 0) {
			participant.setMode(SubscriberParticipant.BOTH_MODE);
		}
		final boolean result[] = {shouldPrompt};
		TeamUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
			public void run() {
				ISynchronizeView view = TeamUI.getSynchronizeManager().showSynchronizeViewInActivePage(null);
				if (view != null) {
					view.display(participant);
					List selectedResources = new ArrayList();
					selectedResources.addAll(Arrays.asList(event.getResources()));
					for (int i = 0; i < infos.length; i++) {
						selectedResources.add(infos[i].getLocal());
					}
					IResource[] resources = (IResource[]) selectedResources.toArray(new IResource[selectedResources.size()]);
					participant.selectResources(resources);
					if (resources.length == 1 && resources[0].getType() == IResource.FILE) {
						IResource file = resources[0];
						SyncInfo info = participant.getSubscriberSyncInfoCollector().getSubscriberSyncInfoSet().getSyncInfo(file);
						if(info != null) {
							CompareUI.openCompareEditor(new SyncInfoCompareInput(participant.getName(), info));
							result[0] = false;
						}
					}
					// Prompt user if preferences are set for this type of refresh.
					if (result[0]) {
						notifyIfNeededModal(event);
					}
				}
			}		
		});
		RefreshSubscriberJob.removeRefreshListener(this);
	}

	private void notifyIfNeededModal(final IRefreshEvent event) {
		TeamUIPlugin.getStandardDisplay().asyncExec(new Runnable() {

			public void run() {
				RefreshCompleteDialog d = new RefreshCompleteDialog(new Shell(TeamUIPlugin.getStandardDisplay()), event, participant);
				d.setBlockOnOpen(false);
				d.open();
			}
		});
	}

	private void notifyIfNeededNonModal(final IRefreshEvent event) {
		String message = Policy.bind("RefreshUserNotificationPolicy.0", event.getSubscriber().getName()); //$NON-NLS-1$
		PlatformUI.getWorkbench().getProgressService().requestInUI(new UIJob(message) {

			public IStatus runInUIThread(IProgressMonitor monitor) {
				RefreshCompleteDialog d = new RefreshCompleteDialog(new Shell(TeamUIPlugin.getStandardDisplay()), event, participant);
				d.setBlockOnOpen(false);
				d.open();
				return Status.OK_STATUS;
			}
		}, message);
	}
}