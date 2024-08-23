package org.ndx.aadarchi.technology.detector.npmjs.model;

import java.util.Collection;
import java.util.LinkedList;

// TODO add versions once we know how to have that versions download numbers counted
public class Package {
	public static class Maintainer {
		public String name;
		public String email;
	}
	public String readme;
	public String description;
	public Collection<Maintainer> maintainers = new LinkedList<>();
	public String homepage;
	public Collection<String> keywords = new LinkedList<String>();
	public String license;
}
