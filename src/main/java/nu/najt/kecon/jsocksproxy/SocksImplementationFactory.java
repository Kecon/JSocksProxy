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
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import nu.najt.kecon.jsocksproxy.socks4.SocksImplementation4;
import nu.najt.kecon.jsocksproxy.socks5.SocksImplementation5;

/**
 * The {@link SocksImplementationFactory} return a {@link SocksImplementation}
 * depending on what the client request to use. Current supported versions are
 * v4 and v5.
 * 
 * @author Kenny Colliander Nordin
 * 
 */
public class SocksImplementationFactory {

	private static final Executor EXECUTOR = Executors.newCachedThreadPool();

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
	public static final SocksImplementation getImplementation(
			final ConfigurationFacade configurationFacade, final Socket socket)
			throws IOException, AccessDeniedException, ProtocolException {
		final InputStream inputStream = socket.getInputStream();

		final int protocol = inputStream.read();

		switch (protocol) {
		case 0x04:
			if (configurationFacade.isAllowSocks4()) {
				return new SocksImplementation4(configurationFacade, socket,
						SocksImplementationFactory.EXECUTOR);
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
						SocksImplementationFactory.EXECUTOR);
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
			throw new ProtocolException("Unknown protocol: 0x"
					+ Integer.toHexString(protocol));
		}
	}
}
