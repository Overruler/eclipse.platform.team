/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.subscribers;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.*;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.core.*;

/**
 * This handler collects changes and removals to resources and calculates their
 * synchronization state in a background job. The result is fed input the SyncSetInput.
 * 
 * Exceptions that occur when the job is processing the events are collected and
 * returned as part of the Job's status.
 */
public class SubscriberEventHandler extends BackgroundEventHandler {
	// The set that receives notification when the resource synchronization state
	// has been calculated by the job.
	private SyncSetInputFromSubscriber syncSetInput;

	// Changes accumulated by the event handler
	private List resultCache = new ArrayList();
	
	private boolean started = false;

	private IProgressMonitor progressGroup;

	private int ticks;
	
	/**
	 * Internal resource synchronization event. Can contain a result.
	 */
	class SubscriberEvent extends Event{
		static final int REMOVAL = 1;
		static final int CHANGE = 2;
		static final int INITIALIZE = 3;
		SyncInfo result;

		SubscriberEvent(IResource resource, int type, int depth) {
			super(resource, type, depth);
		}
		public SubscriberEvent(
			IResource resource,
			int type,
			int depth,
			SyncInfo result) {
				this(resource, type, depth);
				this.result = result;
		}
		public SyncInfo getResult() {
			return result;
		}
		protected String getTypeString() {
			switch (getType()) {
				case REMOVAL :
					return "REMOVAL"; //$NON-NLS-1$
				case CHANGE :
					return "CHANGE"; //$NON-NLS-1$
				case INITIALIZE :
					return "INITIALIZE"; //$NON-NLS-1$
				default :
					return "INVALID"; //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * This is a special event used to reset and connect sync sets.
	 * The preemtive flag is used to indicate that the runnable should take
	 * the highest priority and thus be placed on the front of the queue
	 * and be processed as soon as possible, preemting any event that is currently
	 * being processed. The curent event will continue processing once the 
	 * high priority event has been processed
	 */
	public class RunnableEvent extends Event {
		static final int RUNNABLE = 1000;
		private IWorkspaceRunnable runnable;
		private boolean preemtive;
		public RunnableEvent(IWorkspaceRunnable runnable, boolean preemtive) {
			super(ResourcesPlugin.getWorkspace().getRoot(), RUNNABLE, IResource.DEPTH_ZERO);
			this.runnable = runnable;
			this.preemtive = preemtive;
		}
		public void run(IProgressMonitor monitor) throws CoreException {
			runnable.run(monitor);
		}
		public boolean isPreemtive() {
			return preemtive;
		}
	}
	
	/**
	 * Create a handler. This will initialize all resources for the subscriber associated with
	 * the set.
	 * @param set the subscriber set to feed changes into
	 */
	public SubscriberEventHandler(Subscriber subscriber) {
		super(
			Policy.bind("SubscriberEventHandler.jobName", subscriber.getName()), //$NON-NLS-1$
			Policy.bind("SubscriberEventHandler.errors", subscriber.getName())); //$NON-NLS-1$
		this.syncSetInput = new SyncSetInputFromSubscriber(subscriber, this);
	}
	
	/**
	 * Start the event handler by queuing events to prime the sync set input with the out-of-sync 
	 * resources of the subscriber.
	 */
	public synchronized void start() {
		// Set the started flag to enable event queueing.
		// We are gaurenteed to be the first since this method is synchronized.
		started = true;
		reset(syncSetInput.getSubscriber().roots(), SubscriberEvent.INITIALIZE);
	}

	protected synchronized void queueEvent(Event event, boolean front) {
		// Only post events if the handler is started
		if (started) {
			super.queueEvent(event, front);
		}
	}
	/**
	 * Schedule the job or process the events now.
	 */
	public void schedule() {
		Job job = getEventHandlerJob();
		if(progressGroup != null) {
			job.setSystem(false);
			job.setProgressGroup(progressGroup, ticks);
		} else {
			job.setSystem(true);
		}
		getEventHandlerJob().schedule();
	}
	
	/**
	 * Initialize all resources for the subscriber associated with the set. This will basically recalculate
	 * all synchronization information for the subscriber.
	 * <p>
	 * This method is sycnrhonized with the queueEvent method to ensure that the two events
	 * queued by this method are back-to-back
	 */
	public synchronized void reset(IResource[] roots) {
		if (roots == null) {
			roots = syncSetInput.getSubscriber().roots();
		}
		// First, reset the sync set input to clear the sync set
		run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				syncSetInput.reset(monitor);
			}
		}, false /* keep ordering the same */);
		// Then, prime the set from the subscriber
		reset(roots, SubscriberEvent.CHANGE);
	}
	
