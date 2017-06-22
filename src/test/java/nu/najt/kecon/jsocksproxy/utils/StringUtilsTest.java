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
package nu.najt.kecon.jsocksproxy.utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.*;
import org.junit.Test;

public class StringUtilsTest extends StringUtils {

	private static final String IP_10_123_1_54_80 = "10.123.1.54:80";

	@Test
	public void testFormatSocketAddress() throws Exception {

		final InetAddress inetAddress = InetAddress
				.getByAddress(new byte[] { 10, 123, 1, 54 });
		final InetSocketAddress inetSocketAddress = new InetSocketAddress(
				inetAddress, 80);

		assertEquals(IP_10_123_1_54_80,
				StringUtils.formatSocketAddress(inetSocketAddress));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFormatSocketAddressNulö() throws Exception {
		StringUtils.formatSocketAddress(null);
	}

	@Test
	public void testFormatSocketAddressIPv6() throws Exception {
		final InetAddress inetAddress = InetAddress
				.getByName("1080::8:800:200C:417A");

		final InetSocketAddress inetSocketAddress = new InetSocketAddress(
				inetAddress, 80);

		assertEquals("[1080:0:0:0:8:800:200c:417a]:80",
				StringUtils.formatSocketAddress(inetSocketAddress));
	}

	@Test
	public void testFormatSocketSocket() throws Exception {

		final InetAddress inetAddress = InetAddress
				.getByAddress(new byte[] { 10, 123, 1, 54 });

		final Socket socket = new Socket() {

			@Override
			public InetAddress getInetAddress() {
				return inetAddress;
			}

			@Override
			public int getPort() {
				return 80;
			}

		};

		assertEquals(IP_10_123_1_54_80, StringUtils.formatSocket(socket));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFormatSocketSocketNull() throws Exception {
		StringUtils.formatSocket((Socket) null);
	}

	@Test
	public void testFormatSocketSocketIPv6() throws Exception {
		final InetAddress inetAddress = InetAddress
				.getByName("1080::8:800:200C:417A");

		final Socket socket = new Socket() {

			@Override
			public InetAddress getInetAddress() {
				return inetAddress;
			}

			@Override
			public int getLocalPort() {
				return 80;
			}

		};

		assertEquals("[1080:0:0:0:8:800:200c:417a]:0",
				StringUtils.formatSocket(socket));
	}

	@Test
	public void testFormatSocketServerSocket() throws Exception {
		final InetAddress inetAddress = InetAddress
				.getByAddress(new byte[] { 10, 123, 1, 54 });

		final ServerSocket socket = new ServerSocket() {

			@Override
			public InetAddress getInetAddress() {
				return inetAddress;
			}

			@Override
			public int getLocalPort() {
				return 80;
			}

		};

		assertEquals(IP_10_123_1_54_80, StringUtils.formatSocket(socket));
	}

	@Test
	public void testFormatSocketServerSocketIPv6() throws Exception {
		final InetAddress inetAddress = InetAddress
				.getByName("1080::8:800:200C:417A");

		final ServerSocket socket = new ServerSocket() {

			@Override
			public InetAddress getInetAddress() {
				return inetAddress;
			}

			@Override
			public int getLocalPort() {
				return 80;
			}

		};

		assertEquals("[1080:0:0:0:8:800:200c:417a]:80",
				StringUtils.formatSocket(socket));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFormatSocketServerSocketNull() throws Exception {
		StringUtils.formatSocket((ServerSocket) null);
	}
	/// _----

	@Test
	public void testFormatLocalSocketSocket() throws Exception {

		final InetAddress inetAddress = InetAddress
				.getByAddress(new byte[] { 10, 123, 1, 54 });

		final Socket socket = new Socket() {

			@Override
			public InetAddress getLocalAddress() {
				return inetAddress;
			}

			@Override
			public int getLocalPort() {
				return 80;
			}

		};

		assertEquals(IP_10_123_1_54_80, StringUtils.formatLocalSocket(socket));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFormatLocalSocketSocketNull() throws Exception {
		StringUtils.formatLocalSocket((Socket) null);
	}

	@Test
	public void testFormatLocalSocketSocketIPv6() throws Exception {
		final InetAddress inetAddress = InetAddress
				.getByName("1080::8:800:200C:417A");

		final Socket socket = new Socket() {

			@Override
			public InetAddress getLocalAddress() {
				return inetAddress;
			}

			@Override
			public int getLocalPort() {
				return 80;
			}

		};

		assertEquals("[1080:0:0:0:8:800:200c:417a]:80",
				StringUtils.formatLocalSocket(socket));
	}
}
