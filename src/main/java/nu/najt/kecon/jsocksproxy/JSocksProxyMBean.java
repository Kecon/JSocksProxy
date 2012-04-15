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

import javax.naming.NamingException;

/**
 * MBean interface for application servers.
 * 
 * @author Kenny Colliander Nordin
 * 
 */
public interface JSocksProxyMBean {

	/**
	 * @return the jndiName
	 */
	public String getJndiName();

	/**
	 * Sets the jndiName
	 * 
	 * @param jndiName
	 *            the jndiName
	 * @throws NamingException
	 */
	public void setJndiName(String jndiName) throws NamingException;

	/**
	 * Sets property key for the configuration base path
	 * 
	 * @param configurationBasePathPropertyKey
	 *            the configuration base path property key
	 */
	public void setConfigurationBasePathPropertyKey(
			String configurationBasePathPropertyKey);

	/**
	 * 
	 * @return the configuration base path property key
	 */
	public String getConfigurationBasePathPropertyKey();

	/**
	 * Start the service
	 * 
	 * @throws Exception
	 */
	public void start();

	/**
	 * Stop the service
	 * 
	 * @throws Exception
	 */
	public void stop();

}
