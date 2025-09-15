package com.zenika.tech.lab.ingester.indicators.downloads;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.builder.endpoint.dsl.DirectEndpointBuilderFactory.DirectEndpointBuilder;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;

import com.zenika.tech.lab.ingester.indicators.IndicatorComputer;
import com.zenika.tech.lab.ingester.model.Technology;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class DownloadCountIndicatorComputer extends EndpointRouteBuilder implements IndicatorComputer {
	public static final String DOWNLOAD_COUNT = "download.count";
	private static final String ROUTE_NAME = "compute-"+DOWNLOAD_COUNT.replace('.', '-');
	public static final String DOMAIN_EXTRACTOR = "http(s)?://|www\\.|/.*";
	
	Map<String, DownloadCounterForPackageManager> urlsToDownloaders;
	
	@Inject
	public void setDownloadCounters(Instance<DownloadCounterForPackageManager> downloadCounters) {
		urlsToDownloaders = downloadCounters.stream()
				.collect(Collectors.toMap(
						d ->  d.getPackageManagerUrl().replaceAll(DOMAIN_EXTRACTOR, ""), Function.identity()));
	}
	
	private DirectEndpointBuilder getFromRoute() {
		return direct(ROUTE_NAME);
	}

	@Override
	public String getFromRouteName() {
		return getFromRoute().getUri();
	}

	@Override
	public boolean canCompute(Technology technology) {
		return urlsToDownloaders.containsKey(technology.packageManagerUrl.replaceAll(DOMAIN_EXTRACTOR, ""));
	}

	@Override
	public void configure() throws Exception {
		from(getFromRoute())
			.routeId(ROUTE_NAME)
			.idempotentConsumer()
				.body(Technology.class, t -> String.format("%s-%s", DOWNLOAD_COUNT, t.packageManagerUrl))
				.idempotentRepository(MemoryIdempotentRepository.memoryIdempotentRepository(10*2))
			.process(this::countDownloads)
			.end()
		;
	}

	private void countDownloads(Exchange exchange1) {
		countDownloads(exchange1.getMessage().getBody(Technology.class));
	}

	public void countDownloads(Technology body) {
		String key = body.packageManagerUrl.replaceAll(DOMAIN_EXTRACTOR, "");
		if(urlsToDownloaders.containsKey(key)) {
			urlsToDownloaders.get(key).countDownloadsOf(body);
		}
	}

}
