package org.ndx.aadarchi.technology.detector.model;

import java.util.Date;

public class IndicatorRepositoryFacade {

	private IndicatorRepository repository;
	private String indicatorName;

	public IndicatorRepositoryFacade(IndicatorRepository repository, String indicatorName) {
		this.repository = repository;
		this.indicatorName = indicatorName;
	}

	public boolean hasIndicatorForMonth(Technology technology, Date startOfMonth) {
		return repository.hasIndicatorForMonth(technology, indicatorName, startOfMonth);
	}

	public void saveIndicator(Technology technology, String value) {
		repository.saveIndicator(technology, indicatorName, value);
	}

	public boolean maybePersist(Indicator indicator) {
		return repository.maybePersist(indicator);
	}

}
