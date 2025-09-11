package com.zenika.tech.lab.ingester.model;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.zenika.tech.lab.ingester.model.export.ComputedIndicators.IndicatorDataPoint;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class IndicatorRepository  implements PanacheRepository<Indicator> {
	private final EntityManager entityManager;

	public IndicatorRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

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

	@Transactional
	public boolean maybePersist(Indicator persistent) {
		if(count("id.technology = ?1 and id.indicatorName = ?2 and id.date = ?3", 
				persistent.id.technology, 
				persistent.id.indicatorName, 
				persistent.id.date)==0) {
			persistent.persist();
			return true;
		} else {
			return false;
		}
	}

	public Map<String, Collection<IndicatorDataPoint>> toMap(Technology technology) {
		Query extractIndicatorNames = entityManager.createQuery("""
				select distinct i.id.indicatorName
				from Indicator i
				where i.id.technology = :technology
				""", String.class);
		extractIndicatorNames.setParameter("technology", technology);
		List<String> indicators = extractIndicatorNames.getResultList();
		Map<String, Collection<IndicatorDataPoint>> returned = new TreeMap<String, Collection<IndicatorDataPoint>>();
		for(String indicator : indicators) {
			List<IndicatorDataPoint> results = findAllIndicatorDataPointsFor(technology, indicator);
			returned.put(indicator, results);
		}
		return returned;
	}

	public List<IndicatorDataPoint> findAllIndicatorDataPointsFor(Technology technology, String indicator) {
		Query extractIndicatorValues = entityManager.createQuery("""
				select i.id.date as date, i.indicatorValue as value
				from Indicator i
				where i.id.technology = :technology and i.id.indicatorName = :indicatorName
				order by i.id.date asc
				""", IndicatorDataPoint.class);
		extractIndicatorValues.setParameter("technology", technology);
		extractIndicatorValues.setParameter("indicatorName", indicator);
		List<IndicatorDataPoint> results = extractIndicatorValues.getResultList();
		return results;
	}

	@Transactional
	public Collection<Indicator> findAll(Technology t, String indicatorName) {
		return find("id.technology = ?1 and id.indicatorName = ?2", t, indicatorName).list();
	}

}
