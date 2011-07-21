/**
 * JSocksProxy Copyright (c) 2006-2011 Kenny Colliander Nordin
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
 * Exception that indicate that the address type is invalid
 * 
 * @author Kenny Collander Nordin
 * 
 */
public class IllegalAddressTypeException extends Exception {

	private static final long serialVersionUID = 7031880864591750085L;

	public IllegalAddressTypeException() {
		super();
	}

	public IllegalAddressTypeException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public IllegalAddressTypeException(String message) {
		super(message);
	}

	public IllegalAddressTypeException(Throwable cause) {
		super(cause);
	}

}
