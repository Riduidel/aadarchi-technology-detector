package org.ndx.aadarchi.technology.detector.packagist.model;

import java.util.Collection;
import java.util.LinkedList;

public class Package {
    public String name;
    public String description;
    public String homepage;
    public Collection<String> keywords = new LinkedList<String>();
    public String license;
    public String repository;
    public long downloads;
    public long favers;
    public Collection<String> maintainers = new LinkedList<>();
}
