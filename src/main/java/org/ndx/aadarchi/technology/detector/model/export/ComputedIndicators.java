package org.ndx.aadarchi.technology.detector.model.export;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.avro.reflect.AvroEncode;
import org.apache.avro.reflect.DateAsLongEncoding;
import org.apache.avro.reflect.Nullable;
import org.ndx.aadarchi.technology.detector.model.Technology;

/**
 * A class linking a technology to all its indicators
 */
public class ComputedIndicators {
	public static class IndicatorDataPoint {
		private long date;
		private String value;
		public IndicatorDataPoint() {}
		public IndicatorDataPoint(Timestamp t, String v) {
			this.date = t.getTime();
			this.value = v;
		}
		public Date getDate() {
			return new Date(date);
		}
		public String getValue() {
			return value;
		}
		public void setDate(Date date) {
			this.date = date.getTime();
		}
		public void setValue(String value) {
			this.value = value;
		}
	}
	private @Nullable Map<String, Collection<IndicatorDataPoint>> indicators;
	private Technology technology;
	public ComputedIndicators() {}
	public Map<String, Collection<IndicatorDataPoint>> getIndicators() {
		return indicators;
	}
	public Technology getTechnology() {
		return technology;
	}
	public void setIndicators(Map<String, Collection<IndicatorDataPoint>> indicators) {
		this.indicators = indicators;
	}
	public void setTechnology(Technology technology) {
		this.technology = technology;
	}
}
