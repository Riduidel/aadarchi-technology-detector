package com.zenika.tech.lab.ingester.processors;

import java.util.Date;
import java.util.List;

import org.apache.camel.Exchange;

import com.zenika.tech.lab.ingester.model.IndicatorComputation;
import com.zenika.tech.lab.ingester.model.IndicatorComputationRepository;
import com.zenika.tech.lab.ingester.model.Technology;
import com.zenika.tech.lab.ingester.model.IndicatorComputation.IndicatorComputationStatus;
import com.zenika.tech.lab.ingester.model.export.ComputedIndicators;

import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.Sort.Direction;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@Dependent
public class IndicatorComputationProcessor {
	@Inject IndicatorComputationRepository indicators;

	
	public void generateIndicatorComputationFor(Technology technology, String r) {
		indicators.findOrCreateFromTechnology(technology, r);
	}

	@Transactional
	public Iterable<IndicatorComputation> findAllOldestFirst() {
		List<IndicatorComputation> returned = indicators.findAll(
				// This ensure we first finish currently computing indicators prior to computing new ones
				Sort.by("status", Direction.Descending)
				.and("date", Direction.Ascending)
				.and("id", Direction.Ascending)).list();
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
