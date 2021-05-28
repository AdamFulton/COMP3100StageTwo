
//package Client;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


import java.io.*;

public class Client {

	static final int DEFAULT_PORT = 50000;

	public static void main(String args[]) {
		try {
			Socket sock = new Socket("localhost", DEFAULT_PORT);
			PrintStream output = new PrintStream(sock.getOutputStream());
			
			BufferedReader input = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			String clientOut;
			Boolean gotData = false;
			Boolean gotServers = false;
			boolean test = false;

			int i = 0;
			
			
			List<List<String>> serverData = new ArrayList<List<String>>();
		
			List<List<String>> servers = new ArrayList<List<String>>();

			ArrayList<String> serverUsed = new ArrayList<String>();

			List<List<String>> jobs = new ArrayList<List<String>>();
			List<Integer> serverCPUCores = new ArrayList<Integer>();
			ArrayList<String> largestServerName = new ArrayList<String>();
		
		
		
		


			// Handshake Begins

			clientOut = "HELO" +"\n";
			output.print(clientOut);

			// https://stackoverflow.com/questions/19839172/how-to-read-all-of-inputstream-in-server-socket-java

	

			// https://stackoverflow.com/questions/10475898/receive-byte-using-bytearrayinputstream-from-a-socket



		
			if (!input.readLine().equals("OK")) {
				output.print("QUIT"+"\n");
				sock.close();
			}

			clientOut = "AUTH "+System.getProperty("user.name")+"\n";
			output.print(clientOut);

		

		
			if (!input.readLine().equals("OK")) {
				output.print("QUIT"+"\n");
				sock.close();
			}

			clientOut = "REDY"+"\n";
			output.print(clientOut);

			
			for (String server = input.readLine(); server != null; server = input.readLine()) {




				if (server.equals("NONE")) {

					output.print("QUIT" + "\n");
					sock.close();
					break;

				} else {

					if (serverCmd(server).equals("JOBN") || serverCmd(server).equals("JOBP")) {

						jobs = createList(server);

						String cpuCores = jobs.get(0).get(4);
						String memory = jobs.get(0).get(5);
						String disk = jobs.get(0).get(6);

						output.print("GETS Avail " + cpuCores + " " + memory + " " + disk+"\n");
						
						
					} else if (serverCmd(server).equals("DATA")) {

						serverData = createList(server);

						if (serverData.size() >=1) {

							output.print("OK" + "\n");
							gotData = true;
						
							
						}
						}  if (gotData &&  gotServers == false && test == false) {

							for (String serverList = input.readLine(); serverList != null; serverList = input.readLine()) {

							

								servers.addAll(createList(serverList));
								System.out.println(servers.size());
								System.out.println(Integer.valueOf(serverData.get(0).get(1)));
								
								if (servers.size() == Integer.valueOf(serverData.get(0).get(1))) {
									
									output.print("OK"+ "\n");
									gotServers = true;
									break;
								} else if (serverData.get(0).get(1).equals("0")) {
										output.print("PSHJ" +"\n");
										test = true;
										break;
										
									}
							
	
						 }
					
						}
						
						if (serverCmd(server).equals(".")) {


								largestServerName = findServer(serverData, servers, serverCPUCores);
									String JobId = jobs.get(0).get(2);


								

										output.print("SCHD " + JobId + " " + largestServerName.get(0) + " " + largestServerName.get(1) +"\n");
								
									

								


									

											
										
											
									


									

									

								
									
							


									}

							
							
								

							
						

						if (server.equals("OK")) {

							output.print("REDY"+"\n");
							gotData = false;
							servers.removeAll(servers);
							serverCPUCores.removeAll(serverCPUCores);
							serverData.removeAll(serverData);
							gotServers = false;
							largestServerName.removeAll(largestServerName);
							test = false;
							
						 }
						 if (serverCmd(server).contains("JCPL")) {


							List<List<String>> temp = new ArrayList<List<String>>();
							
							temp = createList(server);

							output.print("LSTJ" + temp.get(0).get(3) + " " + temp.get(0).get(4) + "\n");


							for (String s = input.readLine(); s != null; s = input.readLine()) {

								if (serverCmd(s).equals("DATA")) {

									output.print("OK" + "\n");
										

								} else if (serverCmd(s).equals(".")) {

									if (test == false) {
									output.print("TERM" + temp.get(0).get(3) + " " + temp.get(0).get(4) + "\n");
									output.print("REDY"+"\n");
									break;
									} else {
										output.print("REDY"+"\n");
										break;
									}
								} else {

									test = true;
									output.print("OK"+"\n");
									

								}

							}

							test = false;
						
						 
						 
								


					
						


				
				

						 }
						}
					}
		} catch (Exception e) {
			System.out.println(e);
		}

	}

	/*
	 * function takes in a list of the number of server CPU cores for each server.
	 * and returns the largest number of server cores in the list
	 */

	 public static ArrayList<String> findServer(List<List<String>> serverData, List<List<String>> servers, List<Integer> serverCPUCores) {
		int max = 0;

		ArrayList<String> retval = new ArrayList<String>();
		for (int i = 0; i < Integer.valueOf(serverData.get(0).get(1)); i++) {

			if (servers.size() >= 1) {
				serverCPUCores.add(Integer.valueOf(servers.get(i).get(4)));
			}

		
		}

		max = findSmallestCoreCount(serverCPUCores);

		System.out.println(max);

		// iterates through the server list and returns the name of the server
		// that has the same number of CPU cores as the value stored in max
		for (int i = 0; i < servers.size(); i++) {

			if (Integer.valueOf(servers.get(i).get(4)) == max) {
				retval.add(servers.get(i).get(0));
				retval.add(servers.get(i).get(1));
				break;
			}
		}



		return retval;
	 }
	public static int findSmallestCoreCount(List<Integer> serverCores) {

		Collections.sort(serverCores);
		int retval = 0;
		Integer max = 0;

		max = serverCores.get(0);

		for (int i = 0; i < serverCores.size(); i++) {

			if (serverCores.get(i) <= max) {

				max = serverCores.get(i);
			}
		}
		retval = max;

		return retval;
	

	}

	public static String serverMsg(int count, byte[] data) {

		/*
		 * serverMsg() takes in the byte count and an instantiated byte data array that
		 * contains the byte stream of the server output. This function converts the
		 * byte stream into a legible string.
		 */

		byte[] serverArr = new byte[count];

		for (int i = 0; i < count; i++) {
			serverArr[i] = data[i];

		}

		String serverOutput = new String(serverArr, StandardCharsets.UTF_8);

		return serverOutput;

	}

	public static String serverCmd(String serverOutput) {

		/*
		 * serverCmd() takes the first character (in the case of '.') or word in a given
		 * server output and returns it. Useful for deciding what to do with a given
		 * server command.
		 * 
		 */

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

	public static List<List<String>> createList(String serverOutput) {

		/*
		 * Takes the server output (if it is a list of servers) and returns it as a List
		 * of string lists.
		 */

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
				// since /n is two characters

				field = "";
				List<String> bList = new ArrayList<String>();
				retval.add(bList);

			} else if (Character.isWhitespace(c) || (i == serverOutput.length() - 1)) {
				// stringCount++;
				retval.get(count).add(field);
				field = "";
			} else {
				field = field + c;
			}

		}

		return retval;
	}
	

	
}
