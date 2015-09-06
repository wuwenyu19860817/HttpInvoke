package org.salmon.httpinvoke.keepalive;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

/**
 * 通过解析响应头Keep-Alive 携带的timeout参数值  多个默认值
 * @author salmon
 *
 */
public class RouteConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {
	private long keepAliveDuration = 5 * 1000;
	private Map<String, Long> map = new HashMap<String, Long>();
	
	public RouteConnectionKeepAliveStrategy() {
	}
	
	/**
	 * 初始化默认连接存活时间
	 * @param keepAliveDuration 连接存活毫秒数
	 */
	public RouteConnectionKeepAliveStrategy(long keepAliveDuration) {
		this.keepAliveDuration = keepAliveDuration;
	}

	public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
		final HeaderElementIterator it = new BasicHeaderElementIterator(
				response.headerIterator(HTTP.CONN_KEEP_ALIVE));
		while (it.hasNext()) {
			final HeaderElement he = it.nextElement();
			final String param = he.getName();
			final String value = he.getValue();
			if (value != null && param.equalsIgnoreCase("timeout")) {
				try {
					return Long.parseLong(value) * 1000;
				} catch (final NumberFormatException ignore) {
					return keepAliveDuration;
				}
			}
		}
		
		HttpHost target = (HttpHost) context.getAttribute(HttpClientContext.HTTP_TARGET_HOST);
		String hostName = target.getHostName().toLowerCase();
		Long value = map.get(hostName + target.getPort());
		return value != null ? value : keepAliveDuration;
		
	}

	public void setKeepAliveDuration(long keepAliveDuration) {
		this.keepAliveDuration = keepAliveDuration;
	}
	
	public void setSpecificKeepAliveDuration(HttpHost httpHost, long keepAliveDuration) {
		String hostName = httpHost.getHostName().toLowerCase();
		map.put(hostName + httpHost.getPort(), keepAliveDuration);
	}
	
}
