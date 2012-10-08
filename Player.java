
public class Player {
	final GameState gs;
	String name;
	boolean dead;
	int x, y;

	Player(GameState gs, String name, int x, int y){
		this.gs = gs;
		this.name = name;
		this.x = x;
		this.y = y;
		dead = false;
	}
	
	void move(int dx, int dy){
		int nx = Math.max(0, Math.min(gs.w-1, x + dx));
		int ny = Math.max(0, Math.min(gs.h-1, y + dy));
		
		if(gs.map[nx][ny].type == Tile.CLEAR && !gs.map[nx][ny].bomb){
			x = nx;
			y = ny;
		}
	}
}
