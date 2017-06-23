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

import static nu.najt.kecon.jsocksproxy.utils.StringUtils.formatSocket;
import static nu.najt.kecon.jsocksproxy.utils.StringUtils.formatSocketAddress;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ServerSocketFactory;

import org.slf4j.Logger;
import org.slf4j.MDC;

import nu.najt.kecon.jsocksproxy.socks4.SocksImplementation4;
import nu.najt.kecon.jsocksproxy.socks5.SocksImplementation5;

/**
 * This thread handle incoming connections for a specific listening address
 * 
 * @author Kenny Colliander Nordin
 * @since 3.0
 */
class ListeningThread implements Runnable {

	private final Logger logger;

	private final AtomicBoolean mayRun = new AtomicBoolean(true);

	private final InetSocketAddress inetSocketAddress;

	private final ServerSocket serverSocket;

	private final ConfigurationFacade configuration;

	private final ExecutorService executorService;

	/**
	 * Constructor
	 * 
	 * @param configuration
	 *            the configuration
	 * @param logger
	 *            the logger
	 * @param executorService
	 *            ListeningThread constructor
	 * @param inetSocketAddress
	 *            the address that the listening thread should bind to
	 * @throws IOException
	 */
	public ListeningThread(final ConfigurationFacade configuration,
			final Logger logger, final ExecutorService executorService,
			final InetSocketAddress inetSocketAddress) throws IOException {
		this.configuration = configuration;
		this.logger = logger;
		this.executorService = executorService;
		this.inetSocketAddress = inetSocketAddress;
		MDC.setContextMap(new HashMap<>());
		MDC.put(LoggingConstants.SOCKS_SERVER,
				formatSocketAddress(inetSocketAddress));

		this.logger.info("Listening for incoming connections");

		this.serverSocket = createServerSocket(inetSocketAddress);
	}

	protected ServerSocket createServerSocket(
			final InetSocketAddress inetSocketAddress) throws IOException {
		return ServerSocketFactory.getDefault().createServerSocket(
				inetSocketAddress.getPort(), this.configuration.getBacklog(),
				inetSocketAddress.getAddress());
	}

	@Override
	public void run() {
		try {

			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

			while (this.mayRun.get()) {
				this.acceptConnection();
			}

			this.serverSocket.close();
		} catch (final Exception e) {
			if (this.mayRun.get()) {
				this.logger.error("Unknown error occurred for {}",
						formatSocketAddress(this.inetSocketAddress), e);
			}
		} finally {
			this.logger.info("Shutdown SOCKS proxy for {}",
					formatSocketAddress(this.inetSocketAddress));
		}
	}

	protected void acceptConnection() throws IOException, SocketException {
		Socket socket = this.serverSocket.accept();

		if (socket == null) {
			return;
		}

		socket.setTcpNoDelay(true);
		socket.setKeepAlive(true);

		try {
			this.executorService
					.execute(this.getImplementation(configuration, socket));

		} catch (final ProtocolException e) {
			this.logger.info("Unknown SOCKS VERSION requested by {}",
					formatSocket(socket), e);
		} catch (final AccessDeniedException e) {
			this.logger.warn("Access Denied for {}", formatSocket(socket), e);
		}
	}

	/**
	 * Turn indication on that the thread should shutdown
	 */
	public void shutdown() {
		this.mayRun.set(false);

		if (this.serverSocket != null) {
			try {
				this.serverSocket.close();
			} catch (final IOException e) {
			}
		}
	}

	/**
	 * Return a SOCKS implementation
	 * 
	 * @param socket
	 *            the incoming socket
	 * @return SOCKS implementation
	 * @throws IOException
	 * @throws AccessDeniedException
	 * @throws UnknownSocksVersion
	 */
	public SocksImplementation getImplementation(
			final ConfigurationFacade configurationFacade, final Socket socket)
			throws IOException, AccessDeniedException, ProtocolException {
		final InputStream inputStream = socket.getInputStream();

		final int protocol = inputStream.read();

		switch (protocol) {
		case 0x04:
			if (configurationFacade.isAllowSocks4()) {
				return new SocksImplementation4(configurationFacade, socket,
						executorService);
			} else {
				try {
					socket.close();
				} catch (final Exception e) {
				}
				throw new AccessDeniedException("SOCKS4 is not enabled");
			}

		case 0x05:
			if (configurationFacade.isAllowSocks5()) {
				return new SocksImplementation5(configurationFacade, socket,
						executorService);
			} else {
				try {
					socket.close();
				} catch (final Exception e) {
				}
				throw new AccessDeniedException("SOCKS5 is not enabled");
			}

		default:
			try {
				socket.close();
			} catch (final Exception e) {
			}
			throw new ProtocolException(
					"Unknown protocol: 0x" + Integer.toHexString(protocol));
		}
	}

	public InetSocketAddress getInetSocketAddress() {
		return inetSocketAddress;
	}
}