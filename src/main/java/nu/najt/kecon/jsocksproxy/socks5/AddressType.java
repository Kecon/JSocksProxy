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
package nu.najt.kecon.jsocksproxy.socks5;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nu.najt.kecon.jsocksproxy.IllegalAddressTypeException;

/**
 * Available address types for SOCKS5
 * 
 * @author Kenny Colliander Nordin
 */
public enum AddressType {
	/** Connect */
	IP_V4((byte) 0x01),

	/** Bind */
	DOMAIN((byte) 0x03),

	/** UDP Associate */
	IP_V6((byte) 0x04);

	private static final Map<Byte, AddressType> map;

	private final byte value;

	static {
		final Map<Byte, AddressType> commands = new HashMap<Byte, AddressType>();
		for (final AddressType command : AddressType.values()) {
			commands.put(command.getValue(), command);
		}

		map = Collections.unmodifiableMap(commands);
	}

	private AddressType(final byte value) {
		this.value = value;
	}

	public byte getValue() {
		return this.value;
	}

	public static AddressType valueOf(final Byte b)
			throws IllegalAddressTypeException {
		final AddressType command = AddressType.map.get(b);

		if (command == null) {
			throw new IllegalAddressTypeException(
					"Unknown address type: 0x" + Integer.toHexString(b));
		}

		return command;
	}
}
