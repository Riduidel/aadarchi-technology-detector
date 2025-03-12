package org.ndx.aadarchi.technology.detector.model;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Map;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class IndicatorRepository  implements PanacheRepository<Indicator> {

	@Transactional
	public void saveIndicator(Technology technology, String indicatorIdentifier, String value) {
		Date firstDateOfMonth = atStartOfMonth();
		Indicator i = new Indicator();
		i.id = new Indicator.IndicatorId();
		i.id.technology = technology;
		i.id.indicatorName = indicatorIdentifier;
		i.id.date = firstDateOfMonth;
		i.indicatorValue = value;
		persist(i);
	}

	public static Date atStartOfMonth() {
		LocalDate now = LocalDate.now();
		LocalDate firstDayOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
		Date firstDateOfMonth = Date.from( firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());
		return firstDateOfMonth;
	}

	@Transactional
	public boolean hasIndicatorForMonth(Technology technology, String indicatorIdentifier, Date month) {
		return count("id.technology = ?1 and id.indicatorName = ?2 and id.date = ?3",
				technology,
				indicatorIdentifier,
				month)>0;
	}

}
