package org.ndx.aadarchi.technology.detector.npmjs.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DownloadCount {
	  public long downloads;
	  public String start;
	  public String end;
	  @JsonProperty("package")
	  public String packageName;
}