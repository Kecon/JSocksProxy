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
package nu.najt.kecon.jsocksproxy.configuration;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This is the XML root object for the configuration
 * 
 * @author Kenny Colliander Nordin
 * 
 */
@XmlRootElement
public class Configuration {

	private int backlog;

	private List<String> outgoingAddresses;

	private List<Listen> listen;

	private boolean allowSocks4 = true;

	private boolean allowSocks5 = true;

	/**
	 * @return the backlog
	 */
	public int getBacklog() {
		return this.backlog;
	}

	/**
	 * @param backlog
	 *            the backlog to set
	 */
	public void setBacklog(final int backlog) {
		this.backlog = backlog;
	}

	/**
	 * @return the outgoingAddress
	 */
	@XmlElement(name = "outgoingAddress")
	public List<String> getOutgoingAddresses() {
		return this.outgoingAddresses;
	}

	/**
	 * @param outgoingAddresses
	 *            the outgoingAddress to set
	 */
	public void setOutgoingAddresses(final List<String> outgoingAddresses) {
		this.outgoingAddresses = outgoingAddresses;
	}

	/**
	 * @return the listen
	 */
	public List<Listen> getListen() {
		return this.listen;
	}

	/**
	 * @param listen
	 *            the listen to set
	 */
	public void setListen(final List<Listen> listen) {
		this.listen = listen;
	}

	/**
	 * @return the allowSocks4
	 */
	@XmlElement(defaultValue = "true")
	public boolean isAllowSocks4() {
		return this.allowSocks4;
	}

	/**
	 * @param allowSocks4
	 *            the allowSocks4 to set
	 */
	public void setAllowSocks4(final boolean allowSocks4) {
		this.allowSocks4 = allowSocks4;
	}

	/**
	 * @return the allowSocks5
	 */
	@XmlElement(defaultValue = "true")
	public boolean isAllowSocks5() {
		return this.allowSocks5;
	}

	/**
	 * @param allowSocks5
	 *            the allowSocks5 to set
	 */
	public void setAllowSocks5(final boolean allowSocks5) {
		this.allowSocks5 = allowSocks5;
	}

}
