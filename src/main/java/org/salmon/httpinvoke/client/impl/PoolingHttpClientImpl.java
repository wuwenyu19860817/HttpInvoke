package org.salmon.httpinvoke.client.impl;


import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.apache.http.util.EntityUtils;
import org.salmon.httpinvoke.client.CustomHttpClient;
import org.salmon.httpinvoke.dns.CustomDynamicDnsResolver;
import org.salmon.httpinvoke.evict.IdleConnectionEvictThread;
import org.salmon.httpinvoke.factory.DaemonThreadFactory;
import org.salmon.httpinvoke.keepalive.CustomConnectionKeepAliveStrategy;

/**
 * 自定义配置 复用连接
 * 
 * @author salmon
 * 
 */
public class PoolingHttpClientImpl implements CustomHttpClient {
	private static Log log = LogFactory.getLog(PoolingHttpClientImpl.class);
	private CloseableHttpClient httpClient;
	private ResponseHandler<String> responseHandler;
	private PoolingHttpClientConnectionManager poolingHttpClientConnectionManager;
	private Map<String, String> dnsMap;

    /** TLS or SSL */
    private String protocol;
    /** the path of KeyStore which used to create SSL connections */
    private String keyStorePath;
    /** password of KeyStore */
    private String keyStorePwd;
    /** type of KeyStore */
    private String keyStoreType;

	/** max connections per route */
	private int defaultMaxPerRoute = 2 * Runtime.getRuntime().availableProcessors();
	/** max connections in all */
	private int maxTotal = 10 * 2 * Runtime.getRuntime().availableProcessors();
	/** the idle time(ms) of connections to be closed */
	private long idleTime = 5 * 1000;
	/** time to live */
	private long keepAliveDuration = 5 * 1000;
	/** interval time to evict connection */
	private long evictDelay = 5 * 1000;
	/** time to connect */
	private int connectTimeout = 5 * 1000;
	/**
	 * 'http.socket.timeout': defines the socket timeout (SO_TIMEOUT) in milliseconds, which is the timeout for waiting
	 * for data or, put differently, a maximum period inactivity between two consecutive data packets). A timeout value
	 * of zero is interpreted as an infinite timeout. This parameter expects a value of type java.lang.Integer. If this
	 * parameter is not set read operations will not time out (infinite timeout).
	 */
	private int socketTimeout = 5 * 1000;
	
	private ScheduledExecutorService schedule = null;
	
	private ScheduledFuture<?> future = null;

