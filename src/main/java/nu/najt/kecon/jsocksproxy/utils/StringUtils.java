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
package nu.najt.kecon.jsocksproxy.utils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * String utilities
 * 
 * @author Kenny Colliander Nordin
 */
public class StringUtils {
	private static final char COLON = ':';
	private static final char END_BRACKET = ']';
	private static final char START_BRACKET = '[';

	/**
	 * Create a string representation of a {@link InetSocketAddress}
	 * 
	 * @param inetSocketAddress
	 *            the address that should be represented
	 * @return the string representation
	 * @throws IllegalArgumentException
	 *             if inetSocketAddress is null
	 */
	public static String formatSocketAddress(
			final InetSocketAddress inetSocketAddress) {

		if (inetSocketAddress == null) {
			throw new IllegalArgumentException("inetSocketAddress");
		}

		final StringBuilder builder = new StringBuilder();

		final InetAddress inetAddress = inetSocketAddress.getAddress();

		if (inetAddress instanceof Inet6Address) {
			builder.append(START_BRACKET);
			builder.append(inetAddress.getHostAddress());
			builder.append(END_BRACKET);
		} else {
			builder.append(inetAddress.getHostAddress());
		}

		builder.append(COLON);
		builder.append(inetSocketAddress.getPort());

		return builder.toString();
	}

	/**
	 * Create a string representation of a {@link Socket}
	 * 
	 * @param socket
	 *            the address that should be represented
	 * @return the string representation
	 * @throws IllegalArgumentException
	 *             if socket is null
	 */
	public static String formatSocket(final Socket socket) {

		if (socket == null) {
			throw new IllegalArgumentException("socket");
		}

		final StringBuilder builder = new StringBuilder();

		final InetAddress inetAddress = socket.getInetAddress();

		if (inetAddress instanceof Inet6Address) {
			builder.append(START_BRACKET);
			builder.append(inetAddress.getHostAddress());
			builder.append(END_BRACKET);
		} else {
			builder.append(inetAddress.getHostAddress());
		}
		builder.append(COLON);
		builder.append(socket.getPort());

		return builder.toString();
	}

	/**
	 * Create a string representation of a {@link Socket}
	 * 
	 * @param socket
	 *            the address that should be represented
	 * @return the string representation
	 * @throws IllegalArgumentException
	 *             if socket is null
	 */
	public static String formatLocalSocket(final Socket socket) {

		if (socket == null) {
			throw new IllegalArgumentException("socket");
		}

		final StringBuilder builder = new StringBuilder();

		final InetAddress inetAddress = socket.getLocalAddress();

		if (inetAddress instanceof Inet6Address) {
			builder.append(START_BRACKET);
			builder.append(inetAddress.getHostAddress());
			builder.append(END_BRACKET);
		} else {
			builder.append(inetAddress.getHostAddress());
		}
		builder.append(COLON);
		builder.append(socket.getLocalPort());

		return builder.toString();
	}

	/**
	 * Create a string representation of a {@link Socket}
	 * 
	 * @param socket
	 *            the address that should be represented
	 * @return the string representation
	 * @throws IllegalArgumentException
	 *             if socket is null
	 */
	public static String formatSocket(final ServerSocket socket) {

		if (socket == null) {
			throw new IllegalArgumentException("socket");
		}

		final StringBuilder builder = new StringBuilder();

		final InetAddress inetAddress = socket.getInetAddress();

		if (inetAddress instanceof Inet6Address) {
			builder.append(START_BRACKET);
			builder.append(inetAddress.getHostAddress());
			builder.append(END_BRACKET);
		} else {
			builder.append(inetAddress.getHostAddress());
		}
		builder.append(COLON);
		builder.append(socket.getLocalPort());

		return builder.toString();
	}

}
