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

import static nu.najt.kecon.jsocksproxy.utils.SocketUtils.copy;
import static nu.najt.kecon.jsocksproxy.utils.StringUtils.formatLocalSocket;
import static nu.najt.kecon.jsocksproxy.utils.StringUtils.formatSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 * The common implementation of the SOCKS protocol.
 * 
 * @author Kenny Colliander Nordin
 * 
 */
public abstract class AbstractSocksImplementation
		implements SocksImplementation {

	private static final int BIND_SOCKET_TIMEOUT = 180000;

	private final Socket clientSocket;

	private final ConfigurationFacade configurationFacade;

	protected final Logger logger;

	private final Executor executor;

	private final CountDownLatch countDownLatch = new CountDownLatch(1);

	/**
	 * Constructor
	 * 
	 * @param configurationFacade
	 *            the configuration facade
	 * @param clientSocket
	 *            the clientSocket
	 * @param logger
	 *            the logger
	 * @param executor
	 *            the executor
	 */
	public AbstractSocksImplementation(
			final ConfigurationFacade configurationFacade,
			final Socket clientSocket, final Logger logger,
			final Executor executor) {
		this.clientSocket = clientSocket;
		this.configurationFacade = configurationFacade;
		this.logger = logger;
		this.executor = executor;
	}

	protected void setup() {
		MDC.setContextMap(Collections.emptyMap());
		MDC.put(LoggingConstants.SOCKS_SERVER,
				formatLocalSocket(clientSocket));
		MDC.put(LoggingConstants.CLIENT, formatSocket(clientSocket));
	}

	protected void cleanup() {
		MDC.remove(LoggingConstants.SOCKS_SERVER);
		MDC.remove(LoggingConstants.CLIENT);
		MDC.remove(LoggingConstants.REMOTE_SERVER);
	}

	/**
	 * Open a connection to remote destination
	 * 
	 * @param inetAddress
	 *            the host to connect to
	 * @param port
	 *            the port to connect to
	 * @return established Socket
	 * @throws IOException
	 *             if an I/O error occurs when creating the socket.
	 */
	protected Socket openConnection(final InetAddress inetAddress,
			final int port) throws IOException {
		this.logger.debug("Connecting to {}:{}... ",
				inetAddress.getHostAddress(), port);

		for (final InetAddress localInetAddress : this.configurationFacade
				.getOutgoingSourceAddresses()) {
			if (localInetAddress.getClass() == inetAddress.getClass()) {
				final Socket socket = new Socket(inetAddress, port,
						localInetAddress, 0);
				socket.setKeepAlive(true);
				socket.setTcpNoDelay(true);

				MDC.put(LoggingConstants.REMOTE_SERVER, formatSocket(socket));
				this.logger.trace("Connected");
				return socket;
			}
		}

		throw new IOException(
				"No route to address found using local addresses");
	}

	/**
	 * Bind to connection
	 * 
	 * @param inetAddress
	 *            the submitted inetAddress
	 * @param suggestedPort
	 *            the submitted port
	 * @return the server socket
	 * @throws IOException
	 *             if an I/O error occurs when creating the socket.
	 */
	protected ServerSocket bindConnection(final InetAddress inetAddress,
			final int suggestedPort) throws IOException {
		for (final InetAddress localInetAddress : this.configurationFacade
				.getOutgoingSourceAddresses()) {

			if (localInetAddress.equals(inetAddress)) {
				final ServerSocket serverSocket = new ServerSocket(
						suggestedPort, 1, inetAddress);

				this.logger.info("Bound client socket");

				serverSocket.setSoTimeout(
						AbstractSocksImplementation.BIND_SOCKET_TIMEOUT);

				return serverSocket;
			}
		}

		for (final InetAddress localInetAddress : this.configurationFacade
				.getOutgoingSourceAddresses()) {

			if (localInetAddress.getClass() == inetAddress.getClass()) {
				final ServerSocket serverSocket = new ServerSocket(
						suggestedPort, 1, localInetAddress);

				this.logger.info("Bound client socket");

				serverSocket.setSoTimeout(
						AbstractSocksImplementation.BIND_SOCKET_TIMEOUT);

				return serverSocket;
			}
		}

		final ServerSocket serverSocket = new ServerSocket(suggestedPort, 1,
				this.configurationFacade.getOutgoingSourceAddresses().get(0));

		this.logger.info("Bound client socket");

		serverSocket
				.setSoTimeout(AbstractSocksImplementation.BIND_SOCKET_TIMEOUT);

		return serverSocket;
	}

	/**
	 * Tunnel input to output
	 * 
	 * @param internal
	 *            the internal socket
	 * @param external
	 *            the external socket
	 */
	protected void tunnel(final Socket internal, final Socket external) {

		this.logger.info("Established tunnel");

		this.executor.execute(
				new TunnelThread(this.countDownLatch, external, internal));

		try {
			copy(internal, external);

			// Wait for the other thread to die
			this.logger.trace("Waiting to disconnect");

		} catch (final IOException ioe) {
			this.logger.info("IOException occurred", ioe);
		} finally {

			try {
				this.countDownLatch.await();
			} catch (final InterruptedException e) {
			}

			try {
				external.close();
			} catch (final IOException e) {
			}

			try {
				internal.close();
			} catch (final IOException e) {
			}

			this.logger.info("Shutdown connection");
		}
	}

	/**
	 * Get the client socket
	 * 
	 * @return the clientSocket
	 */
	protected Socket getClientSocket() {
		return this.clientSocket;
	}

	/**
	 * Get the client input stream
	 * 
	 * @return the client input stream
	 * @throws IOException
	 *             if an I/O error occurs when creating the input stream, the
	 *             socket is closed, the socket is not connected, or the socket
	 *             input has been shutdown
	 */
	protected InputStream getClientInputStream() throws IOException {
		return this.clientSocket.getInputStream();
	}

	/**
	 * Get client output stream
	 * 
	 * @return the client output stream
	 * @throws IOException
	 *             if an I/O error occurs when creating the input stream, the
	 *             socket is closed, the socket is not connected, or the socket
	 *             input has been shutdown
	 */
	protected OutputStream getClientOutputStream() throws IOException {
		return this.clientSocket.getOutputStream();
	}
}
