package org.ndx.aadarchi.technology.detector.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jilt.Builder;

@Builder(toBuilder = "toBuilder")
public class ArtifactDetails implements Comparable<ArtifactDetails> {
	public final String coordinates;
	public final String name;
	public final String description;
	public final List<String> licenses;
	public final List<String> categories;
	public final List<String> tags;
	public final long downloads;
	public final int ranking;
	public final int users;
	public final int previousUsers;
	public final int interpolatedUsers;
	public final boolean infered;
	public final List<String> repositories;
	public final Map<String, VersionDetails> versions;

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
				.filter(entry -> entry.getValue().date!=null)
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
		VersionDetails returned = new VersionDetails();
		returned.date = current.date;
		returned.previousUsers = versionBefore==null ? 0 : versionBefore.users;
		returned.interpolatedUsers = interpolateUsers(value.users, currentDate, 
				returned.previousUsers, beforeDate, inferredMonth);
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
		return coordinates.compareTo(o.coordinates);
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
				if(!newVersion.date.equals(oldVersion.date)) {
					VersionDetails changedVersion = new VersionDetails();
					changedVersion.date = newVersion.date;
					changedVersion.usages = oldVersion.usages;
					changedVersion.users = oldVersion.users;
					oldVersion = changedVersion;
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
}
