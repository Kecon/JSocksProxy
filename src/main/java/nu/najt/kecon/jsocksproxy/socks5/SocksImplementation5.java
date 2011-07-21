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
package nu.najt.kecon.jsocksproxy.socks5;

import static nu.najt.kecon.jsocksproxy.utils.StringUtils.*;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import nu.najt.kecon.jsocksproxy.AbstractSocksImplementation;
import nu.najt.kecon.jsocksproxy.IllegalAddressTypeException;
import nu.najt.kecon.jsocksproxy.IllegalCommandException;
import nu.najt.kecon.jsocksproxy.JSocksProxy;
import nu.najt.kecon.jsocksproxy.ProtocolException;

/**
 * This is the SOCKS5 implementation.<br>
 * <br>
 * More about the SOCKS protocol <a
 * href="http://en.wikipedia.org/wiki/SOCKS">http
 * ://en.wikipedia.org/wiki/SOCKS</a>
 * 
 * @author Kenny Colliander Nordin
 * 
 */
public class SocksImplementation5 extends AbstractSocksImplementation {

	private static final byte PROTOCOL_VERSION = 0x05;

	private static final Logger staticLogger = Logger
			.getLogger("nu.najt.kecon.jsocksproxy.socks5");

	/**
	 * Constructor
	 * 
	 * @param jSocksProxy
	 *            the parent instance
	 * @param socket
	 *            the socket
	 */
	public SocksImplementation5(final JSocksProxy jSocksProxy,
			final Socket socket) {
		super(jSocksProxy, socket, staticLogger);
	}

