
package org.ndx.aadarchi.technology.detector.librariesio.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "code_of_conduct_url",
    "contributions_count",
    "contribution_guidelines_url",
    "dependent_repos_count",
    "dependents_count",
    "deprecation_reason",
    "description",
    "forks",
    "funding_urls",
    "homepage",
    "keywords",
    "language",
    "latest_download_url",
    "latest_release_number",
    "latest_release_published_at",
    "latest_stable_release_number",
    "latest_stable_release_published_at",
    "license_normalized",
    "licenses",
    "name",
    "normalized_licenses",
    "package_manager_url",
    "platform",
    "rank",
    "repository_license",
    "repository_status",
    "repository_url",
    "security_policy_url",
    "stars",
    "status",
    "versions"
})
@Generated("jsonschema2pojo")
public class Project {

    @JsonProperty("code_of_conduct_url")
    private String codeOfConductUrl;
    @JsonProperty("contributions_count")
    private Integer contributionsCount;
    @JsonProperty("contribution_guidelines_url")
    private String contributionGuidelinesUrl;
    @JsonProperty("dependent_repos_count")
    private Integer dependentReposCount;
    @JsonProperty("dependents_count")
    private Integer dependentsCount;
    @JsonProperty("deprecation_reason")
    private Object deprecationReason;
    @JsonProperty("description")
    private String description;
    @JsonProperty("forks")
    private Integer forks;
    @JsonProperty("funding_urls")
    private List<String> fundingUrls = new ArrayList<String>();
    @JsonProperty("homepage")
    private String homepage;
    @JsonProperty("keywords")
    private List<String> keywords = new ArrayList<String>();
    @JsonProperty("language")
    private String language;
    @JsonProperty("latest_download_url")
    private String latestDownloadUrl;
    @JsonProperty("latest_release_number")
    private String latestReleaseNumber;
    @JsonProperty("latest_release_published_at")
    private String latestReleasePublishedAt;
    @JsonProperty("latest_stable_release_number")
    private String latestStableReleaseNumber;
    @JsonProperty("latest_stable_release_published_at")
    private String latestStableReleasePublishedAt;
    @JsonProperty("license_normalized")
    private Boolean licenseNormalized;
    @JsonProperty("licenses")
    private String licenses;
    @JsonProperty("name")
    private String name;
    @JsonProperty("normalized_licenses")
    private List<String> normalizedLicenses = new ArrayList<String>();
    @JsonProperty("package_manager_url")
    private String packageManagerUrl;
    @JsonProperty("platform")
    private String platform;
    @JsonProperty("rank")
    private Integer rank;
    @JsonProperty("repository_license")
    private String repositoryLicense;
    @JsonProperty("repository_status")
    private Object repositoryStatus;
    @JsonProperty("repository_url")
    private String repositoryUrl;
    @JsonProperty("security_policy_url")
    private String securityPolicyUrl;
    @JsonProperty("stars")
    private Integer stars;
    @JsonProperty("status")
    private Object status;
    @JsonProperty("versions")
    private List<Version> versions = new ArrayList<Version>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("code_of_conduct_url")
    public String getCodeOfConductUrl() {
        return codeOfConductUrl;
    }

    @JsonProperty("code_of_conduct_url")
    public void setCodeOfConductUrl(String codeOfConductUrl) {
        this.codeOfConductUrl = codeOfConductUrl;
    }

    @JsonProperty("contributions_count")
    public Integer getContributionsCount() {
        return contributionsCount;
    }

    @JsonProperty("contributions_count")
    public void setContributionsCount(Integer contributionsCount) {
        this.contributionsCount = contributionsCount;
    }

    @JsonProperty("contribution_guidelines_url")
    public String getContributionGuidelinesUrl() {
        return contributionGuidelinesUrl;
    }

    @JsonProperty("contribution_guidelines_url")
    public void setContributionGuidelinesUrl(String contributionGuidelinesUrl) {
        this.contributionGuidelinesUrl = contributionGuidelinesUrl;
    }

    @JsonProperty("dependent_repos_count")
    public Integer getDependentReposCount() {
        return dependentReposCount;
    }

    @JsonProperty("dependent_repos_count")
    public void setDependentReposCount(Integer dependentReposCount) {
        this.dependentReposCount = dependentReposCount;
    }

    @JsonProperty("dependents_count")
    public Integer getDependentsCount() {
        return dependentsCount;
    }

    @JsonProperty("dependents_count")
    public void setDependentsCount(Integer dependentsCount) {
        this.dependentsCount = dependentsCount;
    }

    @JsonProperty("deprecation_reason")
    public Object getDeprecationReason() {
        return deprecationReason;
    }

