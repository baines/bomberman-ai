import java.util.*;

public class Tile {
	final GameState gs;
	boolean bomb, exploding, nav_update;
	int x, y, type, fuse, nav_group;
	
	static final int CLEAR = 0;
	static final int BREAKABLE = 1;
	static final int SOLID = 2;
	
	Tile(GameState gs, int x, int y, int type){
		this.gs = gs;
		this.x = x;
		this.y = y;
		this.type = type;
		nav_group = 0;
		fuse = 9;
		bomb = false;
		exploding = false;
		nav_update = false;
	}
	
	public String toString(){
		/*if(nav_group == 0){
			if(type == SOLID) return "  #";
			else return "  -";
		}
		String r = "  " + nav_group;
		if(r.length() > 3) r = r.substring(1);
		return r;*/
		if(exploding) return " *";
		//if(fuse < 9) return " " + fuse;
		if(bomb) return " o";
		switch(type){
			case CLEAR: return "  ";
			case SOLID: return " #";
			case BREAKABLE: return " -";
			default: return " ?";
		}//*/
	}
	
	void addBomb(){
		bomb = true;
		ArrayList<Tile> tiles = gs.getTilesInBombRange(x, y);
		
		int f = 5;
		// check for any bombs that would make this one explode earlier.
		for(Tile t : tiles){
			if(t.bomb && t.fuse < f) f = t.fuse;
		}
		// update all the fuses accordingly.
		for(Tile t : tiles){
			t.fuse = Math.min(t.fuse, f);
		}
	}

}
