package com.zenika.tech.lab.ingester.model.export;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.avro.reflect.AvroDoc;
import org.apache.avro.reflect.AvroEncode;
import org.apache.avro.reflect.AvroIgnore;
import org.apache.avro.reflect.AvroMeta;
import org.apache.avro.reflect.AvroMeta.AvroMetas;

import com.zenika.tech.lab.ingester.model.Technology;

import org.apache.avro.reflect.DateAsLongEncoding;
import org.apache.avro.reflect.Nullable;

/**
 * A class linking a technology to all its indicators
 */
public class ComputedIndicators {
	public static class IndicatorDataPoint {
		@AvroDoc("Indicator date")
		@AvroMetas({
			@AvroMeta(key = "logicalType", value = "date")
		})
		private long date;
		@AvroDoc("Indicator value")
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
	@AvroDoc("Indicators computed for that technology. Key is the indicator name, and value is the collection of data points (dated values for that indicator)")
	private @Nullable Map<String, Collection<IndicatorDataPoint>> indicators;
	@AvroDoc("The technology for which we have computed indicators")
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
