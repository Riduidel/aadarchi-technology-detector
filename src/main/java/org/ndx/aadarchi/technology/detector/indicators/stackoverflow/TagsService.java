package org.ndx.aadarchi.technology.detector.indicators.stackoverflow;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.ndx.aadarchi.technology.detector.indicators.stackoverflow.api.StackExchangeClient;
import org.ndx.aadarchi.technology.detector.indicators.stackoverflow.api.StackExchangeList;
import org.ndx.aadarchi.technology.detector.indicators.stackoverflow.api.Tag;
import org.ndx.aadarchi.technology.detector.model.Technology;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TagsService {
	@ConfigProperty(name = "tech-trends.stackexchange.tags.per.page", defaultValue = "100")
	private int tagsPerPage;
	@RestClient StackExchangeClient stackExchange;
	@Inject TagsRepository tagsRepository;

	public boolean hasTagFor(Technology technology) {
		StackExchangeList<Tag> receivedTags = stackExchange.getTags("stackoverflow", 0, tagsPerPage, null, null);
		throw new UnsupportedOperationException("TODO implement TagsService#hasTagFor");
	}
}
