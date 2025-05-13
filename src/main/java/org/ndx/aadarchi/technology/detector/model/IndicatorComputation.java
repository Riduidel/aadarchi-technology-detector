package org.ndx.aadarchi.technology.detector.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class IndicatorComputation {
	@Embeddable
	public static class IndicatorComputationId implements Serializable {
	    @ManyToOne
	    @JoinColumn(name = "technology_id",insertable = false, updatable = false)
		public Technology technology;
		@Column(name = "INDICATOR_ROUTE")
		public String indicatorRoute;
		@Override
		public int hashCode() {
			return Objects.hash(indicatorRoute, technology);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			IndicatorComputationId other = (IndicatorComputationId) obj;
			return Objects.equals(indicatorRoute, other.indicatorRoute)
					&& Objects.equals(technology, other.technology);
		}
	}
	
	@EmbeddedId
	public IndicatorComputationId id;

	@Column(name = "INDICATOR_DATE")
	public Date date;
	
	public IndicatorComputation() {}
	
	public IndicatorComputation(Technology technology, String indicatorRoute) {
		this();
		id = new IndicatorComputationId();
		id.technology = technology;
		id.indicatorRoute = indicatorRoute;
		// All indicator computations are initialized at EPOCH, to force their recomputation
		date = new Date(0);
	}

}