    @JsonProperty("deprecation_reason")
    public void setDeprecationReason(Object deprecationReason) {
        this.deprecationReason = deprecationReason;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("forks")
    public Integer getForks() {
        return forks;
    }

    @JsonProperty("forks")
    public void setForks(Integer forks) {
        this.forks = forks;
    }

    @JsonProperty("funding_urls")
    public List<String> getFundingUrls() {
        return fundingUrls;
    }

    @JsonProperty("funding_urls")
    public void setFundingUrls(List<String> fundingUrls) {
        this.fundingUrls = fundingUrls;
    }

    @JsonProperty("homepage")
    public String getHomepage() {
        return homepage;
    }

    @JsonProperty("homepage")
    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    @JsonProperty("keywords")
    public List<String> getKeywords() {
        return keywords;
    }

    @JsonProperty("keywords")
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    @JsonProperty("language")
    public String getLanguage() {
        return language;
    }

    @JsonProperty("language")
    public void setLanguage(String language) {
        this.language = language;
    }

    @JsonProperty("latest_download_url")
    public String getLatestDownloadUrl() {
        return latestDownloadUrl;
    }

    @JsonProperty("latest_download_url")
    public void setLatestDownloadUrl(String latestDownloadUrl) {
        this.latestDownloadUrl = latestDownloadUrl;
    }

    @JsonProperty("latest_release_number")
    public String getLatestReleaseNumber() {
        return latestReleaseNumber;
    }

    @JsonProperty("latest_release_number")
    public void setLatestReleaseNumber(String latestReleaseNumber) {
        this.latestReleaseNumber = latestReleaseNumber;
    }

    @JsonProperty("latest_release_published_at")
    public String getLatestReleasePublishedAt() {
        return latestReleasePublishedAt;
    }

    @JsonProperty("latest_release_published_at")
    public void setLatestReleasePublishedAt(String latestReleasePublishedAt) {
        this.latestReleasePublishedAt = latestReleasePublishedAt;
    }

    @JsonProperty("latest_stable_release_number")
    public String getLatestStableReleaseNumber() {
        return latestStableReleaseNumber;
    }

    @JsonProperty("latest_stable_release_number")
    public void setLatestStableReleaseNumber(String latestStableReleaseNumber) {
        this.latestStableReleaseNumber = latestStableReleaseNumber;
    }

    @JsonProperty("latest_stable_release_published_at")
    public String getLatestStableReleasePublishedAt() {
        return latestStableReleasePublishedAt;
    }

    @JsonProperty("latest_stable_release_published_at")
    public void setLatestStableReleasePublishedAt(String latestStableReleasePublishedAt) {
        this.latestStableReleasePublishedAt = latestStableReleasePublishedAt;
    }

    @JsonProperty("license_normalized")
    public Boolean getLicenseNormalized() {
        return licenseNormalized;
    }

    @JsonProperty("license_normalized")
    public void setLicenseNormalized(Boolean licenseNormalized) {
        this.licenseNormalized = licenseNormalized;
    }

    @JsonProperty("licenses")
    public String getLicenses() {
        return licenses;
    }

    @JsonProperty("licenses")
    public void setLicenses(String licenses) {
        this.licenses = licenses;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("normalized_licenses")
    public List<String> getNormalizedLicenses() {
        return normalizedLicenses;
    }

    @JsonProperty("normalized_licenses")
    public void setNormalizedLicenses(List<String> normalizedLicenses) {
        this.normalizedLicenses = normalizedLicenses;
    }

    @JsonProperty("package_manager_url")
    public String getPackageManagerUrl() {
        return packageManagerUrl;
    }

    @JsonProperty("package_manager_url")
    public void setPackageManagerUrl(String packageManagerUrl) {
        this.packageManagerUrl = packageManagerUrl;
    }

    @JsonProperty("platform")
    public String getPlatform() {
        return platform;
    }

    @JsonProperty("platform")
    public void setPlatform(String platform) {
        this.platform = platform;
    }

    @JsonProperty("rank")
    public Integer getRank() {
        return rank;
    }

    @JsonProperty("rank")
    public void setRank(Integer rank) {
        this.rank = rank;
    }

    @JsonProperty("repository_license")
    public String getRepositoryLicense() {
        return repositoryLicense;
    }

    @JsonProperty("repository_license")
    public void setRepositoryLicense(String repositoryLicense) {
        this.repositoryLicense = repositoryLicense;
    }

    @JsonProperty("repository_status")
    public Object getRepositoryStatus() {
        return repositoryStatus;
    }

    @JsonProperty("repository_status")
    public void setRepositoryStatus(Object repositoryStatus) {
        this.repositoryStatus = repositoryStatus;
    }

    @JsonProperty("repository_url")
    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    @JsonProperty("repository_url")
    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    @JsonProperty("security_policy_url")
    public String getSecurityPolicyUrl() {
        return securityPolicyUrl;
    }

    @JsonProperty("security_policy_url")
    public void setSecurityPolicyUrl(String securityPolicyUrl) {
        this.securityPolicyUrl = securityPolicyUrl;
    }

    @JsonProperty("stars")
    public Integer getStars() {
        return stars;
    }

    @JsonProperty("stars")
    public void setStars(Integer stars) {
        this.stars = stars;
    }

    @JsonProperty("status")
    public Object getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(Object status) {
        this.status = status;
    }

    @JsonProperty("versions")
    public List<Version> getVersions() {
        return versions;
    }

    @JsonProperty("versions")
    public void setVersions(List<Version> versions) {
        this.versions = versions;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

	@Override
	public String toString() {
		return "Project [" + (name != null ? "name=" + name + ", " : "")
				+ (homepage != null ? "homepage=" + homepage + ", " : "")
				+ (keywords != null ? "keywords=" + keywords + ", " : "")
				+ (language != null ? "language=" + language : "") + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(description, homepage, keywords, language, name, packageManagerUrl, repositoryUrl);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Project other = (Project) obj;
		return Objects.equals(description, other.description) && Objects.equals(homepage, other.homepage)
				&& Objects.equals(keywords, other.keywords) && Objects.equals(language, other.language)
				&& Objects.equals(name, other.name) && Objects.equals(packageManagerUrl, other.packageManagerUrl)
				&& Objects.equals(repositoryUrl, other.repositoryUrl);
	}
}
