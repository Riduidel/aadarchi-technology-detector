package org.ndx.aadarchi.technology.detector.mvnrepository;

import java.util.Objects;

import org.apache.commons.lang3.builder.CompareToBuilder;

public class ArtifactInformations implements Comparable<ArtifactInformations> {

	public final String groupId;
	public final String artifactId;

	public ArtifactInformations(String groupId, String artifactId) {
		this.groupId = groupId;
		this.artifactId = artifactId;
	}
	
	@Override
	public String toString() {
		return "ArtifactInformations [groupId=" + groupId + ", artifactId=" + artifactId + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(artifactId, groupId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArtifactInformations other = (ArtifactInformations) obj;
		return Objects.equals(artifactId, other.artifactId) && Objects.equals(groupId, other.groupId);
	}

	@Override
	public int compareTo(ArtifactInformations o) {
		return new CompareToBuilder()
				.append(groupId, o.groupId)
				.append(artifactId, o.artifactId)
				.toComparison();
	}
}