package de.akuz.google.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.Arrays;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.Beta;
import com.google.api.client.util.Preconditions;
import com.google.api.client.util.SecurityUtils;
import com.google.api.client.util.SslUtils;
import com.squareup.okhttp.OkHttpClient;

/**
 * I shamelessly copied most if this source code from the NetHttpTransport, but
 * I just wanted to replace the HttpUrlConnection with OkHttp.
 * 
 * @author Till Klocke
 * 
 */
public class OkHttpTransport extends HttpTransport {

	/**
	 * The OkHttpClient to use. Can be set via builder or a new default instance
	 * will be created.
	 */
	private OkHttpClient okClient;

	/**
	 * All valid request methods as specified in
	 * {@link HttpURLConnection#setRequestMethod}, sorted in ascending
	 * alphabetical order.
	 */
	private static final String[] SUPPORTED_METHODS = { HttpMethods.DELETE,
			HttpMethods.GET, HttpMethods.HEAD, HttpMethods.OPTIONS,
			HttpMethods.POST, HttpMethods.PUT, HttpMethods.TRACE };
	static {
		Arrays.sort(SUPPORTED_METHODS);
	}

	/**
	 * HTTP proxy or {@code null} to use the proxy settings from <a href=
	 * "http://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html"
	 * >system properties</a>.
	 */
	private final Proxy proxy;

	/** SSL socket factory or {@code null} for the default. */
	private final SSLSocketFactory sslSocketFactory;

	/** Host name verifier or {@code null} for the default. */
	private final HostnameVerifier hostnameVerifier;

	/**
	 * Constructor with the default behavior.
	 * 
	 * <p>
	 * Instead use {@link Builder} to modify behavior.
	 * </p>
	 */
	public OkHttpTransport() {
		this(new OkHttpClient(), null, null, null);
	}

	/**
	 * @param proxy
	 *            HTTP proxy or {@code null} to use the proxy settings from <a
	 *            href=
	 *            "http://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html"
	 *            > system properties</a>
	 * @param sslSocketFactory
	 *            SSL socket factory or {@code null} for the default
	 * @param hostnameVerifier
	 *            host name verifier or {@code null} for the default
	 */
	OkHttpTransport(OkHttpClient okClient, Proxy proxy,
			SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier) {
		if (okClient != null) {
			this.okClient = okClient;
		} else {
			this.okClient = new OkHttpClient();
		}
		this.proxy = proxy;
		this.sslSocketFactory = sslSocketFactory;
		this.hostnameVerifier = hostnameVerifier;
	}

	@Override
	public boolean supportsMethod(String method) {
		return Arrays.binarySearch(SUPPORTED_METHODS, method) >= 0;
	}

	@Override
	protected OkHttpRequest buildRequest(String method, String url)
			throws IOException {
		Preconditions.checkArgument(supportsMethod(method),
				"HTTP method %s not supported", method);
		// connection with proxy settings
		URL connUrl = new URL(url);
		if (proxy != null) {
			okClient.setProxy(proxy);
		}
		URLConnection conn = okClient.open(connUrl);
		HttpURLConnection connection = (HttpURLConnection) conn;
		connection.setRequestMethod(method);
		// SSL settings
		if (connection instanceof HttpsURLConnection) {
			HttpsURLConnection secureConnection = (HttpsURLConnection) connection;

			if (hostnameVerifier != null) {
				secureConnection.setHostnameVerifier(hostnameVerifier);
			}
			if (sslSocketFactory != null) {
				secureConnection.setSSLSocketFactory(sslSocketFactory);
			}
		}
		return new OkHttpRequest(connection);
	}

	public OkHttpClient getOkHttpClient() {
		return okClient;
	}

	/**
	 * Builder for {@link OkHttpTransport}.
	 * 
	 * <p>
	 * Implementation is not thread-safe.
	 * </p>
	 * 
	 */
	public static final class Builder {

		/** SSL socket factory or {@code null} for the default. */
		private SSLSocketFactory sslSocketFactory;

		/** Host name verifier or {@code null} for the default. */
		private HostnameVerifier hostnameVerifier;

		/**
		 * HTTP proxy or {@code null} to use the proxy settings from <a href=
		 * "http://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html"
		 * >system properties</a>.
		 */
		private Proxy proxy;

		/**
		 * The OkHttpClient to use for building the OkHttpTransport. May be
		 * null, then a default OkHttpClient will be used.
		 */
		private OkHttpClient okClient;

		/**
		 * Sets the HTTP proxy or {@code null} to use the proxy settings from <a
		 * href=
		 * "http://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html"
		 * >system properties</a>.
		 * 
		 * <p>
		 * For example:
		 * </p>
		 * 
		 * <pre>
		 * setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(&quot;127.0.0.1&quot;, 8080)))
		 * </pre>
		 */
		public Builder setProxy(Proxy proxy) {
			this.proxy = proxy;
			return this;
		}

