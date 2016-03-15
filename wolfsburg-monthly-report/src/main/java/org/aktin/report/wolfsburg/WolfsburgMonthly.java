package org.aktin.report.wolfsburg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import javax.inject.Singleton;

import org.aktin.report.Report;

@Singleton
public class WolfsburgMonthly implements Report{

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String[] getEncounterConcepts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getRepeatingConcepts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] copyResourcesForR(Path workingDirectory) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] copyResourcesForFOP(Path workingDirectory) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream readStaticWebResource(String path) {
		// no static content available
		return null;
	}

}
