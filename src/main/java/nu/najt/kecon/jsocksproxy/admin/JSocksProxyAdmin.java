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
package nu.najt.kecon.jsocksproxy.admin;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import nu.najt.kecon.jsocksproxy.JSocksProxy;
import nu.najt.kecon.jsocksproxy.configuration.Configuration;
import nu.najt.kecon.jsocksproxy.configuration.Listen;

/**
 * The Swing administration interface.
 * 
 * @author Kenny Colliander Nordin
 * 
 */
public final class JSocksProxyAdmin extends JFrame implements ActionListener,
		WindowStateListener, Runnable {

	private static final long serialVersionUID = 1646413716016720603L;

	private final JLabel outgoingAddressLabel = new JLabel();

	private final JTextField outgoingAddressTextField = new JTextField(16);

	private final JLabel listenAddressLabel = new JLabel();

	private final JTextField listenAddressTextField = new JTextField(16);

	private final JLabel listenPortLabel = new JLabel();

	private final JTextField listenPortTextField = new JTextField(5);

	private final JLabel backlogLabel = new JLabel();

	private final JTextField backlogTextField = new JTextField(5);

	private final JLabel allowSocks4Label = new JLabel();

	private final JCheckBox allowSocks4Checkbox = new JCheckBox();

	private final JLabel allowSocks5Label = new JLabel();

	private final JCheckBox allowSocks5Checkbox = new JCheckBox();

	private final JButton okButton = new JButton();

	private final JButton cancelButton = new JButton();

	private final JList listenList = new JList();

	private final JButton addListenButton = new JButton();

	private final JButton removeListenButton = new JButton();

	private Configuration configuration = new Configuration();

	private ResourceBundle resourceBundle;

	private static final int PADDING_LEFT = 5;

	private static final int PADDING_TOP = 5;

	private static final int PADDING_RIGHT = 5;

	private static final int PADDING_BOTTOM = 5;

	private static final int PANE_SIZE_X = 500;

	private static final int PANE_SIZE_Y = 300;

	private File configurationFileLocation = new File(
			JSocksProxy.CONFIGURATION_XML);

	/**
	 * Constructor
	 * 
	 * @throws HeadlessException
	 */
	public JSocksProxyAdmin() throws HeadlessException {
		super();

		this.okButton.addActionListener(this);
		this.cancelButton.addActionListener(this);
		this.addListenButton.addActionListener(this);
		this.removeListenButton.addActionListener(this);

		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setState(Frame.NORMAL | Frame.MAXIMIZED_BOTH);

		this.updateComponents(this.getLocale());

		this.setResizable(false);

		this.pack();
		this.setLocationRelativeTo(null);

		this.addWindowStateListener(this);
	}

	/**
	 * Update text and position of components
	 * 
	 * @param locale
	 *            the locale that should be used
	 */
	private void updateComponents(final Locale locale) {
		final JTabbedPane tabbedPane = new JTabbedPane();

		this.setText(locale);

		tabbedPane.setPreferredSize(new Dimension(JSocksProxyAdmin.PANE_SIZE_X,
				JSocksProxyAdmin.PANE_SIZE_Y));

		this.setLayout(new FlowLayout());
		this.add(tabbedPane);

		final JPanel listenTab = new JPanel();
		final JPanel outgoingTab = new JPanel();
		final JPanel advancedTab = new JPanel();

		tabbedPane.removeAll();

		tabbedPane.addTab(this.resourceBundle.getString("listen.title"),
				listenTab);

		tabbedPane.addTab(this.resourceBundle.getString("outgoing.title"),
				outgoingTab);

		tabbedPane.addTab(this.resourceBundle.getString("advanced.title"),
				advancedTab);

		// Listen
		final SpringLayout listenTabLayout = new SpringLayout();
		listenTab.setLayout(listenTabLayout);

		this.setPreferredSize(new Dimension(JSocksProxyAdmin.PANE_SIZE_X + 20,
				JSocksProxyAdmin.PANE_SIZE_Y + 80));

		final int listenTabTextFieldX = (JSocksProxyAdmin.PADDING_LEFT * 2)
				+ this.getMaxSizeX(new JComponent[] { this.listenAddressLabel,
						this.listenPortLabel });

		final int listenTabRowHeight = this.getMaxSizeY(new JComponent[] {
				this.listenAddressLabel, this.listenAddressTextField,
				this.addListenButton });

		this.setComponentPosition(listenTab, listenTabLayout,
				JSocksProxyAdmin.PADDING_LEFT, this.getPosistion(
						JSocksProxyAdmin.PADDING_TOP, listenTabRowHeight, 0),
				this.listenAddressLabel);
		this.setComponentPosition(listenTab, listenTabLayout,
				listenTabTextFieldX, this.getPosistion(
						JSocksProxyAdmin.PADDING_TOP, listenTabRowHeight, 0),
				this.listenAddressTextField);

		this.setComponentPosition(listenTab, listenTabLayout,
				JSocksProxyAdmin.PADDING_LEFT, this.getPosistion(
						JSocksProxyAdmin.PADDING_TOP, listenTabRowHeight, 1),
				this.listenPortLabel);
		this.setComponentPosition(listenTab, listenTabLayout,
				listenTabTextFieldX, this.getPosistion(
						JSocksProxyAdmin.PADDING_TOP, listenTabRowHeight, 1),
				this.listenPortTextField);

		final int listenButtonWidth = this.getMaxSizeX(new JComponent[] {
				this.addListenButton, this.removeListenButton });

		final int listenPaneSizeX = JSocksProxyAdmin.PANE_SIZE_X
				- ((JSocksProxyAdmin.PADDING_LEFT * 3)
						+ JSocksProxyAdmin.PADDING_RIGHT + listenButtonWidth);
		final int listenPaneSizeY = JSocksProxyAdmin.PANE_SIZE_Y
				- (this.getPosistion(JSocksProxyAdmin.PADDING_TOP,
						listenTabRowHeight, 3) + JSocksProxyAdmin.PADDING_BOTTOM);

		final JScrollPane listenPane = new JScrollPane(this.listenList);
		listenPane.setPreferredSize(new Dimension(listenPaneSizeX,
				listenPaneSizeY));

		this.setComponentPosition(listenTab, listenTabLayout,
				JSocksProxyAdmin.PADDING_LEFT, this.getPosistion(
						JSocksProxyAdmin.PADDING_TOP, listenTabRowHeight, 2),
				listenPane);

		this.addListenButton.setPreferredSize(new Dimension(listenButtonWidth,
				listenTabRowHeight));
		this.setComponentPosition(listenTab, listenTabLayout,
				(JSocksProxyAdmin.PADDING_LEFT * 2) + listenPaneSizeX, this
						.getPosistion(JSocksProxyAdmin.PADDING_TOP,
								listenTabRowHeight, 1), this.addListenButton);

		this.removeListenButton.setPreferredSize(new Dimension(
				listenButtonWidth, listenTabRowHeight));
		this.setComponentPosition(listenTab, listenTabLayout,
				(JSocksProxyAdmin.PADDING_LEFT * 2) + listenPaneSizeX, this
						.getPosistion(JSocksProxyAdmin.PADDING_TOP,
								listenTabRowHeight, 2), this.removeListenButton);

		// Outgoing
		final SpringLayout outgoingTabLayout = new SpringLayout();
		outgoingTab.setLayout(outgoingTabLayout);

		final int outgoingTabTextFieldX = (2 * JSocksProxyAdmin.PADDING_LEFT)
				+ this.getMaxSizeX(new JComponent[] { this.outgoingAddressLabel });

		this.setComponentPosition(outgoingTab, outgoingTabLayout,
				JSocksProxyAdmin.PADDING_LEFT, JSocksProxyAdmin.PADDING_TOP,
				this.outgoingAddressLabel);
		this.setComponentPosition(outgoingTab, outgoingTabLayout,
				outgoingTabTextFieldX, JSocksProxyAdmin.PADDING_TOP,
				this.outgoingAddressTextField);

		// Advanced
		final SpringLayout advancedTabLayout = new SpringLayout();
		advancedTab.setLayout(advancedTabLayout);

		final int advancedTabTextFieldX = (JSocksProxyAdmin.PADDING_LEFT * 2)
				+ this.getMaxSizeX(new JComponent[] { this.backlogLabel });

		final int advancedTabRowHeight = this.getMaxSizeY(new JComponent[] {
				this.backlogLabel, this.backlogTextField,
				this.allowSocks4Checkbox, this.allowSocks4Label,
				this.allowSocks5Checkbox, this.allowSocks5Label });

		this.setComponentPosition(advancedTab, advancedTabLayout,
				JSocksProxyAdmin.PADDING_LEFT, this.getPosistion(
						JSocksProxyAdmin.PADDING_TOP, advancedTabRowHeight, 0),
				this.backlogLabel);

		this.setComponentPosition(advancedTab, advancedTabLayout,
				advancedTabTextFieldX, this.getPosistion(
						JSocksProxyAdmin.PADDING_TOP, advancedTabRowHeight, 0),
				this.backlogTextField);

		this.setComponentPosition(advancedTab, advancedTabLayout,
				JSocksProxyAdmin.PADDING_LEFT, this.getPosistion(
						JSocksProxyAdmin.PADDING_TOP, advancedTabRowHeight, 1),
				this.allowSocks4Label);

		this.setComponentPosition(advancedTab, advancedTabLayout,
				advancedTabTextFieldX, this.getPosistion(
						JSocksProxyAdmin.PADDING_TOP, advancedTabRowHeight, 1),
				this.allowSocks4Checkbox);

		this.setComponentPosition(advancedTab, advancedTabLayout,
				JSocksProxyAdmin.PADDING_LEFT, this.getPosistion(
						JSocksProxyAdmin.PADDING_TOP, advancedTabRowHeight, 2),
				this.allowSocks5Label);

		this.setComponentPosition(advancedTab, advancedTabLayout,
				advancedTabTextFieldX, this.getPosistion(
						JSocksProxyAdmin.PADDING_TOP, advancedTabRowHeight, 2),
				this.allowSocks5Checkbox);

		// OK and Cancel buttons
		final JPanel buttonPanel = new JPanel();

		final int buttonWidth = this.getMaxSizeX(new JComponent[] {
				this.okButton, this.cancelButton });

		final int buttonHeight = this.getMaxSizeY(new JComponent[] {
				this.okButton, this.cancelButton });

		this.okButton
				.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
		this.cancelButton.setPreferredSize(new Dimension(buttonWidth,
				buttonHeight));

		buttonPanel.add(this.okButton);
		buttonPanel.add(this.cancelButton);

		this.add(buttonPanel);
	}

	/**
	 * Set component position
	 * 
	 * @param panel
	 *            the panel
	 * @param layout
	 *            the layout
	 * @param x
	 *            the x offset
	 * @param y
	 *            the y offset
	 * @param component
	 *            the componentFs
	 */
	private void setComponentPosition(final JPanel panel,
			final SpringLayout layout, final int x, final int y,
			final Component component) {
		layout.putConstraint(SpringLayout.WEST, component, x,
				SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.NORTH, component, y,
				SpringLayout.NORTH, panel);

		panel.add(component);
	}

	/**
	 * Calculate position for an element
	 * 
	 * @param padding
	 *            the padding
	 * @param size
	 *            the height or width
	 * @param n
	 *            the row or column
	 * @return the pixel position
	 */
	private int getPosistion(final int padding, final int size, final int n) {
		return padding + ((padding + size) * n);
	}

	/**
	 * Set text to all components
	 * 
	 * @param locale
	 *            the locale
	 */
	private void setText(final Locale locale) {
		this.resourceBundle = ResourceBundle.getBundle(
				"nu.najt.kecon.jsocksproxy.admin.bundles.messages", locale);
		this.setTitle(this.resourceBundle.getString("title"));

		this.setText(this.outgoingAddressLabel, "outgoing.address");
		this.setText(this.listenAddressLabel, "listen.address");
		this.setText(this.listenPortLabel, "listen.port");
		this.setText(this.backlogLabel, "backlog");
		this.setText(this.okButton, "button.ok");
		this.setText(this.cancelButton, "button.cancel");
		this.setText(this.addListenButton, "button.add");
		this.setText(this.removeListenButton, "button.remove");
		this.setText(this.allowSocks4Label, "allow.v4");
		this.setText(this.allowSocks5Label, "allow.v5");

	}

	/**
	 * Set localized text to component
	 * 
	 * @param component
	 *            the component
	 * @param key
	 *            the key
	 */
	protected void setText(final JLabel component, final String key) {
		component.setText(this.resourceBundle.getString(key));
	}

	/**
	 * Set localized text to component
	 * 
	 * @param component
	 *            the component
	 * @param key
	 *            the key
	 */
	protected void setText(final JButton component, final String key) {
		component.setText(this.resourceBundle.getString(key));
	}

	/**
	 * Get the maximum x size of a {@link Component}
	 * 
	 * @param components
	 *            the values to check
	 * @return the maximum x size of a {@link Component} in the array
	 */
	protected int getMaxSizeX(final JComponent[] components) {
		int max = 0;

		for (final JComponent component : components) {

			final Dimension dimension = component.getMaximumSize();
			if (dimension != null) {
				if ((dimension.width > max)
						&& (dimension.width != Integer.MAX_VALUE)) {
					max = dimension.width;
				}
			}
		}

		return max;
	}

	/**
	 * Get the maximum y size of a {@link Component}
	 * 
	 * @param components
	 *            the values to check
	 * @return the maximum y size of a {@link Component} in the array
	 */
	private int getMaxSizeY(final JComponent[] components) {
		int max = 0;

		for (final JComponent component : components) {

			final Dimension dimension = component.getMaximumSize();
			if (dimension != null) {
				if ((dimension.height > max)
						&& (dimension.height != Integer.MAX_VALUE)) {
					max = dimension.height;
				}
			}
		}

		return max;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {

		if (event.getSource() == this.okButton) {
			if (this.saveConfiguration()) {
				this.dispose();
			} else {
				JOptionPane.showMessageDialog(this, this.resourceBundle
						.getString("dialog.saveFailed"), this.resourceBundle
						.getString("dialog.saveFailed.title"),
						JOptionPane.ERROR_MESSAGE);
			}
		} else if (event.getSource() == this.cancelButton) {
			this.dispose();
		} else if (event.getSource() == this.addListenButton) {
			final String address = this.listenAddressTextField.getText();
			final String port = this.listenPortTextField.getText();

			List<Listen> list = this.configuration.getListen();
			if (list == null) {
				list = new ArrayList<Listen>();
				this.configuration.setListen(list);
			}

			final Listen listen = new Listen();
			listen.setAddress(address);

			try {
				listen.setPort(Integer.parseInt(port));
			} catch (final NumberFormatException e) {
				listen.setPort(1080);
			}

			list.add(listen);

			this.reloadListenBox();

		} else if (event.getSource() == this.removeListenButton) {
			final int index = this.listenList.getSelectedIndex();

			if (index != -1) {
				this.configuration.getListen().remove(index);
				this.reloadListenBox();
			}
		}
	}

	@Override
	public void windowStateChanged(final WindowEvent event) {
		this.pack();
		this.setLocationRelativeTo(null);
	}

	/**
	 * Load configuration from file
	 * 
	 * @param file
	 *            the file
	 */
	public boolean loadConfiguration(final File file) {

		if (file == null) {
			return false;
		}

		try {
			final JAXBContext context = JAXBContext
					.newInstance(Configuration.class);
			final Unmarshaller unmarshaller = context.createUnmarshaller();

			this.configuration = (Configuration) unmarshaller.unmarshal(file);

			this.outgoingAddressTextField.setText(this.configuration
					.getOutgoingAddresses().get(0)); // TODO: fix!!

			this.backlogTextField.setText(Integer.toString(this.configuration
					.getBacklog()));

			this.allowSocks4Checkbox.setSelected(this.configuration
					.isAllowSocks4());

			this.allowSocks5Checkbox.setSelected(this.configuration
					.isAllowSocks5());

		} catch (final JAXBException e) {
			return false;
		}

		this.reloadListenBox();
		return true;
	}

	/**
	 * Save configuration
	 */
	private boolean saveConfiguration() {
		this.configuration
				.setOutgoingAddresses(Arrays
						.asList(new String[] { this.outgoingAddressTextField
								.getText() })); // TODO: fix!!
		try {
			this.configuration.setBacklog(Integer
					.parseInt(this.backlogTextField.getText()));
		} catch (final NumberFormatException e) {
			this.configuration.setBacklog(100);
		}

		this.outgoingAddressTextField.setText(this.configuration
				.getOutgoingAddresses().get(0)); // TODO: fix!!

		this.backlogTextField.setText(Integer.toString(this.configuration
				.getBacklog()));

		this.configuration
				.setAllowSocks4(this.allowSocks4Checkbox.isSelected());
		this.configuration
				.setAllowSocks5(this.allowSocks5Checkbox.isSelected());

		if (this.configurationFileLocation != null) {
			return this.saveConfiguration(this.configurationFileLocation);
		}

		return false;
	}

	/**
	 * Save configuration to file
	 * 
	 * @param file
	 *            the file
	 */
	private boolean saveConfiguration(final File file) {
		OutputStream outputStream = null;
		try {
			final JAXBContext context = JAXBContext
					.newInstance(Configuration.class);

			final Marshaller marshaller = context.createMarshaller();

			outputStream = new FileOutputStream(file);

			marshaller.marshal(this.configuration, outputStream);

			return true;
		} catch (final JAXBException e) {
			return false;
		} catch (final FileNotFoundException e) {
			return false;
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (final Exception e) {
				}
				outputStream = null;
			}
		}
	}

	/**
	 * Reload the listen box
	 */
	private void reloadListenBox() {
		final List<String> listData = new ArrayList<String>();

		if (this.configuration.getListen() != null) {
			for (final Listen listen : this.configuration.getListen()) {
				listData.add(listen.getAddress() + ":" + listen.getPort());
			}
		}

		this.listenList
				.setListData(listData.toArray(new String[listData.size()]));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while (!this.loadConfiguration(this.configurationFileLocation)) {
			JOptionPane.showMessageDialog(this,
					this.resourceBundle.getString("dialog.loadFailed"),
					this.resourceBundle.getString("dialog.loadFailed.title"),
					JOptionPane.ERROR_MESSAGE);

			final JFileChooser fileChooser = new JFileChooser(
					this.configurationFileLocation.getParentFile());

			// Create a filter for the file chooser
			class CustomFileFilter extends FileFilter {

				@Override
				public boolean accept(final File path) {

					if ((path != null)
							&& path.getName().equalsIgnoreCase(
									JSocksProxy.CONFIGURATION_XML)
							&& path.isFile()) {
						return true;
					} else if ((path != null) && path.isDirectory()) {
						return true;
					}

					return false;
				}

				@Override
				public String getDescription() {
					return JSocksProxy.CONFIGURATION_XML;
				}
			}

			fileChooser.addChoosableFileFilter(new CustomFileFilter());

			if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

				final File selectedFile = fileChooser.getSelectedFile();

				if (selectedFile != null) {
					this.configurationFileLocation = selectedFile;
				}
			} else {
				this.dispose();
				return;
			}
		}

		this.setVisible(true);
	}

	/**
	 * @param args
	 *            the arguments
	 */
	public static void main(final String[] args) {

		final JSocksProxyAdmin admin = new JSocksProxyAdmin();

		admin.run();
	}
}
