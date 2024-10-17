package org.ndx.aadarchi.technology.detector.model;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jilt.Builder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.ndx.aadarchi.technology.detector.exception.ArtifactCoordinateException;
import org.ndx.aadarchi.technology.detector.exception.ArtifactPropertiesLoadingException;

@Builder(toBuilder = "toBuilder")
public class ArtifactDetails implements Comparable<ArtifactDetails> {
	private static final Logger logger = Logger.getLogger(ArtifactDetails.class.getName());
	public static final String BADLY_NAMED_ARTIFACTS_MAPPING = "badly_named_artifacts.properties";

	/**
	 * Stupidly enough, I had some code where coordinates were stored as one field (bad)
	 * And with "." as separator (worse for Java)
	 * 
	 * To change that, I took last version, and run (with https://github.com/ddopson/underscore-cli)
	 * 
	 * cat artifacts.json | underscore map 'value.groupId+"."+value.artifactId+"="+value.groupId+":"+value.artifactId' > badly_named_artifacts.properties
	 * 
	 * on it
	 */
	static Properties badlyNamedArtifactsToCorrectlyNamedOnes = new Properties();
	static {
		if(ArtifactDetails.class.getClassLoader().getResource(BADLY_NAMED_ARTIFACTS_MAPPING)==null) {
			logger.warning(String.format("There is no %s file in CLASSPATH, no mapping will be done", BADLY_NAMED_ARTIFACTS_MAPPING));
		} else {
			try(InputStream p = ArtifactDetails.class.getClassLoader().getResourceAsStream(BADLY_NAMED_ARTIFACTS_MAPPING)) {
				badlyNamedArtifactsToCorrectlyNamedOnes.load(p);
			} catch (IOException e) {
				throw new ArtifactPropertiesLoadingException("This one should never happen", e);
			}
		}
	}
	
	private static Comparator<String> nullSafeStringComparator = Comparator
	        .nullsFirst(String::compareToIgnoreCase); 

	private static final Comparator<ArtifactDetails> COMPARATOR_BY_COORDINATES_THEN_NAME =
			Comparator
	        .comparing(ArtifactDetails::getGroupId, nullSafeStringComparator)
	        .thenComparing(ArtifactDetails::getArtifactId, nullSafeStringComparator)
	        .thenComparing(ArtifactDetails::getName, nullSafeStringComparator);

	private static final Comparator<String> COMPARATOR_BY_MAVEN_VERSION = new Comparator<String>() {
		private Map<String, DefaultArtifactVersion> versionsCache = new TreeMap<>();
		@Override
		public int compare(String o1, String o2) {
			DefaultArtifactVersion v1 = versionsCache.computeIfAbsent(o1, v -> new DefaultArtifactVersion(v));
			DefaultArtifactVersion v2 = versionsCache.computeIfAbsent(o2, v -> new DefaultArtifactVersion(v));
			return v1.compareTo(v2);
		}
		
	};
	public static final TypeReference<List<ArtifactDetails>> LIST =  new TypeReference<List<ArtifactDetails>>() {};
	public static final List<Function<ArtifactDetails, String>> GITHUB_REPO_EXTRACTORS = Arrays.asList(
			ArtifactDetails::getCoordinates,
			ArtifactDetails::getGroupId,
			ArtifactDetails::getName);
	
	private String groupId;
	private String artifactId;
	private String name;
	private String description;
	private List<String> licenses;
	private List<String> categories;
	private List<String> tags;
	// We use object types because Gson won't serialize them when they're null
	// i.e. when they have not be set in any fashion
	private Long downloads;
	private Integer ranking;
	private Integer users;
	private Integer previousUsers;
	private Integer interpolatedUsers;
	private Boolean infered;
	private List<String> repositories;
	@JsonDeserialize(as=TreeMap.class)
	private SortedMap<String, VersionDetails> versions;
	@JsonDeserialize(as=LinkedHashMap.class)
	private Map<String, String> urls;
	private GitHubDetails githubDetails;
	
	public ArtifactDetails() {}

