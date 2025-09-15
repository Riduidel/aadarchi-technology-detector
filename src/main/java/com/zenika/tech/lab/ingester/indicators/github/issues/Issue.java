package com.zenika.tech.lab.ingester.indicators.github.issues;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

@Entity(name = "GITHUB_ISSUES")
@NamedNativeQueries({
	@NamedNativeQuery(
			name="GITHUB_ISSUES.CSV.EXPORT",
			query="""
                    select 
                    to_char(issue_date, 'YYYY-MM-DD HH:MM:SS') as issue_date,
                    repo_owner,
                    repo_name,
                    issue_user
                    from github_issues;
                    """)
})
public class Issue extends PanacheEntityBase {
	@Embeddable
	public static class IssuesId implements Serializable {
	    @Column(name = "REPO_OWNER") public String owner;
	    @Column(name = "REPO_NAME") public String repo;
	    @Column(name = "ISSUE_DATE") public Date date;
	    @Column(name = "ISSUE_USER") public String user;

	}

	@EmbeddedId
	public IssuesId id;

	public Issue() {
		super();
	}

	public Issue(String owner, String repo, Date date, String user) {
		this.id = new IssuesId();
		this.id.owner = owner;
		this.id.repo = repo;
		this.id.date = date;
		this.id.user = user;
	}
}
