/**
 * JSocksProxy Copyright (c) 2006-2017 Kenny Colliander Nordin
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nu.najt.kecon.jsocksproxy.socks5;

import java.net.InetAddress;

/**
 * Object containing information regarding the end point that should be
 * connected
 * 
 * @author Kenny Colliander Nordin
 */
public final class EndPoint {

	private final AddressType addressType;

	private final byte[] hostname;

	private final InetAddress remoteInetAddress;

	private final String host;

	private final int port;

	public EndPoint(final AddressType addressType,
			final InetAddress remoteInetAddress, final byte[] hostname,
			final String host, final int port) {
		super();
		this.addressType = addressType;
		this.remoteInetAddress = remoteInetAddress;
		this.hostname = hostname;
		this.host = host;
		this.port = port;
	}

	public AddressType getAddressType() {
		return addressType;
	}

	public InetAddress getRemoteInetAddress() {
		return remoteInetAddress;
	}

	public byte[] getHostname() {
		return hostname;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}
}
