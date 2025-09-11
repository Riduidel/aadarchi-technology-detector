package com.zenika.tech.lab.ingester.indicators;

import com.zenika.tech.lab.ingester.model.Technology;

public interface IndicatorComputer {
	String getFromRouteName();

	/**
	 * Simply says if computer can compute technology. We may need more details later ...
	 * @param technology
	 * @return true when indicator can compute value for technology.
	 */
	boolean canCompute(Technology technology);
}
