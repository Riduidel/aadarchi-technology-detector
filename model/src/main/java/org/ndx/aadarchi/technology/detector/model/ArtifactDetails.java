package org.ndx.aadarchi.technology.detector.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jilt.Builder;

@Builder(toBuilder = "toBuilder")
public class ArtifactDetails implements Comparable<ArtifactDetails> {
	
	private static Comparator<String> nullSafeStringComparator = Comparator
	        .nullsFirst(String::compareToIgnoreCase); 

	private static final Comparator<ArtifactDetails> COMPARATOR_BY_COORDINATES_THEN_NAME =
			Comparator
	        .comparing(ArtifactDetails::getCoordinates, nullSafeStringComparator)
	        .thenComparing(ArtifactDetails::getName, nullSafeStringComparator);
	
	public static ArtifactDetails toArtifactDetails(Map values) {
		return null;
	}
	
	private String coordinates;
	private String name;
	private String description;
	private List<String> licenses;
	private List<String> categories;
	private List<String> tags;
	private long downloads;
	private int ranking;
	private int users;
	private int previousUsers;
	private int interpolatedUsers;
	private boolean infered;
	private List<String> repositories;
	private Map<String, VersionDetails> versions;
	
	public ArtifactDetails() {}

	public ArtifactDetails(
			String coordinates, 
			String name, 
			String description, 
			List<String> licenses,
			List<String> categories, 
			List<String> tags, 
			long downloads,
			int ranking, 
			int users, 
			int previousUsers,
			int interpolatedUsers, 
			boolean infered, 
			List<String> repositories, 
			Map<String, VersionDetails> versions) {
		super();
		this.coordinates = coordinates;
		this.name = name;
		this.description = description;
		this.licenses = licenses;
		this.categories = categories;
		this.tags = tags;
		this.downloads = downloads;
		this.ranking = ranking;
		this.users = users;
		this.previousUsers = previousUsers;
		this.interpolatedUsers = interpolatedUsers;
		this.infered = infered;
		this.repositories = repositories;
		this.versions = versions;
	}

	/**
	 * This method creates an **infered** artifact details from this one and an optional previous one.
	 * @param currentData 
	 * @param thisDate date at which this artifact details have been generated
	 * @param inferredMonth target month
	 * @param dataBefore optional (so, nullable) previous artifact details
	 * @param beforeDate optional (so, nullable) date of previous artifact details
	 * 
	 * @return
	 */
	public ArtifactDetails inferDataPoint(
			ArtifactDetails currentData, LocalDate thisDate,
			LocalDate inferredMonth,
			LocalDate beforeDate,
			ArtifactDetails dataBefore) {
		return ArtifactDetailsBuilder.toBuilder(this)
			.users(0)
			.previousUsers(dataBefore==null ? 0 : dataBefore.users==0 ? dataBefore.previousUsers : dataBefore.users)
			.interpolatedUsers(interpolateUsers(users, thisDate, dataBefore==null ? 0 : dataBefore.users, beforeDate, thisDate))
			.infered(true)
			.versions(versionsUpTo(currentData.versions, thisDate, inferredMonth, beforeDate, dataBefore))
			.build();
	}

	/**
	 * Generate map of versions from existing ones
	 * When possible, usage of version will be "correctly" computed
	 * @param versions2 
	 * @param inferredMonth the month at which we want that usage
	 * @param beforeDate the optional (maybe nullable) date of the previous data point
	 * @param dataBefore the potional (maybe nullable) previous data point
	 * @return a map of previous versions
	 */
	private Map<String, VersionDetails> versionsUpTo(
			Map<String, VersionDetails> currentVersions, LocalDate thisDate, LocalDate inferredMonth,
			LocalDate beforeDate, ArtifactDetails dataBefore) {
		return versions.entrySet().stream()
				// Only keep versions older that inferredMonth
				.filter(entry -> entry.getValue().getDate()!=null)
				.filter(entry -> entry.getValue().getParsedDate(Formats.MVN_DATE_FORMAT).compareTo(inferredMonth)<=0)
				.filter(entry -> currentVersions.containsKey(entry.getKey()))
				.filter(entry -> currentVersions.get(entry.getKey())!=null)
				.collect(Collectors.toMap(Entry::getKey, 
						entry -> computePreviousVersionFor(currentVersions.get(entry.getKey()),  entry.getValue(), thisDate, inferredMonth, beforeDate, 
								dataBefore==null? null : 
									dataBefore.versions == null ? null :
										dataBefore.versions.containsKey(entry.getKey()) ?
												dataBefore.versions.get(entry.getKey()) : null
									),
						(a, b) -> a,
						() -> new TreeMap<String, VersionDetails>(new ByArtifactVersionComparator().reversed())))
				;
	}
	
	private static class ByArtifactVersionComparator implements Comparator<String> {

