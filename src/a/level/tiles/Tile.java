package a.level.tiles;

import a.gfx.Colours;
import a.gfx.Screen;
import a.level.Level;

public abstract class Tile {

	public static final Tile[] tiles = new Tile[256];
	public static final Tile VOID = new BasicSolidTile(0, 0, 0, Colours.get(0, 0, -1, -1), 0xff000000);
	public static final Tile STONE = new BasicSolidTile(1, 1, 0,Colours.get(-1, 333, -1, -1), 0xff555555);
	public static final Tile GRASS = new BasicTile(2, 2, 0, Colours.get(-1, 131, 141, -1), 0xff00ff00);
	public static final Tile WATER = new AnimatedTile(3, new int [][] {{0,5},{1, 5},{2,5},{1,5}}, Colours.get(-1, 004,115, -1), 0xff0000ff, 1000);
	
	protected byte id;
	protected boolean sollid;
	protected boolean emitter;
	private int levelColour;
	
	public Tile(int id, boolean isSollid, boolean isEmiter, int levelColour){
		this.id = (byte)id;
		if(tiles[id] != null)throw new RuntimeException("Duplicate Tile id on "+ id);
		this.sollid = isSollid;
		this.emitter = isEmiter;
		this.levelColour = levelColour;
		tiles[id] = this;
	}
	
	public byte getId(){
		return id;
	}
	
	public boolean isSolid(){
		return sollid;
	}
	
	
	
	public boolean isEmiter(){
		return emitter;
	}
	
	public int getLevelColour(){
		return levelColour;
	}
	public abstract void tick();
	public abstract void render(Screen screen, Level level, int x, int y);
	
}
