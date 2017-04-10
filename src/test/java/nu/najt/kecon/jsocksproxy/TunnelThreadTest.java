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
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TunnelThreadTest {

	@Mock
	CountDownLatch countDownLatch;

	@Mock
	Socket inputSocket;

	@Mock
	Socket outputSocket;

	TunnelThread tunnelThread;

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
