package com.zenika.tech.lab.ingester.indicators.github.discussions;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedNativeQuery;

import java.io.Serializable;
import java.util.Date;

@Entity(name = "GITHUB_DISCUSSIONS")
@NamedNativeQuery(
		name = "GITHUB_DISCUSSIONS.CSV.EXPORT",
		query = """
				select 
				to_char(discussion_date, 'YYYY-MM-DD HH:MM:SS') as discussion_date,
				repo_owner,
				repo_name,
				discussion_user
				from github_discussions;
				""")
public class Discussion extends PanacheEntityBase {

	@Embeddable
	public static class DiscussionsId implements Serializable {
		@Column(name = "REPO_OWNER")
		public String owner;
		@Column(name = "REPO_NAME")
		public String repo;
		@Column(name = "DISCUSSION_DATE")
		public Date date;
		@Column(name = "DISCUSSION_USER")
		public String user;

	}

	@EmbeddedId
	public DiscussionsId id;

	public Discussion() {
		super();
	}

	public Discussion(String owner, String repo, Date date, String user) {
		this.id = new DiscussionsId();
		this.id.owner = owner;
		this.id.repo = repo;
		this.id.date = date;
		this.id.user = user;
	}
}
