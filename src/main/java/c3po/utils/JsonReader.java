package c3po.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * From: http://stackoverflow.com/questions/4308554/simplest-way-to-read-json-from-a-url-in-java
 */
public class JsonReader {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonReader.class);
	public static boolean debug = false;

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException,
			JSONException {
		if (debug)
			LOGGER.debug("GET " + url);

		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			LOGGER.debug("Response: " + jsonText);

			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	/**
	 * The POST variant of the readJsonFromUrl method. POST is needed for the
	 * authenticated calls.
	 * 
	 * @param url
	 * @param params
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	public static String readJsonFromUrl(String url, List<NameValuePair> params) throws IOException, JSONException {
		if (debug)
			LOGGER.debug("POST " + url + " with params " + params);

		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(url);

		// Request parameters and other properties.
		httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

		// Execute and get the response.
		HttpResponse response = httpclient.execute(httppost);
		HttpEntity entity = response.getEntity();

		InputStream is = entity.getContent();
		String result;
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			result = readAll(rd);
			LOGGER.debug("Response: " + result);
		} finally {
			is.close();
			EntityUtils.consume(entity);
			httppost.releaseConnection();
		}

		return result;
	}
}