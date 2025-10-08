package com.zenika.tech.lab.ingester.processors;

import java.util.Date;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.zenika.tech.lab.ingester.Configuration;
import com.zenika.tech.lab.ingester.model.IndicatorComputation;
import com.zenika.tech.lab.ingester.model.IndicatorComputation.IndicatorComputationStatus;
import com.zenika.tech.lab.ingester.model.IndicatorComputationRepository;
import com.zenika.tech.lab.ingester.model.Technology;

import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.Sort.Direction;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

@Dependent
public class IndicatorComputationProcessor {
	@Inject IndicatorComputationRepository indicators;

	public void generateIndicatorComputationFor(Technology technology, String r) {
		indicators.findOrCreateFromTechnology(technology, r);
	}

	/**
	 * Find all indicator computations sorted by priority.
	 * This priority first takes in account the fact that technology is in 
	 * the priorized technologies list, then sort by indicator computation date
	 * @return
	 */
	@Transactional
	public Iterable<IndicatorComputation> findAllPriorized() {
		return indicators.findAllPriorized();
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
