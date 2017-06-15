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

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TunnelThreadTest {

	@Mock
	private CountDownLatch countDownLatch;

	@Mock
	private Socket inputSocket;

	@Mock
	private Socket outputSocket;

	private TunnelThread tunnelThread;

	private static final byte[] DATA = "Happy Panda"
			.getBytes(StandardCharsets.UTF_8);

	@Before
	public void before() {
		this.tunnelThread = new TunnelThread(this.countDownLatch,
				this.inputSocket, this.outputSocket);
	}

	@Test
	public void testRun() throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		when(this.inputSocket.getInputStream())
				.thenReturn(new ByteArrayInputStream(DATA));
		when(this.outputSocket.getOutputStream()).thenReturn(outputStream);

		this.tunnelThread.run();

		verify(this.countDownLatch).countDown();
		verify(this.inputSocket).shutdownInput();
		verify(this.outputSocket).shutdownOutput();

		assertArrayEquals(DATA, outputStream.toByteArray());
	}

}
