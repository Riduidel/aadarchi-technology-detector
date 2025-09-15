package com.zenika.tech.lab.ingester.indicators.downloads.npm;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.zenika.tech.lab.ingester.indicators.downloads.DownloadCountIndicatorComputer;
import com.zenika.tech.lab.ingester.indicators.downloads.DownloadCounterForPackageManager;
import com.zenika.tech.lab.ingester.model.Indicator;
import com.zenika.tech.lab.ingester.model.IndicatorNamed;
import com.zenika.tech.lab.ingester.model.IndicatorRepositoryFacade;
import com.zenika.tech.lab.ingester.model.Technology;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class NpmDownloadCounter implements DownloadCounterForPackageManager {
	/**
	 * @see https://github.com/npm/registry/blob/main/docs/download-counts.md#limits
	 */
	public static final LocalDate NPMJS_CREATION_DATE = LocalDate.of(2015, 1, 10);
	@Inject @IndicatorNamed(DownloadCountIndicatorComputer.DOWNLOAD_COUNT)  IndicatorRepositoryFacade indicators;

	@RestClient NpmjsRestClient npm;

	@Override
	public String getPackageManagerUrl() {
		return "https://www.npmjs.com";
	}

	@Override
	public void countDownloadsOf(Technology technology) {
		// Now we download counts per months backwards
		LocalDate now = LocalDate.now(ZoneOffset.UTC);
		LocalDate month = now.with(TemporalAdjusters.firstDayOfMonth());
		month = month.minusMonths(1);
		// We check value of download count because it reaches zero when library is not present
		long downloadCount = Long.MAX_VALUE;
		while(month.isAfter(NPMJS_CREATION_DATE) && !indicators.hasIndicatorForMonth(technology, toUTCDate(month)) && downloadCount>0 ) {
			DownloadCount count = npm.getDownloadCountBetween(month, month.plusMonths(1).minusDays(1), technology);
			downloadCount = count.downloads;
			Indicator indicator = new Indicator(technology, 
					DownloadCountIndicatorComputer.DOWNLOAD_COUNT, 
					toUTCDate(month),
					Long.toString(downloadCount));
			indicators.maybePersist(indicator);
			month = month.minusMonths(1);
		}
	}

	public Date toUTCDate(LocalDate localDate) {
		return Date.from(localDate.atStartOfDay().toInstant(ZoneOffset.UTC));
	}
}
