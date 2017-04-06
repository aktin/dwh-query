package org.aktin.request.manager;

import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.aktin.broker.request.RequestManager;
import org.aktin.broker.request.RequestStatus;
import org.aktin.broker.request.RetrievedRequest;
import org.aktin.broker.request.Status;

@javax.ejb.Singleton
@javax.ejb.Startup
public class RequestScheduler {
	private static final Logger log = Logger.getLogger(RequestScheduler.class.getName());

	RequestManager manager;

	@Resource
    private TimerService timerService;
	private Timer nextExecution;
	private TimerConfig timerConfig;
	private Consumer<RetrievedRequest> executor;

	private LinkedList<RetrievedRequest> queue;

	public RequestScheduler(){
		timerConfig = new TimerConfig();
		timerConfig.setPersistent(false);
		this.queue = new LinkedList<>();
	}

	@Inject
	public void setRequestExecutor(RequestProcessor executor){
		this.executor = executor;
	}

	private void updateTimer(){
		// clear previous timer
		if( nextExecution != null ){
			// TODO handle exceptions?
			nextExecution.cancel();
			nextExecution = null;
		}
		Instant next;
		synchronized( queue ){
			if( queue.isEmpty() ){
				// nothing to do
				return;
			}
			// get next scheduled time
			next = queue.getFirst().getRequest().getScheduledTimestamp();
		}
		long waitMilli = next.toEpochMilli() - System.currentTimeMillis();
		nextExecution = timerService.createSingleActionTimer(waitMilli, timerConfig);
		log.info("Next execution scheduled for "+nextExecution.getNextTimeout());
	}

	private void startExecution(RetrievedRequest request){
		executor.accept(request);
	}

	private void startDueExecutions(){
		Instant now = Instant.now();
		synchronized( queue ){
			Iterator<RetrievedRequest> i = queue.iterator();
			while( i.hasNext() ){
				RetrievedRequest r = i.next();
				Instant rt = r.getRequest().getScheduledTimestamp();
				if( rt == null || rt.isBefore(now) ){
					// no scheduled time, or due
					i.remove();
					startExecution(r);
				}else{
					// scheduled later, stop iterating
					break;
				}
			}
		}
	}
	protected void scheduleRequest(@Observes @Status(RequestStatus.Queued) RetrievedRequest request){
		Instant ts = request.getRequest().getScheduledTimestamp();
		// queries without timestamp are forwarded immediately
		if( ts == null ){
			startExecution(request);
		}
		// insert at right place in queue
		int pos = 0;
		synchronized( queue ){
			for( RetrievedRequest req : queue ){
				Instant other = req.getRequest().getScheduledTimestamp();
				if( ts.isAfter(other) ){
					pos ++;
				}else{
					// found our place
					break;
				}
			}
			queue.add(pos, request);
		}
		// if there is an existing timer and the new request is not at the first position
		// then we don't need to change the timer
		if( nextExecution == null || pos == 0 ){
			// no previous timer or request scheduled before other requests
			// update timer
			updateTimer();
		}
	}

	@Timeout
	private void timerCallback(Timer timer){
		// one shot timer is already expired
		this.nextExecution = null;
		startDueExecutions();
		updateTimer();
	}
}
