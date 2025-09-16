package com.zenika.tech.lab.ingester.indicators.github.graphql.entities;

/*
* DTO for the response of the query for the total number of discussions
* */
public class RepositoryWithDiscussionCount {

    public Discussions discussions;

    public static class Discussions {
        public int totalCount;

    }
}
