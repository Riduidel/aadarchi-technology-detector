package org.ndx.aadarchi.technology.detector.indicators;

import org.ndx.aadarchi.technology.detector.model.Technology;

public interface IndicatorComputer {
	String getFromRouteName();

	/**
	 * Simply says if computer can compute technology. We may need more details later ...
	 * @param technology
	 * @return true when indicator can compute value for technology.
	 */
	boolean canCompute(Technology technology);
}
