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
package nu.najt.kecon.jsocksproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import nu.najt.kecon.jsocksproxy.utils.SocketUtils;
import nu.najt.kecon.jsocksproxy.utils.StringUtils;

/**
 * The common implementation of the SOCKS protocol.
 * 
 * @author Kenny Colliander Nordin
 * 
 */
public abstract class AbstractSocksImplementation implements
		SocksImplementation {

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

	/**
	 * Open a connection to remote destination
	 * 
	 * @param host
	 *            the host to connect to
	 * @param port
	 *            the port to connect to
	 * @return established Socket
	 * @throws IOException
	 */
	protected Socket openConnection(final InetAddress inetAddress,
			final int port) throws IOException {

		for (final InetAddress localInetAddress : this.configurationFacade
				.getOutgoingSourceAddresses()) {
			if (localInetAddress.getClass() == inetAddress.getClass()) {
				final Socket socket = new Socket(inetAddress, port,
						localInetAddress, 0);
				socket.setKeepAlive(true);
				socket.setTcpNoDelay(true);

				return socket;
			}
		}

		throw new IOException("No route to address found using local addresses");
	}

	/**
	 * Bind to connection
	 * 
	 * @param inetAddress
	 * @param suggestedPort
	 * @return
	 * @throws IOException
	 */
	protected ServerSocket bindConnection(final InetAddress inetAddress,
			final int suggestedPort) throws IOException {
		for (final InetAddress localInetAddress : this.configurationFacade
				.getOutgoingSourceAddresses()) {

			if (localInetAddress.equals(inetAddress)) {
				final ServerSocket serverSocket = new ServerSocket(
						suggestedPort, 1, inetAddress);

				this.logger.info("Bound clientSocket "
						+ StringUtils.formatSocket(serverSocket) + " for "
						+ StringUtils.formatSocket(this.getClientSocket()));

				serverSocket
						.setSoTimeout(AbstractSocksImplementation.BIND_SOCKET_TIMEOUT);

				return serverSocket;
			}
		}

		for (final InetAddress localInetAddress : this.configurationFacade
				.getOutgoingSourceAddresses()) {

			if (localInetAddress.getClass() == inetAddress.getClass()) {
				final ServerSocket serverSocket = new ServerSocket(
						suggestedPort, 1, localInetAddress);

				this.logger.info("Bound clientSocket "
						+ StringUtils.formatSocket(serverSocket) + " for "
						+ StringUtils.formatSocket(this.getClientSocket()));

				serverSocket
						.setSoTimeout(AbstractSocksImplementation.BIND_SOCKET_TIMEOUT);

				return serverSocket;
			}
		}

		final ServerSocket serverSocket = new ServerSocket(suggestedPort, 1,
				this.configurationFacade.getOutgoingSourceAddresses().get(0));

		this.logger.info("Bound clientSocket "
				+ StringUtils.formatSocket(serverSocket) + " for "
				+ StringUtils.formatSocket(this.getClientSocket()));

		serverSocket
				.setSoTimeout(AbstractSocksImplementation.BIND_SOCKET_TIMEOUT);

		return serverSocket;
	}

	/**
	 * Tunnel input to output
	 * 
	 * @param internal
	 * @param external
	 */
	protected void tunnel(final Socket internal, final Socket external) {

		this.logger.info("Established tunnel between "
				+ StringUtils.formatSocket(internal) + " and "
				+ StringUtils.formatSocket(external));

		this.executor.execute(new TunnelThread(external, internal));

		try {
			SocketUtils.copy(internal, external);

			// Wait for the other thread to die
			if (this.logger.isLoggable(Level.FINE)) {
				this.logger.fine("Waiting to disconnect");
			}

		} catch (final IOException ioe) {
			if (this.logger.isLoggable(Level.FINE)) {
				this.logger.log(Level.FINE, "IOException occurred between "
						+ StringUtils.formatSocket(internal) + " and "
						+ StringUtils.formatSocket(external), ioe);
			}
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

			this.logger.info("Shutdown connection between "
					+ StringUtils.formatSocket(internal) + " and "
					+ StringUtils.formatSocket(external));
		}
	}

	/**
	 * The thread which make this connection go in duplex mode
	 */
	private class TunnelThread implements Runnable {

		private final Socket inputSocket;

		private final Socket outputSocket;

		/**
		 * Constructor
		 * 
		 * @param inputSocket
		 *            the input clientSocket
		 * @param outputSocket
		 *            the output clientSocket
		 */
		public TunnelThread(final Socket inputSocket, final Socket outputSocket) {
			this.inputSocket = inputSocket;
			this.outputSocket = outputSocket;
		}

		@Override
		public void run() {
			try {
				SocketUtils.copy(this.inputSocket, this.outputSocket);
			} catch (final IOException e) {
			}

			AbstractSocksImplementation.this.countDownLatch.countDown();

		}
	}

	/**
	 * @return the clientSocket
	 */
	protected Socket getClientSocket() {
		return this.clientSocket;
	}

	/**
	 * @return the client input stream
	 * @throws IOException
	 */
	protected InputStream getClientInputStream() throws IOException {
		return this.clientSocket.getInputStream();
	}

	/**
	 * @return the client output stream
	 * @throws IOException
	 */
	protected OutputStream getClientOutputStream() throws IOException {
		return this.clientSocket.getOutputStream();
	}
}
