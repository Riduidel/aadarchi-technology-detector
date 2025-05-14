package org.ndx.aadarchi.technology.detector.indicators.github.stars;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.ndx.aadarchi.technology.detector.model.Indicator;
import org.ndx.aadarchi.technology.detector.model.Technology;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class StargazerRepository implements PanacheRepository<Stargazer> {
	@ConfigProperty(name = "tech-trends.indicators.github.stars.sql.indicator")
	public String groupStarsByMonths;
	private EntityManager entityManager;

	public StargazerRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Transactional
	public boolean maybePersist(Stargazer persistent) {
		// TODO Change how date is used here, since it clearly doesn't work correctly
		if(count("id.technology = ?1 and id.date = ?2 and id.user = ?3", 
				persistent.id.technology, 
				persistent.id.date,
				persistent.id.user
				)==0) {
			persistent.persist();
			return true;
		} else {
			return false;
		}
	}

	@Transactional
	public long count(Technology t) {
		return count("id.technology", t);
	}

	/**
	 * Group stargazers by month and year.
	 * We simply return that and let the caller manipulate the database
	 * @param technology TODO
	 */
	@Transactional
	public List<Indicator> groupStarsByMonths(Technology technology) {
		Query extractionQuery = entityManager.createNativeQuery(groupStarsByMonths);
		extractionQuery.setParameter("technology_id", technology.id);
		List<Object[]> results = extractionQuery.getResultList();
		return results.stream()
			.map(row -> toIndicator(technology, row))
			.collect(Collectors.toList());
	}

	private Indicator toIndicator(Technology technology, Object[] row) {
		LocalDate localDate = LocalDate.of(Integer.parseInt(row[0].toString()), 
				Integer.parseInt(row[1].toString()), 1);
		Date d = Date.from(localDate.atStartOfDay(ZoneId.of("UTC")).toInstant());
		return new Indicator(
				technology,
				GitHubStars.GITHUB_STARS,
				d,
				row[2].toString()
				);
	}

}
