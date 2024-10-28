package org.ndx.aadarchi.technology.detector.loader;

import java.nio.file.Path;

public abstract class AbstractContext {
	private final String githubToken;
	private final Path cache;

	public AbstractContext(Path cache, String githubToken) {
		super();
		this.cache = cache;
		this.githubToken = githubToken;
	}
	
	public Path getCache() {
		return cache;
	}

	public String getGithubToken() {
		return githubToken;
	}
}