	@Override
	public void handle() {
		DataInputStream inputStream = null;
		DataOutputStream outputStream = null;
		Socket clientSocket = null;
		AddressType addressType = AddressType.IP_V4;
		String host = null;

		try {
			// Handshake
			inputStream = new DataInputStream(this.getClientInputStream());
			outputStream = new DataOutputStream(new BufferedOutputStream(
					this.getClientOutputStream()));

			this.authenticate(inputStream, outputStream);

			final byte socksVersion = inputStream.readByte();

			if (socksVersion != PROTOCOL_VERSION) {
				throw new ProtocolException("Unsupported version: 0x"
						+ Integer.toHexString(socksVersion));
			}

			final Command command = Command.valueOf(inputStream.readByte());

			inputStream.readByte(); // reserved byte

			addressType = AddressType.valueOf(inputStream.readByte());
			final InetAddress remoteInetAddress;
			if (addressType == AddressType.IP_V4) {

				final byte[] address = new byte[4];
				inputStream.readFully(address);
				remoteInetAddress = InetAddress.getByAddress(address);

			} else if (addressType == AddressType.IP_V6) {

				final byte[] address = new byte[16];
				inputStream.readFully(address);
				remoteInetAddress = InetAddress.getByAddress(address);

			} else if (addressType == AddressType.DOMAIN) {

				final int hostLength = inputStream.readByte() & 0xFF;
				final byte[] hostBuf = new byte[hostLength];

				inputStream.readFully(hostBuf);
				remoteInetAddress = InetAddress.getByName(new String(hostBuf,
						"US-ASCII"));

			} else {
				// Should be impossible
				throw new IllegalAddressTypeException(
						"Unsupported address type: " + addressType);
			}

			host = remoteInetAddress.getHostAddress();
			final int port = inputStream.readShort() & 0xFFFF;

			if (command == Command.CONNECT) {
				try {

					if (this.logger.isLoggable(Level.FINE)) {
						this.logger.fine("Connecting to " + host + ":" + port
								+ "...");
					}

					clientSocket = this.openConnection(remoteInetAddress, port);

					if (this.logger.isLoggable(Level.FINE)) {
						this.logger.fine("Connected to " + host + ":" + port);
					}

				} catch (IOException e) {
					this.logger.info("Failed to connect to: " + host + ":"
							+ port);
					this.writeResponse(outputStream, Status.HOST_UNREACHABLE,
							addressType, remoteInetAddress, port);
					return;
				}

				this.writeResponse(outputStream, Status.SUCCEEDED, addressType,
						clientSocket.getInetAddress(),
						clientSocket.getLocalPort());

				this.tunnel(this.getSocket(), clientSocket);

				if (this.logger.isLoggable(Level.FINE)) {
					this.logger.log(Level.FINE, "Disconnected from: " + host
							+ ":" + port);
				}
			} else if (command == Command.BIND) {
				final ServerSocket serverSocket = this.bindConnection(
						remoteInetAddress, port);

				try {

					this.writeResponse(outputStream, Status.SUCCEEDED,
							addressType, serverSocket.getInetAddress(),
							serverSocket.getLocalPort());

					if (this.logger.isLoggable(Level.FINE)) {
						this.logger.log(Level.FINE,
								"Bound to "
										+ serverSocket.getInetAddress()
												.getHostAddress() + ":"
										+ serverSocket.getLocalPort() + ":"
										+ port + " for "
										+ formatSocket(getSocket()));
					}

					clientSocket = serverSocket.accept();
					if (this.logger.isLoggable(Level.FINE)) {
						this.logger.log(Level.FINE, "Accepted "
								+ formatSocket(clientSocket) + " to "
								+ formatSocket(getSocket()));
					}
				} finally {
					serverSocket.close();
				}

				this.writeResponse(outputStream, Status.SUCCEEDED, addressType,
						clientSocket.getInetAddress(), clientSocket.getPort());

				this.tunnel(this.getSocket(), clientSocket);

				if (this.logger.isLoggable(Level.FINE)) {
					this.logger.log(Level.FINE, "Disconnected from: " + host
							+ ":" + port);
				}

			} else {
				throw new IllegalCommandException("Unknown command: " + command);
			}
		} catch (UnknownHostException e) {
			this.logger.log(Level.INFO, "Failed to resolve host: " + host, e);
			try {
				this.writeResponse(outputStream, Status.HOST_UNREACHABLE,
						addressType, null, 0);
			} catch (IOException ioe) {
			}
		} catch (RuntimeException e) {
			this.logger.log(Level.WARNING, e.getMessage(), e);

			try {
				this.writeResponse(outputStream,
						Status.GENERAL_SOCKS_SERVER_FAILURE, addressType, null,
						0);
			} catch (IOException ioe) {
			}
		} catch (ProtocolException e) {
			try {
				this.writeResponse(outputStream, Status.COMMAND_NOT_SUPPORTED,
						addressType, null, 0);
			} catch (IOException ioe) {
			}
		} catch (IllegalCommandException e) {
			try {
				this.writeResponse(outputStream, Status.COMMAND_NOT_SUPPORTED,
						addressType, null, 0);
			} catch (IOException ioe) {
			}
		} catch (IOException e) {
		} catch (IllegalAddressTypeException e) {
			try {
				this.writeResponse(outputStream,
						Status.ADDRESS_TYPE_NOT_SUPPORTED, addressType, null, 0);
			} catch (IOException ioe) {
			}
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
			}

			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
				}
			}

			if (this.getSocket() != null) {
				try {
					this.getSocket().close();
				} catch (IOException e) {
				}
			}

			if (clientSocket != null) {
				try {
					clientSocket.close();
				} catch (IOException e) {
				}
			}
		}
	}

	protected void authenticate(final DataInputStream inputStream,
			final DataOutputStream outputStream) throws IOException {
		final int numberOfAuthMethods = inputStream.readByte() & 0xFF;

		boolean supported = false;
		for (int i = 0; i < numberOfAuthMethods; i++) {
			byte authMethod = inputStream.readByte();

			if (authMethod == 0x00) {
				supported = true;
			}
		}

		final byte handshakeResponse[] = new byte[2];
		handshakeResponse[0] = PROTOCOL_VERSION;

		if (supported) {
			handshakeResponse[1] = 0x00;
			outputStream.write(handshakeResponse);
			outputStream.flush();
		} else {
			handshakeResponse[1] = (byte) 0xff;
			outputStream.write(handshakeResponse);
			outputStream.flush();
			this.logger
					.info("No supported authentication methods specified from "
							+ formatSocket(this.getSocket()));
			throw new EOFException();
		}
	}

	/**
	 * Write response back to client
	 * 
	 * @param originalOutputStream
	 *            the {@link OutputStream}
	 * @param status
	 *            the status <br>
	 * @param addressType
	 *            the addressType<br>
	 * @param address
	 *            the address
	 * @param port
	 *            the port
	 * @throws IOException
	 */
	protected void writeResponse(final DataOutputStream outputStream,
			final Status status, final AddressType addressType,
			final InetAddress boundAddress, final int port) throws IOException {

		byte[] safeAddress = null;
		if (boundAddress == null) {
			if (addressType == AddressType.IP_V6) {
				safeAddress = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						0, 0, 0, 0 };
			} else {
				safeAddress = new byte[] { 0, 0, 0, 0 };
			}
		} else {
			safeAddress = boundAddress.getAddress();
		}

		if (status != Status.SUCCEEDED) {
			this.logger.info("Client "
					+ this.getSocket().getInetAddress().getHostAddress() + ":"
					+ this.getSocket().getPort() + " failed to connected to "
					+ InetAddress.getByAddress(safeAddress).getHostAddress()
					+ ":" + port + ", result 0x"
					+ Integer.toHexString(status.getValue()) + " " + status);
		}

		outputStream.write(PROTOCOL_VERSION);
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
			throw new IllegalStateException("Unknown address type: "
					+ addressType);
		}

		outputStream.writeShort(port & 0xFFFF);
		outputStream.flush();
	}
}
