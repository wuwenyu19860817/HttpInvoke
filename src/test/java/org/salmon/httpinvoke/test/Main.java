package org.salmon.httpinvoke.test;

import java.util.HashMap;
import java.util.Map;

import org.salmon.httpinvoke.client.impl.PoolingHttpClientImpl;

/**
 * 测试
 * 
 * @author salmon
 *
 */
public class Main {

	public static void main(String[] args) throws Exception {
		PoolingHttpClientImpl customHttpClient = new PoolingHttpClientImpl();

//		Map<String, String> dnsMap = new HashMap<String, String>();
//		dnsMap.put("www.baidu.com", "180.97.33.107");
//		dnsMap.put("www.sina.com", "202.102.75.147");
//		dnsMap.put("ddfsdfasdfasdf", "127.0.0.1");
//		dnsMap.put("www.wuwenyu.com", "123.57.3.169");
//		customHttpClient.setDnsMap(dnsMap); 
		customHttpClient.init();
		
		System.out.println(customHttpClient.get("https://www.baidu2.com"));

//		customHttpClient.get("http://www.baidu.com");
//		customHttpClient.get("http://www.sina.com");
//		customHttpClient.get("http://ddfsdfasdfasdf:8080");
//		System.out.println(customHttpClient.get("http://123.57.3.169"));
//		System.out.println(customHttpClient.getTotalPoolStats());
	}

}
