/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Multichat;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


public class ServerGUI extends JFrame implements ActionListener, WindowListener {

	private static final long serialVersionUID = 1L;
	
	private JButton stopStart;
	
	private JTextArea chat, event;
	
	private JTextField tPortNumber;
	private JTextField tServerName;
	
	private Server server;

	
	ServerGUI(int port) {
		super("Chat Server");
		server = null;
		
		JPanel north = new JPanel();
		north.add(new JLabel("Port number: "));
		tPortNumber = new JTextField("  " + port);
		north.add(tPortNumber);
		north.add(new JLabel("Name: "));
		tServerName = new JTextField("  " + "máximo 15 caracteres");
		north.add(tServerName);
		
		stopStart = new JButton("Start");
		stopStart.addActionListener(this);
		north.add(stopStart);
		add(north, BorderLayout.NORTH);

		
		JPanel center = new JPanel(new GridLayout(2, 1));
		chat = new JTextArea(80, 80);
		chat.setEditable(false);
		appendRoom("Chat room.\n");
		center.add(new JScrollPane(chat));
		event = new JTextArea(80, 80);
		event.setEditable(false);
		appendEvent("Events log.\n");
		center.add(new JScrollPane(event));
		add(center);

		
		addWindowListener(this);
		setSize(400, 600);
		setVisible(true);
	}

	/**
         * Mostra as mensagens na janela do utilizador e do servidor
         * @param str 
         */
	void appendRoom(String str) {
		chat.append(str);
		chat.setCaretPosition(chat.getText().length() - 1);
	}

	void appendEvent(String str) {
		event.append(str);
		event.setCaretPosition(chat.getText().length() - 1);

	}

	/**
         * Liga e desliga o servidor quando o respetivo botão é desligado
         * @param e 
         */
	@Override
	public void actionPerformed(ActionEvent e) {
		
		if (server != null) {
			server.stop();
			server = null;
			tPortNumber.setEditable(true);
			stopStart.setText("Start");
			return;
		}
		
		int port;
		String name;
		try {
			port = Integer.parseInt(tPortNumber.getText().trim());
			name = tServerName.getText().trim();
			if (name.equalsIgnoreCase("máximo 15 caracteres") || name == null || name.
				isEmpty() == true) {
				appendEvent("Nome inválido\n");
				return;
			}
		} catch (Exception er) {
			appendEvent("Invalid port number or name");
			return;
		}
		
		server = new Server(port, name, this);
		
		new ServerRunning().start();
		stopStart.setText("Stop");
		tPortNumber.setEditable(false);
	}

	
	public static void main(String[] arg) {
		
		new ServerGUI(1500);
	}

	/**
         * Fecha a janela clicando no X
         * @param e 
         */
	@Override
	public void windowClosing(WindowEvent e) {
		
		if (server != null) {
			try {
				server.stop();			
			} catch (Exception eClose) {
			}
			server = null;
		}
		
		dispose();
		System.exit(0);
	}

	
	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	
	class ServerRunning extends Thread {

		@Override
		public void run() {
			server.start();         
			
			stopStart.setText("Start");
			tPortNumber.setEditable(true);
			appendEvent("Server crashed\n");
			server = null;
		}
	}

}
