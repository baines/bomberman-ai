import java.io.*;
import java.util.*;

public class AI {

	private final GameState gs;
	private final LinkedList<String> moves;
	private final Player bot;
	private final Random random;
	private ArrayList<String> options;
	private EnemyDistanceComp edc;
	private int bomb_counter;

	AI(GameState gs){
		this.gs = gs;
		moves = new LinkedList<String>();
		options = new ArrayList<String>();
		options.add("UP");
		options.add("DOWN");
		options.add("LEFT");
		options.add("RIGHT");
		bomb_counter = 0;
		bot = gs.players.get(Client.bot_name);
		random = new Random();
		edc = new EnemyDistanceComp(gs);
		generateNavMap();
	}

	void makeMove(BufferedReader in, PrintWriter out) throws IOException {
		updateNav();
		if(bomb_counter > 0) bomb_counter--;
		
		String move = moves.poll();
		String method = "move in queue";
		if(move == null || !moveSafe(move) || enemyCheck()){
			moves.clear();
			if(canPlaceBombAndEscape(moves)){
				move = "BOMB";
				method = "ok bomb spot";
				bomb_counter = 5;
			} else {
				move = "none";
				method = "no safe moves, uh oh...";
				Collections.shuffle(options);
				for(int i = 0; i < 4; ++i){
					move = options.get(i);
					if(moveSafe(move)){
						method = "random safe";
						break;
					}
				}
			}
		}
		
		System.out.printf("Chose move %s (%s).\n", move, method);
		if(!move.equals("none")){
			out.println("ACTION " + move);
	
			String res = in.readLine();
			if(!res.equals(move)){
				System.out.println("Bad move: " + res);
			}
		}
	}
	
	private boolean enemyCheck(){
		if(bomb_counter != 0) return false;
		return gs.isEnemyInRange(bot.x, bot.y);
	}
	
	private int[] xyFromMove(int ox, int oy, String move){
		int x = ox, y = oy;
		switch(move.charAt(0)){
			case 'U': --y; break;
			case 'D': ++y; break;
			case 'L': --x; break;
			case 'R': ++x; break;
		}
		x = Math.max(0, Math.min(x, gs.w-1));
		y = Math.max(0, Math.min(y, gs.h-1));
		if(gs.map[x][y].type == Tile.CLEAR)
			return new int[]{x, y};
		else
			return new int[]{ox, oy};
	}
	
	private boolean moveSafe(String move){
		int[] pos = xyFromMove(bot.x, bot.y, move);
		Tile t = gs.map[pos[0]][pos[1]];
		if(t.fuse == 1 || t.fuse == 2) return false;
		return true;
	}
	
	private boolean canPlaceBombAndEscape(LinkedList<String> moves){
		if(bomb_counter > 0 || !moveSafe("none")) return false;
		ArrayList<Tile> bombtiles = gs.getTilesInBombRange(bot.x, bot.y);
		ArrayList<Tile> movetiles = getAccessibleTiles(gs.map[bot.x][bot.y]);
		boolean good_place = false, safe = false;
		for(Tile t : bombtiles){
			if(!good_place && t.type == Tile.BREAKABLE)
				good_place = true;
		}
		// always place a bomb if an enemy is nearby, or on a random chance
		if(gs.isEnemyInRange(bot.x, bot.y) || random.nextInt(5) == 0) good_place = true;
		if(!good_place) return false;
		for(Tile t : movetiles){
			if(!safe && !bombtiles.contains(t) && getRouteToTile(t, moves))
				safe = true;
		}
		return safe;
	}

	private void generateNavMap(){
		/* tag adjacent clear tiles with a unique number to allow for quick
		lookup of which tiles are accessible to our bot */
		int group = 0, next_group = 0;
		boolean inc = true;
		
		for(int x = 0; x < gs.w; ++x){
			for(int y = 0; y < gs.h; ++y){
				Tile t = gs.map[x][y];
				if(t.type != Tile.CLEAR){
					inc = true;
					continue;
				}
				if(inc){
					group = ++next_group;
					inc = false;
				}
				t.nav_group = group;
				if(x > 0) {
					int left_group = gs.map[x-1][y].nav_group;
					if(left_group == 0) continue;
					int min = Math.min(t.nav_group, left_group);
					int max = Math.max(t.nav_group, left_group);
					changeNavGroup(max, min);
					group = min;
				}
			}
			inc = true;
		}
	}
	
