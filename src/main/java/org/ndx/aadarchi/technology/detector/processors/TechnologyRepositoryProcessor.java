package org.ndx.aadarchi.technology.detector.processors;

import java.util.List;

import org.apache.camel.Exchange;
import org.ndx.aadarchi.technology.detector.model.Technology;
import org.ndx.aadarchi.technology.detector.model.TechnologyRepository;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@Dependent
public class TechnologyRepositoryProcessor {
	@Inject TechnologyRepository technologies;

	@Transactional
	public void findAllTechnologies(Exchange exchange) {
		// Maybe this list will become huge ... 
		List<Technology> allTechnologies = technologies.findAll().list();
		Log.infof("Loaded %d technologies", allTechnologies.size());
		exchange.getMessage().setBody(allTechnologies);
	}

}
