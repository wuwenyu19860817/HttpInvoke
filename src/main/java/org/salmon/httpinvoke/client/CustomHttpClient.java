package org.salmon.httpinvoke.client;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;

/**
 * 自定义httpclient接口
 * @author salmon
 * 
 */
public interface CustomHttpClient {
	/**
	 * 
	 * @param uri
	 *            request address
	 * @param list
	 *            parameters
	 * @return the response object as generated by the response handler
	 */
	String post(String uri, List<NameValuePair> list);

	/**
	 * 
	 * @param uri
	 *            request address
	 * @param list
	 *            parameters
	 * @param headers
	 *            usage: new BasicHeader("pin","test");
	 * @return the response object as generated by the response handler
	 */
	String post(String uri, List<NameValuePair> list, List<Header> headers);

	/**
	 * 
	 * @param uri
	 *            the combination of address and parameters
	 * @return the response object as generated by the response handler
	 */
	String get(String uri);
	 

	/**
	 * 
	 * @param uri
	 *            the combination of address and parameters
	 * @param headers
	 *            usage: new BasicHeader("pin","test");
	 * @return the response object as generated by the response handler
	 */
	String get(String uri, List<Header> headers);

	/**
	 * Closes this stream and releases any system resources associated with it. If the stream is already closed then
	 * invoking this method has no effect.
	 */
	void closeHttpClient();

	/**
	 * 
	 * @return the number of persistent connections, including leased, pending, available, max.
	 */
	String getTotalPoolStats();
	
	/**
	 * 
	 * @param hostname  the hostname (IP or DNS name)
     * @param port      the port number.
	 * @return the number of persistent connections for specified httpRoute.
	 */
	String getPoolStats(String hostname, int port);
	
	/**
	 * 
	 * @param uri address
	 * @return inputStream
	 */
	InputStream getInputStream(String uri) throws ClientProtocolException, IOException;
}
