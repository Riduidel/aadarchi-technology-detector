package com.zenika.tech.lab.ingester.indicators;

import org.apache.camel.Exchange;

import com.zenika.tech.lab.ingester.model.Technology;

public interface TechnologyBased {
	default Technology getTechnology(Exchange exchange) {
		return exchange.getMessage().getBody(Technology.class);
	}
}
