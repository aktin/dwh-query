package org.aktin.report.schedule;

import java.io.IOException;

public abstract class AbstractTarget {

	public abstract void submit(Result result)throws IOException;
}
