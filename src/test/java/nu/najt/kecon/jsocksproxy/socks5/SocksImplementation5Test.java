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
package nu.najt.kecon.jsocksproxy.socks5;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import nu.najt.kecon.jsocksproxy.ConfigurationFacade;

import org.junit.Assert;
import org.junit.Test;

public class SocksImplementation5Test {

	@Test
	public void testRunSuccess() throws Exception {

		final CountDownLatch countDownLatch = new CountDownLatch(2);
		final AtomicInteger count = new AtomicInteger(0);
		final Executor executor = Executors.newCachedThreadPool();
		final InetAddress serverAddress = InetAddress.getByAddress(new byte[] {
				11, 12, 13, 14 });
		final InetAddress listeningAddress = InetAddress
				.getByAddress(new byte[] { 22, 33, 44, 55 });
		final InetAddress clientAddress = InetAddress.getByAddress(new byte[] {
				55, 66, 77, 88 });
		final int serverPort = 80;
		final ByteArrayOutputStream request = new ByteArrayOutputStream();

		/* starting 0x05 is stripped before this */
		final byte[] request1 = new byte[] { 1, 0 };
		final byte[] response1 = new byte[] { 0x05, 0x00 };
		final byte[] request2 = new byte[] { 5, 1, 0, 1, 11, 12, 13, 14, 0, 80 };
		final byte[] response2 = new byte[] { 0x05, 0x00, 0x00, 0x01, 0, 0, 0,
				0, -1, -1 };

		request.write(request1);
		request.write(request2);

		final ByteArrayOutputStream clientOutputStream = new ByteArrayOutputStream();
		final ByteArrayOutputStream serverResponseStream = new ByteArrayOutputStream();

		serverResponseStream.write(response1);
		serverResponseStream.write(response2);

		final Socket clientSocket = new Socket() {
			final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
					request.toByteArray());

			final AtomicBoolean close = new AtomicBoolean(Boolean.TRUE);

			@Override
			public void connect(final SocketAddress endpoint)
					throws IOException {
				Assert.fail();
			}

			@Override
			public InputStream getInputStream() throws IOException {
				return this.byteArrayInputStream;
			}

			@Override
			public OutputStream getOutputStream() throws IOException {
				return clientOutputStream;
			}

			@Override
			public synchronized void close() throws IOException {
				if (this.close.getAndSet(Boolean.FALSE)) {
					countDownLatch.countDown();
				}
			}

			@Override
			public void shutdownInput() throws IOException {
			}

			@Override
			public void shutdownOutput() throws IOException {
			}

			@Override
			public InetAddress getInetAddress() {
				return clientAddress;
			}

			@Override
			public InetAddress getLocalAddress() {
				return listeningAddress;
			}

			@Override
			public int getLocalPort() {
				return 1080;
			}

			@Override
			public int getPort() {
				return 1337;
			}

		};

		final Socket serverSocket = new Socket() {
			final AtomicBoolean close = new AtomicBoolean(Boolean.TRUE);

			@Override
			public synchronized void close() throws IOException {
				if (this.close.getAndSet(Boolean.FALSE)) {
					countDownLatch.countDown();
				}
			}

			@Override
			public void connect(final SocketAddress endpoint)
					throws IOException {
				Assert.assertArrayEquals(new byte[] { 11, 12, 13, 14 },
						((InetSocketAddress) endpoint).getAddress()
								.getAddress());
			}

			@Override
			public void shutdownInput() throws IOException {
			}

			@Override
			public void shutdownOutput() throws IOException {
			}

		};

		final ConfigurationFacade configurationFacade = new ConfigurationFacade() {

			@Override
			public List<InetAddress> getOutgoingSourceAddresses() {
				return Arrays.asList(listeningAddress);
			}

			@Override
			public boolean isAllowSocks4() {
				return false;
			}

			@Override
			public boolean isAllowSocks5() {
				return false;
			}
		};

		executor.execute(new SocksImplementation5(configurationFacade,
				clientSocket, executor) {

			@Override
			protected Socket openConnection(final InetAddress inetAddress,
					final int port) throws IOException {

				Assert.assertEquals(serverAddress, inetAddress);
				Assert.assertEquals(serverPort, port);

				Assert.assertEquals(1, count.incrementAndGet());

				return serverSocket;
			}

			@Override
			protected void tunnel(final Socket internal, final Socket external) {
				Assert.assertEquals(clientSocket, internal);
				Assert.assertEquals(serverSocket, external);

				Assert.assertEquals(2, count.incrementAndGet());
			}
		});

		countDownLatch.await(5, TimeUnit.SECONDS);

		Assert.assertArrayEquals(serverResponseStream.toByteArray(),
				clientOutputStream.toByteArray());
	}

	@Test
	public void testAuthenticateNoPassword() throws Exception {
		final SocksImplementation5 implementation5 = new SocksImplementation5(
				null, null, null);
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final DataOutputStream dataOutputStream = new DataOutputStream(
				byteArrayOutputStream);

		implementation5.authenticate(new DataInputStream(
				new ByteArrayInputStream(new byte[] { 1, 0 })),
				dataOutputStream);

		Assert.assertArrayEquals(new byte[] { 5, 0 },
				byteArrayOutputStream.toByteArray());
	}

	@Test
	public void testWriteResponseIPv4() throws Exception {

		final SocksImplementation5 implementation5 = new SocksImplementation5(
				null, null, null);
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final DataOutputStream dataOutputStream = new DataOutputStream(
				byteArrayOutputStream);
		final InetAddress boundAddress = InetAddress.getByAddress(new byte[] {
				10, 11, 12, 13 });

		implementation5.writeResponse(dataOutputStream, Status.SUCCEEDED,
				AddressType.IP_V4, boundAddress, null, 80);

		Assert.assertArrayEquals(
				new byte[] { 5, 0, 0, 1, 10, 11, 12, 13, 0, 80 },
				byteArrayOutputStream.toByteArray());
	}

	@Test
	public void testWriteResponseIPv6() throws Exception {

		final SocksImplementation5 implementation5 = new SocksImplementation5(
				null, null, null);
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final DataOutputStream dataOutputStream = new DataOutputStream(
				byteArrayOutputStream);
		final InetAddress boundAddress = InetAddress
				.getByAddress(new byte[] { 10, 11, 12, 13, 10, 11, 12, 13, 10,
						11, 12, 13, 10, 11, 12, 13 });

		implementation5.writeResponse(dataOutputStream, Status.SUCCEEDED,
				AddressType.IP_V6, boundAddress, null, 80);

		Assert.assertArrayEquals(new byte[] { 5, 0, 0, 4, 10, 11, 12, 13, 10,
				11, 12, 13, 10, 11, 12, 13, 10, 11, 12, 13, 0, 80 },
				byteArrayOutputStream.toByteArray());
	}

	@Test
	public void testWriteResponseDomain() throws Exception {

		final SocksImplementation5 implementation5 = new SocksImplementation5(
				null, null, null);
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final DataOutputStream dataOutputStream = new DataOutputStream(
				byteArrayOutputStream);
		final InetAddress boundAddress = InetAddress
				.getByAddress(new byte[] { 10, 11, 12, 13, 10, 11, 12, 13, 10,
						11, 12, 13, 10, 11, 12, 13 });

		implementation5.writeResponse(dataOutputStream, Status.SUCCEEDED,
				AddressType.DOMAIN, boundAddress, "test".getBytes("US-ASCII"),
				80);

		Assert.assertArrayEquals(new byte[] { 5, 0, 0, 3, 4, 't', 'e', 's',
				't', 0, 80 }, byteArrayOutputStream.toByteArray());
	}

}
