package com.zenika.tech.lab.ingester.indicators.github;

import com.zenika.tech.lab.ingester.model.Indicator;
import com.zenika.tech.lab.ingester.model.Technology;
import org.apache.camel.util.Pair;

import java.util.List;

public interface GithubIndicatorRepository<T extends HasGithubIndicatorId> {

    boolean maybePersist(T persistent);

    long count(Pair<String> path);

    List<Indicator> groupIndicatorsByMonths(Technology technology, Pair<String> pair);
}
