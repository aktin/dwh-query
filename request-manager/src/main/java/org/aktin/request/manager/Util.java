package org.aktin.request.manager;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;


public class Util {

	public static final String stringStackTrace(Throwable t){
		StringWriter w = new StringWriter();
		PrintWriter p = new PrintWriter(w);
		t.printStackTrace(p);
		p.close();
		try {
			w.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return w.toString();
	}
}
