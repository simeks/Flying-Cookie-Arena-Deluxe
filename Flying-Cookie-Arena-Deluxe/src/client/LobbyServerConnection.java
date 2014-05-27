package client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class LobbyServerConnection implements Runnable {
	private String address;
	private int port;
	private Socket socket;
	private BufferedReader fromServer;
	private DataOutputStream toServer;
	private LobbyServerCallback callback = null;
	final protected String serverDelimiter = "#del#"; // protocol specific
	
	enum STATUS {
		CONNECTED, CONNECTING, DISCONNECTED, CONNECTION_FAILED
	};
	private STATUS status = STATUS.DISCONNECTED;
	
	public LobbyServerConnection(String address, int port) {
		this.address = address;
		this.port = port;
	}
	
	@Override
	public void run() {
		if(status == STATUS.DISCONNECTED) {
			connect();
		}
		while(status == STATUS.CONNECTED || status == STATUS.CONNECTING) {
			try {
				while(status == STATUS.CONNECTING || !fromServer.ready()) {
					Thread.sleep(100);
					if(status == STATUS.DISCONNECTED || status == STATUS.CONNECTION_FAILED) {
						return;
					}
				}
				handleMessage(fromServer.readLine());
			} catch(IOException e){
				e.printStackTrace();
				close();
				return;
			} catch (InterruptedException e) {
				e.printStackTrace();
				close();
				return;
			}
		}
	}
	
	public boolean isConnected() {
		if(status == STATUS.CONNECTED) {
			return true;
		}
		return false;
	}
	
	public void close() {
		if(status == STATUS.DISCONNECTED || status == STATUS.CONNECTION_FAILED) {
			return;
		}
		status = STATUS.DISCONNECTED;
		try {
			toServer.close();
			fromServer.close();
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setCallback(LobbyServerCallback callback) {
		this.callback = callback;
	}

	/// @brief will start to receive list of servers to callback
	public boolean subscribeToServerList() {
		return sendMessage(formatMessage("l", null, null));
	}
	
	/// @author http://stackoverflow.com/a/2845292
	private String getInternAddress() throws SocketException {
		String ret = "";
		for (
				final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces( );
			    interfaces.hasMoreElements( );
			)
			{
			    final NetworkInterface cur = interfaces.nextElement( );

				if ( cur.isLoopback( ) )
				{
				    continue;
				}

			    for ( final InterfaceAddress addr : cur.getInterfaceAddresses( ) )
			    {
			        final InetAddress inet_addr = addr.getAddress( );

			        if ( !( inet_addr instanceof Inet4Address ) )
			        {
			            continue;
			        }

			        ret += inet_addr.getHostAddress( );
			    }
			}
		return ret;
	}

	/// @brief sends info about my server
	public boolean createServer(String name, int port) {
		String iip = "";
		try {
			iip = getInternAddress();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Vector<String> keys = new Vector<String>();
		Vector<String> values = new Vector<String>();
		keys.add("n");
		values.add(name);
		keys.add("p");
		values.add(port+"");
		keys.add("c");
		values.add("1");
		keys.add("m");
		values.add("20");
		keys.add("iip");
		values.add(iip);
		return sendMessage(formatMessage("n", keys, values));
	}
	
	/// @brief sends message of information about my server to be updated
	public boolean updateServer(int count, int maxCount) {
		Vector<String> keys = new Vector<String>();
		Vector<String> values = new Vector<String>();
		keys.add("c");
		values.add(count+"");
		keys.add("m");
		values.add(maxCount+"");
		return sendMessage(formatMessage("r", keys, values));
	}
	
	/// @brief builds a json string from message
	private String formatMessage(String messageId, Vector<String> keys, Vector<String> values) {
		String message = "{"+'"'+"m"+'"'+":"+'"'+""+messageId.replace(serverDelimiter, "")+'"';
		if(keys != null) {
			message += ","+'"'+"d"+'"'+":{";
			message += ""+'"'+""+keys.get(0).replace(serverDelimiter, "")+""+'"'+":"+'"'+""+values.get(0).replace(serverDelimiter, "")+'"';
			keys.remove(0);
			values.remove(0);
			for(String key : keys) {
				if(values.size() > keys.indexOf(key)) {
					message += ","+'"'+""+key.replace(serverDelimiter, "")+""+'"'+":"+'"'+""+values.get(keys.indexOf(key)).replace(serverDelimiter, "")+'"';
				}
			}
			message +="}";
		}
		message +="}";
		
		return message;
	}
	
	private boolean sendMessage(String m) {
		if(status != STATUS.CONNECTED) {
			System.out.println("can't send message since not connected. ");
			return false;
		}
		byte[] b = (m+serverDelimiter).getBytes(Charset.forName("UTF-8"));
		try {
			toServer.write(b);
			toServer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void handleMessage(String raw) {
		if(callback == null) {
			System.out.println("Callback is null. "+raw);
			return ;
		}
		Map<String, String> map = new HashMap<String, String>();
		String status = "";
		
		JSONParser parser = new JSONParser();
		JSONObject json = new JSONObject();
		try {
			json = (JSONObject)parser.parse(raw);
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// route message
		if(json.containsKey("m")) {
			String message = (String) json.get("m");
			
			if(message.equals("l")) {
				if(json.containsKey("d")) {
					
					JSONObject data = (JSONObject)json.get("d");
					if(data.containsKey("l")) {
						JSONObject serverList = (JSONObject)data.get("l");
					    Iterator<?> iter = serverList.entrySet().iterator();
					    while(iter.hasNext()) {
					    	Map.Entry entry = (Map.Entry)iter.next();
					    	JSONObject server = (JSONObject)entry.getValue();
					    	String address = (String) entry.getKey();
					    	address = address.substring(0, address.indexOf(":"));

					    	String name="?",count="?",maxCount="?", port="";
					    	if(server.containsKey("p")) {
					    		port = "";
					    	}
					    	if(server.containsKey("n")) {
					    		name = (String)server.get("n");
					    	}
					    	if(server.containsKey("c")) {
					    		count = (String)server.get("c");
					    	}
					    	if(server.containsKey("m")) {
					    		maxCount = (String)server.get("m");
					    	}
					    	
				    		address += ":"+port;
					    	if(server.containsKey("iip")) {
					    		address += ";"+(String)server.get("iip");
					    		address += ":"+port;
					    	}
					    	address += ";127.0.0.1:"+port;
					    	
					    	map.put(address, name+". "+count+"/"+maxCount);
					    }
				    }
				    if(data.containsKey("t")) {
						status = String.valueOf(data.get("t"));
					}
					callback.onReceiveServerList(map, status);
				}
			} else if(message.equals("f")) {
				System.out.println("failed: "+json);
			} else if(message.equals("k")) {
				
			} else {
				callback.onError("unknown message: "+message);
			}
		}
	}
	
	/// @brief connects to the server 
	public void connect() {
		if(status == STATUS.CONNECTED || status == STATUS.CONNECTING) {
			return;
		}
		status = STATUS.CONNECTING;
		System.out.println("Connecting to server.");
		try {
			InetAddress host = InetAddress.getByName(address); 
			socket = new Socket(host,port);
			toServer = new DataOutputStream(socket.getOutputStream());
			fromServer = new BufferedReader(
				new InputStreamReader(
					socket.getInputStream()
				)
			);
			status = STATUS.CONNECTED;
		} catch(UnknownHostException e) {
			e.printStackTrace();
			status = STATUS.CONNECTION_FAILED;
			close();
		} catch(IOException e){
			e.printStackTrace();
			status = STATUS.CONNECTION_FAILED;
			close();
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if(!socket.isClosed()) {
			System.out.println("Garbage collector is closing the socket. ");
			close();
		}
	}
}
