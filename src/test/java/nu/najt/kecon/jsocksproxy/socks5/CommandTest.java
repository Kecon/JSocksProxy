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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import nu.najt.kecon.jsocksproxy.IllegalCommandException;

import org.junit.Test;

public class CommandTest {

	@Test
	public void testValueOf() {

		try {
			assertEquals(Command.CONNECT, Command.valueOf((byte) 0x01));
			assertEquals(Command.BIND, Command.valueOf((byte) 0x02));
			assertEquals(Command.UDP_ASSOCIATE, Command.valueOf((byte) 0x03));
		} catch (IllegalCommandException e) {
			fail();
		}

		try {
			Command.valueOf((byte) 0x00);
			fail();
		} catch (IllegalCommandException e) {
		}
	}

	public void testGetValue() {
		assertEquals(0x01, Command.CONNECT.getValue());
		assertEquals(0x02, Command.BIND.getValue());
		assertEquals(0x03, Command.UDP_ASSOCIATE.getValue());
	}
}
