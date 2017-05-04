package org.aktin.request.manager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;

import org.junit.Test;

public class TestTimeCalculations {

	@Test
	public void instantDifferenceToPeriod(){
		Instant a = Instant.now();
		Period p = Period.ofMonths(-3);
		// only works for days and weeks. month differences require local time
		//a.plus(p);
		//
		ZoneId tz = ZoneId.of("Europe/Berlin");
		LocalDateTime l = a.atZone(tz).toLocalDateTime().plus(p);
		Instant b = l.atZone(tz).toInstant();
		// also possible with zoned time
		b = a.atZone(tz).plus(p).toInstant();
		System.out.println(a);
		System.out.println(b);
	}
}
