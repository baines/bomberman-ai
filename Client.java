import java.net.*;
import java.io.*;
import java.util.Date;

public class Client {
	
	private Socket sock = null;
	private GameState state;
	private BufferedReader in;
	private PrintWriter out;
	private AI ai;
	
	public static String bot_name = "botnamehere";
	private static final String bot_pass = "password";
	private static final int port = 8037;
	private static boolean keep_playing = true;
	private static ScoreKeeper scores;
		
	public static void main(String[] args) throws Exception {
		scores = new ScoreKeeper();
		String h = args.length > 0 ? args[0] : "localhost";
		Client client = new Client(h);
		do {
			try {
				while(client.run());
			} catch(Exception e){
				e.printStackTrace();
			} finally {
				client.reset(h);
			}
		} while(keep_playing);
	}
	
	Client(String host) throws Exception {
		reset(host);
	}

	void reset(String host) throws Exception {
		if(sock == null || !sock.isConnected()){
			sock = new Socket(host, port);
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintWriter(sock.getOutputStream(), true);
		}
		String line = "";
		do {
			line = in.readLine();
		} while(!line.equals("INIT"));
		
		out.printf("REGISTER %s %s\n", bot_name, bot_pass);
		if(!in.readLine().equals("REGISTERED")) throw new Exception("no reg");
		
		state = new GameState(in);
		ai = new AI(state);
	}
	
	boolean run() throws Exception {
		String line = in.readLine();
				
		if(line.equals("END")){
			System.out.println("Game over man!");
			if((line = in.readLine()).startsWith("SCORES")){
				System.out.println("Scores:");
				int num = Integer.parseInt(line.split(" ")[1]);
				for(int i = 0; i < num; ++i)
					scores.update(in.readLine());
				scores.write();
			}
			return false;
		} else if(line.startsWith("TICK")){
			System.out.println(line + ":");
			ai.makeMove(in, out);
		} else if(line.startsWith("ACTIONS")){
			int num = Integer.parseInt(line.split(" ")[1]);
			state.updateActions(in, num);
			System.out.println(state);
		} else if(line.startsWith("DEAD")){
			int num = Integer.parseInt(line.split(" ")[1]);
			return !state.updateDead(in, num);
		} else {
			System.out.println("Unknown command: " + line);
		}
		return true;
	}
}
