package org.aktin.report.manager;

import org.apache.fop.events.Event;
import org.apache.fop.events.EventListener;
import org.apache.fop.events.model.EventSeverity;

/**
 * Collect FOP event messages
 * 
 * @author R.W.Majeed
 *
 */
class FOEventListener implements EventListener{
	StringBuilder sb;

	public FOEventListener() {
		sb = new StringBuilder();
	}
	@Override
	public void processEvent(Event event) {
		if( event.getSeverity() == EventSeverity.INFO 
				||  event.getSeverity() == EventSeverity.WARN ){
			// ignore info messages and warnings
			return;
		}
		sb.append(event);
		sb.append('\n');
//		if( event.getSeverity() == EventSeverity.ERROR ){
//		}else if( event.getSeverity() == EventSeverity.FATAL ){
//			
//		}else if( event.getSeverity() == EventSeverity.WARN ){
//			
//		}
	}

	public boolean isEmpty(){
		return sb.length() == 0;
	}
	public String getSummary(){
		return sb.toString();
	}

}
