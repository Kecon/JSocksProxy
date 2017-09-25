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
package nu.najt.kecon.jsocksproxy.socks5;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nu.najt.kecon.jsocksproxy.AbstractSocksImplementation;
import nu.najt.kecon.jsocksproxy.ConfigurationFacade;
import nu.najt.kecon.jsocksproxy.IllegalAddressTypeException;
import nu.najt.kecon.jsocksproxy.IllegalCommandException;
import nu.najt.kecon.jsocksproxy.ProtocolException;

/**
 * This is the SOCKS5 implementation.<br>
 * <br>
 * More about the SOCKS protocol
 * <a href="http://en.wikipedia.org/wiki/SOCKS">http
 * ://en.wikipedia.org/wiki/SOCKS</a>
 * 
 * @author Kenny Colliander Nordin
 * 
 */
public class SocksImplementation5 extends AbstractSocksImplementation {

	private static final byte PROTOCOL_VERSION = 0x05;

	private static final Logger LOG = LoggerFactory
			.getLogger(SocksImplementation5.class.getPackage().getName());

	/**
	 * Constructor
	 * 
	 * @param configurationFacade
	 *            the configuration facade
	 * @param clientSocket
	 *            the client socket
	 * @param executor
	 *            the executor
	 */
	public SocksImplementation5(final ConfigurationFacade configurationFacade,
			final Socket clientSocket, final Executor executor) {
		super(configurationFacade, clientSocket, SocksImplementation5.LOG,
				executor);
	}

	@Override
	public void run() {
		DataInputStream inputStream = null;
		DataOutputStream outputStream = null;
		Socket clientSocket = null;
		EndPoint endPoint = null;

		try {
			this.setup();

			// Handshake
			inputStream = getInputStream();
			outputStream = getOutputStream();

			this.authenticate(inputStream, outputStream);
			this.readVersion(inputStream);
			final Command command = Command.valueOf(inputStream);
			inputStream.readByte(); // reserved byte
			endPoint = this.parseRemoteAddressPort(inputStream);
			clientSocket = handleCommand(outputStream, command, endPoint);
		} catch (final UnknownHostException e) {
			this.logger.warn("Failed to resolve host", e);
			try {
				this.writeResponse(outputStream, Status.HOST_UNREACHABLE,
						getAddressType(endPoint), null, null, 0);
			} catch (final IOException ioe) {
			}
		} catch (final RuntimeException e) {
			this.logger.warn("Unknown error occurred", e);

			try {
				this.writeResponse(outputStream,
						Status.GENERAL_SOCKS_SERVER_FAILURE,
						getAddressType(endPoint), null, null, 0);
			} catch (final IOException ioe) {
			}
		} catch (final ProtocolException | IllegalCommandException e) {
			try {
				this.writeResponse(outputStream, Status.COMMAND_NOT_SUPPORTED,
						getAddressType(endPoint), null, null, 0);
			} catch (final IOException ioe) {
			}
		} catch (final IOException e) {
		} catch (final IllegalAddressTypeException e) {
			try {
				this.writeResponse(outputStream,
						Status.ADDRESS_TYPE_NOT_SUPPORTED,
						getAddressType(endPoint), null, null, 0);
			} catch (final IOException ioe) {
			}
		} finally {
			safeClose(inputStream);
			safeClose(outputStream);
			safeClose(this.getClientSocket());
			safeClose(clientSocket);

			this.cleanup();
		}
	}

	private DataOutputStream getOutputStream() throws IOException {
		return new DataOutputStream(
				new BufferedOutputStream(this.getClientOutputStream()));
	}

	private DataInputStream getInputStream() throws IOException {
		return new DataInputStream(this.getClientInputStream());
	}

	private void readVersion(final DataInputStream inputStream)
			throws IOException, ProtocolException {
		final byte socksVersion = inputStream.readByte();

		if (socksVersion != SocksImplementation5.PROTOCOL_VERSION) {
			throw new ProtocolException("Unsupported version: 0x"
					+ Integer.toHexString(socksVersion));
		}
	}

	protected void authenticate(final DataInputStream inputStream,
			final DataOutputStream outputStream) throws IOException {
		final int numberOfAuthMethods = inputStream.readByte() & 0xFF;

		boolean supported = false;
		for (int i = 0; i < numberOfAuthMethods; i++) {
			final byte authMethod = inputStream.readByte();

			if (authMethod == 0x00) {
				supported = true;
			}
		}

		final byte handshakeResponse[] = new byte[2];
		handshakeResponse[0] = SocksImplementation5.PROTOCOL_VERSION;

		if (supported) {
			handshakeResponse[1] = 0x00;
			outputStream.write(handshakeResponse);
			outputStream.flush();
		} else {
			handshakeResponse[1] = (byte) 0xff;
			outputStream.write(handshakeResponse);
			outputStream.flush();
			this.logger.info("No supported authentication methods specified");
			throw new EOFException();
		}
	}

