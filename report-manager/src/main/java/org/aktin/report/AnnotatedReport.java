package org.aktin.report;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.net.URL;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.stream.StreamSource;


public abstract class AnnotatedReport implements Report{

	private ReportOption<?>[] reportOptions;
	private Period defaultPeriod;
	private String id;
	
	public AnnotatedReport() throws IllegalArgumentException{
		reportOptions = scanAnnotatedOptions();
		AnnotatedReport.Report an = getClass().getAnnotation(AnnotatedReport.Report.class);
		if( an == null ){
			throw new IllegalArgumentException("Must specify the AbstractReport.Report annotation");
		}
		if( an.defaultPeriod().equals("") ){
			defaultPeriod = null;
		}else{
			defaultPeriod = Period.parse(an.defaultPeriod());
		}
		if( an.id().length() == 0 ){
			this.id = getClass().getCanonicalName();
		}else{
			this.id = an.id();
		}
	}

	protected StreamSource createStreamSource(URL url){
		StreamSource source = new StreamSource();
		source.setSystemId(url.toExternalForm());
		try {
			source.setInputStream(url.openStream());
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to read export descriptor", e);
		}
		return source;
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Report{
		/**
		 * Report id. If not specified, the {@link Class#getCanonicalName()} is used.
		 * @return
		 */
		String id() default "";
		String displayName();
		String description();
		String defaultPeriod() default "";
	}
	
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	protected @interface Option {
		/**
		 * option's display name. If not specified, the field name is used.
		 * @return display name
		 */
		String displayName() default "";
		String description() default "";
		/**
		 * default value. If not specified, the following defaults are used:
		 * String type will be the empty string {@code ""}. All other types
		 * will have {@code null} as default.
		 * @return default value
		 */
		String defaultValue() default "";
	}

	@SuppressWarnings("rawtypes")
	private static class AnnotatedOption implements ReportOption{
		private Option option;
		private Field field;
		public AnnotatedOption(Option opt, Field field){
			this.option = opt;
			this.field = field;
		}

		@Override
		public String getDisplayName() {
			String name = option.displayName();
			if( name.length() == 0 ){
				// use field name
				name = field.getName();
			}
			return name;
		}

		@Override
		public String getDescription() {
			return option.description();
		}

		@Override
		public Class<?> getType() {
			return field.getType();
		}

		@Override
		public Object getDefaultValue() {
			Class<?> c = getType();
			if( c == String.class ){
				return option.defaultValue();
			}else if( option.defaultValue().length() == 0 ){
				return null;
			}else if( c == Boolean.class ){
				return new Boolean(option.defaultValue());
			}else{
				throw new UnsupportedOperationException("Option of type "+c.getName()+" not implemented");
			}
		}		
	}
	private ReportOption<?>[] scanAnnotatedOptions(){
		List<ReportOption<?>> options = new ArrayList<>();
		Field[] fields = getClass().getDeclaredFields();
		for( int i=0; i<fields.length; i++ ){
			Option opt = fields[i].getAnnotation(Option.class);
			options.add(new AnnotatedOption(opt, fields[i]));
			// allow access to private fields
			fields[i].setAccessible(true);
		}
		return options.toArray(new ReportOption<?>[options.size()]);
	}

	@Override
	public final String getId(){
		return this.id;
	}
	@Override
	public final String getName() {
		return getClass().getAnnotation(AnnotatedReport.Report.class).displayName();
	}
	@Override
	public final String getDescription() {
		return getClass().getAnnotation(AnnotatedReport.Report.class).description();
	}
	@Override
	public final Period getDefaultPeriod() {
		return defaultPeriod;
	}
	@Override
	public final ReportOption<?>[] getConfigurationOptions() {
		return reportOptions;
	}
	@Override
	public final <U> void setOption(ReportOption<U> option, U value) throws IllegalArgumentException {
		if( option.getClass() != AnnotatedOption.class ){
			throw new IllegalArgumentException("Wrong option type");
		}
		try {
			((AnnotatedOption)option).field.set(this, value);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Unable to access annotated field",e);
		}
	}
	@SuppressWarnings("unchecked")
	@Override
	public final <U> U getOption(ReportOption<U> option) throws IllegalArgumentException {
		if( option.getClass() != AnnotatedOption.class ){
			throw new IllegalArgumentException("Wrong option type");
		}
		try {
			return (U)((AnnotatedOption)option).field.get(this);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Unable to access annotated field",e);
		}
	}
}
