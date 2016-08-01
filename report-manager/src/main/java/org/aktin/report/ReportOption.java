package org.aktin.report;

import java.util.Objects;

public interface ReportOption<T>{
	String getDisplayName();
	String getDescription();
	
	/**
	 * Type of the option. 
	 * Permitted types are {@link String}, {@link Integer}, {@link Boolean} and {@link Enum}
	 * @return option type
	 */
	Class<T> getType();
	
	/**
	 * Default value for the option. {@code null} is permitted.
	 * @return
	 */
	public T getDefaultValue();
	
	/**
	 * Default equals implementation. Two options are equal if and only if
	 * display name, description, type and default value are equal.
	 * @param other other option
	 * @return true if this option is equal to the other option, false otherwise
	 */
	public default <U> boolean equals(ReportOption<U> other){
		return getDisplayName().equals(other.getDisplayName())
				&& getDescription().equals(other.getDescription())
				&& getType().equals(other.getType())
				&& Objects.equals(this.getDefaultValue(), other.getDefaultValue());
	}
	/**
	 * Create a string option
	 * @param name name
	 * @param description description
	 * @param defaultValue default value
	 * @return option
	 */
	public static ReportOption<String> newStringOption(String name, String description, String defaultValue){
		return new ReportOption<String>() {
			@Override
			public String getDisplayName() {return name;}

			@Override
			public String getDescription() {return description;}

			@Override
			public Class<String> getType() {return String.class;}

			@Override
			public String getDefaultValue() {return defaultValue;}
		};
	}
	public static ReportOption<Boolean> newBooleanOption(String name, String description, Boolean defaultValue){
		return new ReportOption<Boolean>() {
			@Override
			public String getDisplayName() {return name;}

			@Override
			public String getDescription() {return description;}

			@Override
			public Class<Boolean> getType() {return Boolean.class;}

			@Override
			public Boolean getDefaultValue() {return defaultValue;}
		};
	}
}