		@Override
		public int compare(String o1, String o2) {
			ArtifactVersion a1 = new DefaultArtifactVersion(o1);
			ArtifactVersion a2 = new DefaultArtifactVersion(o1);
			int returned = a1.compareTo(a2);
			if(returned==0) {
				returned = o1.compareTo(o2);
			}
			return returned;
		}
		
	}

	private VersionDetails computePreviousVersionFor(VersionDetails current, VersionDetails value, LocalDate currentDate,
			LocalDate inferredMonth, LocalDate beforeDate, VersionDetails versionBefore) {
		VersionDetails returned = VersionDetailsBuilder.toBuilder(versionBefore)
			.previousUsers(versionBefore==null ? 0 : versionBefore.getUsers())
			.build();
		returned.setPreviousUsers(interpolateUsers(value.getUsers(), currentDate, 
				returned.getPreviousUsers(), beforeDate, inferredMonth));
		return returned;
	}

	/**
	 * Uses classic linear interpolation
	 * if (yb-ya)/(xb-xa)=(yb-yc)/(xb-xc)
	 * then (yb-ya)/(xb-xa)*(xb-xc)=(yb-yc)
	 * and so yc=yb-(yb-ya)/(xb-xa)*(xb-xc)
	 * @param toUsers yb
	 * @param toDate xb
	 * @param fromUsers ya
	 * @param fromDate xa
	 * @param targetDate xc
	 * @return
	 */
	private int interpolateUsers(int toUsers, LocalDate toDate, 
			int fromUsers, LocalDate fromDate, LocalDate targetDate) {
		long xb_xa = ChronoUnit.DAYS.between(fromDate, toDate);
		long xb_xc = ChronoUnit.DAYS.between(targetDate, toDate);
		long yb_ya = toUsers-fromUsers;
		return (int) (toUsers-yb_ya/xb_xa*xb_xc);
	}

	@Override
	public int compareTo(ArtifactDetails o) {
		return COMPARATOR_BY_COORDINATES_THEN_NAME
			.compare(this, o);
	}

	/**
	 * Copy versions date from the given artifact details object.
	 * Obviously, interpolation is done whenever possible
	 * @param details
	 * @return
	 */
	public Optional<ArtifactDetails> copyDatesFrom(ArtifactDetails details) {
		Set<String> oldVersions = new LinkedHashSet<>(versions.keySet());
		Map<String, VersionDetails> updatedVersions = new LinkedHashMap<>();
		boolean changed = false;
		for(String version : oldVersions) {
			if(details.versions.containsKey(version) && versions.containsKey(version)) {
				VersionDetails newVersion = details.versions.get(version);
				VersionDetails oldVersion = versions.get(version);
				if(!newVersion.getDate().equals(oldVersion.getDate())) {
					oldVersion = VersionDetailsBuilder.toBuilder(oldVersion)
							.date(newVersion.getDate())
							.build();
					changed = true;
				}
				updatedVersions.put(version, oldVersion);
			}
		}
		if(changed) {
			return Optional.ofNullable(
					ArtifactDetailsBuilder.toBuilder(this)
						.versions(updatedVersions)
						.build()
					);
		} else {
			return Optional.empty();
		}
	}

	public Artifact toArtifact() {
		String[] split = coordinates.split(":");
		return new FakeArtifact(split[0], split.length>1 ? split[1] : null);
	}

	public String getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(String coordinates) {
		this.coordinates = coordinates;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getLicenses() {
		return licenses;
	}

	public void setLicenses(List<String> licenses) {
		this.licenses = licenses;
	}

	public List<String> getCategories() {
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public long getDownloads() {
		return downloads;
	}

	public void setDownloads(long downloads) {
		this.downloads = downloads;
	}

	public int getRanking() {
		return ranking;
	}

	public void setRanking(int ranking) {
		this.ranking = ranking;
	}

	public int getPreviousUsers() {
		return previousUsers;
	}

	public void setPreviousUsers(int previousUsers) {
		this.previousUsers = previousUsers;
	}

	public int getInterpolatedUsers() {
		return interpolatedUsers;
	}

	public void setInterpolatedUsers(int interpolatedUsers) {
		this.interpolatedUsers = interpolatedUsers;
	}

	public boolean isInfered() {
		return infered;
	}

	public void setInfered(boolean infered) {
		this.infered = infered;
	}

	public List<String> getRepositories() {
		return repositories;
	}

	public void setRepositories(List<String> repositories) {
		this.repositories = repositories;
	}

	public Map<String, VersionDetails> getVersions() {
		return versions;
	}

	public void setVersions(Map<String, VersionDetails> versions) {
		this.versions = versions;
	}

	public int getUsers() {
		return users;
	}

	public void setUsers(int users) {
		this.users = users;
	}
}
