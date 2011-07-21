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
package nu.najt.kecon.jsocksproxy.socks4;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import nu.najt.kecon.jsocksproxy.AbstractSocksImplementation;
import nu.najt.kecon.jsocksproxy.IllegalCommandException;
import nu.najt.kecon.jsocksproxy.JSocksProxy;

/**
 * This is the SOCKS4 implementation. <br>
 * <br>
 * More about the SOCKS protocol <a
 * href="http://en.wikipedia.org/wiki/SOCKS">http
 * ://en.wikipedia.org/wiki/SOCKS</a>
 * 
 * @author Kenny Colliander Nordin
 * 
 */
public class SocksImplementation4 extends AbstractSocksImplementation {

	protected static final byte REQUEST_GRANTED = 0x5a;

	protected static final byte REQUEST_REJECTED = 0x5b;

	protected static final byte AUTHORIZATION_FAILED = 0x5d;

	protected static final byte NULL = 0x00;

	private static final Logger staticLogger = Logger
			.getLogger("nu.najt.kecon.jsocksproxy.socks4");

	/**
	 * Constructor
	 * 
	 * @param jSocksProxy
	 *            the parent instance
	 * @param socket
	 *            the socket
	 */
	public SocksImplementation4(final JSocksProxy jSocksProxy,
			final Socket socket) {
		super(jSocksProxy, socket, staticLogger);
	}

	public void handle() {
		DataInputStream inputStream = null;
		DataOutputStream outputStream = null;
		String host = null;
		int port = -1;
		try {
			inputStream = new DataInputStream(this.getSocket().getInputStream());
			outputStream = new DataOutputStream(this.getSocket()
					.getOutputStream());

			final Command command = Command.valueOf(inputStream.readByte());

			if (command != Command.CONNECT) {
				throw new IllegalCommandException("Unknown command: " + command);
			}

			port = inputStream.readShort() & 0xFFFF;

			final byte[] rawIp = new byte[4];

			inputStream.readFully(rawIp);

			InetAddress inetAddress = InetAddress.getByAddress(rawIp);

			final byte[] buf = new byte[1];

			int length;
			while ((length = inputStream.read(buf)) >= 0) {
				if (length > 0 && buf[0] == 0) {
					break;
				}
			}

			// SOCKS4a extension
			if (rawIp[0] == 0 && rawIp[1] == 0 && rawIp[2] == 0
					&& rawIp[3] != 0) {
				final StringBuilder builder = new StringBuilder();
				while ((length = inputStream.read(buf)) >= 0) {
					if (length > 0 && buf[0] == 0) {
						break;
					}
					builder.append(buf);
				}
				host = builder.toString();
				inetAddress = InetAddress.getByName(host);
			}

			host = inetAddress.getHostAddress();

			final Socket hostSocket;
			try {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Connecting to " + host + ":" + port + "...");
				}
				hostSocket = this.openConnection(inetAddress, port);

				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Connected to " + host + ":" + port);
				}
			} catch (IOException e) {
				this.logger
						.info("Client "
								+ this.getSocket().getInetAddress()
										.getHostAddress() + ":"
								+ this.getSocket().getPort()
								+ " failed to connected to " + host + ":"
								+ port + ", result 0x"
								+ Integer.toHexString(REQUEST_REJECTED));

				final ByteBuffer response = ByteBuffer.allocate(8);

				response.put(NULL);
				response.put(REQUEST_REJECTED);
				response.putShort((short) port);
				response.put(rawIp);

				outputStream.write(response.array());
				outputStream.flush();
				return;
			}

			final ByteBuffer response = ByteBuffer.allocate(8);

			response.put(NULL);
			response.put(REQUEST_GRANTED);
			response.putShort((short) port);
			response.put(hostSocket.getInetAddress().getAddress());

			outputStream.write(response.array());
			outputStream.flush();

			this.logger.info("Established connection between "
					+ this.getSocket().getInetAddress().getHostAddress() + ":"
					+ this.getSocket().getPort() + " and "
					+ hostSocket.getInetAddress().getHostAddress() + ":"
					+ hostSocket.getPort());

			this.tunnel(this.getSocket(), hostSocket);

		} catch (Exception e) {
			this.logger.log(Level.WARNING, "Client "
					+ this.getSocket().getInetAddress().getHostAddress() + ":"
					+ this.getSocket().getPort()
					+ " failed to setup connection to " + host + ":" + port, e);
		} finally {
			try {
				inputStream.close();
			} catch (Exception e) {
			}

			try {
				outputStream.close();
			} catch (Exception e) {
			}
		}
	}
}
