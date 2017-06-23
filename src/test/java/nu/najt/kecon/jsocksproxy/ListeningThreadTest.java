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
package nu.najt.kecon.jsocksproxy;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import nu.najt.kecon.jsocksproxy.socks4.SocksImplementation4;
import nu.najt.kecon.jsocksproxy.socks5.SocksImplementation5;

/**
 * Testing <code>ListeningThread</code>
 * 
 * @author Kenny Colliander Nordin
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ListeningThreadTest {

	private static final String IP_192_168_0_1 = "192.168.0.1";

	private static final String IP_192_168_0_2 = "192.168.0.2";

	@Mock
	private ConfigurationFacade configuration;

	@Mock
	private Logger logger;

	@Mock
	private ExecutorService executorService;

	@Mock
	private ServerSocket serverSocket;

	@Mock
	private Socket socket;

	private InetSocketAddress inetSocketAddress;

	private ListeningThread listeningThread;

	@Before
	public void before() throws IOException {

		InetAddress inetAddress = InetAddress.getByName(IP_192_168_0_1);
		inetSocketAddress = new InetSocketAddress(inetAddress, 1080);

		this.listeningThread = new ListeningThread(configuration, logger,
				executorService, inetSocketAddress) {

			@Override
			protected ServerSocket createServerSocket(
					InetSocketAddress inetSocketAddress) throws IOException {
				assertEquals(ListeningThreadTest.this.inetSocketAddress,
						inetSocketAddress);
				return serverSocket;
			}

			@Override
			public void acceptConnection()
					throws IOException, SocketException {
				super.acceptConnection();
			}

		};
	}

	@Test
	public void testAcceptConnectionSocks4() throws IOException {
		when(serverSocket.accept()).thenReturn(socket);
		when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(
				new byte[] { 0x04, 0x02, 0x00, 0x50, 0x42, 0x66, 0x07, 0x63,
						0x46, 0x72, 0x65, 0x64, 0x00 }));

		when(configuration.isAllowSocks4()).thenReturn(true);

		listeningThread.acceptConnection();

		verify(executorService).execute(any(SocksImplementation4.class));
	}

	@Test
	public void testAcceptConnectionSocks5() throws IOException {
		when(serverSocket.accept()).thenReturn(socket);
		when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(
				new byte[] { 0x05, 0x02, 0x00, 0x50, 0x42, 0x66, 0x07, 0x63,
						0x46, 0x72, 0x65, 0x64, 0x00 }));

		when(configuration.isAllowSocks5()).thenReturn(true);

		listeningThread.acceptConnection();

		verify(executorService).execute(any(SocksImplementation5.class));
	}

	@Test
	public void testAcceptConnectionInvalidProtocol() throws IOException {
		when(serverSocket.accept()).thenReturn(socket);
		when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(
				new byte[] { 0x03, 0x02, 0x00, 0x50, 0x42, 0x66, 0x07, 0x63,
						0x46, 0x72, 0x65, 0x64, 0x00 }));
		when(socket.getInetAddress())
				.thenReturn(InetAddress.getByName(IP_192_168_0_2));

		listeningThread.acceptConnection();

		verify(executorService, never()).execute(any());
	}

	@Test
	public void testAcceptConnectionAccessDenied4() throws IOException {
		when(serverSocket.accept()).thenReturn(socket);
		when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(
				new byte[] { 0x04, 0x02, 0x00, 0x50, 0x42, 0x66, 0x07, 0x63,
						0x46, 0x72, 0x65, 0x64, 0x00 }));

		when(socket.getInetAddress())
				.thenReturn(InetAddress.getByName(IP_192_168_0_2));
		when(configuration.isAllowSocks4()).thenReturn(false);

		listeningThread.acceptConnection();

		verify(executorService, never()).execute(any());
	}

	@Test
	public void testAcceptConnectionAccessDenied5() throws IOException {
		when(serverSocket.accept()).thenReturn(socket);
		when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(
				new byte[] { 0x05, 0x02, 0x00, 0x50, 0x42, 0x66, 0x07, 0x63,
						0x46, 0x72, 0x65, 0x64, 0x00 }));

		when(socket.getInetAddress())
				.thenReturn(InetAddress.getByName(IP_192_168_0_2));
		when(configuration.isAllowSocks5()).thenReturn(false);

		listeningThread.acceptConnection();

		verify(executorService, never()).execute(any());
	}
}
