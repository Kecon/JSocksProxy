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
package nu.najt.kecon.jsocksproxy.socks4;

import nu.najt.kecon.jsocksproxy.IllegalCommandException;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Testing of <code>Command</code>
 * 
 * @author Kenny Colliander Nordin
 * @since 2017-06-20
 */
public class CommandTest {

	@Test
	public void testValueOfConnect() throws IllegalCommandException {
		assertEquals(Command.CONNECT, Command.valueOf((byte) 0x01));
	}

	@Test
	public void testValueOfBind() throws IllegalCommandException {
		assertEquals(Command.BIND, Command.valueOf((byte) 0x02));
	}

	@Test(expected = IllegalCommandException.class)
	public void testValueOfUnknown() throws IllegalCommandException {
		Command.valueOf((byte) 0x00);
	}

	@Test
	public void testGetValueConnect() {
		assertEquals(0x01, Command.CONNECT.getValue());
	}

	@Test
	public void testGetValueBind() {
		assertEquals(0x02, Command.BIND.getValue());
	}
}
