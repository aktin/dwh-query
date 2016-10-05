package org.aktin.report.aktin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

/**
 * Tests copying of resources
 * @author R.W.Majeed
 *
 */
public class TestCopyResources {
	/**
	 * Verifies that all resources are available, readable
	 * and can be copied to the target path. Also verifies
	 * that no additional files are copied.
	 * 
	 * @throws IOException if anything fails
	 */
	@Test
	public void verifyResourcesCopied_R() throws IOException{
		AktinMonthly m = new AktinMonthly();
		Path temp = Files.createTempDirectory("test-res-copy");
		// should not throw any exception
		String[] names = m.copyResourcesForR(temp);

		// remove copied files
		for( String name : names ){
			Files.delete(temp.resolve(name));
		}
		// remove temp directory
		// will also verify that no other resources were copied
		// since directory needs to be empty for delete
		Files.delete(temp);
	}
}
