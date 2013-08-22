package de.akuz.google.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.LowLevelHttpResponse;

public class OkHttpResponse extends LowLevelHttpResponse {

	private final HttpURLConnection connection;
	private final int responseCode;
	private final String responseMessage;
	private final ArrayList<String> headerNames = new ArrayList<String>();
	private final ArrayList<String> headerValues = new ArrayList<String>();

	OkHttpResponse(HttpURLConnection connection) throws IOException {
		this.connection = connection;
		int responseCode = connection.getResponseCode();
		this.responseCode = responseCode == -1 ? 0 : responseCode;
		responseMessage = connection.getResponseMessage();
		List<String> headerNames = this.headerNames;
		List<String> headerValues = this.headerValues;
		for (Map.Entry<String, List<String>> entry : connection
				.getHeaderFields().entrySet()) {
			String key = entry.getKey();
			if (key != null) {
				for (String value : entry.getValue()) {
					if (value != null) {
						headerNames.add(key);
						headerValues.add(value);
					}
				}
			}
		}
	}

	@Override
	public int getStatusCode() {
		return responseCode;
	}

	@Override
	public InputStream getContent() throws IOException {
		HttpURLConnection connection = this.connection;
		return HttpStatusCodes.isSuccess(responseCode) ? connection
				.getInputStream() : connection.getErrorStream();
	}

	@Override
	public String getContentEncoding() {
		return connection.getContentEncoding();
	}

	@Override
	public long getContentLength() {
		String string = connection.getHeaderField("Content-Length");
		return string == null ? -1 : Long.parseLong(string);
	}

	@Override
	public String getContentType() {
		return connection.getHeaderField("Content-Type");
	}

	@Override
	public String getReasonPhrase() {
		return responseMessage;
	}

	@Override
	public String getStatusLine() {
		String result = connection.getHeaderField(0);
		return result != null && result.startsWith("HTTP/1.") ? result : null;
	}

	@Override
	public int getHeaderCount() {
		return headerNames.size();
	}

	@Override
	public String getHeaderName(int index) {
		return headerNames.get(index);
	}

	@Override
	public String getHeaderValue(int index) {
		return headerValues.get(index);
	}

	/**
	 * Closes the connection to the HTTP server.
	 * 
	 */
	@Override
	public void disconnect() {
		connection.disconnect();
	}

}
