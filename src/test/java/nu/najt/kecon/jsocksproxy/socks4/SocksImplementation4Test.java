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
package nu.najt.kecon.jsocksproxy.socks4;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.rmi.ConnectException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import nu.najt.kecon.jsocksproxy.ConfigurationFacade;

/**
 * Testing of <code>SocksImplementation4</code>
 * 
 * @author Kenny Colliander Nordin
 * @since 2017-06-21
 */
@RunWith(MockitoJUnitRunner.class)
public class SocksImplementation4Test {

	private static final String REPLY = "Reply";

	private static final String TEST = "Test";

	private static final String EXTERNAL_IP2_66_102_7_100 = "66.102.7.100";

	private static final String EXTERNAL_IP_66_102_7_99 = "66.102.7.99";

	private static final String SERVER_IP_5_6_7_8 = "5.6.7.8";

	private static final String CLIENT_IP_1_2_3_4 = "1.2.3.4";

	private SocksImplementation4 socksImplementation4;

	@Mock
	private ConfigurationFacade configurationFacade;

	@Mock
	private Socket socket;

	private ExecutorService executor;

	@Mock
	private InetAddress clientAddress;

	@Mock
	private InetAddress serverAddress;

	@Mock
	private Socket remoteServerSocket;

	@Mock
	private ServerSocket serverSocket;

	private InetAddress expectedInetAddress;

	private int expectedPort;

	@Before
	public void setUp() throws Exception {
		executor = Executors.newSingleThreadExecutor();

		socksImplementation4 = new SocksImplementation4(configurationFacade,
				socket, executor) {

			@Override
			protected Socket openConnection(InetAddress inetAddress, int port)
					throws IOException {
				assertEquals(expectedInetAddress, inetAddress);
				assertEquals(expectedPort, port);

				return remoteServerSocket;
			}

			@Override
			protected ServerSocket bindConnection(InetAddress inetAddress,
					int suggestedPort) throws IOException {
				assertEquals(expectedInetAddress, inetAddress);
				assertEquals(expectedPort, suggestedPort);

				return serverSocket;
			}
		};

		when(socket.getInetAddress()).thenReturn(clientAddress);
		when(clientAddress.getHostAddress()).thenReturn(CLIENT_IP_1_2_3_4);

		when(socket.getLocalAddress()).thenReturn(serverAddress);
		when(serverAddress.getHostAddress()).thenReturn(SERVER_IP_5_6_7_8);
		when(serverAddress.getAddress()).thenReturn(
				InetAddress.getByName(SERVER_IP_5_6_7_8).getAddress());
	}

	@After
	public void tearDown() {
		executor.shutdown();
	}

	@Test
	public void testRunConnectOk() throws Exception {
		// Version (0x04) has already been parsed
		byte[] request = { 0x01, 0x00, 0x50, 0x42, 0x66, 0x07, 0x63, 0x46,
				0x72, 0x65, 0x64, 0x00, 'T', 'e', 's', 't' };
		byte[] expectedResponse = { 0x00, 0x5A, 0x00, 0x50, 0x42, 0x66, 0x07,
				0x63, 'R', 'e', 'p', 'l', 'y' };

		ByteArrayOutputStream clientOutputStream = new ByteArrayOutputStream();
		when(socket.getInputStream())
				.thenReturn(new ByteArrayInputStream(request));
		when(socket.getOutputStream()).thenReturn(clientOutputStream);

		expectedInetAddress = InetAddress.getByName(EXTERNAL_IP_66_102_7_99);
		expectedPort = 80;

		when(remoteServerSocket.getInetAddress())
				.thenReturn(expectedInetAddress);

		when(remoteServerSocket.getInputStream())
				.thenReturn(new ByteArrayInputStream(
						REPLY.getBytes(StandardCharsets.US_ASCII)));

		ByteArrayOutputStream remoteServerOutputStream = new ByteArrayOutputStream();

		when(remoteServerSocket.getOutputStream())
				.thenReturn(remoteServerOutputStream);

		socksImplementation4.run();

		assertArrayEquals(expectedResponse, clientOutputStream.toByteArray());

		assertArrayEquals(TEST.getBytes(StandardCharsets.US_ASCII),
				remoteServerOutputStream.toByteArray());
	}

