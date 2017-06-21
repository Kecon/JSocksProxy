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

@RunWith(MockitoJUnitRunner.class)
public class SocksImplementation4Test {

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
		when(clientAddress.getHostAddress()).thenReturn("1.2.3.4");

		when(socket.getLocalAddress()).thenReturn(serverAddress);
		when(serverAddress.getHostAddress()).thenReturn("5.6.7.8");
		when(serverAddress.getAddress())
				.thenReturn(InetAddress.getByName("5.6.7.8").getAddress());
	}

	@After
	public void tearDown() {
		executor.shutdown();
	}

	@Test
	public void testRun_connectOk() throws Exception {
		// Version (0x04) has already been parsed
		byte[] request = { 0x01, 0x00, 0x50, 0x42, 0x66, 0x07, 0x63, 0x46,
				0x72, 0x65, 0x64, 0x00, 'T', 'e', 's', 't' };
		byte[] expectedResponse = { 0x00, 0x5A, 0x00, 0x50, 0x42, 0x66, 0x07,
				0x63, 'R', 'e', 'p', 'l', 'y' };

		ByteArrayOutputStream clientOutputStream = new ByteArrayOutputStream();
		when(socket.getInputStream())
				.thenReturn(new ByteArrayInputStream(request));
		when(socket.getOutputStream()).thenReturn(clientOutputStream);

		expectedInetAddress = InetAddress.getByName("66.102.7.99");
		expectedPort = 80;

		when(remoteServerSocket.getInetAddress())
				.thenReturn(expectedInetAddress);

		when(remoteServerSocket.getInputStream())
				.thenReturn(new ByteArrayInputStream(
						"Reply".getBytes(StandardCharsets.US_ASCII)));

		ByteArrayOutputStream remoteServerOutputStream = new ByteArrayOutputStream();

		when(remoteServerSocket.getOutputStream())
				.thenReturn(remoteServerOutputStream);

		socksImplementation4.run();

		assertArrayEquals(expectedResponse, clientOutputStream.toByteArray());

		assertArrayEquals("Test".getBytes(StandardCharsets.US_ASCII),
				remoteServerOutputStream.toByteArray());
	}

	@Test
	public void testRun_connectOk4A() throws Exception {

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

		expectedInetAddress = InetAddress.getByName("66.102.7.99");
		expectedPort = 80;

		when(remoteServerSocket.getInetAddress())
				.thenReturn(expectedInetAddress);

		when(remoteServerSocket.getInputStream())
				.thenReturn(new ByteArrayInputStream(
						"Reply".getBytes(StandardCharsets.US_ASCII)));

		ByteArrayOutputStream remoteServerOutputStream = new ByteArrayOutputStream();

		when(remoteServerSocket.getOutputStream())
				.thenReturn(remoteServerOutputStream);

		socksImplementation4.run();

		assertArrayEquals(expectedResponse, clientOutputStream.toByteArray());

		assertArrayEquals("Test".getBytes(StandardCharsets.US_ASCII),
				remoteServerOutputStream.toByteArray());
	}

	@Test
	public void testRun_connectUnknownCommand() throws Exception {
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
	public void testRun_connectTimeout() throws Exception {

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
	public void testRun_bindOk() throws Exception {
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

		expectedInetAddress = InetAddress.getByName("66.102.7.99");
		expectedPort = 80;

		when(serverSocket.getLocalPort()).thenReturn(1337);
		when(serverSocket.getInetAddress()).thenReturn(serverAddress);
		when(serverSocket.accept()).thenReturn(remoteServerSocket);

		when(remoteServerSocket.getInetAddress())
				.thenReturn(expectedInetAddress);

		when(remoteServerSocket.getPort()).thenReturn(80);

		when(remoteServerSocket.getInputStream())
				.thenReturn(new ByteArrayInputStream(
						"Reply".getBytes(StandardCharsets.US_ASCII)));

		ByteArrayOutputStream remoteServerOutputStream = new ByteArrayOutputStream();

		when(remoteServerSocket.getOutputStream())
				.thenReturn(remoteServerOutputStream);

		socksImplementation4.run();

		assertArrayEquals(expectedResponse, clientOutputStream.toByteArray());
		assertArrayEquals("Test".getBytes(StandardCharsets.US_ASCII),
				remoteServerOutputStream.toByteArray());
	}

	@Test
	public void testRun_bindWrongRemote() throws Exception {
		// Version (0x04) has already been parsed
		byte[] request = { 0x02, 0x00, 0x50, 0x42, 0x66, 0x07, 0x63, 0x46,
				0x72, 0x65, 0x64, 0x00 };
		byte[] expectedResponse = { 0x00, 0x5A, 0x05, 0x39, 0x05, 0x06, 0x07,
				0x08, 0x00, 0x5B, 0x00, 0x50, 0x42, 0x66, 0x07, 0x64 };

		ByteArrayOutputStream clientOutputStream = new ByteArrayOutputStream();
		when(socket.getInputStream())
				.thenReturn(new ByteArrayInputStream(request));
		when(socket.getOutputStream()).thenReturn(clientOutputStream);

		expectedInetAddress = InetAddress.getByName("66.102.7.99");
		expectedPort = 80;

		when(serverSocket.getLocalPort()).thenReturn(1337);
		when(serverSocket.getInetAddress()).thenReturn(serverAddress);
		when(serverSocket.accept()).thenReturn(remoteServerSocket);

		when(remoteServerSocket.getInetAddress())
				.thenReturn(InetAddress.getByName("66.102.7.100"));
		when(remoteServerSocket.getPort()).thenReturn(80);

		ByteArrayOutputStream remoteServerOutputStream = new ByteArrayOutputStream();

		socksImplementation4.run();

		assertArrayEquals(expectedResponse, clientOutputStream.toByteArray());
		assertArrayEquals("".getBytes(StandardCharsets.US_ASCII),
				remoteServerOutputStream.toByteArray());
	}
}
