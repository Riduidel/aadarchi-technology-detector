package com.zenika.tech.lab.ingester.indicators.downloads;

import com.zenika.tech.lab.ingester.model.Technology;

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
