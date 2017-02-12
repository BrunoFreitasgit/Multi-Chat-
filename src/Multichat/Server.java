/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Multichat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Server {

	private static int uniqueId;

	private ArrayList<ClientThread> al;

	private ServerGUI sg;

	private SimpleDateFormat sdf;

	private int port;

	private boolean keepGoing;
	private String name;

	public Server(int port, String p_name) {
		this(port, p_name, null);
	}

	public Server(int port, String p_name, ServerGUI sg) {
		this.sg = sg;
		this.port = port;
		sdf = new SimpleDateFormat("HH:mm:ss");
		al = new ArrayList<>();
		this.name = p_name;
	}

	/**
	 * Inicia o servidor e fica à "espera" de clientes pra iniciar o "chatting"
	 */
	public void start() {
		keepGoing = true;
		String ip;

		try {

			ServerSocket serverSocket = new ServerSocket(port);
			UdpListenerThread udp_thread = new UdpListenerThread(26000);
			udp_thread.start();

			while (keepGoing) {

				display("Server waiting for Clients on port " + port + ".");
				Socket socket = serverSocket.accept();

				if (!keepGoing) {
					break;
				}
				ClientThread t = new ClientThread(socket);
				if (!validarCliente(t)) {
					t.writeMsg("Username em uso\n");
				} else {
					al.add(t);
					t.start();
				}
			}

			try {
				serverSocket.close();
				for (int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
						tc.sInput.close();
						tc.sOutput.close();
						tc.socket.close();
					} catch (IOException ioE) {

					}
				}
			} catch (Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		} catch (IOException e) {
			String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}

	}

	/**
	 * Desliga o servidor
	 */
	protected void stop() {
		keepGoing = false;

		try {
			new Socket("localhost", port);
		} catch (Exception e) {
			// nothing I can really do
		}
	}

	/**
	 * Mostra as mensagens numa JTextArea na interface do servidor
	 *
	 * @param msg
	 */
	private void display(String msg) {
		String time = sdf.format(new Date()) + " " + msg;
		sg.appendEvent(time + "\n");
	}

	/**
	 * Envia uma mensagem para todos os Clientes
	 *
	 * @param message
	 */
	private void broadcast(String message) {

		String time = sdf.format(new Date());
		String messageLf = time + " " + message + "\n";
		sg.appendRoom(messageLf);
		for (int i = al.size(); --i >= 0;) {
			ClientThread ct = al.get(i);

			if (!ct.writeMsg(messageLf)) {
				al.remove(i);
				display("Disconnected Client " + ct.username + " removed from list.");
			}
		}
	}

	/**
	 * Remove o utilizador aquando deste fazer LogOut
	 *
	 * @param id
	 */
	synchronized void remove(int id) {

		for (int i = 0; i < al.size(); ++i) {
			ClientThread ct = al.get(i);

			if (ct.id == id) {
				al.remove(i);
				return;
			}
		}
	}

	/**
	 * Verifica se o username do cliente já pertence a outro ligado ao servidor
	 *
	 * @param t
	 * @return
	 */
	private boolean validarCliente(ClientThread t) {
		return al.stream().
			noneMatch((ct) -> (t.username.equals(ct.username)));
	}

	/**
	 * Esta classe é instaciada uma vez por cliente
	 */
	class ClientThread extends Thread {

		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;

		int id;

		String username;

		ChatMessage cm;

		String date;

		ClientThread(Socket socket) {
			id = ++uniqueId;
			this.socket = socket;
			System.out.
				println("Thread trying to create Object Input/Output Streams");
			try {

				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput = new ObjectInputStream(socket.getInputStream());

				username = (String) sInput.readObject();
				display(username + " just connected.");
			} catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			} catch (ClassNotFoundException e) {
			}
			date = new Date().toGMTString() + "\n";

		}

		@Override
		public void run() {

			boolean keepGoing = true;
			while (keepGoing) {

				try {
					cm = (ChatMessage) sInput.readObject();
				} catch (IOException e) {
					display(username + " Exception reading Streams: " + e);
					break;
				} catch (ClassNotFoundException e2) {
					break;
				}

				String message = cm.getMessage();

				switch (cm.getType()) {
					case ChatMessage.MESSAGE:
						broadcast(username + ": " + message);
						break;
					case ChatMessage.LOGOUT:
						display(username + " disconnected with a LOGOUT message.");
						keepGoing = false;
						break;
					case ChatMessage.WHOISIN:
						writeMsg("List of the users connected at " + sdf.
							format(new Date()) + "\n");

						for (int i = 0; i < al.size(); ++i) {
							ClientThread ct = al.get(i);
							writeMsg((i + 1) + ") " + ct.username + " since " + ct.date);
						}
						break;
				}
			}

			remove(id);
			close();
		}

		/**
		 * Fecha a conecção e as caixas de mensagens dos utilizadores
		 */
		private void close() {

			try {
				if (sOutput != null) {
					sOutput.close();
				}

			} catch (Exception e) {
			}
			try {
				if (sInput != null) {
					sInput.close();
				}

			} catch (Exception e) {
			}
			try {
				if (socket != null) {
					socket.close();
				}
			} catch (Exception e) {
			}
		}

		/**
		 * Escreve uma String no output do Cliente
		 *
		 * @param msg
		 * @return
		 */
		private boolean writeMsg(String msg) {
			if (!socket.isConnected()) {
				close();
				return false;
			}
			try {
				sOutput.writeObject("(" + name + ")" + msg);
			} catch (IOException e) {
				display("Error sending message to " + username);
				display(e.toString());
			}
			return true;
		}
	}

	public class UdpListenerThread extends Thread {

		protected boolean listen = true;
		int udp_port;

		public UdpListenerThread(int port) throws IOException {
			udp_port = port;
		}

		@Override
		public void run() {
			try {
				byte[] buf = new byte[300];
				DatagramPacket pedido;
				InetAddress clientIp;
				int clientPort;
				String ip;
				DatagramSocket socket_udp = new DatagramSocket(udp_port);
				while (keepGoing) {
					try {
						System.out.
							println("Servidor está à escuta na porta: " + socket_udp.
								getPort());
						pedido = new DatagramPacket(buf, buf.length);
						socket_udp.receive(pedido);
						clientIp = pedido.getAddress();
						clientPort = pedido.getPort();

						DatagramPacket resposta = new DatagramPacket(name.
							getBytes(), name.getBytes().length, clientIp, clientPort);
						socket_udp.send(resposta);
					} catch (Exception e) {
						e.printStackTrace();
						listen = false;
					}
				}
				socket_udp.close();
			} catch (SocketException ex) {
				System.out.println("erro sv udp");
			}
		}
	}
}
