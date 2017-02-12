/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Multichat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 *
 * @author Bruno Freitas
 */
public class MySocket {

	private Socket socket;
	private String sv_ip;
	private ObjectInputStream sInput;
	private ObjectOutputStream sOutput;
	private int flag_recebe;
	private int flag_envia;

	public MySocket(String ip, int port, ObjectInputStream sInput,
					ObjectOutputStream sOutput) throws IOException {
		this.sv_ip = ip;
		this.socket = new Socket(ip, port);
		this.sInput = sInput;
		this.sOutput = sOutput;
		this.flag_recebe = 1;
		this.flag_recebe = 1;
	}

	public MySocket() {
		this.flag_recebe = 1;
		this.flag_envia = 1;
	}

	public Socket getSocket() {
		return socket;
	}

	public ObjectInputStream getsInput() {
		return sInput;
	}

	public ObjectOutputStream getsOutput() {
		return sOutput;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public void setsInput(ObjectInputStream sInput) {
		this.sInput = sInput;
	}

	public void setsOutput(ObjectOutputStream sOutput) {
		this.sOutput = sOutput;
	}

	public int getFlag_recebe() {
		return flag_recebe;
	}

	public void setFlag_recebe(int flag_recebe) {
		this.flag_recebe = flag_recebe;
	}

	public void setFlag_envia(int flag_envia) {
		this.flag_envia = flag_envia;
	}

	public int getFlag_envia() {
		return flag_envia;
	}

	public String getSv_ip() {
		return sv_ip;
	}

	public void setSv_ip(String sv_ip) {
		this.sv_ip = sv_ip;
	}

}
