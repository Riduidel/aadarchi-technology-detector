package com.zenika.tech.lab.ingester.indicators.github;

import com.zenika.tech.lab.ingester.indicators.github.discussions.Discussion;
import com.zenika.tech.lab.ingester.model.Indicator;
import com.zenika.tech.lab.ingester.model.Technology;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import org.apache.camel.util.Pair;

import java.util.List;

public interface GithubIndicatorRepository<T> extends PanacheRepository<Discussion> {

	boolean maybePersist(T persistent);

	long count(Pair<String> path);

	List<Indicator> groupIndicatorsByMonths(Technology technology, Pair<String> pair);
}