		/**
		 * Sets the SSL socket factory based on root certificates in a Java
		 * KeyStore.
		 * 
		 * <p>
		 * Example usage:
		 * </p>
		 * 
		 * <pre>
		 * trustCertificatesFromJavaKeyStore(new FileInputStream(&quot;certs.jks&quot;), &quot;password&quot;);
		 * </pre>
		 * 
		 * @param keyStoreStream
		 *            input stream to the key store (closed at the end of this
		 *            method in a finally block)
		 * @param storePass
		 *            password protecting the key store file
		 * @since 1.14
		 */
		public Builder trustCertificatesFromJavaKeyStore(
				InputStream keyStoreStream, String storePass)
				throws GeneralSecurityException, IOException {
			KeyStore trustStore = SecurityUtils.getJavaKeyStore();
			SecurityUtils.loadKeyStore(trustStore, keyStoreStream, storePass);
			return trustCertificates(trustStore);
		}

		/**
		 * Sets the SSL socket factory based root certificates generated from
		 * the specified stream using
		 * {@link CertificateFactory#generateCertificates(InputStream)}.
		 * 
		 * <p>
		 * Example usage:
		 * </p>
		 * 
		 * <pre>
		 * trustCertificatesFromStream(new FileInputStream(&quot;certs.pem&quot;));
		 * </pre>
		 * 
		 * @param certificateStream
		 *            certificate stream
		 * @since 1.14
		 */
		public Builder trustCertificatesFromStream(InputStream certificateStream)
				throws GeneralSecurityException, IOException {
			KeyStore trustStore = SecurityUtils.getJavaKeyStore();
			trustStore.load(null, null);
			SecurityUtils.loadKeyStoreFromCertificates(trustStore,
					SecurityUtils.getX509CertificateFactory(),
					certificateStream);
			return trustCertificates(trustStore);
		}

		/**
		 * Sets the SSL socket factory based on a root certificate trust store.
		 * 
		 * @param trustStore
		 *            certificate trust store (use for example
		 *            {@link SecurityUtils#loadKeyStore} or
		 *            {@link SecurityUtils#loadKeyStoreFromCertificates})
		 * @since 1.14
		 */
		public Builder trustCertificates(KeyStore trustStore)
				throws GeneralSecurityException {
			SSLContext sslContext = SslUtils.getTlsSslContext();
			SslUtils.initSslContext(sslContext, trustStore,
					SslUtils.getPkixTrustManagerFactory());
			return setSslSocketFactory(sslContext.getSocketFactory());
		}

		/**
		 * {@link Beta} <br/>
		 * Disables validating server SSL certificates by setting the SSL socket
		 * factory using {@link SslUtils#trustAllSSLContext()} for the SSL
		 * context and {@link SslUtils#trustAllHostnameVerifier()} for the host
		 * name verifier.
		 * 
		 * <p>
		 * Be careful! Disabling certificate validation is dangerous and should
		 * only be done in testing environments.
		 * </p>
		 */
		@Beta
		public Builder doNotValidateCertificate()
				throws GeneralSecurityException {
			hostnameVerifier = SslUtils.trustAllHostnameVerifier();
			sslSocketFactory = SslUtils.trustAllSSLContext().getSocketFactory();
			return this;
		}

		/** Returns the SSL socket factory. */
		public SSLSocketFactory getSslSocketFactory() {
			return sslSocketFactory;
		}

		/** Sets the SSL socket factory or {@code null} for the default. */
		public Builder setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
			this.sslSocketFactory = sslSocketFactory;
			return this;
		}

		/** Returns the host name verifier or {@code null} for the default. */
		public HostnameVerifier getHostnameVerifier() {
			return hostnameVerifier;
		}

		/** Sets the host name verifier or {@code null} for the default. */
		public Builder setHostnameVerifier(HostnameVerifier hostnameVerifier) {
			this.hostnameVerifier = hostnameVerifier;
			return this;
		}

		/**
		 * Set the OkHttpClient to use, if you don't want to use an OkHttpClient
		 * with default configuration.
		 * 
		 * @param okClient
		 * @return
		 */
		public Builder setOkHttpClient(OkHttpClient okClient) {
			this.okClient = okClient;
			return this;
		}

		/**
		 * Returns a new instance of {@link NetHttpTransport} based on the
		 * options.
		 */
		public OkHttpTransport build() {
			return new OkHttpTransport(okClient, proxy, sslSocketFactory,
					hostnameVerifier);
		}
	}

}