	public void init() throws Exception {
		responseHandler = new ResponseHandler<String>() {
			public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
				int status = response.getStatusLine().getStatusCode();
				System.out.println("status=>"+status);
				if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) {
					HttpEntity entity = response.getEntity();
					
					Charset defaultCharset = Charset.forName("UTF-8");;
					
					return entity != null ? EntityUtils.toString(entity, defaultCharset) : null;
				} else {
					throw new ClientProtocolException("Unexpected response status: " + status);
				}
			}
		};

		CustomDynamicDnsResolver dnsResolver = CustomDynamicDnsResolver.INSTANCE;
		if (dnsMap != null && !dnsMap.isEmpty()) {
			Iterator<Entry<String, String>> iterator = dnsMap.entrySet().iterator();
			try {
				while (iterator.hasNext()) {
					Entry<String, String> entry = iterator.next();
					String key = entry.getKey();
					String value = entry.getValue();
					dnsResolver.add(key, value);
				}
			} catch (UnknownHostException e) {
				log.error("dns init UnknownHostException, " + e);
			} catch (Exception e) {
				log.error("dns init Exception, " + e);
			}
		}

        SSLConnectionSocketFactory sslsf;
        if (keyStorePath == null || keyStorePath.length() == 0) {
            sslsf = SSLConnectionSocketFactory.getSocketFactory();
        } else {
            if (keyStoreType == null || keyStoreType.length() == 0) {
                keyStoreType = KeyStore.getDefaultType();
            }
            KeyStore myTrustStore = KeyStore.getInstance(keyStoreType);
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(keyStorePath);
            if (keyStorePwd == null) {
            	keyStorePwd = "";
            }
            myTrustStore.load(inputStream, keyStorePwd.toCharArray());
            SSLContext sslContext = SSLContexts.custom()
                    .useProtocol(protocol)
                    .loadTrustMaterial(myTrustStore)
                    .build();
            sslsf = new SSLConnectionSocketFactory(sslContext);
        }

        poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(
				RegistryBuilder.<ConnectionSocketFactory> create()
						.register("http", PlainConnectionSocketFactory.getSocketFactory())
						.register("https", sslsf).build(), null, dnsResolver);
		poolingHttpClientConnectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
		poolingHttpClientConnectionManager.setMaxTotal(maxTotal);

		IdleConnectionEvictThread idleConnectionMonitor = new IdleConnectionEvictThread(
				poolingHttpClientConnectionManager, idleTime);
		

		ConnectionKeepAliveStrategy connectionKeepAliveStrategy = new CustomConnectionKeepAliveStrategy(
				keepAliveDuration);

		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectTimeout)
				.setSocketTimeout(socketTimeout).build();

		
		schedule = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
		future = schedule.scheduleWithFixedDelay(idleConnectionMonitor, 0, evictDelay, TimeUnit.MILLISECONDS);

		httpClient = HttpClients.custom().setKeepAliveStrategy(connectionKeepAliveStrategy).setConnectionManager(poolingHttpClientConnectionManager)
				.setDefaultRequestConfig(requestConfig).build();
		 

	} 

	public String post(String uri, List<NameValuePair> list) {
		return post(uri, list, null);
	}
	
	public String post(String uri, List<NameValuePair> list, List<Header> headers) {
		String responseBody = null;

		HttpPost httpPost = new HttpPost(uri);
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, Consts.UTF_8);
		httpPost.setEntity(entity);
		if (headers != null) {
			for (Header header : headers) {
				httpPost.addHeader(header);
			}
		}

		try {
			responseBody = httpClient.execute(httpPost, responseHandler);
		} catch (ClientProtocolException e) {
			log.error("pooling post() method executing ClientProtocolException:" + e);
		} catch (IOException e) {
			log.error("pooling post() method executing IOException:" + e);
		} catch (Exception e) {
			log.error("pooling post() method executing Exception:" + e);
		}

		return responseBody;
	}

	
	public String get(String uri) {
		return get(uri, null);
	}
	
	public String get(String uri, List<Header> headers) {
		String responseBody = null;
		HttpGet httpGet = new HttpGet(uri); 
		if (headers != null) {
			for (Header header : headers) {
				httpGet.addHeader(header);
			}
		}
		try {
			responseBody = httpClient.execute(httpGet, responseHandler);
		} catch (ClientProtocolException e) {
			log.error("pooling get() method executing ClientProtocolException:" + e);
		} catch (IOException e) {
			log.error("pooling get() method executing IOException:" + e);
		} catch (Exception e) {
			log.error("pooling get() method executing Exception:" + e);
		}
		return responseBody;
	}
	
	


	public InputStream getInputStream(String uri) throws ClientProtocolException, IOException {
		HttpGet httpGet = new HttpGet(uri);
		HttpResponse response = httpClient.execute(httpGet);
		int status = response.getStatusLine().getStatusCode();
		if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) { 
			HttpEntity entity = response.getEntity();
			return entity.getContent();
		}
		return null;
	}

	public void closeHttpClient() {
		try{
			future.cancel(true);
			schedule.shutdown();
		}catch(Exception e){
			log.error("shutdown ScheduledExecutorService error., " + e);
		}
		
		try {
			httpClient.close();
		} catch (IOException e) {
			log.error("pooling httpclient shutdown IOException, " + e);
		} catch (Exception e) {
			log.error("pooling httpclient shutdown Exception, " + e);
		}
	}
	
	

	public String getTotalPoolStats() {
		PoolStats stats = poolingHttpClientConnectionManager.getTotalStats();
		return stats.toString();
	}
	
	public String getPoolStats(String hostname, int port) {
		PoolStats stats = poolingHttpClientConnectionManager.getStats(new HttpRoute(new HttpHost(hostname, port)));
		return stats.toString();
	}

	public void setDefaultMaxPerRoute(int defaultMaxPerRoute) {
		this.defaultMaxPerRoute = defaultMaxPerRoute;
	}

	public void setMaxTotal(int maxTotal) {
		this.maxTotal = maxTotal;
	}

	public void setIdleTime(long idleTime) {
		this.idleTime = idleTime;
	}

	public void setDnsMap(Map<String, String> dnsMap) {
		this.dnsMap = dnsMap;
	}

	public void setKeepAliveDuration(long keepAliveDuration) {
		this.keepAliveDuration = keepAliveDuration;
	}

	public void setEvictDelay(long evictDelay) {
		this.evictDelay = evictDelay;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public void setKeyStorePwd(String keyStorePwd) {
        this.keyStorePwd = keyStorePwd;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