	@Test
	public void testRunConnectOk4a() throws Exception {

		socksImplementation4 = new SocksImplementation4(configurationFacade,
				socket, executor) {

			@Override
			protected Socket openConnection(InetAddress inetAddress, int port)
					throws IOException {
				assertEquals(expectedInetAddress, inetAddress);
				assertEquals(expectedPort, port);

				return remoteServerSocket;
			}

			@Override
			protected InetAddress resolveHostname(StringBuilder builder)
					throws UnknownHostException {

				assertEquals("host", builder.toString());
				return expectedInetAddress;
			}
		};
		// Version (0x04) has already been parsed
		byte[] request = { 0x01, 0x00, 0x50, 0x00, 0x00, 0x00, 0x7f, 0x46,
				0x72, 0x65, 0x64, 0x00, 'h', 'o', 's', 't', 0x00, 'T', 'e',
				's', 't' };
		byte[] expectedResponse = { 0x00, 0x5A, 0x00, 0x50, 0x42, 0x66, 0x07,
				0x63, 'R', 'e', 'p', 'l', 'y' };

		ByteArrayOutputStream clientOutputStream = new ByteArrayOutputStream();
		when(socket.getInputStream())
				.thenReturn(new ByteArrayInputStream(request));
		when(socket.getOutputStream()).thenReturn(clientOutputStream);

		expectedInetAddress = InetAddress.getByName(EXTERNAL_IP_66_102_7_99);
		expectedPort = 80;

		when(remoteServerSocket.getInetAddress())
				.thenReturn(expectedInetAddress);

		when(remoteServerSocket.getInputStream())
				.thenReturn(new ByteArrayInputStream(
						REPLY.getBytes(StandardCharsets.US_ASCII)));

		ByteArrayOutputStream remoteServerOutputStream = new ByteArrayOutputStream();

		when(remoteServerSocket.getOutputStream())
				.thenReturn(remoteServerOutputStream);

		socksImplementation4.run();

		assertArrayEquals(expectedResponse, clientOutputStream.toByteArray());

		assertArrayEquals(TEST.getBytes(StandardCharsets.US_ASCII),
				remoteServerOutputStream.toByteArray());
	}

	@Test
	public void testRunConnectUnknownCommand() throws Exception {
		// Version (0x04) has already been parsed
		byte[] request = { 0x05, 0x00, 0x50, 0x42, 0x66, 0x07, 0x63, 0x46,
				0x72, 0x65, 0x64, 0x00, 'T', 'e', 's', 't' };
		byte[] expectedResponse = { 0x00, 0x5B, -1, -1, 0x00, 0x00, 0x00,
				0x00 };

		ByteArrayOutputStream clientOutputStream = new ByteArrayOutputStream();
		when(socket.getInputStream())
				.thenReturn(new ByteArrayInputStream(request));
		when(socket.getOutputStream()).thenReturn(clientOutputStream);

		socksImplementation4.run();

		assertArrayEquals(expectedResponse, clientOutputStream.toByteArray());
	}

	@Test
	public void testRunConnectTimeout() throws Exception {

		socksImplementation4 = new SocksImplementation4(configurationFacade,
				socket, executor) {

			@Override
			protected Socket openConnection(InetAddress inetAddress, int port)
					throws IOException {
				throw new ConnectException("Timeout");
			}
		};

		// Version (0x04) has already been parsed
		byte[] request = { 0x01, 0x00, 0x50, 0x42, 0x66, 0x07, 0x63, 0x46,
				0x72, 0x65, 0x64, 0x00, 'T', 'e', 's', 't' };
		byte[] expectedResponse = { 0x00, 0x5B, 0x00, 0x50, 0x42, 0x66, 0x07,
				0x63 };

		ByteArrayOutputStream clientOutputStream = new ByteArrayOutputStream();
		when(socket.getInputStream())
				.thenReturn(new ByteArrayInputStream(request));
		when(socket.getOutputStream()).thenReturn(clientOutputStream);

		socksImplementation4.run();

		assertArrayEquals(expectedResponse, clientOutputStream.toByteArray());
	}