	private void updateNav(){
		for(int x = 0; x < gs.w; ++x){
			for(int y = 0; y < gs.h; ++y){
				Tile t = gs.map[x][y];
				if(!t.nav_update) continue;
				int u = 999, d = 999, l = 999, r = 999, g;
				if(y > 0 && (g = gs.map[x][y-1].nav_group) != 0) u = g;
				if(y < gs.h-1 && (g = gs.map[x][y+1].nav_group) != 0) d = g;
				if(x > 0 && (g = gs.map[x-1][y].nav_group) != 0) l = g;
				if(x < gs.w-1 && (g = gs.map[x+1][y].nav_group) != 0) r = g;
				
				int min = Math.min(Math.min(u, d), Math.min(l, r));
				
				if(u != 999) changeNavGroup(u, min);
				if(d != 999) changeNavGroup(d, min);
				if(l != 999) changeNavGroup(l, min);
				if(r != 999) changeNavGroup(r, min);
				
				t.nav_group = min;
				t.nav_update = false;
			}
		}
	}
	
	private void changeNavGroup(int from, int to){
		for(int x = 0; x < gs.w; ++x){
			for(int y = 0; y < gs.h; ++y){
				Tile t = gs.map[x][y];
				if(t.nav_group == from)
					t.nav_group = to;
			}
		}
	}
	
	private ArrayList<Tile> getAccessibleTiles(Tile from){
		ArrayList<Tile> tiles = new ArrayList<Tile>();
		Tile t;
		for(int x = 0; x < gs.w; ++x){
			for(int y = 0; y < gs.h; ++y){
				if((t = gs.map[x][y]).nav_group == from.nav_group)
					tiles.add(t);
			}
		}
		// move towards an enemy, or away randomly if we've set them up the bomb
		if(gs.isEnemyInRange(bot.x, bot.y)){
			Collections.shuffle(tiles);
		} else {
			Collections.sort(tiles, edc);
		}
		return tiles;
	}
	
	private void BFSCheck(BFSTile bt, Queue<BFSTile> tqueue, List<Tile> tiles, 
	                                          boolean[][] visited, String dir){
		int[] pos = xyFromMove(bt.tile.x, bt.tile.y, dir);
		if(!visited[pos[0]][pos[1]]){
			visited[pos[0]][pos[1]] = true;
			Tile t = gs.map[pos[0]][pos[1]];
			if(tiles.contains(t)) tqueue.add(new BFSTile(t, bt, dir));
		}
	}
	
	private boolean getRouteToTile(Tile to, LinkedList<String> route){
		if(gs.map[bot.x][bot.y].nav_group != to.nav_group) return false;
		
		boolean visited[][] = new boolean[gs.w][gs.h];
		
		ArrayList<Tile> tiles = getAccessibleTiles(gs.map[bot.x][bot.y]);
		Queue<BFSTile> tqueue = new LinkedList<BFSTile>();
		tqueue.add(new BFSTile(gs.map[bot.x][bot.y], null, null));
		
		while(true){
			BFSTile bt = tqueue.peek();
			if(bt == null) return false;
			if(bt.tile == to) break;
			
			if(bt.tile.y > 0)      BFSCheck(bt, tqueue, tiles, visited, "UP");
			if(bt.tile.y < gs.h-1) BFSCheck(bt, tqueue, tiles, visited, "DOWN");
			if(bt.tile.x > 0)      BFSCheck(bt, tqueue, tiles, visited, "LEFT");
			if(bt.tile.x < gs.w-1) BFSCheck(bt, tqueue, tiles, visited, "RIGHT");

			tqueue.poll();
		}

		for(BFSTile t = tqueue.poll(); t != null; t = t.prev)
			if(t.dir != null) route.addFirst(t.dir);

		return true;
	}

	private class EnemyDistanceComp implements Comparator<Tile> {
		Collection<Player> players;	
		EnemyDistanceComp(GameState gs){
			players = gs.players.values();
		}
		@Override
		public int compare(Tile one, Tile two){
			int dist1 = Integer.MAX_VALUE, dist2 = Integer.MAX_VALUE;			
			for(Player p : players){
				if(p.dead || p.name.equals(Client.bot_name)) continue;
				dist1 = Math.min(Math.abs(one.x - p.x) + Math.abs(one.y - p.y), dist1);
				dist2 = Math.min(Math.abs(two.x - p.x) + Math.abs(two.y - p.y), dist2);
			}
			return dist1 - dist2;
		}
	}
	
	private class BFSTile {
		BFSTile(Tile t, BFSTile b, String s){
			tile = t;
			prev = b;
			dir = s;
		}
		Tile tile;
		BFSTile prev;
		String dir;
	}
}
