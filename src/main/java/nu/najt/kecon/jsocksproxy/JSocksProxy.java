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
package nu.najt.kecon.jsocksproxy;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import nu.najt.kecon.jsocksproxy.configuration.Configuration;
import nu.najt.kecon.jsocksproxy.configuration.Listen;
import static nu.najt.kecon.jsocksproxy.utils.StringUtils.*;

/**
 * This SOCKS proxy supports basic unauthenticated v4 and v5 versions.
 * 
 * @author Kenny Colliander Nordin
 */
public class JSocksProxy
		implements JSocksProxyMBean, ConfigurationFacade, Runnable {

	private static final String VERSION = (JSocksProxy.class.getPackage()
			.getImplementationVersion() != null)
					? JSocksProxy.class.getPackage().getImplementationVersion()
					: "n/a";

	private static final JSocksProxy SINGLETON = new JSocksProxy();;

	public static final String CONFIGURATION_XML = "jsocksproxy.xml";

	private static final Logger LOG = LoggerFactory
			.getLogger(JSocksProxy.class);

	private final List<InetSocketAddress> listeningAddresses = new ArrayList<InetSocketAddress>();

	private final List<ListeningThread> listeningThreads = new CopyOnWriteArrayList<ListeningThread>();

	private final ExecutorService executorService = Executors
			.newCachedThreadPool();

	private List<InetAddress> outgoingSourceAddresses = null;

	private int backlog = 100;

	private long configurationFileModified = -1;

	private final AtomicBoolean canRun = new AtomicBoolean(Boolean.FALSE);

	private Configuration configuration;

	public static final long RELOAD_INTERVAL = 60000;

	private String jndiName;

	private String configurationBasePathPropertyKey;

	private final Map<String, Object> contextMap = new HashMap<String, Object>();

	/**
	 * The main method for this SocksImplementation.
	 * 
	 * @param args
	 *            A String[] containing startup parameters.
	 */
	public static void main(final String args[]) {
		JSocksProxy.startService();
	}

	@Override
	public String getJndiName() {
		return this.jndiName;
	}

	@Override
	public void setJndiName(final String jndiName) throws NamingException {

		final String oldName = this.jndiName;
		this.jndiName = jndiName;
		if (this.canRun.get()) {
			this.unbind(oldName);
			try {
				this.rebind();
			} catch (final Exception e) {
				final NamingException ne = new NamingException(
						"Failed to update jndiName");
				ne.setRootCause(e);
				throw ne;
			}
		}
	}

	@Override
	public String getConfigurationBasePathPropertyKey() {
		return this.configurationBasePathPropertyKey;
	}

	@Override
	public void setConfigurationBasePathPropertyKey(
			final String configurationBasePathPropertyKey) {
		this.configurationBasePathPropertyKey = configurationBasePathPropertyKey;
	}

	/**
	 * Static start method
	 */
	public static void startService() {
		JSocksProxy.SINGLETON.start();
	}

	/**
	 * Static stop method
	 */
	public static void stopService() {
		JSocksProxy.SINGLETON.stop();
	}

	/**
	 * Start method
	 */
	@Override
	public void start() {
		this.executorService.execute(this);
	}

	/**
	 * Stop method
	 */
	@Override
	public void stop() {

		synchronized (this) {
			this.canRun.getAndSet(Boolean.FALSE);
			this.notifyAll();

			this.unbind(this.jndiName);
		}
	}

	@Override
	public void run() {
		MDC.setContextMap(Collections.emptyMap());

		LOG.info("Starting SOCKS Proxy {}", JSocksProxy.VERSION);

		synchronized (this) {
			if (this.canRun.getAndSet(Boolean.TRUE)) {
				throw new IllegalStateException("Server is already running");
			}

			try {
				this.rebind();
			} catch (final NamingException e) {
				LOG.info("Failed to bind MBean", e);
			}

			while (this.canRun.get()) {
				try {
					this.readConfiguration();
					this.checkListeningThreads();
					this.wait(JSocksProxy.RELOAD_INTERVAL);
				} catch (final InterruptedException e) {
				}
			}
		}

		for (final ListeningThread listeningThread : this.listeningThreads) {
			listeningThread.shutdown();
		}

		this.listeningThreads.clear();

		LOG.info("Shutdown SOCKS Proxy");
	}

	/**
	 * Rebind MBean
	 * 
	 * @throws NamingException
	 */
	private void rebind() throws NamingException {
		final InitialContext rootCtx = new InitialContext();
		final Name name = rootCtx.getNameParser("").parse(this.jndiName);

		rootCtx.rebind(name, this.contextMap);
	}

	/**
	 * Unbind MBean
	 * 
	 * @param jndiName
	 *            the jndiName
	 */
	private void unbind(final String jndiName) {
		try {
			final InitialContext rootCtx = new InitialContext();
			rootCtx.unbind(jndiName);
		} catch (final NamingException e) {
			LOG.info("Failed to unbind MBean", e);
		}
	}

	/**
	 * Check that the listeningAddresses property matches all binding threads.
	 */
	protected void checkListeningThreads() {

		final List<ListeningThread> threadsForRemoval = new ArrayList<ListeningThread>();

		for (final ListeningThread listeningThread : this.listeningThreads) {
			threadsForRemoval.add(listeningThread);
		}

		for (final InetSocketAddress inetSocketAddress : this.listeningAddresses) {
			boolean found = false;
			for (final ListeningThread listeningThread : this.listeningThreads) {

				if (listeningThread.getInetSocketAddress()
						.equals(inetSocketAddress)) {
					found = true;
					threadsForRemoval.remove(listeningThread);
					break;
				}
			}

			if (!found) {
				try {
					final ListeningThread listeningThread = new ListeningThread(
							this, LOG, executorService, inetSocketAddress);

					this.listeningThreads.add(listeningThread);

					executorService.execute(listeningThread);

				} catch (final IOException e) {
					LOG.error("Failed to setup listening address for {}",
							formatSocketAddress(inetSocketAddress), e);
				}
			}
		}

		for (final ListeningThread listeningThread : threadsForRemoval) {
			listeningThread.shutdown();
			this.listeningThreads.remove(listeningThread);
		}
	}

	/**
	 * Reading and update configuration from configuration.xml
	 */
	protected void readConfiguration() {

		final File file;
		URI basePath = this.getBasePath();

		file = this.getConfigurationFile(basePath);

		if (!file.isFile()) {
			LOG.error("Failed to read configuration from {}",
					file.getAbsolutePath());
			return;
		}

		if (file.lastModified() == this.configurationFileModified) {
			return;
		}

		LOG.trace("Reading configuration");

		this.readConfigurationFromFile(file);

		this.configurationFileModified = file.lastModified();

		if (this.configuration == null) {
			LOG.error("No configuration read from {}",
					JSocksProxy.CONFIGURATION_XML);
			return;
		}

		this.listeningAddresses.clear();
		this.updateOutgoingAddresses();
		this.updateListenAddresses();
		this.updateBacklog();
	}

	private void updateBacklog() {
		if ((this.configuration.getBacklog() <= 0)
				|| (this.configuration.getBacklog() > 100)) {
			LOG.warn(
					"Backlog value must be between 0 and 101; supplied value: {} ; using default 100",
					this.configuration.getBacklog());
			this.backlog = 100;
		} else {
			this.backlog = this.configuration.getBacklog();
			LOG.info("Using backlog {}", this.backlog);
		}
	}

	private void updateListenAddresses() {
		for (final Listen listen : this.configuration.getListen()) {

			int port = -1;
			InetAddress address = null;

			try {
				address = InetAddress.getByName(listen.getAddress());
			} catch (final UnknownHostException e) {
				LOG.error("Failed to resolve {}", listen.getAddress(), e);
				continue;
			}

			port = listen.getPort();
			if ((port <= 0) || (port >= 65536)) {
				LOG.error("Invalid port number: {}", port);
				continue;
			}

			try {
				final InetSocketAddress inetSocketAddress = new InetSocketAddress(
						address, port);
				this.listeningAddresses.add(inetSocketAddress);

				LOG.info("Added listening address ",
						formatSocketAddress(inetSocketAddress));

			} catch (final IllegalArgumentException e) {
				LOG.error("Failed to create address {}:{}",
						listen.getAddress(), listen.getPort(), e);
				continue;
			}
		}
	}

	private void updateOutgoingAddresses() {
		if ((this.configuration.getOutgoingAddresses() != null)
				&& !this.configuration.getOutgoingAddresses().isEmpty()) {

			final Set<InetAddress> outgoingAddresses = new HashSet<InetAddress>();

			for (final String address : this.configuration
					.getOutgoingAddresses()) {
				try {
					outgoingAddresses.addAll(
							Arrays.asList(InetAddress.getAllByName(address)));
				} catch (final UnknownHostException e) {
					LOG.error("Failed to resolve {}", address, e);
				}
			}

			this.outgoingSourceAddresses = new CopyOnWriteArrayList<InetAddress>(
					outgoingAddresses);
		}

		if (this.outgoingSourceAddresses == null) {
			try {
				this.outgoingSourceAddresses = Collections
						.singletonList(InetAddress.getLocalHost());
			} catch (final UnknownHostException e) {
				// Not much to do if this occur
			}
		}

		if (LOG.isInfoEnabled()) {
			final StringBuilder builder = new StringBuilder();
			if (this.outgoingSourceAddresses != null) {

				for (final InetAddress inetAddress : this.outgoingSourceAddresses) {
					if (builder.length() > 0) {
						builder.append(", ");
					}

					builder.append(inetAddress.getHostAddress());
				}

			}

			LOG.info("Using outgoing source addresses: " + builder);
		}
	}

	private void readConfigurationFromFile(final File file) {
		try {
			final JAXBContext context = JAXBContext
					.newInstance(Configuration.class);
			final Unmarshaller unmarshaller = context.createUnmarshaller();

			this.configuration = (Configuration) unmarshaller.unmarshal(file);

		} catch (final JAXBException e) {
			LOG.error("Failed to read configuration", e);
		}
	}

	private File getConfigurationFile(URI basePath) {
		final File file;
		if (basePath == null) {
			file = new File(JSocksProxy.CONFIGURATION_XML);
		} else {

			final File basePathFile = new File(basePath);

			if (basePathFile.isDirectory()) {
				file = new File(basePathFile, JSocksProxy.CONFIGURATION_XML);
			} else {
				file = basePathFile;
			}
		}
		return file;
	}

	private URI getBasePath() {
		URI uri = null;
		if (this.configurationBasePathPropertyKey != null) {
			try {
				uri = new URL(System
						.getProperty(this.configurationBasePathPropertyKey))
								.toURI();
			} catch (final Exception e) {
			}
		}
		return uri;
	}

	/**
	 * @return the outgoing address
	 */
	@Override
	public List<InetAddress> getOutgoingSourceAddresses() {
		return this.outgoingSourceAddresses;
	}

	/**
	 * @return true if usage of SOCKS4 is allowed
	 */
	@Override
	public boolean isAllowSocks4() {
		if (this.configuration == null) {
			return false;
		}

		return this.configuration.isAllowSocks4();
	}

	/**
	 * @return true if usage of SOCKS5 is allowed
	 */
	@Override
	public boolean isAllowSocks5() {
		if (this.configuration == null) {
			return false;
		}

		return this.configuration.isAllowSocks5();
	}

	@Override
	public int getBacklog() {
		return this.backlog;
	}
}
