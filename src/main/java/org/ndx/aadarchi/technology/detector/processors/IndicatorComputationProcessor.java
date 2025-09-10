package org.ndx.aadarchi.technology.detector.processors;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.ndx.aadarchi.technology.detector.model.IndicatorComputation;
import org.ndx.aadarchi.technology.detector.model.IndicatorComputation.IndicatorComputationStatus;
import org.ndx.aadarchi.technology.detector.model.IndicatorComputationRepository;
import org.ndx.aadarchi.technology.detector.model.Technology;
import org.ndx.aadarchi.technology.detector.model.export.ComputedIndicators;

import io.quarkus.logging.Log;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.Sort.Direction;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

@Dependent
public class IndicatorComputationProcessor {
	@Inject IndicatorComputationRepository indicators;

	@ConfigProperty(name = "tech-trends.indicators.computations.interesting.technologies", defaultValue = "")
	private List<String> interestingTechnologies;
	@ConfigProperty(name = "tech-trends.indicators.computations.interesting.only", defaultValue = "false")
	private boolean onlyProcessInterestingTechnologies;
	
	public void generateIndicatorComputationFor(Technology technology, String r) {
		indicators.findOrCreateFromTechnology(technology, r);
	}

	@Transactional
	public Iterable<IndicatorComputation> findAllOldestFirst() {
		Sort sort = Sort.by("status", Direction.Descending)
				.and("date", Direction.Ascending)
				.and("id", Direction.Ascending);
		List<IndicatorComputation> returned = new LinkedList<IndicatorComputation>();
		returned.addAll(indicators
				.list("id.technology.id in (:interesting)",
						sort,
						Parameters.with("interesting", interestingTechnologies)));
		if(onlyProcessInterestingTechnologies) {
			Log.warnf("The tech-trends.indicators.computations.interesting.only flag has been set to true, so we will only process %d indicators computations, and not the whole %d", returned.size(), indicators.count());
		} else {
			returned.addAll(indicators
					.list("id.technology.id not in (:interesting)",
							sort,
							Parameters.with("interesting", interestingTechnologies)));
		}
		return returned;
	}

	@Transactional
	public void markIndicator(IndicatorComputation indicator, IndicatorComputationStatus status, boolean updateDate) {
		IndicatorComputation updated = indicators.find("id", indicator.id).firstResult();
		updated.status = status;
		if(updateDate) {
			updated.date = new Date();
		}
	}
}
