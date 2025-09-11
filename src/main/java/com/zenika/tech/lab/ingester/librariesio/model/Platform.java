
package com.zenika.tech.lab.ingester.librariesio.model;

import java.util.LinkedHashMap;
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
    "name",
    "project_count",
    "homepage",
    "color",
    "default_language"
})
@Generated("jsonschema2pojo")
public class Platform {

    @JsonProperty("name")
    private String name;
    @JsonProperty("project_count")
    private Integer projectCount;
    @JsonProperty("homepage")
    private String homepage;
    @JsonProperty("color")
    private String color;
    @JsonProperty("default_language")
    private String defaultLanguage;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("project_count")
    public Integer getProjectCount() {
        return projectCount;
    }

    @JsonProperty("project_count")
    public void setProjectCount(Integer projectCount) {
        this.projectCount = projectCount;
    }

    @JsonProperty("homepage")
    public String getHomepage() {
        return homepage;
    }

    @JsonProperty("homepage")
    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    @JsonProperty("color")
    public String getColor() {
        return color;
    }

    @JsonProperty("color")
    public void setColor(String color) {
        this.color = color;
    }

    @JsonProperty("default_language")
    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    @JsonProperty("default_language")
    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
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
        sb.append(Platform.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("projectCount");
        sb.append('=');
        sb.append(((this.projectCount == null)?"<null>":this.projectCount));
        sb.append(',');
        sb.append("homepage");
        sb.append('=');
        sb.append(((this.homepage == null)?"<null>":this.homepage));
        sb.append(',');
        sb.append("color");
        sb.append('=');
        sb.append(((this.color == null)?"<null>":this.color));
        sb.append(',');
        sb.append("defaultLanguage");
        sb.append('=');
        sb.append(((this.defaultLanguage == null)?"<null>":this.defaultLanguage));
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
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.projectCount == null)? 0 :this.projectCount.hashCode()));
        result = ((result* 31)+((this.defaultLanguage == null)? 0 :this.defaultLanguage.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.color == null)? 0 :this.color.hashCode()));
        result = ((result* 31)+((this.homepage == null)? 0 :this.homepage.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Platform) == false) {
            return false;
        }
        Platform rhs = ((Platform) other);
        return (((((((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name)))&&((this.projectCount == rhs.projectCount)||((this.projectCount!= null)&&this.projectCount.equals(rhs.projectCount))))&&((this.defaultLanguage == rhs.defaultLanguage)||((this.defaultLanguage!= null)&&this.defaultLanguage.equals(rhs.defaultLanguage))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.color == rhs.color)||((this.color!= null)&&this.color.equals(rhs.color))))&&((this.homepage == rhs.homepage)||((this.homepage!= null)&&this.homepage.equals(rhs.homepage))));
    }

}
