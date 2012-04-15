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
package nu.najt.kecon.jsocksproxy.socks4;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nu.najt.kecon.jsocksproxy.IllegalCommandException;

/**
 * Available commands for SOCKS v4 and v4a
 * 
 * @author Kenny Colliander Nordin
 */
public enum Command {
	/** Connect */
	CONNECT((byte) 0x01),

	/** Bind */
	BIND((byte) 0x02);

	private final byte value;

	private Command(final byte value) {
		this.value = value;
	}

	public byte getValue() {
		return this.value;
	}

	private static final Map<Byte, Command> map;
	static {
		final Map<Byte, Command> commands = new HashMap<Byte, Command>();
		for (final Command command : Command.values()) {
			commands.put(command.getValue(), command);
		}

		map = Collections.unmodifiableMap(commands);
	}

	public static Command valueOf(final Byte b) throws IllegalCommandException {
		final Command command = Command.map.get(b);

		if (command == null) {
			throw new IllegalCommandException("Unknown command command: 0x"
					+ Integer.toHexString(b));
		}

		return command;
	}

}
