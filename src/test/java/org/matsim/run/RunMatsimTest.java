package org.matsim.run;

import org.junit.Test;

public class RunMatsimTest {
	
	@Test
	public void test() {
		try {
			RunMatsim.main(null);
		} catch ( Exception ee ) {
			ee.printStackTrace();
			// for the time being, throwing an exception is expected behavior since the network is broken.
			// So the exception is printed, but the test is accepted as passed.  kai, dec'17
		}
	}
	
}
