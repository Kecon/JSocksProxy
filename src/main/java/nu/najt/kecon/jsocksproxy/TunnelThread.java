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

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import nu.najt.kecon.jsocksproxy.utils.SocketUtils;

/**
 * The thread which make copy a socket from input to output and signal count
 * down when EOF has been received.
 * 
 * @author Kenny Colliander Nordin
 * @since 3.0
 */
public final class TunnelThread implements Runnable {

	private final CountDownLatch countDownLatch;

	private final Socket inputSocket;

	private final Socket outputSocket;

	/**
	 * Constructor
	 * 
	 * @param countDownLatch
	 *            the count down latch that will count down when copy completes
	 * @param inputSocket
	 *            the input socket
	 * @param outputSocket
	 *            the output socket
	 */
	public TunnelThread(final CountDownLatch countDownLatch,
			final Socket inputSocket, final Socket outputSocket) {
		this.countDownLatch = countDownLatch;
		this.inputSocket = inputSocket;
		this.outputSocket = outputSocket;
	}

	@Override
	public void run() {
		try {
			SocketUtils.copy(this.inputSocket, this.outputSocket);
		} catch (final IOException ignore) {
		}

		this.countDownLatch.countDown();
	}
}