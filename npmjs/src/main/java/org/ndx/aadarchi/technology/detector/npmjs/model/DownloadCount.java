package org.ndx.aadarchi.technology.detector.npmjs.model;

import com.google.gson.annotations.SerializedName;

public class DownloadCount {
	  public long downloads;
	  public String start;
	  public String end;
	  @SerializedName("package")
	  public String packageName;
}