	@Test
	public void testRunBindOk() throws Exception {
		// Version (0x04) has already been parsed
		byte[] request = { 0x02, 0x00, 0x50, 0x42, 0x66, 0x07, 0x63, 0x46,
				0x72, 0x65, 0x64, 0x00, 'T', 'e', 's', 't' };
		byte[] expectedResponse = { 0x00, 0x5A, 0x05, 0x39, 0x05, 0x06, 0x07,
				0x08, 0x00, 0x5A, 0x00, 0x50, 0x42, 0x66, 0x07, 0x63, 'R', 'e',
				'p', 'l', 'y' };

		ByteArrayOutputStream clientOutputStream = new ByteArrayOutputStream();
		when(socket.getInputStream())
				.thenReturn(new ByteArrayInputStream(request));
		when(socket.getOutputStream()).thenReturn(clientOutputStream);

		expectedInetAddress = InetAddress.getByName(EXTERNAL_IP_66_102_7_99);
		expectedPort = 80;

		when(serverSocket.getLocalPort()).thenReturn(1337);
		when(serverSocket.getInetAddress()).thenReturn(serverAddress);
		when(serverSocket.accept()).thenReturn(remoteServerSocket);

		when(remoteServerSocket.getInetAddress())
				.thenReturn(expectedInetAddress);

		when(remoteServerSocket.getPort()).thenReturn(80);

		when(remoteServerSocket.getInputStream())
				.thenReturn(new ByteArrayInputStream(
						REPLY.getBytes(StandardCharsets.US_ASCII)));

		ByteArrayOutputStream remoteServerOutputStream = new ByteArrayOutputStream();

		when(remoteServerSocket.getOutputStream())
				.thenReturn(remoteServerOutputStream);

		socksImplementation4.run();

		assertArrayEquals(expectedResponse, clientOutputStream.toByteArray());
		assertArrayEquals(TEST.getBytes(StandardCharsets.US_ASCII),
				remoteServerOutputStream.toByteArray());
	}

	@Test
	public void testRunBindWrongRemote() throws Exception {
		// Version (0x04) has already been parsed
		byte[] request = { 0x02, 0x00, 0x50, 0x42, 0x66, 0x07, 0x63, 0x46,
				0x72, 0x65, 0x64, 0x00 };
		byte[] expectedResponse = { 0x00, 0x5A, 0x05, 0x39, 0x05, 0x06, 0x07,
				0x08, 0x00, 0x5B, 0x00, 0x50, 0x42, 0x66, 0x07, 0x64 };

		ByteArrayOutputStream clientOutputStream = new ByteArrayOutputStream();
		when(socket.getInputStream())
				.thenReturn(new ByteArrayInputStream(request));
		when(socket.getOutputStream()).thenReturn(clientOutputStream);

		expectedInetAddress = InetAddress.getByName(EXTERNAL_IP_66_102_7_99);
		expectedPort = 80;

		when(serverSocket.getLocalPort()).thenReturn(1337);
		when(serverSocket.getInetAddress()).thenReturn(serverAddress);
		when(serverSocket.accept()).thenReturn(remoteServerSocket);

		when(remoteServerSocket.getInetAddress())
				.thenReturn(InetAddress.getByName(EXTERNAL_IP2_66_102_7_100));
		when(remoteServerSocket.getPort()).thenReturn(80);

		ByteArrayOutputStream remoteServerOutputStream = new ByteArrayOutputStream();

		socksImplementation4.run();

		assertArrayEquals(expectedResponse, clientOutputStream.toByteArray());
		assertArrayEquals("".getBytes(StandardCharsets.US_ASCII),
				remoteServerOutputStream.toByteArray());
	}
}
