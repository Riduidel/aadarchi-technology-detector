package org.ndx.aadarchi.technology.detector.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * An indicator refers to a technology, so the technology has to be part of the id
 * @see https://javanexus.com/blog/mastering-hibernate-composite-ids
 */
@Entity
public class Indicator extends PanacheEntityBase {
	@Embeddable
	public static class IndicatorId implements Serializable {
	    @ManyToOne
	    @JoinColumn(name = "technology",insertable = false, updatable = false)
		public Technology technology;
		@Column(name = "INDICATOR_NAME")
		public String indicatorName;
		@Column(name = "INDICATOR_DATE")
		public Date date;
		@Override
		public int hashCode() {
			return Objects.hash(date, indicatorName, technology);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			IndicatorId other = (IndicatorId) obj;
			return Objects.equals(date, other.date) && Objects.equals(indicatorName, other.indicatorName)
					&& Objects.equals(technology, other.technology);
		}
	}
	
	@EmbeddedId
	public IndicatorId id;

	/**
	 * Various indicators may have various id types
	 */
	@Column(columnDefinition = "text", name="INDICATOR_VALUE")
	public String indicatorValue;
}
