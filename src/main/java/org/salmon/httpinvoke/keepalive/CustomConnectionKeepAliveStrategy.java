package org.salmon.httpinvoke.keepalive;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

/**
 * 通过解析响应头Keep-Alive 携带的timeout参数值  单个默认值
 * @author salmon
 *
 */
public class CustomConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {
	private long keepAliveDuration = 5 * 1000;
	
	public CustomConnectionKeepAliveStrategy() {
	}
	
	/**
	 * 
	 * @param keepAliveDuration 连接存活毫秒数
	 */
	public CustomConnectionKeepAliveStrategy(long keepAliveDuration) {
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
		
		
		return keepAliveDuration;
	}

	public void setKeepAliveDuration(long keepAliveDuration) {
		this.keepAliveDuration = keepAliveDuration;
	}
}