	/**
	 * Called by a client to indicate that a resource has changed and its synchronization state
	 * should be recalculated.  
	 * @param resource the changed resource
	 * @param depth the depth of the change calculation
	 */
	public void change(IResource resource, int depth) {
		queueEvent(new SubscriberEvent(resource, SubscriberEvent.CHANGE, depth), false);
	}
	
	/**
	 * Called by a client to indicate that a resource has been removed and should be removed. The
	 * removal will propagate to the set.
	 * @param resource the resource that was removed
	 */
	public void remove(IResource resource) {
		queueEvent(
			new SubscriberEvent(resource, SubscriberEvent.REMOVAL, IResource.DEPTH_INFINITE), false);
	}
	
	/**
	 * Collect the calculated synchronization information for the given resource at the given depth. The
	 * results are added to the provided list.
	 */
	private void collect(
		IResource resource,
		int depth,
		IProgressMonitor monitor) {
		
		// handle any preemtive events before continuing
		handlePreemptiveEvents(monitor);
		
		if (resource.getType() != IResource.FILE
			&& depth != IResource.DEPTH_ZERO) {
			try {
				IResource[] members =
					syncSetInput.getSubscriber().members(resource);
				for (int i = 0; i < members.length; i++) {
					collect(
						members[i],
						depth == IResource.DEPTH_INFINITE
							? IResource.DEPTH_INFINITE
							: IResource.DEPTH_ZERO,
						monitor);
				}
			} catch (TeamException e) {
				handleException(e, resource, ITeamStatus.SYNC_INFO_SET_ERROR, "The members of folder {0} could not be retrieved." + resource.getFullPath().toString());
			}
		}

		monitor.subTask(Policy.bind("SubscriberEventHandler.2", resource.getFullPath().toString())); //$NON-NLS-1$
		try {
			SyncInfo info = syncSetInput.getSubscriber().getSyncInfo(resource);
			// resource is no longer under the subscriber control
			if (info == null) {
				resultCache.add(
					new SubscriberEvent(resource, SubscriberEvent.REMOVAL, IResource.DEPTH_ZERO));
			} else {
				resultCache.add(
					new SubscriberEvent(resource, SubscriberEvent.CHANGE, IResource.DEPTH_ZERO, info));
			}
			handlePendingDispatch(monitor);
		} catch (TeamException e) {
			handleException(e, resource, ITeamStatus.RESOURCE_SYNC_INFO_ERROR, "The synchronization state for resource {0} could not be determined." + resource.getFullPath().toString());
		}
		monitor.worked(1);
	}
	
	private void handlePendingDispatch(IProgressMonitor monitor) {
		if (isReadyForDispatch(false /*don't wait if queue is empty*/)) {
			dispatchEvents(Policy.subMonitorFor(monitor, 5));
			eventsDispatched();
		}
	}

	/*
	 * Handle the exception by returning it as a status from the job but also by
	 * dispatching it to the sync set input so any down stream views can react
	 * accordingly.
	 */
	private void handleException(CoreException e, IResource resource, int code, String message) {
		handleException(e);
		syncSetInput.handleError(new TeamStatus(IStatus.ERROR, TeamPlugin.ID, code, message, e, resource));
	}

