/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Multichat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Client {

	// Lista de sockets
	private CopyOnWriteArrayList<MySocket> lista_sockets = new CopyOnWriteArrayList<>();
	// Lista de servidores em ficheiro txt
	private ArrayList<String> serverList;
	// Lista de serviores a que se pretende ligar
	private ArrayList<String> lista_servidores_ligados;
	// hashmap com Key->"nome do servidor" Value-> "ip do servidor"
	private HashMap<String, String> lista_servidores_disponiveis = new HashMap<>();
	// GUI do cliente
	private ClientGUI cg;
	// Username do cliente
	private String username;
	// Porta para ligação TCP
	private int port;

	Client(ArrayList<String> servers, int port, String username, ClientGUI cg) {
		this.lista_servidores_ligados = servers;
		this.port = port;
		this.username = username;
		this.cg = cg;
	}

	// Construtor obsoleto
	Client(String server, int port, String username, ClientGUI cg) {
		this.port = port;
		this.username = username;
		this.cg = cg;
	}

	Client() {
	}

	/**
	 * Para cada IP na lista_servidores_ligados vai tentar estabelecer uma
	 * ligação TCP Se a ligação for possivel, inicia-se uma nova thread
	 * ListenFromServer para ligação ao Servidor
	 *
	 * @return
	 */
	public boolean start() {

		for (String s : lista_servidores_ligados) {
			MySocket mysock = new MySocket();
			try {
				Socket sock = new Socket(s, port);
				mysock.setSv_ip(sock.getInetAddress().getHostAddress());
				mysock.setSocket(sock);
			} catch (Exception ec) {
				display("Error connectiong to server:" + ec);
				return false;
			}

			String msg = "Connection accepted " + mysock.getSocket().
				getInetAddress() + ":" + mysock.getSocket().
				getPort();
			display(msg);

			try {
				mysock.setsInput(new ObjectInputStream(mysock.getSocket().
					getInputStream()));
				mysock.setsOutput(new ObjectOutputStream(mysock.getSocket().
					getOutputStream()));
				lista_sockets.add(mysock);
			} catch (IOException eIO) {
				display("Exception creating new Input/output Streams: " + eIO);
				return false;
			}
			new ListenFromServer().start();
			try {
				mysock.getsOutput().writeObject(username);
			} catch (IOException eIO) {
				display("Exception doing login : " + eIO);
				disconnect();
				return false;
			}
		}
		return true;
	}

	/**
	 * Mostra a mensagem passada por parâmetro na UI do cliente
	 */
	private void display(String msg) {
		cg.append(msg + "\n");
	}

	/**
	 * Envia a mensagem passada por parâmetro para o servidor
	 *
	 * @param msg
	 */
	void sendMessage(ChatMessage msg) {
		for (MySocket socket : lista_sockets) {
			try {
				synchronized (socket.getsOutput()) {
					if (socket.getFlag_envia() == 1) {
						socket.getsOutput().writeObject(msg);
					}
				}
			} catch (IOException e) {
				display("Exception writing to server: " + e);
			}
		}
	}

	/**
	 * Modifica a flag Envia
	 *
	 * @param msg ip do servidor que quer alterar a flag
	 */
	void changeServerEnviaFlag(String msg) {
		for (MySocket sock : lista_sockets) {
			if (sock.getSv_ip().equals(msg)) {
				if (sock.getFlag_envia() == 1) {
					sock.setFlag_envia(0);
				} else {
					sock.setFlag_envia(1);
				}
			}
		}
	}

	/**
	 * Modifica a flag Recebe
	 *
	 * @param msg ip do servidor que quer alterar a flag
	 */
	void changeServerRecebeFlag(String msg) {
		for (MySocket sock : lista_sockets) {
			if (sock.getSv_ip().equals(msg)) {
				if (sock.getFlag_recebe() == 1) {
					sock.setFlag_recebe(0);
				} else {
					sock.setFlag_recebe(1);
				}
			}
		}
	}

	/**
	 * Disconecta o cliente do servidor
	 */
	synchronized private void disconnect() {
		for (MySocket sock : lista_sockets) {
			synchronized (sock.getsInput()) {
				try {
					if (sock.getsInput() != null) {
						sock.getsInput().close();
					}
				} catch (Exception e) {
					display("Erro ao fechar InputStream");
				}
			}
			synchronized (sock.getsOutput()) {
				try {
					synchronized (sock.getsOutput()) {
						if (sock.getsOutput() != null) {
							sock.getsOutput().close();
						}
					}
				} catch (Exception e) {
					display("Erro ao fechar OutputStream");
				}
			}
			try {
				if (sock.getSocket() != null) {
					sock.getSocket().close();
				}
			} catch (Exception e) {
				display("Erro ao fechar o socket");
			}
		}
		cg.connectionFailed();

	}

	/**
	 * Classe responsável por "ouvir" o que sai do servidor extends Thread para
	 * que cada cliente possa "ouvir" vários servidores
	 *
	 */
	class ListenFromServer extends Thread {

		private boolean flag;

		ListenFromServer() {
			this.flag = true;
		}

		@Override
		public void run() {
			while (flag) {
				for (MySocket sock : lista_sockets) {
					try {
						String msg = "";
						synchronized (sock.getsInput()) {
							msg = (String) sock.getsInput().
								readObject();
						}
						if (msg.equalsIgnoreCase("Username em uso\n")) { // verificação da mensagem enviado pelo servidor de o username já estiver em uso
							disconnect();
						}
						if (sock.getFlag_recebe() == 1) {
							cg.append(msg);
						}

					} catch (IOException e) {
						display("Server has close the connection: " + e);
						if (cg != null) {
							cg.connectionFailed();
						}
						flag = false;
					} catch (ClassNotFoundException e2) {
						System.out.println("Erro desconhecido");
						if (cg != null) {
							cg.connectionFailed();
						}
						flag = false;
					}
				}
			}
		}
	}

	/**
	 * Esta método "lê" um ficheiro com informação sobre os servidores e chama
	 * outro que faz a verificação se estão à escuta (UDP)
	 *
	 * @param port
	 * @return
	 */
	public HashMap<String, String> ServerFileReader(int port) {

		serverList = new ArrayList<>();

		try {

			FileReader inputFile = new FileReader("servers.txt");

			BufferedReader bufferReader = new BufferedReader(inputFile);

			String line;

			while ((line = bufferReader.readLine()) != null) {
				serverList.add(line);
				if (hostAvailabilityCheck(line, port)) {
					System.out.println("Servidor adicionado à lista");
				}
			}
			bufferReader.close();
		} catch (IOException | InterruptedException e) {
			System.out.println("ServerFileReader error");
		}
		if (!lista_servidores_disponiveis.isEmpty()) {
			for (String s : lista_servidores_disponiveis.keySet()) {
				String key = s;
				String value = lista_servidores_disponiveis.get(s);
				System.out.println("Nome: " + key + " Ip: " + value);
			}
		}

		return lista_servidores_disponiveis;
	}

	/**
	 * Verifica se o servidor recebido por parâmetro está à escuta de pedidos
	 * UDP Se estiver, adiciona o nome recebido pelo DatagramPacket num hashmap
	 * juntamente com o ip
	 *
	 * @param serv
	 * @param port
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public boolean hostAvailabilityCheck(String serv, int port) throws InterruptedException, IOException {
		int TIMEOUT = 50;

		String connect = "connect_request";
		byte[] connect_data = new byte[300];
		InetAddress host = InetAddress.getByName(serv);
		DatagramSocket socket = new DatagramSocket();
		socket.setSoTimeout(TIMEOUT);
		System.out.println("Testando ip: " + serv);
		DatagramPacket request = new DatagramPacket(connect.getBytes(), connect.
													getBytes().length, host, port);
		socket.send(request);
		String reply_srt;
		String reply_ip;
		DatagramPacket reply = new DatagramPacket(connect_data, 300);
		try {
			socket.receive(reply);
			reply_srt = new String(reply.getData(), 0, reply.getLength());
			System.out.
				println("Dados recebidos: " + reply_srt + "do servidor " + reply.
					getAddress().getHostAddress());
			reply_ip = reply.getAddress().getHostAddress();
			if (!checkHashMap(lista_servidores_disponiveis, reply_srt)) {
				lista_servidores_disponiveis.put(reply_srt, reply_ip);
			}
		} catch (SocketTimeoutException ex) {
			System.out.println("Socket timeout");
			return false;
		}
		socket.close();
		return true;
	}

	/**
	 * Verifica se já o nome do servidor já existe
	 *
	 * @param hash_map
	 * @param name
	 * @return
	 */
	private boolean checkHashMap(HashMap<String, String> hash_map, String name) {
		return hash_map.containsKey(name);
	}
}