	public EndPoint parseRemoteAddressPort(DataInputStream inputStream)
			throws IllegalAddressTypeException, IOException {
		final AddressType addressType = AddressType
				.valueOf(inputStream.readByte());
		final byte[] hostname;
		final String host;
		final int port;
		final InetAddress remoteInetAddress;

		if (addressType == AddressType.IP_V4) {

			final byte[] address = new byte[4];
			inputStream.readFully(address);
			remoteInetAddress = InetAddress.getByAddress(address);
			hostname = null;

		} else if (addressType == AddressType.IP_V6) {

			final byte[] address = new byte[16];
			inputStream.readFully(address);
			remoteInetAddress = InetAddress.getByAddress(address);
			hostname = null;

		} else if (addressType == AddressType.DOMAIN) {

			final int hostLength = inputStream.readByte() & 0xFF;
			final byte[] hostBuf = new byte[hostLength];

			inputStream.readFully(hostBuf);
			remoteInetAddress = InetAddress
					.getByName(new String(hostBuf, "US-ASCII"));
			hostname = hostBuf;

		} else {
			// Should be impossible
			throw new IllegalAddressTypeException(
					"Unsupported address type: " + addressType);
		}

		host = remoteInetAddress.getHostAddress();
		port = inputStream.readShort() & 0xFFFF;
		return new EndPoint(addressType, remoteInetAddress, hostname, host,
				port);
	}

	protected Socket handleConnect(final DataOutputStream outputStream,
			final EndPoint endPoint) throws IOException {
		Socket clientSocket = null;
		try {
			clientSocket = this.openConnection(endPoint.getRemoteInetAddress(),
					endPoint.getPort());

			this.writeResponse(outputStream, Status.SUCCEEDED,
					endPoint.getAddressType(), clientSocket.getLocalAddress(),
					endPoint.getHostname(), clientSocket.getLocalPort());

			this.tunnel(this.getClientSocket(), clientSocket);

			this.logger.debug("Disconnected");
		} catch (final IOException e) {
			this.logger.info("Failed to connect to: {}:{}", endPoint.getHost(),
					endPoint.getPort());

			this.writeResponse(outputStream, Status.HOST_UNREACHABLE,
					endPoint.getAddressType(), endPoint.getRemoteInetAddress(),
					endPoint.getHostname(), endPoint.getPort());
		}
		return clientSocket;
	}

	protected Socket handleBind(final DataOutputStream outputStream,
			final EndPoint endPoint) throws IOException {
		final Socket clientSocket;
		final ServerSocket serverSocket = this.bindConnection(
				endPoint.getRemoteInetAddress(), endPoint.getPort());

		try {

			this.writeResponse(outputStream, Status.SUCCEEDED,
					endPoint.getAddressType(), serverSocket.getInetAddress(),
					endPoint.getHostname(), serverSocket.getLocalPort());

			clientSocket = serverSocket.accept();
			this.logger.info("Accepted");
		} finally {
			serverSocket.close();
		}

		this.writeResponse(outputStream, Status.SUCCEEDED,
				endPoint.getAddressType(), clientSocket.getInetAddress(), null,
				clientSocket.getPort());

		this.tunnel(this.getClientSocket(), clientSocket);
		return clientSocket;
	}

	private AddressType getAddressType(final EndPoint endPoint) {
		return endPoint == null ? AddressType.IP_V4
				: endPoint.getAddressType();
	}

	private Socket handleCommand(final DataOutputStream outputStream,
			final Command command, EndPoint endPoint)
			throws IOException, IllegalCommandException {

		if (command == Command.CONNECT) {
			return handleConnect(outputStream, endPoint);
		} else if (command == Command.BIND) {
			return handleBind(outputStream, endPoint);
		} else {
			throw new IllegalCommandException("Unknown command: " + command);
		}
	}

	private void safeClose(final Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (final IOException e) {
			}
		}
	}

	/**
	 * Write response back to client
	 * 
	 * @param outputStream
	 *            the output stream
	 * @param status
	 *            the status
	 * @param addressType
	 *            the address type
	 * @param boundAddress
	 *            the bound address
	 * @param hostname
	 *            the hostname
	 * @param port
	 *            the port
	 * @throws IOException
	 *             if an I/O exception occurs
	 */
	protected void writeResponse(final DataOutputStream outputStream,
			final Status status, final AddressType addressType,
			final InetAddress boundAddress, final byte[] hostname,
			final int port) throws IOException {

		byte[] safeAddress = null;
		if (boundAddress == null) {
			if (addressType == AddressType.IP_V6) {
				safeAddress = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						0, 0, 0, 0 };
			} else {
				safeAddress = new byte[] { 0, 0, 0, 0 };
			}
		} else if (hostname == null) {
			safeAddress = boundAddress.getAddress();
		} else {
			safeAddress = hostname;
		}

		if (status != Status.SUCCEEDED) {
			this.logger.info("Client failed to connect, result 0x{} {}",
					Integer.toHexString(status.getValue()), status);
		}

		outputStream.write(SocksImplementation5.PROTOCOL_VERSION);
		outputStream.write(status.getValue());
		outputStream.write(0x00); // reserved

		if (addressType == AddressType.IP_V4) {
			outputStream.write(AddressType.IP_V4.getValue());
			outputStream.write(safeAddress);

		} else if (addressType == AddressType.IP_V6) {
			outputStream.write(AddressType.IP_V6.getValue());
			outputStream.write(safeAddress);

		} else if (addressType == AddressType.DOMAIN) {
			outputStream.write(AddressType.DOMAIN.getValue());
			if (safeAddress == null) {
				outputStream.write(0x00);
			} else {
				outputStream.write(safeAddress.length);
				outputStream.write(safeAddress);
			}
		} else {
			throw new IllegalStateException(
					"Unknown address type: " + addressType);
		}

		outputStream.writeShort(port & 0xFFFF);
		outputStream.flush();
	}
}