	/**
	 * Called to initialize to calculate the synchronization information using the optimized subscriber method. For
	 * subscribers that don't support the optimization, all resources in the subscriber are manually re-calculated. 
	 * @param resources the resources to check
	 * @param depth the depth
	 * @param monitor
	 * @return Event[] the change events
	 * @throws TeamException
	 */
	private void collectAll(
		IResource resource,
		int depth,
		IProgressMonitor monitor) {
		
		monitor.beginTask(null, 100);
		try {
			SyncInfo[] infos = null;
				try {
					infos = syncSetInput.getSubscriber().getAllOutOfSync(new IResource[] { resource }, depth, Policy.subMonitorFor(monitor, 10));
				} catch (TeamException e) {
					// Log the exception and fallback to using hierarchical search
					TeamPlugin.log(e);
				}
	
			// The subscriber hasn't cached out-of-sync resources. We will have to
			// traverse all resources and calculate their state. 
			if (infos == null) {
				IProgressMonitor subMonitor = Policy.infiniteSubMonitorFor(monitor, 90);
				subMonitor.beginTask(null, 20);
				collect(
						resource,
						IResource.DEPTH_INFINITE,
						subMonitor);
			} else {
				// The subscriber has returned the list of out-of-sync resources.
				for (int i = 0; i < infos.length; i++) {
					SyncInfo info = infos[i];
					resultCache.add(
						new SubscriberEvent(info.getLocal(), SubscriberEvent.CHANGE, depth, info));
				}
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Feed the given events to the set. The appropriate method on the set is called
	 * for each event type. 
	 * @param events
	 */
	private void dispatchEvents(SubscriberEvent[] events, IProgressMonitor monitor) {
		// this will batch the following set changes until endInput is called.
		try {
			syncSetInput.getSyncSet().beginInput();
			for (int i = 0; i < events.length; i++) {
				SubscriberEvent event = events[i];
				switch (event.getType()) {
					case SubscriberEvent.CHANGE :
						syncSetInput.collect(event.getResult(), monitor);
						break;
					case SubscriberEvent.REMOVAL :
						syncSetInput.getSyncSet().remove(event.getResource(), event.getDepth());
						break;
				}
			}
		} finally {
			syncSetInput.getSyncSet().endInput(monitor);
		}
	}
	
	/**
	 * Initialize all resources for the subscriber associated with the set. This will basically recalculate
	 * all synchronization information for the subscriber.
	 * @param type can be Event.CHANGE to recalculate all states or Event.INITIALIZE to perform the
	 *   optimized recalculation if supported by the subscriber.
	 */
	private void reset(IResource[] roots, int type) {
		IResource[] resources = roots;
		for (int i = 0; i < resources.length; i++) {
			queueEvent(new SubscriberEvent(resources[i], type, IResource.DEPTH_INFINITE), false);
		}
	}

	protected void processEvent(Event event, IProgressMonitor monitor) {
		try {
			// Cancellation is dangerous because this will leave the sync info in a bad state.
			// Purposely not checking -
			int type = event.getType();
			switch (type) {
				case RunnableEvent.RUNNABLE :
					executeRunnable(event, monitor);
					break;
				case SubscriberEvent.REMOVAL :
					resultCache.add(event);
					break;
				case SubscriberEvent.CHANGE :
					collect(
						event.getResource(),
						event.getDepth(),
						monitor);
					break;
				case SubscriberEvent.INITIALIZE :
					getEventHandlerJob().setSystem(false);
					monitor.subTask(Policy.bind("SubscriberEventHandler.2", event.getResource().getFullPath().toString())); //$NON-NLS-1$
					collectAll(
							event.getResource(),
							event.getDepth(),
							Policy.subMonitorFor(monitor, 64));
					break;
			}
		} catch (RuntimeException e) {
			// handle the exception and keep processing
			handleException(new TeamException("An internal error occurred processing subscriber events.", e), event.getResource(), ITeamStatus.SYNC_INFO_SET_ERROR, "An internal error occurred processing resource {0}" + event.getResource().getFullPath().toString());
		}
	}
		
	/*
	 * Execute the RunnableEvent
	 */
	private void executeRunnable(Event event, IProgressMonitor monitor) {
		// Dispatch any queued results to clear pending output events
		dispatchEvents(Policy.subMonitorFor(monitor, 1));
		eventsDispatched();
		try {
			((RunnableEvent)event).run(Policy.subMonitorFor(monitor, 1));
		} catch (CoreException e) {
			handleException(e, event.getResource(), ITeamStatus.SYNC_INFO_SET_ERROR, "An internal error has occurred.");
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.BackgroundEventHandler#dispatchEvents()
	 */
	protected void dispatchEvents(IProgressMonitor monitor) {
		if (!resultCache.isEmpty()) {
			dispatchEvents((SubscriberEvent[]) resultCache.toArray(new SubscriberEvent[resultCache.size()]), monitor);
			resultCache.clear();
		}
	}

	/**
	 * Queue up the given runnable in an event to be processed by this job
	 * @param runnable the runnable to be run by the handler
	 */
	public void run(IWorkspaceRunnable runnable, boolean frontOnQueue) {
		queueEvent(new RunnableEvent(runnable, frontOnQueue), frontOnQueue);
	}

	/**
	 * Return the sync set input that was created by this event handler
	 * @return
	 */
	public SyncSetInputFromSubscriber getSyncSetInput() {
		return syncSetInput;
	}
	
	public void setProgressGroupHint(IProgressMonitor progressGroup, int ticks) {
		this.progressGroup = progressGroup;
		this.ticks = ticks;
	}
	
	/**
	 * @return Returns the started.
	 */
	protected boolean isStarted() {
		return started;
	}
	
	private void handlePreemptiveEvents(IProgressMonitor monitor) {
		Event event = peek();
		if (event instanceof RunnableEvent && ((RunnableEvent)event).isPreemtive()) {
			executeRunnable(nextElement(), monitor);
		}
	}
}