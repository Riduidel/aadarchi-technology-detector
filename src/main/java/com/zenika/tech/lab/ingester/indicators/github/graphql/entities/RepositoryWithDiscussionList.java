package com.zenika.tech.lab.ingester.indicators.github.graphql.entities;

import java.time.OffsetDateTime;
import java.util.List;

/*
* DTO for the discussion history query response
* */
public class RepositoryWithDiscussionList {
    public Discussions discussions;

    public static class Discussions {
        public int totalCount;
        public List<DiscussionNode> nodes;
        public PageInfo pageInfo;
    }

    public static class DiscussionNode {
        public OffsetDateTime createdAt;
        public DiscussionAuthor author;
    }

    public static class DiscussionAuthor {
        public String login;
    }

    public static class PageInfo {
        public boolean hasPreviousPage;
        public String startCursor;
    }
}