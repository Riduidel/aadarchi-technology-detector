package org.ndx.aadarchi.technology.detector.indicators.stackoverflow.api;

import static org.junit.jupiter.api.Assertions.*;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * We voluntary test the stack exchange client on a non-it stackexchange site 
 * (to avoid consuming our tokens bucket)
 */
@QuarkusTest
class StackExchangeClientTest {
	@RestClient StackExchangeClient client;

	@Test
	void can_get_list_of_tags() {
		// When
		StackExchangeList<Tag> tags = client.getTags("scifi", 0, 100, null, null);
		// Then
		SoftAssertions.assertSoftly(assertions -> {
			assertions.assertThat(tags.hasMore)
				.describedAs("There are more than 100 tags on scifi site")
				.isTrue();
			assertions.assertThat(tags.quota_max)
				.describedAs("max quota is standard one")
				.isEqualTo(10_000);
			assertions.assertThat(tags.quota_remaining)
				.describedAs("We should have some quota remaining")
				.isPositive();
			assertions.assertThat(tags.items)
				.describedAs("We got some tags")
				.isNotEmpty();
		});
	}

}
