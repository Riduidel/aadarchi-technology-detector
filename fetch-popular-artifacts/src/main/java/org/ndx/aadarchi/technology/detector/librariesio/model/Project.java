
package org.ndx.aadarchi.technology.detector.librariesio.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        StringBuilder sb = new StringBuilder();
        sb.append(Project.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("codeOfConductUrl");
        sb.append('=');
        sb.append(((this.codeOfConductUrl == null)?"<null>":this.codeOfConductUrl));
        sb.append(',');
        sb.append("contributionsCount");
        sb.append('=');
        sb.append(((this.contributionsCount == null)?"<null>":this.contributionsCount));
        sb.append(',');
        sb.append("contributionGuidelinesUrl");
        sb.append('=');
        sb.append(((this.contributionGuidelinesUrl == null)?"<null>":this.contributionGuidelinesUrl));
        sb.append(',');
        sb.append("dependentReposCount");
        sb.append('=');
        sb.append(((this.dependentReposCount == null)?"<null>":this.dependentReposCount));
        sb.append(',');
        sb.append("dependentsCount");
        sb.append('=');
        sb.append(((this.dependentsCount == null)?"<null>":this.dependentsCount));
        sb.append(',');
        sb.append("deprecationReason");
        sb.append('=');
        sb.append(((this.deprecationReason == null)?"<null>":this.deprecationReason));
        sb.append(',');
        sb.append("description");
        sb.append('=');
        sb.append(((this.description == null)?"<null>":this.description));
        sb.append(',');
        sb.append("forks");
        sb.append('=');
        sb.append(((this.forks == null)?"<null>":this.forks));
        sb.append(',');
        sb.append("fundingUrls");
        sb.append('=');
        sb.append(((this.fundingUrls == null)?"<null>":this.fundingUrls));
        sb.append(',');
        sb.append("homepage");
        sb.append('=');
        sb.append(((this.homepage == null)?"<null>":this.homepage));
        sb.append(',');
        sb.append("keywords");
        sb.append('=');
        sb.append(((this.keywords == null)?"<null>":this.keywords));
        sb.append(',');
        sb.append("language");
        sb.append('=');
        sb.append(((this.language == null)?"<null>":this.language));
        sb.append(',');
        sb.append("latestDownloadUrl");
        sb.append('=');
        sb.append(((this.latestDownloadUrl == null)?"<null>":this.latestDownloadUrl));
        sb.append(',');
        sb.append("latestReleaseNumber");
        sb.append('=');
        sb.append(((this.latestReleaseNumber == null)?"<null>":this.latestReleaseNumber));
        sb.append(',');
        sb.append("latestReleasePublishedAt");
        sb.append('=');
        sb.append(((this.latestReleasePublishedAt == null)?"<null>":this.latestReleasePublishedAt));
        sb.append(',');
        sb.append("latestStableReleaseNumber");
        sb.append('=');
        sb.append(((this.latestStableReleaseNumber == null)?"<null>":this.latestStableReleaseNumber));
        sb.append(',');
        sb.append("latestStableReleasePublishedAt");
        sb.append('=');
        sb.append(((this.latestStableReleasePublishedAt == null)?"<null>":this.latestStableReleasePublishedAt));
        sb.append(',');
        sb.append("licenseNormalized");
        sb.append('=');
        sb.append(((this.licenseNormalized == null)?"<null>":this.licenseNormalized));
        sb.append(',');
        sb.append("licenses");
        sb.append('=');
        sb.append(((this.licenses == null)?"<null>":this.licenses));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("normalizedLicenses");
        sb.append('=');
        sb.append(((this.normalizedLicenses == null)?"<null>":this.normalizedLicenses));
        sb.append(',');
        sb.append("packageManagerUrl");
        sb.append('=');
        sb.append(((this.packageManagerUrl == null)?"<null>":this.packageManagerUrl));
        sb.append(',');
        sb.append("platform");
        sb.append('=');
        sb.append(((this.platform == null)?"<null>":this.platform));
        sb.append(',');
        sb.append("rank");
        sb.append('=');
        sb.append(((this.rank == null)?"<null>":this.rank));
        sb.append(',');
        sb.append("repositoryLicense");
        sb.append('=');
        sb.append(((this.repositoryLicense == null)?"<null>":this.repositoryLicense));
        sb.append(',');
        sb.append("repositoryStatus");
        sb.append('=');
        sb.append(((this.repositoryStatus == null)?"<null>":this.repositoryStatus));
        sb.append(',');
        sb.append("repositoryUrl");
        sb.append('=');
        sb.append(((this.repositoryUrl == null)?"<null>":this.repositoryUrl));
        sb.append(',');
        sb.append("securityPolicyUrl");
        sb.append('=');
        sb.append(((this.securityPolicyUrl == null)?"<null>":this.securityPolicyUrl));
        sb.append(',');
        sb.append("stars");
        sb.append('=');
        sb.append(((this.stars == null)?"<null>":this.stars));
        sb.append(',');
        sb.append("status");
        sb.append('=');
        sb.append(((this.status == null)?"<null>":this.status));
        sb.append(',');
        sb.append("versions");
        sb.append('=');
        sb.append(((this.versions == null)?"<null>":this.versions));
        sb.append(',');
        sb.append("additionalProperties");
        sb.append('=');
        sb.append(((this.additionalProperties == null)?"<null>":this.additionalProperties));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.latestReleaseNumber == null)? 0 :this.latestReleaseNumber.hashCode()));
        result = ((result* 31)+((this.keywords == null)? 0 :this.keywords.hashCode()));
        result = ((result* 31)+((this.fundingUrls == null)? 0 :this.fundingUrls.hashCode()));
        result = ((result* 31)+((this.description == null)? 0 :this.description.hashCode()));
        result = ((result* 31)+((this.language == null)? 0 :this.language.hashCode()));
        result = ((result* 31)+((this.repositoryLicense == null)? 0 :this.repositoryLicense.hashCode()));
        result = ((result* 31)+((this.platform == null)? 0 :this.platform.hashCode()));
        result = ((result* 31)+((this.deprecationReason == null)? 0 :this.deprecationReason.hashCode()));
        result = ((result* 31)+((this.latestReleasePublishedAt == null)? 0 :this.latestReleasePublishedAt.hashCode()));
        result = ((result* 31)+((this.contributionsCount == null)? 0 :this.contributionsCount.hashCode()));
        result = ((result* 31)+((this.dependentsCount == null)? 0 :this.dependentsCount.hashCode()));
        result = ((result* 31)+((this.latestStableReleasePublishedAt == null)? 0 :this.latestStableReleasePublishedAt.hashCode()));
        result = ((result* 31)+((this.rank == null)? 0 :this.rank.hashCode()));
        result = ((result* 31)+((this.forks == null)? 0 :this.forks.hashCode()));
        result = ((result* 31)+((this.securityPolicyUrl == null)? 0 :this.securityPolicyUrl.hashCode()));
        result = ((result* 31)+((this.dependentReposCount == null)? 0 :this.dependentReposCount.hashCode()));
        result = ((result* 31)+((this.latestStableReleaseNumber == null)? 0 :this.latestStableReleaseNumber.hashCode()));
        result = ((result* 31)+((this.licenseNormalized == null)? 0 :this.licenseNormalized.hashCode()));
        result = ((result* 31)+((this.repositoryStatus == null)? 0 :this.repositoryStatus.hashCode()));
        result = ((result* 31)+((this.stars == null)? 0 :this.stars.hashCode()));
        result = ((result* 31)+((this.latestDownloadUrl == null)? 0 :this.latestDownloadUrl.hashCode()));
        result = ((result* 31)+((this.packageManagerUrl == null)? 0 :this.packageManagerUrl.hashCode()));
        result = ((result* 31)+((this.repositoryUrl == null)? 0 :this.repositoryUrl.hashCode()));
        result = ((result* 31)+((this.licenses == null)? 0 :this.licenses.hashCode()));
        result = ((result* 31)+((this.contributionGuidelinesUrl == null)? 0 :this.contributionGuidelinesUrl.hashCode()));
        result = ((result* 31)+((this.versions == null)? 0 :this.versions.hashCode()));
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.normalizedLicenses == null)? 0 :this.normalizedLicenses.hashCode()));
        result = ((result* 31)+((this.codeOfConductUrl == null)? 0 :this.codeOfConductUrl.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.homepage == null)? 0 :this.homepage.hashCode()));
        result = ((result* 31)+((this.status == null)? 0 :this.status.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Project) == false) {
            return false;
        }
        Project rhs = ((Project) other);
        return (((((((((((((((((((((((((((((((((this.latestReleaseNumber == rhs.latestReleaseNumber)||((this.latestReleaseNumber!= null)&&this.latestReleaseNumber.equals(rhs.latestReleaseNumber)))&&((this.keywords == rhs.keywords)||((this.keywords!= null)&&this.keywords.equals(rhs.keywords))))&&((this.fundingUrls == rhs.fundingUrls)||((this.fundingUrls!= null)&&this.fundingUrls.equals(rhs.fundingUrls))))&&((this.description == rhs.description)||((this.description!= null)&&this.description.equals(rhs.description))))&&((this.language == rhs.language)||((this.language!= null)&&this.language.equals(rhs.language))))&&((this.repositoryLicense == rhs.repositoryLicense)||((this.repositoryLicense!= null)&&this.repositoryLicense.equals(rhs.repositoryLicense))))&&((this.platform == rhs.platform)||((this.platform!= null)&&this.platform.equals(rhs.platform))))&&((this.deprecationReason == rhs.deprecationReason)||((this.deprecationReason!= null)&&this.deprecationReason.equals(rhs.deprecationReason))))&&((this.latestReleasePublishedAt == rhs.latestReleasePublishedAt)||((this.latestReleasePublishedAt!= null)&&this.latestReleasePublishedAt.equals(rhs.latestReleasePublishedAt))))&&((this.contributionsCount == rhs.contributionsCount)||((this.contributionsCount!= null)&&this.contributionsCount.equals(rhs.contributionsCount))))&&((this.dependentsCount == rhs.dependentsCount)||((this.dependentsCount!= null)&&this.dependentsCount.equals(rhs.dependentsCount))))&&((this.latestStableReleasePublishedAt == rhs.latestStableReleasePublishedAt)||((this.latestStableReleasePublishedAt!= null)&&this.latestStableReleasePublishedAt.equals(rhs.latestStableReleasePublishedAt))))&&((this.rank == rhs.rank)||((this.rank!= null)&&this.rank.equals(rhs.rank))))&&((this.forks == rhs.forks)||((this.forks!= null)&&this.forks.equals(rhs.forks))))&&((this.securityPolicyUrl == rhs.securityPolicyUrl)||((this.securityPolicyUrl!= null)&&this.securityPolicyUrl.equals(rhs.securityPolicyUrl))))&&((this.dependentReposCount == rhs.dependentReposCount)||((this.dependentReposCount!= null)&&this.dependentReposCount.equals(rhs.dependentReposCount))))&&((this.latestStableReleaseNumber == rhs.latestStableReleaseNumber)||((this.latestStableReleaseNumber!= null)&&this.latestStableReleaseNumber.equals(rhs.latestStableReleaseNumber))))&&((this.licenseNormalized == rhs.licenseNormalized)||((this.licenseNormalized!= null)&&this.licenseNormalized.equals(rhs.licenseNormalized))))&&((this.repositoryStatus == rhs.repositoryStatus)||((this.repositoryStatus!= null)&&this.repositoryStatus.equals(rhs.repositoryStatus))))&&((this.stars == rhs.stars)||((this.stars!= null)&&this.stars.equals(rhs.stars))))&&((this.latestDownloadUrl == rhs.latestDownloadUrl)||((this.latestDownloadUrl!= null)&&this.latestDownloadUrl.equals(rhs.latestDownloadUrl))))&&((this.packageManagerUrl == rhs.packageManagerUrl)||((this.packageManagerUrl!= null)&&this.packageManagerUrl.equals(rhs.packageManagerUrl))))&&((this.repositoryUrl == rhs.repositoryUrl)||((this.repositoryUrl!= null)&&this.repositoryUrl.equals(rhs.repositoryUrl))))&&((this.licenses == rhs.licenses)||((this.licenses!= null)&&this.licenses.equals(rhs.licenses))))&&((this.contributionGuidelinesUrl == rhs.contributionGuidelinesUrl)||((this.contributionGuidelinesUrl!= null)&&this.contributionGuidelinesUrl.equals(rhs.contributionGuidelinesUrl))))&&((this.versions == rhs.versions)||((this.versions!= null)&&this.versions.equals(rhs.versions))))&&((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name))))&&((this.normalizedLicenses == rhs.normalizedLicenses)||((this.normalizedLicenses!= null)&&this.normalizedLicenses.equals(rhs.normalizedLicenses))))&&((this.codeOfConductUrl == rhs.codeOfConductUrl)||((this.codeOfConductUrl!= null)&&this.codeOfConductUrl.equals(rhs.codeOfConductUrl))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.homepage == rhs.homepage)||((this.homepage!= null)&&this.homepage.equals(rhs.homepage))))&&((this.status == rhs.status)||((this.status!= null)&&this.status.equals(rhs.status))));
    }

}
