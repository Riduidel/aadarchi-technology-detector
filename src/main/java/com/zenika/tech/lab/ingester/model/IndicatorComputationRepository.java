package com.zenika.tech.lab.ingester.model;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.zenika.tech.lab.ingester.Configuration;
import com.zenika.tech.lab.ingester.model.export.ComputedIndicators.IndicatorDataPoint;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class IndicatorComputationRepository implements PanacheRepository<IndicatorComputation>{
	@ConfigProperty(name = Configuration.CONFIGURATION_PREFIX + "indicators.computations.findAllPriorized.sql")
	public String findAllPriorized;
	@Inject EntityManager entityManager;
	private List<Long> priorizedIds;
	@Transactional
	public IndicatorComputation findOrCreateFromTechnology(Technology body, String indicatorRoute) {
		IndicatorComputation returned = null;
		// First find the reference url
		if(returned==null && body.id!=null) {
			returned = find("id.technology=?1 and id.indicatorRoute=?2", body, indicatorRoute).firstResult();
			if(returned!=null) {
				return returned;
			}
		}
		// If not found, create it and persist it immediatly
		returned = new IndicatorComputation(body, indicatorRoute);
		persist(returned);
		return returned;
	}
	
	@Inject public void buildPriorizedIds(List<Technology> priorized) {
		priorizedIds = priorized.stream()
				.map(t -> t.id)
				.collect(Collectors.toList());
	}

	public Iterable<IndicatorComputation> findAllPriorized() {
		Query query = entityManager.createNativeQuery(findAllPriorized, IndicatorComputation.class);
		query.setParameter("priorized", getPriorizedIds());
		return query.getResultList();
	}

	List<Long> getPriorizedIds() {
		return priorizedIds;
	}
}
