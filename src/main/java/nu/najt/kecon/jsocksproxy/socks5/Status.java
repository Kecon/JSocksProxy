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
package nu.najt.kecon.jsocksproxy.socks5;

/**
 * Available status codes for SOCKS 5
 * 
 * @author Kenny Colliander Nordin
 */
public enum Status {
	/** Succeeded */
	SUCCEEDED((byte) 0x00),

	/** General SOCKS server failure */
	GENERAL_SOCKS_SERVER_FAILURE((byte) 0x01),

	/** Connection not allowed by ruleset */
	CONNECTION_NOT_ALLOWED_BY_RULESET((byte) 0x02),

	/** Network unreachable */
	NETWORK_UNREACHABLE((byte) 0x03),

	/** Host unreachable */
	HOST_UNREACHABLE((byte) 0x04),

	/** Connection refused by destination host */
	CONNECTION_REFUSED_BY_DESTINATION_HOST((byte) 0x05),

	/** TTL expired */
	TTL_EXPIRED((byte) 0x06),

	/** Command not supported */
	COMMAND_NOT_SUPPORTED((byte) 0x07),

	/** Address type not supported */
	ADDRESS_TYPE_NOT_SUPPORTED((byte) 0x08);

	private final byte value;

	private Status(final byte value) {
		this.value = value;
	}

	public byte getValue() {
		return this.value;
	}
}
