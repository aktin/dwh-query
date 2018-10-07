package org.aktin.dwh.optinout.sic;

public class CodeGeneratorFactory {

	public CodeGenerator createInstance(String definition, String state) {
		CodeGenerator gen;
		int pos = definition.indexOf('(');
		String name;
		String[] args;
		if( pos == -1 ) {
			// no arguments, just use the name
			name = definition;
			args = new String[0];
		}else {
			name = definition.substring(0, pos);
			if( definition.charAt(definition.length()-1) != ')' ) {
				throw new IllegalArgumentException("Expecting ')' at end of "+definition);
			}
			args = definition.substring(pos+1, definition.length()-1).split(",");
		}
		switch( name ) {
		case SequenceGenerator.NAME:
			gen = new SequenceGenerator(args);
			break;
		default:
			throw new IllegalArgumentException("Code generator definition not supported: "+definition);
		}
		gen.initializeState(state);
		return gen;
	}
}