	public ArtifactDetails(
			String groupId,
			String artifactId,
			String name, 
			String description, 
			List<String> licenses,
			List<String> categories, 
			List<String> tags, 
			Long downloads,
			Integer ranking, 
			Integer users, 
			Integer previousUsers,
			Integer interpolatedUsers, 
			Boolean infered, 
			List<String> repositories, 
			SortedMap<String, VersionDetails> versions,
			Map<String, String> urls,
			GitHubDetails githubDetails) {
		super();
		this.groupId = groupId;
		this.artifactId = artifactId;
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
		this.urls = urls;
		this.githubDetails = githubDetails;
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
	private SortedMap<String, VersionDetails> versionsUpTo(
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
		SortedMap<String, VersionDetails> updatedVersions = new TreeMap<>();
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

	public Long getDownloads() {
		return downloads;
	}

	public void setDownloads(long downloads) {
		this.downloads = downloads;
	}

	public Integer getRanking() {
		return ranking;
	}

	public void setRanking(int ranking) {
		this.ranking = ranking;
	}

	public Integer getPreviousUsers() {
		return previousUsers;
	}

	public void setPreviousUsers(int previousUsers) {
		this.previousUsers = previousUsers;
	}

	public Integer getInterpolatedUsers() {
		return interpolatedUsers;
	}

	public void setInterpolatedUsers(int interpolatedUsers) {
		this.interpolatedUsers = interpolatedUsers;
	}
	
	public Boolean isInfered() {
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

	public SortedMap<String, VersionDetails> getVersions() {
		return versions;
	}

	@JsonSetter
	public void setVersions(Map<String, VersionDetails> versions) {
		this.versions = new TreeMap<String, VersionDetails>(COMPARATOR_BY_MAVEN_VERSION);
		if(versions!=null)
			this.versions.putAll(versions);
	}

	public void setVersions(SortedMap<String, VersionDetails> versions) {
		setVersions((Map<String, VersionDetails>) versions);
	}

	public Integer getUsers() {
		return users;
	}

	public void setUsers(int users) {
		this.users = users;
	}

	public Map<String, String> getUrls() {
		return urls;
	}

	public void setUrls(Map<String, String> urls) {
		this.urls = urls;
	}

	@Override
	public String toString() {
		return "ArtifactDetails ["
				+ (groupId != null ? "groupId=" + groupId + ", " : "")
				+ (artifactId != null ? "artifactId=" + artifactId + ", " : "")
				+ (name != null ? "name=" + name + ", " : "")
				+ (description != null ? "description=" + description + ", " : "")
				+ (licenses != null ? "licenses=" + licenses + ", " : "")
				+ (categories != null ? "categories=" + categories + ", " : "")
				+ (tags != null ? "tags=" + tags + ", " : "") 
				+ (downloads==null || downloads ==0 ? "" : "downloads=" + downloads + ",")
				+ (ranking==null || ranking==0 ? "" : ", ranking=" + ranking+ ",")
				+ (users==null || users ==0 ? "" : ", users=" + users + ",")
				+ (previousUsers==null || previousUsers ==0 ? "" : ", previousUsers=" + previousUsers + ",")
				+ (interpolatedUsers==null || interpolatedUsers==0 ? "" : ", interpolatedUsers=" + interpolatedUsers+ ",")
				+ (infered==null || infered ? "infered=" + infered + ", " : "" )
				+ (repositories != null ? "repositories=" + repositories + ", " : "")
				+ (versions != null ? "versions=" + versions : "") + "]"
				+ (urls != null ? "urls=" + urls: "") + "]";
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}
	
	public void setCoordinates(String c) {
		if(c==null) {
		} else if(c.contains(":")) {
			String[] parts = c.split(":");
			if(parts.length!=2)
				throw new ArtifactCoordinateException("Can't extract coordinates when they're not groupId:artifactId.\nInput string is "+c);
			setGroupId(parts[0]);
			setArtifactId(parts[1]);
		} else if(c.contains(".")){
			if(c.endsWith("."))
				c = c.substring(0, c.length()-1);
			// Oh the dumbass shit of the man who thought it could be "clever" to use "." as group:artifact separator.
			// I'm sorry to be soooo stupid
			if(badlyNamedArtifactsToCorrectlyNamedOnes.containsKey(c)) {
				setCoordinates(badlyNamedArtifactsToCorrectlyNamedOnes.getProperty(c));
			} else {
				throw new ArtifactCoordinateException("Can't extract coordinates when they're not groupId:artifactId.\nInput string is "+c);
			}
		}
	}

	public String getCoordinates() {
		StringBuilder returned = new StringBuilder();
		if(getGroupId()!=null)
			returned.append(getGroupId());
		if(getArtifactId()!=null) {
			returned.append(':').append(getArtifactId());
		}
		if(returned.isEmpty())
			return null;
		else
			return returned.toString();
	}

	@JsonIgnore
	public String getIdentifier() {
		return getCoordinates() == null ? getName() : getCoordinates();
	}

	public GitHubDetails getGithubDetails() {
		return githubDetails;
	}

	public void setGithubDetails(GitHubDetails githubDetails) {
		this.githubDetails = githubDetails;
	}
}
