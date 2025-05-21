package org.ndx.aadarchi.technology.detector.model.export;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.ndx.aadarchi.technology.detector.model.Technology;

/**
 * A class linking a technology to all its indicators
 */
public class ComputedIndicators {
	public static class IndicatorDataPoint {
		public IndicatorDataPoint() {}
		public IndicatorDataPoint(Timestamp t, String v) {
			this.date = t;
			this.value = v;
		}
		public Date date;
		public String value;
	}
	public Technology technology;
	public Map<String, Collection<IndicatorDataPoint>> indicators;
	public Technology getTechnology() {
		return technology;
	}
}
