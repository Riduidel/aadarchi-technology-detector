package org.ndx.aadarchi.technology.detector.processors;

import org.ndx.aadarchi.technology.detector.model.IndicatorComputationRepository;
import org.ndx.aadarchi.technology.detector.model.Technology;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class IndicatorComputationProcessor {
	@Inject IndicatorComputationRepository indicators;

	public void generateIndicatorComputationFor(Technology technology, String r) {
		indicators.findOrCreateFromTechnology(technology, r);
	}

}
