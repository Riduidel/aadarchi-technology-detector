
package com.zenika.tech.lab.ingester.librariesio.model;

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
    "number",
    "published_at",
    "spdx_expression",
    "original_license",
    "researched_at",
    "repository_sources"
})
@Generated("jsonschema2pojo")
public class Version {

    @JsonProperty("number")
    private String number;
    @JsonProperty("published_at")
    private String publishedAt;
    @JsonProperty("spdx_expression")
    private String spdxExpression;
    @JsonProperty("researched_at")
    private Object researchedAt;
    @JsonProperty("repository_sources")
    private List<String> repositorySources = new ArrayList<String>();
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("number")
    public String getNumber() {
        return number;
    }

    @JsonProperty("number")
    public void setNumber(String number) {
        this.number = number;
    }

    @JsonProperty("published_at")
    public String getPublishedAt() {
        return publishedAt;
    }

    @JsonProperty("published_at")
    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    @JsonProperty("spdx_expression")
    public String getSpdxExpression() {
        return spdxExpression;
    }

    @JsonProperty("spdx_expression")
    public void setSpdxExpression(String spdxExpression) {
        this.spdxExpression = spdxExpression;
    }

    @JsonProperty("researched_at")
    public Object getResearchedAt() {
        return researchedAt;
    }

    @JsonProperty("researched_at")
    public void setResearchedAt(Object researchedAt) {
        this.researchedAt = researchedAt;
    }

    @JsonProperty("repository_sources")
    public List<String> getRepositorySources() {
        return repositorySources;
    }

    @JsonProperty("repository_sources")
    public void setRepositorySources(List<String> repositorySources) {
        this.repositorySources = repositorySources;
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
        sb.append(Version.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("number");
        sb.append('=');
        sb.append(((this.number == null)?"<null>":this.number));
        sb.append(',');
        sb.append("publishedAt");
        sb.append('=');
        sb.append(((this.publishedAt == null)?"<null>":this.publishedAt));
        sb.append(',');
        sb.append("spdxExpression");
        sb.append('=');
        sb.append(((this.spdxExpression == null)?"<null>":this.spdxExpression));
        sb.append(',');
        sb.append("researchedAt");
        sb.append('=');
        sb.append(((this.researchedAt == null)?"<null>":this.researchedAt));
        sb.append(',');
        sb.append("repositorySources");
        sb.append('=');
        sb.append(((this.repositorySources == null)?"<null>":this.repositorySources));
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
        result = ((result* 31)+((this.number == null)? 0 :this.number.hashCode()));
        result = ((result* 31)+((this.spdxExpression == null)? 0 :this.spdxExpression.hashCode()));
        result = ((result* 31)+((this.publishedAt == null)? 0 :this.publishedAt.hashCode()));
        result = ((result* 31)+((this.repositorySources == null)? 0 :this.repositorySources.hashCode()));
        result = ((result* 31)+((this.researchedAt == null)? 0 :this.researchedAt.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Version) == false) {
            return false;
        }
        Version rhs = ((Version) other);
        return ((((((((this.number == rhs.number)||((this.number!= null)&&this.number.equals(rhs.number))))&&((this.spdxExpression == rhs.spdxExpression)||((this.spdxExpression!= null)&&this.spdxExpression.equals(rhs.spdxExpression))))&&((this.publishedAt == rhs.publishedAt)||((this.publishedAt!= null)&&this.publishedAt.equals(rhs.publishedAt))))&&((this.repositorySources == rhs.repositorySources)||((this.repositorySources!= null)&&this.repositorySources.equals(rhs.repositorySources))))&&((this.researchedAt == rhs.researchedAt)||((this.researchedAt!= null)&&this.researchedAt.equals(rhs.researchedAt))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

}
