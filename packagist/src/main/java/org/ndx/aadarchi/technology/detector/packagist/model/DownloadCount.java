package org.ndx.aadarchi.technology.detector.packagist.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DownloadCount {
    public long total;
    public long monthly;
    public long daily;
    @JsonProperty("package")
    public String packageName;
}
