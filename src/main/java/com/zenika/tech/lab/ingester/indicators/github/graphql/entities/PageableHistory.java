package com.zenika.tech.lab.ingester.indicators.github.graphql.entities;

public interface PageableHistory {

	boolean hasPreviousPage();

	String startCursor();

	boolean hasNoData();
}
