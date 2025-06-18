package org.ndx.aadarchi.technology.detector.export.bigquery.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface BigQueryTable {

	String table() default "";
	String namespace() default "";
}
