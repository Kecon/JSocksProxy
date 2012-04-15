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
 * UnknownCommand is thrown is the client requesting a unsupported command
 * 
 * @author Kenny Colliander Nordin
 */
public class IllegalCommandException extends Exception {

	private static final long serialVersionUID = -8269863958875814009L;

	/**
	 * Constructor
	 */
	public IllegalCommandException() {
		super();
	}

	/**
	 * Constructor
	 * 
	 * @param message
	 *            the message
	 * @param throwable
	 *            the thorwable
	 */
	public IllegalCommandException(final String message,
			final Throwable throwable) {
		super(message, throwable);
	}

	/**
	 * Constructor
	 * 
	 * @param message
	 *            the message
	 */
	public IllegalCommandException(final String message) {
		super(message);
	}

	/**
	 * Constructor
	 * 
	 * @param throwable
	 *            the thorwable
	 */
	public IllegalCommandException(final Throwable throwable) {
		super(throwable);
	}

}
