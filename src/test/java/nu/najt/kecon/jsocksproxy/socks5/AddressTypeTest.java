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

import nu.najt.kecon.jsocksproxy.IllegalAddressTypeException;

import org.junit.Assert;
import org.junit.Test;

public class AddressTypeTest {

	@Test
	public void testValueOf() {

		try {
			Assert.assertEquals(AddressType.IP_V4,
					AddressType.valueOf((byte) 0x01));
			Assert.assertEquals(AddressType.DOMAIN,
					AddressType.valueOf((byte) 0x03));
			Assert.assertEquals(AddressType.IP_V6,
					AddressType.valueOf((byte) 0x04));
		} catch (final IllegalAddressTypeException e) {
			Assert.fail();
		}
		try {
			AddressType.valueOf((byte) 0x00);
			Assert.fail();
		} catch (final IllegalAddressTypeException e) {
		}
	}

	public void testGetValue() {
		Assert.assertEquals(0x01, AddressType.IP_V4.getValue());
		Assert.assertEquals(0x03, AddressType.DOMAIN.getValue());
		Assert.assertEquals(0x04, AddressType.IP_V6.getValue());
	}
}
