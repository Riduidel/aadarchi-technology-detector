package org.ndx.aadarchi.technology.detector.loader;

import java.nio.file.Path;

import org.kohsuke.github.GitHub;

public abstract class AbstractContext {
	private final GitHub github;
	private final Path cache;

	public AbstractContext(Path cache, GitHub github) {
		super();
		this.cache = cache;
		this.github = github;
	}
	
	public Path getCache() {
		return cache;
	}

	public GitHub getGithub() {
		return github;
	}
}
