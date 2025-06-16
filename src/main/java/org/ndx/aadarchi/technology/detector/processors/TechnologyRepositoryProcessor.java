package org.ndx.aadarchi.technology.detector.processors;

import java.util.List;

import org.apache.camel.Exchange;
import org.ndx.aadarchi.technology.detector.export.ComputedIndicators;
import org.ndx.aadarchi.technology.detector.model.IndicatorRepository;
import org.ndx.aadarchi.technology.detector.model.Technology;
import org.ndx.aadarchi.technology.detector.model.TechnologyRepository;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@Dependent
public class TechnologyRepositoryProcessor {
	@Inject TechnologyRepository technologies;
	@Inject IndicatorRepository indicators;

	@Transactional
	public void findAllTechnologies(Exchange exchange) {
		// Maybe this list will become huge ... 
		List<Technology> allTechnologies = technologies.findAll().list();
		Log.infof("Loaded %d technologies", allTechnologies.size());
		exchange.getMessage().setBody(allTechnologies);
	}


	/** 
	 * Generate a ComputedTechnology from a Technology.
	 * @param exchange1
	 */
	@Transactional
	public void toComputedIndicators(Exchange exchange1) {
		Technology technology = exchange1.getMessage().getBody(Technology.class);
		exchange1.getMessage().setBody(toComputedIndicators(technology));
	}

	private ComputedIndicators toComputedIndicators(Technology technology) {
		ComputedIndicators returned = new ComputedIndicators();
		returned.setTechnology(technology);
		returned.setIndicators(indicators.toMap(technology));
		return returned;
	}
}
