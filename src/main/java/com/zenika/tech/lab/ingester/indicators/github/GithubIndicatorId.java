package com.zenika.tech.lab.ingester.indicators.github;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Date;

@Embeddable
public class GithubIndicatorId implements Serializable {
    public String owner;
    public String repo;
    public Date date;
    public String user;
}
