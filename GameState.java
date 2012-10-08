import java.net.*;
import java.io.*;
import java.util.*;

public class GameState {

	int w, h;
	Tile[][] map;
	Map<String, Player> players;
	
	public GameState(BufferedReader in) throws IOException {
		players = new TreeMap<String, Player>();
		
		// MAP
		String[] splitline = in.readLine().split(" ");
		if(!splitline[0].equals("MAP")) 
			throw new IOException("Expected MAP, got " + splitline[0]);
		h = Integer.parseInt(splitline[1]);
		w = Integer.parseInt(splitline[2]);
		map = new Tile[w][h];
		for(int i = 0; i < h; ++i){
			splitline = in.readLine().split(" ");
			for(int j = 0; j < w; ++j){
				map[j][i] = new Tile(this, j, i, Integer.parseInt(splitline[j]));
			}
		}
		
		// PLAYERS
		splitline = in.readLine().split(" ");
		if(!splitline[0].equals("PLAYERS")) 
			throw new IOException("Expected PLAYERS, got " + splitline[0]);
		int num_players = Integer.parseInt(splitline[1]);
		for(int i = 0; i < num_players; ++i){
			splitline = in.readLine().split(" ");
			players.put(splitline[0], new Player(this, splitline[0], 
				Integer.parseInt(splitline[2]), 
				Integer.parseInt(splitline[1])
			));
		}
	}
	
	public void updateActions(BufferedReader in, int num) throws IOException {
		tickBombs();
		for(int i = 0; i < num; ++i){
			String[] splitline = in.readLine().split(" ");
			Player player = players.get(splitline[0]);
			if(player == null || player.dead) continue;
			String action = splitline[1];
		
			if(action.equals("UP")){
				player.move(0, -1);
			} else if(action.equals("DOWN")){
				player.move(0, 1);
			} else if(action.equals("LEFT")){
				player.move(-1, 0);
			} else if(action.equals("RIGHT")){
				player.move(1, 0);
			} else if(action.equals("BOMB")){
				map[player.x][player.y].addBomb();
			} else {
				throw new IOException("Unknown action: " + action);
			}
		}
	}
	
	// returns true if we are dead.
	public boolean updateDead(BufferedReader in, int num) throws IOException {
		for(int i = 0; i < num; ++i){
			Player p = players.get(in.readLine());
			if(p != null){
				p.dead = true;
				players.remove(p.name);
				if(p.name.equals(Client.bot_name)){
					System.out.println("We're dead! :(");	
					return true;
				}
			}
		}
		return false;
	}
	
	public String toString(){
		String str = "";
		for(int j = 0; j < h; ++j){
			for(int i = 0; i < w; ++i){
				str += map[i][j];
			}
			str += "\n";
		}
		int cpl = str.length() / h, cpt = (cpl-1) / w;
		StringBuilder sb = new StringBuilder(str);
		for(Map.Entry<String, Player> e : players.entrySet()){
			Player p = e.getValue();
			if(p.dead) continue;
			int pos = (p.y * cpl) + (p.x * cpt) + (cpt - 1);
			sb.setCharAt(pos, p.name.equals(Client.bot_name) ? 'X' : '!');
		}
		return sb.toString();
	}
	
	private void getTilesInBombDir(int x, int y, int xdir, int ydir, List<Tile> tiles){
		for(int i = 1; i < 4; ++i){
			int xx = x + (i * xdir), yy = y + (i * ydir);
			if(xx < 0 || xx >= w || yy < 0 || yy >= h) break;
			Tile t = map[xx][yy];
			if(t.type == Tile.CLEAR || t.type == Tile.BREAKABLE) tiles.add(t);
			if(t.type == Tile.SOLID || t.type == Tile.BREAKABLE) break;
		}
	}
	
	public ArrayList<Tile> getTilesInBombRange(int x, int y){
		ArrayList<Tile> tiles = new ArrayList<Tile>();
		tiles.add(map[x][y]);
		
		getTilesInBombDir(x, y, 1, 0, tiles);
		getTilesInBombDir(x, y, -1, 0, tiles);
		getTilesInBombDir(x, y, 0, 1, tiles);
		getTilesInBombDir(x, y, 0, -1, tiles);
		
		return tiles;
	}

	public boolean isEnemyInRange(int x, int y){
		ArrayList<Tile> tiles = getTilesInBombRange(x, y);
		for(Tile t : tiles){
			for(Map.Entry<String, Player> e : players.entrySet()){
				Player p = e.getValue();
				if(!p.name.equals(Client.bot_name) && p.x == t.x && p.y == t.y)
					return true;
			}
		}
		return false;
	}
	
	public void tickBombs(){
		for(int i = 0; i < w; ++i){
			for(int j = 0; j < h; ++j){
				Tile t = map[i][j];
				if(t.fuse < 9) t.fuse--;
			}
		}
		for(int i = 0; i < w; ++i){
			for(int j = 0; j < h; ++j){
				Tile t = map[i][j];
				if(t.fuse == 0){
					t.exploding = true;
					t.bomb = false;
				} else if(t.fuse < 0){
					t.fuse = 9;
					for(Tile tt : getTilesInBombRange(i, j)){
						if(tt.bomb) t.fuse = Math.min(t.fuse, tt.fuse);
					}
					t.exploding = false;
					if(t.type == Tile.BREAKABLE){
						t.type = Tile.CLEAR;
						t.nav_update = true;
					}
				}
			}
		}
	
	}
}
