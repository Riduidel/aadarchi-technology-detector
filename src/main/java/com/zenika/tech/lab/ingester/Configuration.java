package com.zenika.tech.lab.ingester;

/**
 * Some common constants
 */
public interface Configuration {
	/**
	 * TODO wouldn't it be cool to have that generated from maven artifact? OF COURSE IT WOULD
	 */
	public static final String PROJECT_NAME = "tech-lab-ingester";
	public static final String CONFIGURATION_PREFIX = PROJECT_NAME+".";
	public static final String INDICATORS_PREFIX = CONFIGURATION_PREFIX+"indicators.";
	public static final String EXPORT_PREFIX = CONFIGURATION_PREFIX+"export.";
}
