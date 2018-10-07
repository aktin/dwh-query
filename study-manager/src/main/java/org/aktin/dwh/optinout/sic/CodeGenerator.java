package org.aktin.dwh.optinout.sic;

import java.io.IOException;

public interface CodeGenerator {
	String generateCode() throws IOException;

	void initializeState(String state) throws IllegalStateException;

	String getState();
}
