package org.ndx.aadarchi.technology.detector.indicators.stackoverflow.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StackExchangeList<Type> {
	public List<Type> items;
	@JsonProperty("has_more")
	public boolean hasMore;
	@JsonProperty("quota_max") int quota_max;
	@JsonProperty("quota_remaining") int quota_remaining;
}
