/**
 * JSocksProxy Copyright (c) 2006-2012 Kenny Colliander Nordin
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

import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTest extends StringUtils {

	@Test
	public void testFormatSocketAddress() throws Exception {

		final InetAddress inetAddress = InetAddress.getByAddress(new byte[] {
				10, 123, 1, 54 });
		final InetSocketAddress inetSocketAddress = new InetSocketAddress(
				inetAddress, 80);

		Assert.assertEquals("10.123.1.54:80",
				StringUtils.formatSocketAddress(inetSocketAddress));
	}

	@Test
	public void testFormatSocketSocket() throws Exception {

		final InetAddress inetAddress = InetAddress.getByAddress(new byte[] {
				10, 123, 1, 54 });

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

		Assert.assertEquals("10.123.1.54:80", StringUtils.formatSocket(socket));
	}

	@Test
	public void testFormatSocketServerSocket() throws Exception {
		final InetAddress inetAddress = InetAddress.getByAddress(new byte[] {
				10, 123, 1, 54 });

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

		Assert.assertEquals("10.123.1.54:80", StringUtils.formatSocket(socket));
	}
}
