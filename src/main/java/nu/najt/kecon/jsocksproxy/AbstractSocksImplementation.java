/**
 * JSocksProxy Copyright (c) 2006-2011 Kenny Colliander Nordin
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

import static nu.najt.kecon.jsocksproxy.utils.StringUtils.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The common implementation of the SOCKS protocol.
 * 
 * @author Kenny Colliander Nordin
 * 
 */
public abstract class AbstractSocksImplementation implements
		SocksImplementation, Runnable {

	private static final int BIND_SOCKET_TIMEOUT = 180000;

	private final Socket socket;

	private final JSocksProxy jSocksProxy;

	private volatile boolean shutdown = false;

	protected final Logger logger;

	private static final Executor executor = Executors.newCachedThreadPool();

	private final CountDownLatch countDownLatch = new CountDownLatch(1);

	/**
	 * Constructor
	 * 
	 * @param jSocksProxy
	 *            the socks proxy
	 * @param socket
	 *            the socket
	 * @param logger
	 *            the logger
	 */
	public AbstractSocksImplementation(final JSocksProxy jSocksProxy,
			final Socket socket, final Logger logger) {
		this.socket = socket;
		this.jSocksProxy = jSocksProxy;
		this.logger = logger;

		executor.execute(this);
	}

	public void run() {
		try {
			this.jSocksProxy.getConnections().add(this);
			this.handle();
		} finally {
			this.jSocksProxy.getConnections().remove(
					AbstractSocksImplementation.this);
		}
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

		for (final InetAddress localInetAddress : this.jSocksProxy
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

	protected ServerSocket bindConnection(final InetAddress inetAddress,
			final int suggestedPort) throws IOException {
		for (final InetAddress localInetAddress : this.jSocksProxy
				.getOutgoingSourceAddresses()) {

			if (localInetAddress.equals(inetAddress)) {
				final ServerSocket serverSocket = new ServerSocket(
						suggestedPort, 1, inetAddress);

				this.logger.info("Bound socket " + formatSocket(serverSocket)
						+ " for " + formatSocket(this.getSocket()));

				serverSocket.setSoTimeout(BIND_SOCKET_TIMEOUT);

				return serverSocket;
			}
		}

		for (final InetAddress localInetAddress : this.jSocksProxy
				.getOutgoingSourceAddresses()) {

			if (localInetAddress.getClass() == inetAddress.getClass()) {
				final ServerSocket serverSocket = new ServerSocket(
						suggestedPort, 1, localInetAddress);

				this.logger.info("Bound socket " + formatSocket(serverSocket)
						+ " for " + formatSocket(this.getSocket()));

				serverSocket.setSoTimeout(BIND_SOCKET_TIMEOUT);

				return serverSocket;
			}
		}

		final ServerSocket serverSocket = new ServerSocket(suggestedPort, 1,
				this.jSocksProxy.getOutgoingSourceAddresses().get(0));

		this.logger.info("Bound socket " + formatSocket(serverSocket) + " for "
				+ formatSocket(this.getSocket()));

		serverSocket.setSoTimeout(BIND_SOCKET_TIMEOUT);

		return serverSocket;
	}

	/**
	 * Tunnel input to output
	 * 
	 * @param internal
	 * @param external
	 */
	protected void tunnel(final Socket internal, final Socket external) {

		this.logger.info("Established tunnel between " + formatSocket(internal)
				+ " and " + formatSocket(external));

		executor.execute(new TunnelThread(external, internal));

		this.doTunnel(internal, external);

		// Wait for the other thread to die
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Waiting to disconnect");
		}

		try {
			this.countDownLatch.await();
		} catch (InterruptedException e) {
		}

		try {
			external.close();
		} catch (IOException e) {
		}
		try {
			internal.close();
		} catch (IOException e) {
		}

		this.logger.info("Shutdown connection between "
				+ formatSocket(internal) + " and " + formatSocket(external));
	}

	/**
	 * @param inputSocket
	 *            the input socket
	 * @param outputSocket
	 *            the output socket
	 */
	private void doTunnel(final Socket inputSocket, final Socket outputSocket) {

		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			inputStream = inputSocket.getInputStream();
			outputStream = outputSocket.getOutputStream();

			final byte buf[] = new byte[3000];
			int length;
			while (true) {
				try {
					length = inputStream.read(buf);

					if (length > 0) {
						outputStream.write(buf, 0, length);
						outputStream.flush();
					} else if (length == -1) {
						break;
					}
				} catch (InterruptedIOException ioe) {
				}
			}
		} catch (Exception e) {
			if (!this.shutdown) {
				this.logger.log(Level.WARNING,
						"Unexpected exception when tunneling; input: "
								+ formatSocket(inputSocket) + ", output: "
								+ formatSocket(outputSocket) + ".", e);
			}
		} finally {
			this.shutdown = true;
			try {
				inputSocket.shutdownInput();
			} catch (Exception e) {
			}
			try {
				outputSocket.shutdownOutput();
			} catch (Exception e) {
			}
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
		 *            the input socket
		 * @param outputSocket
		 *            the output socket
		 */
		public TunnelThread(final Socket inputSocket, final Socket outputSocket) {
			this.inputSocket = inputSocket;
			this.outputSocket = outputSocket;
		}

		public void run() {
			AbstractSocksImplementation.this.doTunnel(this.inputSocket,
					this.outputSocket);

			countDownLatch.countDown();

		}
	}

	/**
	 * @return the socket
	 */
	protected Socket getSocket() {
		return socket;
	}

	protected InputStream getClientInputStream() throws IOException {
		return socket.getInputStream();
	}

	protected OutputStream getClientOutputStream() throws IOException {
		return socket.getOutputStream();
	}
}
