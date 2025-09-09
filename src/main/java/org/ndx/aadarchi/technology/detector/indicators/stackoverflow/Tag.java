package org.ndx.aadarchi.technology.detector.indicators.stackoverflow;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="STACKOVERFLOW_TAG")
public class Tag {
	@Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TECHNOLOGY_ID_SEQ")
	public Long id;
	
	public String name;
}
