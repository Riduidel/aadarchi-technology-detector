package org.ndx.aadarchi.technology.detector.indicators.downloads;

import org.ndx.aadarchi.technology.detector.model.Technology;

public interface DownloadCounterForPackageManager {
	/**
	 * @return the package manager for which this counter is able to count downloads
	 */
	public String getPackageManagerUrl();

	/**
	 * Count downloads for technologies per month and store them in the indicator table
	 * @param body
	 */
	public void countDownloadsOf(Technology body);
}
