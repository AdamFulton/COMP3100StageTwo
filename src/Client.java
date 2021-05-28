
//package Client;
import java.net.*;
import java.util.*;

import java.io.*;

public class Client {

	static final int DEFAULT_PORT = 50000;
	static final String DEFAULT_ADDRESS = "localhost";

	public static void main(String args[]) {
		try {

			// variables to recieve and send data to and from the server.
			Socket sock = new Socket(DEFAULT_ADDRESS, DEFAULT_PORT);
			PrintStream output = new PrintStream(sock.getOutputStream());
			BufferedReader input = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			String clientOut;

			// variables to control the state of the client
			Boolean gotData = false;
			Boolean gotServers = false;
			boolean noServers = false;
			Boolean noJobs = false;

			// variables to store data recieved from the server.
			List<List<String>> serverData = new ArrayList<List<String>>();
			List<List<String>> servers = new ArrayList<List<String>>();
			List<List<String>> jobs = new ArrayList<List<String>>();
			List<Integer> serverCPUCores = new ArrayList<Integer>();
			ArrayList<String> selectedServer = new ArrayList<String>();

			// Handshake Begins

			clientOut = "HELO" + "\n";
			output.print(clientOut);
			System.out.println(input.readLine());

			// https://stackoverflow.com/questions/19839172/how-to-read-all-of-inputstream-in-server-socket-java

			// https://stackoverflow.com/questions/10475898/receive-byte-using-bytearrayinputstream-from-a-socket
			System.out.println(input.readLine());
			if (!input.readLine().equals("OK")) {
				output.print("QUIT" + "\n");
				sock.close();
			}
		

			clientOut = "AUTH " + System.getProperty("user.name") + "\n";
			output.print(clientOut);

			System.out.println(input.readLine();
			if (!input.readLine().equals("OK")) {
				output.print("QUIT" + "\n");
				sock.close();
			}

			clientOut = "REDY" + "\n";
			output.print(clientOut);
			System.out.println(input.readLine());
			// Handshake completed. Loop begins now	
			for (String server = input.readLine(); server != null; server = input.readLine()) {

				System.out.println(server);
				// checks if there is no more jobs left to schedule and closes the socket if true
				if (server.equals("NONE")) {

					output.print("QUIT" + "\n");
					sock.close();
					break;

				} else {

					// checks if the server has sent a job to be scheduled or a job resubmission to be scheduled
					if (serverCmd(server).equals("JOBN") || serverCmd(server).equals("JOBP")) {

						jobs = createList(server);

						String cpuCores = jobs.get(0).get(4);
						String memory = jobs.get(0).get(5);
						String disk = jobs.get(0).get(6);

						output.print("GETS Avail " + cpuCores + " " + memory + " " + disk + "\n");

						/*
						 * Checks if the server has sent a data command in response to a gets command
						 * sent by the client
						 */
					} else if (serverCmd(server).equals("DATA")) {

						serverData = createList(server);

						// checks if the client has recieved the data before sending the OK command
						if (serverData.size() >= 1) {

							output.print("OK" + "\n");
							gotData = true;

						}
					}
					// checks if the client has received the data about the current available servers
					// for the job, and that it has not already calculated the optimal server.
					if (gotData && gotServers == false && noServers == false) {

						for (String serverList = input.readLine(); serverList != null; serverList = input.readLine()) {

							servers.addAll(createList(serverList));

							// checks to see if the client has recieved data from all the available servers
							// before issuing to OK command
							if (servers.size() == Integer.valueOf(serverData.get(0).get(1))) {

								output.print("OK" + "\n");
								gotServers = true;
								break;
							
							// if there is no servers available for the current job the client
							// pushes the job back in the queue
							} else if (serverData.get(0).get(1).equals("0")) {
								output.print("PSHJ" + "\n");
								noServers = true;
								break;

							}

						}

					}

					/*
						 * Checks if the server has sent the . command and if so calcaluates the optimal server
						 * and schedules the job to the server.
						 */
					if (serverCmd(server).equals(".")) {

						selectedServer = findServer(serverData, servers, serverCPUCores);
						String JobId = jobs.get(0).get(2);

						output.print("SCHD " + JobId + " " + selectedServer.get(0) + " " + selectedServer.get(1)+ "\n");

					}
						/*	
						 * checks if the server has sent an OK command and if true the client sends a
						 * REDY command telling the server it is ready for the next job and resets
						 * the state of the client.
						 */

					if (server.equals("OK")) {

						output.print("REDY" + "\n");
						gotData = false;
						servers.removeAll(servers);
						serverCPUCores.removeAll(serverCPUCores);
						serverData.removeAll(serverData);
						gotServers = false;
						selectedServer.removeAll(selectedServer);
						noServers = false;

					}
						/*
						 * checks if the server has sent a job complete command. if so the server
						 * responds with the REDY command
						 */
					if (serverCmd(server).contains("JCPL")) {

						List<List<String>> temp = new ArrayList<List<String>>();

						temp = createList(server);

						// checks if there is any current jobs on the server that has just finished a job
						output.print("LSTJ" + temp.get(0).get(3) + " " + temp.get(0).get(4) + "\n");

						for (String s = input.readLine(); s != null; s = input.readLine()) {

							if (serverCmd(s).equals("DATA")) {

								output.print("OK" + "\n");

							} else if (serverCmd(s).equals(".")) {

								// if the server that has just finished a job has no other jobs running the client will
								// tell the server to terminate it to save costs.
								if (noJobs == false) {
									output.print("TERM" + temp.get(0).get(3) + " " + temp.get(0).get(4) + "\n");
									output.print("REDY" + "\n");

									break;
								} else {
									output.print("REDY" + "\n");
									break;
								}
							} else {

								noJobs = true;
								output.print("OK" + "\n");

							}

						}

						noJobs = false;
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e);
		}

	}


	public static ArrayList<String> findServer(List<List<String>> serverData, List<List<String>> servers,List<Integer> serverCPUCores) {
		
		int cpuCoreMin = 0;
		ArrayList<String> retval = new ArrayList<String>();

		for (int i = 0; i < Integer.valueOf(serverData.get(0).get(1)); i++) {

			if (servers.size() >= 1) {
				serverCPUCores.add(Integer.valueOf(servers.get(i).get(4)));
			}

		}

		cpuCoreMin = findSmallestCoreCount(serverCPUCores);

		
		for (int i = 0; i < servers.size(); i++) {

			if (Integer.valueOf(servers.get(i).get(4)) == cpuCoreMin) {

				retval.add(servers.get(i).get(0));
				retval.add(servers.get(i).get(1));
				break;
			}
		}

		return retval;
	}
	/*
	 * function takes in a list of the number of server CPU cores for each server.
	 * and returns the smallest number of server cores in the list in ascending order
	 */
	public static int findSmallestCoreCount(List<Integer> serverCores) {

		Collections.sort(serverCores);
		int retval = 0;
		Integer min = 0;

		min = serverCores.get(0);

		for (int i = 0; i < serverCores.size(); i++) {

			if (serverCores.get(i) <= min) {

				min = serverCores.get(i);
			}
		}
		retval = min;

		return retval;

	}
		/*
		 * serverCmd() takes the first character (in the case of '.') or word in a given
		 * server output and returns it. Useful for deciding what to do with a given
		 * server command.
		 * 
		 */
	public static String serverCmd(String serverOutput) {
		
		String field = "";
		for (int i = 0; i < serverOutput.length(); i++) {
			Character c = serverOutput.charAt(i);

			if (c.equals('.')) {
				field = field + c;
				return field;
			}

			if (Character.isWhitespace(c) || (i == serverOutput.length() - 1)) {
				break;
			} else {
				field = field + c;
			}

		}
		return field;

	}
	/*
		 * Takes the server output (if it is a list of servers) and returns it as a List
		 * of string lists.
		 */
	public static List<List<String>> createList(String serverOutput) {

		List<List<String>> retval = new ArrayList<List<String>>();
		String field = "";
		List<String> aList = new ArrayList<String>();
		int count = 0;

		retval.add(aList);

		for (int i = 0; i < serverOutput.length(); i++) {
			Character c = serverOutput.charAt(i);

			if ((i == serverOutput.length() - 1)) {
				field = field + c;
				retval.get(count).add(field);
				break;
			}

			if (!String.valueOf(c).matches(".")) {
				// If the character is a new line, create a new list of strings.
				// https://stackoverflow.com/questions/25915073/detect-line-breaks-in-a-char
				retval.get(count).add(field);
				count++;
				
				field = "";
				List<String> bList = new ArrayList<String>();
				retval.add(bList);

			} else if (Character.isWhitespace(c) || (i == serverOutput.length() - 1)) {
			
				retval.get(count).add(field);
				field = "";
			} else {
				field = field + c;
			}

		}
		return retval;
	}

}
