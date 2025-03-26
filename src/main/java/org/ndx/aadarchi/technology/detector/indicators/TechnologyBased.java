package org.ndx.aadarchi.technology.detector.indicators;

import org.apache.camel.Exchange;
import org.ndx.aadarchi.technology.detector.model.Technology;

public interface TechnologyBased {
	public default Technology getTechnology(Exchange exchange) {
		return exchange.getMessage().getBody(Technology.class);
	}


}
