import java.util.*;
import java.io.*;

class ScoreKeeper {
	
	private Map<String, Integer> tally;

	ScoreKeeper(){
		tally = new TreeMap<String, Integer>();
		File f = new File("scores.txt");
		if(!f.exists()){
			try {
				f.createNewFile();
			} catch(Exception e){
				System.out.println("Cant create score file.");
			}
		} else {
			loadScores(f);
		}
	}

	private void loadScores(File f){
		try {
			BufferedReader file_in = new BufferedReader(new FileReader(f));
			while(file_in.ready()){
				String[] line = file_in.readLine().split(" ");
				tally.put(line[0], Integer.valueOf(line[1]));
			}
		} catch(Exception e){
			System.out.println("Error loading scores.");
		}
	}
	
	void update(String line){
		try {
			String[] s = line.split(" ");
			Integer i = tally.get(s[0]);
			if(i == null){
				tally.put(s[0], Integer.valueOf(s[1]));
			} else {
				tally.put(s[0], new Integer(Integer.parseInt(s[1]) + i.intValue()));
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	void write(){
		try {
			PrintWriter out = new PrintWriter(new FileWriter("scores.txt"), true);
			for(Map.Entry<String, Integer> e : tally.entrySet()){
				out.println(e.getKey() + " " + e.getValue());
				System.out.println(" " + e.getKey() + " " + e.getValue());
			}
			out.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
