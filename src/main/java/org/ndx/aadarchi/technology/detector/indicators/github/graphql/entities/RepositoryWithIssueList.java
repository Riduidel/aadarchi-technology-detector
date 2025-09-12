package org.ndx.aadarchi.technology.detector.indicators.github.graphql.entities;

import java.time.OffsetDateTime;
import java.util.List;

/*
* DTO for the issue history query response
* */
public class RepositoryWithIssueList {
    public Issues issues;

    public static class Issues {
        public List<IssueNode> nodes;
        public PageInfo pageInfo;
    }

    public static class IssueNode {
        public OffsetDateTime createdAt;
        public IssueAuthor owner;
    }

    public static class IssueAuthor {
        public String login;
    }

    public static class PageInfo {
        public boolean hasPreviousPage;
        public String startCursor;
    }
}