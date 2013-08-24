package de.akuz.google.api;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;

public class OkHttpTransportTest {

	@Test
	public void testBuilder() throws Exception {
		OkHttpTransport.Builder builder = new OkHttpTransport.Builder();
		assertNull(builder.getHostnameVerifier());
		assertNull(builder.getSslSocketFactory());

		OkHttpTransport transport = builder.build();
		assertNotNull(transport.getOkHttpClient());
		assertNotNull(transport.createRequestFactory());
	}

	@Test
	public void testRequestTransport() throws Exception {
		OkHttpTransport.Builder builder = new OkHttpTransport.Builder();
		HttpRequest request = builder.build().createRequestFactory()
				.buildGetRequest(new GenericUrl("http://example.com"));
		assertTrue(OkHttpTransport.class.equals(request.getTransport()
				.getClass()));
	}
}
