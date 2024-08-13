package org.ndx.aadarchi.technology.detector.model;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;

public class FakeArtifact implements Artifact {

	private String groupId;
	private String artifactId;

	public FakeArtifact(String string, String string2) {
		this.groupId = string;
		this.artifactId = string2;
	}

	@Override
	public int compareTo(Artifact a) {
	        int result = groupId.compareTo( a.getGroupId() );
	        if ( result == 0 )
	        {
	            result = artifactId.compareTo( a.getArtifactId() );
	        }
		return result;
	}

	@Override
	public String getGroupId() {
		return groupId;
	}

	@Override
	public String getArtifactId() {
		return artifactId;
	}

	@Override
	public String getVersion() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setVersion(String version) {
		throw new UnsupportedOperationException("artifact is fake");

	}

	@Override
	public String getScope() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public String getType() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public String getClassifier() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public boolean hasClassifier() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public File getFile() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setFile(File destination) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public String getBaseVersion() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setBaseVersion(String baseVersion) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public String getId() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public String getDependencyConflictId() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void addMetadata(ArtifactMetadata metadata) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public ArtifactMetadata getMetadata(Class<?> metadataClass) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public Collection<ArtifactMetadata> getMetadataList() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setRepository(ArtifactRepository remoteRepository) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public ArtifactRepository getRepository() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void updateVersion(String version, ArtifactRepository localRepository) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public String getDownloadUrl() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setDownloadUrl(String downloadUrl) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public ArtifactFilter getDependencyFilter() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setDependencyFilter(ArtifactFilter artifactFilter) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public ArtifactHandler getArtifactHandler() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public List<String> getDependencyTrail() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setDependencyTrail(List<String> dependencyTrail) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setScope(String scope) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public VersionRange getVersionRange() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setVersionRange(VersionRange newRange) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void selectVersion(String version) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setGroupId(String groupId) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setArtifactId(String artifactId) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public boolean isSnapshot() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setResolved(boolean resolved) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public boolean isResolved() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setResolvedVersion(String version) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setArtifactHandler(ArtifactHandler handler) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public boolean isRelease() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setRelease(boolean release) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public List<ArtifactVersion> getAvailableVersions() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setAvailableVersions(List<ArtifactVersion> versions) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public boolean isOptional() {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public void setOptional(boolean optional) {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public ArtifactVersion getSelectedVersion() throws OverConstrainedVersionException {
		throw new UnsupportedOperationException("artifact is fake");
	}

	@Override
	public boolean isSelectedVersionKnown() throws OverConstrainedVersionException {
		throw new UnsupportedOperationException("artifact is fake");
	}

}
