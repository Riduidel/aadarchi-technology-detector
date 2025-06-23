package org.ndx.aadarchi.technology.detector.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.Table;

/**
 * An indicator refers to a technology, so the technology has to be part of the id
 * @see https://javanexus.com/blog/mastering-hibernate-composite-ids
 */
@Entity
@Table(name="INDICATOR")
@NamedNativeQueries({
	@NamedNativeQuery(name="INDICATOR.CSV.EXPORT", query="""
select 
to_char(indicator_date, 'YYYY-MM-DD') as indicator_date,
indicator_name ,
indicator_value,
technology_id
from indicator;			
			""")
})
public class Indicator extends PanacheEntityBase {
	@Embeddable
	public static class IndicatorId implements Serializable {
	    @ManyToOne
	    @JoinColumn(name = "TECHNOLOGY_ID",insertable = false, updatable = false, foreignKey = @ForeignKey(name="fk_technology_id"))
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
	
	public Indicator()  {
		
	}
	
	public Indicator(Technology t, String indicator, Date d, String value) {
		this.id = new IndicatorId();
		this.id.technology = t;
		this.id.indicatorName = indicator;
		this.id.date = d;
		this.indicatorValue = value;
	}
}
