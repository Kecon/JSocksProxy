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
package nu.najt.kecon.jsocksproxy.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Socket utilities
 * 
 * @author Kenny Colliander Nordin
 */
public class SocketUtils {
	/**
	 * Copy data from input socket to output socket
	 * 
	 * @param inputSocket
	 *            the input socket
	 * @param outputSocket
	 *            the output socket
	 */
	public static void copy(final Socket inputSocket, final Socket outputSocket)
			throws IOException {

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
				} catch (final InterruptedIOException ioe) {
				}
			}
		} finally {
			try {
				inputSocket.shutdownInput();
			} catch (final Exception e) {
			}
			try {
				outputSocket.shutdownOutput();
			} catch (final Exception e) {
			}
		}
	}
}
