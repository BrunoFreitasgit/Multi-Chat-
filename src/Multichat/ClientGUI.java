/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Multichat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class ClientGUI extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;

	private JLabel label;

	private JTextField tf;
	private JTextField tfServer, tfPort;
	private JButton login, logout, whoIsIn, search;
	private JTextArea ta;
	private boolean connected;
	private Client client;
	private int defaultPort;
	private String defaultHost;
	private String username;
	private ArrayList<String> serversList = new ArrayList<>();
	private HashMap<String, String> lista_servidores_a_ligar = new HashMap<String, String>();
	private int flag;

	private String server;
	JPanel northPanel = new JPanel(new GridLayout(3, 1));
	JPanel eastPanel = new JPanel(new GridLayout(3, 1));
	JPanel westPanel = new JPanel(new GridLayout(1, 1));

	ClientGUI(String host, int port) {

		super("Chat Client");
		defaultPort = port;
		defaultHost = host;

		JPanel serverAndPort = new JPanel(new GridLayout(1, 5, 1, 3));
		tfServer = new JTextField(host);
		tfPort = new JTextField("" + port);
		tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

		serverAndPort.add(new JLabel(""));

		northPanel.add(serverAndPort);

		tf = new JTextField("Enter your username here");
		tf.setBackground(Color.WHITE);
		northPanel.add(tf);
		add(northPanel, BorderLayout.NORTH);

		ta = new JTextArea("Welcome to the Chat room\n", 80, 40);

		westPanel.add(new JScrollPane(ta));
		ta.setEditable(false);
		add(westPanel, BorderLayout.WEST);

		search = new JButton("Pesquisar Servidores");
		search.addActionListener(this);
		login = new JButton("Login");
		login.addActionListener(this);
		logout = new JButton("Logout");
		logout.addActionListener(this);
		logout.setEnabled(false);
		whoIsIn = new JButton("Who is in");
		whoIsIn.addActionListener(this);
		whoIsIn.setEnabled(false);

		JPanel southPanel = new JPanel();
		southPanel.add(login);
		southPanel.add(logout);
		southPanel.add(whoIsIn);
		add(southPanel, BorderLayout.SOUTH);

		northPanel.add(search);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(600, 600);
		setVisible(true);
		tf.requestFocus();

	}

	/**
	 * Coloca texto na área respetiva para ser mostrado ao utilizador
	 *
	 * @param str
	 */
	void append(String str) {
		ta.append(str);
		ta.setCaretPosition(ta.getText().length() - 1);
	}

	/**
	 * Quando a conecção ao servidor falha faz "reset" aos servidores
	 */
	void connectionFailed() {
		login.setEnabled(true);
		logout.setEnabled(false);
		whoIsIn.setEnabled(false);
		tf.setText("Enter your username below");
		//tfPort.setText("" + defaultPort);
		//tfServer.setText(defaultHost);

		tfServer.setEditable(false);
		tfPort.setEditable(false);

		tf.removeActionListener(this);
		connected = false;
	}

	/**
	 * Acções despoletadas pelos botões
	 *
	 * @param e
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();

		if (o == logout) {
			client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
			return;
		}

		if (o == whoIsIn) {
			client.
				sendMessage(new ChatMessage(ChatMessage.WHOISIN, this.username));
			return;
		}

		if (connected) {

			if (tf.getText().startsWith("/")) {
				String s[] = tf.getText().split(" ");
				if (s[0].equals("/envia")) {
					client.changeServerEnviaFlag(s[1].trim());
				} else if (s[0].equals("/recebe")) {
					client.changeServerRecebeFlag(s[1].trim());
				}
				tf.setText("");
			} else {
				client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, tf.
												   getText()));
				tf.setText("");
			}

			return;
		}

		if (o == login) {

			username = tf.getText().trim();
			if (username.length() == 0) {
				return;
			}

			this.server = tfServer.getText().trim();
			String portNumber = tfPort.getText().trim();
			if (portNumber.length() == 0) {
				return;
			}
			int port = 0;
			try {
				port = Integer.parseInt(portNumber);
			} catch (Exception en) {
				return;
			}

			if (flag == 0) {
				client = new Client(server, port, username, this);
			} else {
				client = new Client(serversList, port, username, this);
			}

			if (!client.start()) {
				return;
			}

			tf.setText("Enter your message here");
			connected = true;

			login.setEnabled(false);

			logout.setEnabled(true);
			whoIsIn.setEnabled(true);
			search.setEnabled(false);

			tfServer.setEditable(false);
			tfPort.setEditable(false);

			tf.addActionListener(this);
		}
		if (o == search) {

			eastPanel.removeAll();
			client = new Client();
			HashMap<String, String> lista = client.ServerFileReader(26000);
			final JPanel panel = new JPanel();

			if (lista.isEmpty()) {
				JOptionPane.
					showMessageDialog(panel, "Não há servidores ativos", "Warning",
									  JOptionPane.WARNING_MESSAGE);
				return;
			}

			Box box = Box.createVerticalBox();
			for (String serverName : lista.keySet()) {
				String sv_ip = lista.get(serverName);
				String sv_name_ip = serverName.concat(" " + sv_ip);
				JCheckBox check = new JCheckBox(sv_name_ip);
				ActionListener actionListener = (ActionEvent actionEvent) -> {
					AbstractButton abstractButton = (AbstractButton) actionEvent.
						getSource();
					this.server = abstractButton.getText().trim();
					if (!serversList.contains(sv_ip)) {
						serversList.add(sv_ip);
					}
				};
				check.addActionListener(actionListener);
				box.add(check);
			}

			eastPanel.add(new JScrollPane(box));

			eastPanel.setLayout(new BoxLayout(eastPanel, BoxLayout.Y_AXIS));

			add(eastPanel, BorderLayout.EAST);
			this.flag = 1;
			repaint();
			revalidate();

			return;
		}

	}

	public static void main(String[] args) {
		new ClientGUI(" ", 1500);
	}

}
