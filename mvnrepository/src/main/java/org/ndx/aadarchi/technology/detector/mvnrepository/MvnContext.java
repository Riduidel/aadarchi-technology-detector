package org.ndx.aadarchi.technology.detector.mvnrepository;

import org.ndx.aadarchi.technology.detector.helper.ExtractionContext;

import com.microsoft.playwright.BrowserContext;

public class MvnContext implements ExtractionContext {
	public final BrowserContext context;

	public MvnContext(BrowserContext context) {
		super();
		this.context = context;
	}
}