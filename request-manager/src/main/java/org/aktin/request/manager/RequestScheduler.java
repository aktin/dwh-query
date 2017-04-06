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

import org.aktin.broker.request.RequestManager;
import org.aktin.broker.request.RequestStatus;
import org.aktin.broker.request.RetrievedRequest;
import org.aktin.broker.request.Status;

public class RequestScheduler {
	private static final Logger log = Logger.getLogger(RequestScheduler.class.getName());

	RequestManager manager;

	@Resource
    private TimerService timerService;
	private Timer nextExecution;
	private TimerConfig timerConfig;
	private Consumer<RetrievedRequest> executor;

	LinkedList<RetrievedRequest> queue;

	public RequestScheduler(){
		timerConfig = new TimerConfig();
		timerConfig.setPersistent(false);
	}

	public void setRequestExecutor(Consumer<RetrievedRequest> executor){
		this.executor = executor;
	}
	private void setTimer(){
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

	private void clearTimer(){
		if( nextExecution != null ){
			// TODO handle exceptions?
			nextExecution.cancel();
			nextExecution = null;
		}
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
		// insert at right place in queue
		synchronized( queue ){
			int pos = 0;
			for( RetrievedRequest req : queue ){
				Instant other = req.getRequest().getScheduledTimestamp();
				if( ts == null ){
					// if both timestamp are null, add new request after all other null timestamps
					if( other == null ){
						// continue search
					}else{
						// found our place
						break;
					}
				}else if( other == null ){
					// leave null timestamps in front and add afterwards
					// continue search
				}else if( ts.isBefore(other) ){
					// found our place
					break;
				}// else continue
				pos ++;
			}
			queue.add(pos, request);
		}
		// timer update only needed, if the new request is scheduled before
		// the timer expires or if no previous timer is configured
		// TODO timer management
	}

	@Timeout
	private void timerCallback(Timer timer){
		// one shot timer is already expired
		this.nextExecution = null;
		startDueExecutions();
		setTimer();
	}
}
