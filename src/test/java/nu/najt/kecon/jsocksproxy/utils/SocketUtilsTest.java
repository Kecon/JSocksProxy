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

import static nu.najt.kecon.jsocksproxy.utils.SocketUtils.copy;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Testing copy method
 * 
 * @author Kenny Colliander Nordin
 */
@RunWith(MockitoJUnitRunner.class)
public class SocketUtilsTest {

	private static final byte[] TEST_BYTES = new byte[10000];

	static {
		new Random(1l).nextBytes(TEST_BYTES);
	}

	@Mock
	private Socket input;

	@Mock
	private Socket output;

	@Test
	public void testCopy() throws IOException {
		try (InputStream inputStream = new ByteArrayInputStream(TEST_BYTES)) {
			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

				when(input.getInputStream()).thenReturn(inputStream);
				when(output.getOutputStream()).thenReturn(outputStream);

				copy(input, output);

				assertArrayEquals(TEST_BYTES, outputStream.toByteArray());
			}
		}
	}

}
