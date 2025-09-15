package com.zenika.tech.lab.ingester.indicators.github.graphql.entities;

/*
* DTO for the response of the query for the total number of issues
* */
public class RepositoryWithIssueCount {

    public Issues issues;

    public static class Issues {
        public int totalCount;
    }
}
