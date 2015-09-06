package org.salmon.httpinvoke.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.conn.DnsResolver;

/**
 * It provides add() method for customers to add dynamic hosts.
 * @author salmon
 *
 */
public class CustomDynamicDnsResolver implements DnsResolver {

	public static final CustomDynamicDnsResolver INSTANCE = new CustomDynamicDnsResolver();

	/**
	 * In-memory collection that will hold the associations between a host name and an array of InetAddress instances.
	 */
	private final Map<String, InetAddress[]> dnsMap;

	/**
	 * Builds a DNS resolver that will resolve the host names against a collection held in-memory.
	 */
	public CustomDynamicDnsResolver() {
		dnsMap = new ConcurrentHashMap<String, InetAddress[]>();
	}

	/**
	 * Associates the given array of IP addresses to the given host in this DNS overrider. The IP addresses are assumed
	 * to be already resolved.
	 * 
	 * @param host
	 *            The host name to be associated with the given IP.
	 * @param ips
	 *            array of IP addresses to be resolved by this DNS overrider to the given host name.
	 */
	public void add(final String host, final InetAddress... ips) {
		dnsMap.put(host, ips);
	}

	public void add(final String host, final String... ips) throws UnknownHostException {
		int length = ips.length;
		InetAddress[] addresses = new InetAddress[length];
		for (int i = 0; i < length; i++) {
			if (ips[i] == null) {
				continue;
			}
			addresses[i] = getByAdress(ips[i]);
		}
		add(host, addresses);
	}

	private InetAddress getByAdress(String ip) throws UnknownHostException {
		String[] array = ip.split("\\.");
		int length = array.length;
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; i++) {
			int tem = Integer.parseInt(array[i]);
			byte b = (byte) tem;
			bytes[i] = b;
		}

		return InetAddress.getByAddress(bytes);

	}

	public InetAddress[] resolve(String host) throws UnknownHostException {
		final InetAddress[] resolvedAddresses = dnsMap.get(host);
		if (resolvedAddresses != null) {
			return resolvedAddresses;
		}
		return InetAddress.getAllByName(host);
	}

}
