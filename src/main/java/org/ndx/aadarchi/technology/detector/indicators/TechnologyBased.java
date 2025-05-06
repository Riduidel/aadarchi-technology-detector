package org.ndx.aadarchi.technology.detector.indicators;

import org.apache.camel.Exchange;
import org.ndx.aadarchi.technology.detector.model.Technology;

public interface TechnologyBased {
	default Technology getTechnology(Exchange exchange) {
		return exchange.getMessage().getBody(Technology.class);
	}
}
