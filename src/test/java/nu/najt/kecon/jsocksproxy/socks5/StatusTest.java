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

import org.junit.Assert;
import org.junit.Test;

public class StatusTest {

	@Test
	public void testGetValue() {
		Assert.assertEquals(0x00, Status.SUCCEEDED.getValue());
		Assert.assertEquals(0x01,
				Status.GENERAL_SOCKS_SERVER_FAILURE.getValue());
		Assert.assertEquals(0x02,
				Status.CONNECTION_NOT_ALLOWED_BY_RULESET.getValue());
		Assert.assertEquals(0x03, Status.NETWORK_UNREACHABLE.getValue());
		Assert.assertEquals(0x04, Status.HOST_UNREACHABLE.getValue());
		Assert.assertEquals(0x05,
				Status.CONNECTION_REFUSED_BY_DESTINATION_HOST.getValue());
		Assert.assertEquals(0x06, Status.TTL_EXPIRED.getValue());
		Assert.assertEquals(0x07, Status.COMMAND_NOT_SUPPORTED.getValue());
	}

}
