package org.aktin.dwh.optinout.sic;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;

public class SequenceGenerator implements CodeGenerator{
	public static final String NAME="SEQUENCE";

	private BigInteger start;
	private BigInteger increment;
	private BigInteger state;

	public SequenceGenerator(String... args) {
		if( args.length != 2 ) {
			throw new IllegalArgumentException("Expecting exactly two arguments, but got "+args.length);
		}
		start = new BigInteger(args[0]);
		increment = new BigInteger(args[1]);
	}
	@Override
	public String generateCode() throws IOException {
		Objects.requireNonNull(state, "State not initialized");
		// use current state for code
		String code = this.state.toString();
		BigInteger next = state.add(increment);
		this.state = next;
		return code;
	}

	@Override
	public void initializeState(String state) throws IllegalStateException {
		if( state == null ) {
			// no previous state, use starting value
			this.state = start;
		}else {
			// use previous state
			this.state = new BigInteger(state);
		}
	}

	@Override
	public String getState() {
		return state.toString();
	}

	
}
