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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nu.najt.kecon.jsocksproxy.AbstractSocksImplementation;
import nu.najt.kecon.jsocksproxy.ConfigurationFacade;
import nu.najt.kecon.jsocksproxy.IllegalCommandException;

/**
 * This is the SOCKS4 implementation. <br>
 * <br>
 * More about the <a href="http://en.wikipedia.org/wiki/SOCKS">SOCKS</a>
 * protocol
 * 
 * @author Kenny Colliander Nordin
 * 
 */
public class SocksImplementation4 extends AbstractSocksImplementation {

	protected static final byte REQUEST_GRANTED = 0x5a;

	protected static final byte REQUEST_REJECTED = 0x5b;

	protected static final byte AUTHORIZATION_FAILED = 0x5d;

	protected static final byte NULL = 0x00;

	private static final Logger LOG = LoggerFactory
			.getLogger(SocksImplementation4.class.getPackage().getName());

	/**
	 * Constructor
	 * 
	 * @param configurationFacade
	 *            the configuration facade
	 * @param socket
	 *            the socket
	 * @param executor
	 *            the executor
	 */
	public SocksImplementation4(final ConfigurationFacade configurationFacade,
			final Socket socket, final Executor executor) {
		super(configurationFacade, socket, SocksImplementation4.LOG, executor);
	}

	@Override
	public void run() {
		DataInputStream inputStream = null;
		OutputStream outputStream = null;
		InetAddress inetAddress = null;
		int port = -1;
		try {
			this.setup();

			inputStream = this.getInputStream();
			outputStream = this.getOutputStream();

			final Command command = Command.valueOf(inputStream.readByte());
			port = this.getPort(inputStream);
			inetAddress = this.getAddress(inputStream);

			if (command == Command.CONNECT) {
				this.handleConnect(outputStream, inetAddress, port);
			} else {
				this.handleBind(outputStream, inetAddress, port);
			}
		} catch (IllegalCommandException e) {
			this.logger.info("Illegal command", e);

			try {
				writeResponse(outputStream,
						SocksImplementation4.REQUEST_REJECTED, port,
						inetAddress);
			} catch (IOException e1) {
			}
		} catch (final RuntimeException | IOException e) {
			this.logger.info("Failed to setup connection to {}:{}",
					inetAddress, port, e);
		} finally {
			try {
				inputStream.close();
			} catch (final Exception e) {
			}

			try {
				outputStream.close();
			} catch (final Exception e) {
			}

			this.cleanup();
		}
	}

	protected void handleConnect(OutputStream outputStream,
			InetAddress inetAddress, int port) throws IOException {
		final Socket hostSocket;
		try {
			hostSocket = this.openConnection(inetAddress, port);
		} catch (final IOException e) {
			this.logger.info("Failed to connected to {}:{}, result 0x{}",
					inetAddress.getHostAddress(), port, Integer.toHexString(
							SocksImplementation4.REQUEST_REJECTED));

			writeResponse(outputStream, SocksImplementation4.REQUEST_REJECTED,
					port, inetAddress);
			return;
		}

		writeResponse(outputStream, SocksImplementation4.REQUEST_GRANTED, port,
				hostSocket.getInetAddress());

		this.tunnel(this.getClientSocket(), hostSocket);
	}

	protected void handleBind(OutputStream outputStream,
			InetAddress inetAddress, int port) throws IOException {

		try (final ServerSocket serverSocket = this.bindConnection(inetAddress,
				port)) {

			writeResponse(outputStream, SocksImplementation4.REQUEST_GRANTED,
					serverSocket.getLocalPort(),
					serverSocket.getInetAddress());

			try (Socket remoteSocket = serverSocket.accept()) {
				if (remoteSocket.getInetAddress().equals(inetAddress)) {
					writeResponse(outputStream,
							SocksImplementation4.REQUEST_GRANTED,
							remoteSocket.getPort(),
							remoteSocket.getInetAddress());
					this.tunnel(this.getClientSocket(), remoteSocket);
				} else {
					writeResponse(outputStream,
							SocksImplementation4.REQUEST_REJECTED,
							remoteSocket.getPort(),
							remoteSocket.getInetAddress());
				}
			}
		} catch (IOException e) {
			writeResponse(outputStream, SocksImplementation4.REQUEST_REJECTED,
					port, null);
		}
	}

	protected OutputStream getOutputStream() throws IOException {
		return this.getClientSocket().getOutputStream();
	}

	protected DataInputStream getInputStream() throws IOException {
		return new DataInputStream(this.getClientSocket().getInputStream());
	}

	protected InetAddress getAddress(final DataInputStream inputStream)
			throws IOException, UnknownHostException {
		final byte[] rawIp = new byte[4];
		inputStream.readFully(rawIp);

		final byte[] buf = new byte[1];

		int length;
		while ((length = inputStream.read(buf)) >= 0) {
			if ((length > 0) && (buf[0] == 0)) {
				break;
			}
		}

		// SOCKS4a extension
		if ((rawIp[0] == 0) && (rawIp[1] == 0) && (rawIp[2] == 0)
				&& (rawIp[3] != 0)) {
			final StringBuilder builder = new StringBuilder();
			while ((length = inputStream.read(buf)) >= 0) {
				if ((length > 0) && (buf[0] == 0)) {
					break;
				}
				builder.append((char) buf[0]);
			}
			return resolveHostname(builder);
		} else {
			return InetAddress.getByAddress(rawIp);
		}
	}

	protected InetAddress resolveHostname(final StringBuilder builder)
			throws UnknownHostException {
		return InetAddress.getByName(builder.toString());
	}

	protected int getPort(final DataInputStream inputStream)
			throws IOException {
		return inputStream.readShort() & 0xFFFF;
	}

	protected void writeResponse(final OutputStream outputStream,
			final byte status, final int port, final InetAddress inetAddress)
			throws IOException {
		final ByteBuffer response = ByteBuffer.allocate(8);

		response.put(SocksImplementation4.NULL);
		response.put(status);
		response.putShort((short) port);

		if (inetAddress != null) {
			response.put(inetAddress.getAddress());
		}

		outputStream.write(response.array());
		outputStream.flush();
	}
}
