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

/**
 * The base interface for SOCKS implementations.
 * 
 * @author Kenny Colliander Nordin
 */
public interface SocksImplementation extends Runnable {

	/**
	 * This method is the method which handle the handshake (except for the
	 * first version check) and tunneling.
	 * 
	 */
	@Override
	public void run();

}
