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
package nu.najt.kecon.jsocksproxy;

import java.net.InetAddress;
import java.util.List;

/**
 * Contains methods for accessing the running configuration
 * 
 * @author Kenny Colliander Nordin
 * 
 */
public interface ConfigurationFacade {
	/**
	 * @return list of outgoing addresses
	 */
	public List<InetAddress> getOutgoingSourceAddresses();

	/**
	 * @return true if SOCKS version 4 is allowed
	 */
	public boolean isAllowSocks4();

	/**
	 * @return true if SOCKS version 5 is allowed
	 */
	public boolean isAllowSocks5();
}
