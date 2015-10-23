package com.g.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainGame {

    public static final int SPAWN_ANIMATION = -1;
    public static final int MOVE_ANIMATION = 0;
    public static final int MERGE_ANIMATION = 1;

    public static final int FADE_GLOBAL_ANIMATION = 0;

    public static final long MOVE_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME;
    public static final long SPAWN_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME;
    public static final long NOTIFICATION_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME * 5;
    public static final long NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME + SPAWN_ANIMATION_TIME;
    private static final String HIGH_SCORE = "high score";

    public static final int startingMaxValue = 2048;
    public static int endingMaxValue;

    //Odd state = game is not active
    //Even state = game is active
    //Win state = active state + 1
    public static final int GAME_WIN = 1;
    public static final int GAME_LOST = -1;
    public static final int GAME_NORMAL = 0;
    public static final int GAME_NORMAL_WON = 1;
    public static final int GAME_ENDLESS = 2;
    public static final int GAME_ENDLESS_WON = 3;

    public Grid grid = null;
    public AnimationGrid aGrid;
    final int numSquaresX = 3;  //  纵排
    final int numSquaresY = 3;  // 横排

    final int startTiles = 1;

    public int gameState = 0;
    public boolean canUndo;

    public long score = 0;
    public long highScore = 0;

    public long lastScore = 0;
    public int lastGameState = 0;

    private long bufferScore = 0;
    private int bufferGameState = 0;

    private Context mContext;

    public static final String TILE_TITLE_X = "×";
    public static final String TILE_TITLE_O = "O";

    private MainView mView;

    public MainGame(Context context, MainView view) {
        mContext = context;
        mView = view;
        endingMaxValue = (int) Math.pow(2, view.numCellTypes - 1);
    }

    public void newGame() {
        if (grid == null) {
            grid = new Grid(numSquaresX, numSquaresY);
        } else {
            prepareUndoState();
            saveUndoState();
            grid.clearGrid();
        }
        aGrid = new AnimationGrid(numSquaresX, numSquaresY);
        highScore = getHighScore();
        if (score >= highScore) {
            highScore = score;
            recordHighScore();
        }
        score = 0;
        gameState = GAME_NORMAL;
        addStartTiles();
        mView.refreshLastTime = true;
        mView.resyncTime();
        mView.invalidate();
    }

    /**
     * 添加方块
     * */
    private void addStartTiles() {
        for (int xx = 0; xx < startTiles; xx++) {
            this.addRandomTile();
        }
    }

    private void addRandomTile() {
        if (grid.isCellsAvailable()) {
            Tile tile = new Tile(randomAvailableCell(),2, TILE_TITLE_O);
//            Tile tile = new Tile(grid.randomAvailableCell(),2, TILE_TITLE_O);
            spawnTile(tile);
        }
    }

    public void userAddTile(Tile tile){
        prepareUndoState();
        prepareTiles();
        saveUndoState();
        spawnTile(tile);
        if(checkLose()){
            addRandomTile();
            if(!checkLose()){
                endGame();
            }
        }else{
            endGame();
        }
    }

    private void spawnTile(Tile tile) {
        grid.insertTile(tile);
        aGrid.startAnimation(tile.getX(), tile.getY(), SPAWN_ANIMATION,
                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null); //Direction: -1 = EXPANDING
    }

    private void recordHighScore() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(HIGH_SCORE, highScore);
        editor.commit();
    }

    private long getHighScore() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        return settings.getLong(HIGH_SCORE, -1);
    }

    private void prepareTiles() {
        for (Tile[] array : grid.field) {
            for (Tile tile : array) {
                if (grid.isCellOccupied(tile)) {
                    tile.setMergedFrom(null);
                }
            }
        }
    }

    private void moveTile(Tile tile, Cell cell) {
        grid.field[tile.getX()][tile.getY()] = null;
        grid.field[cell.getX()][cell.getY()] = tile;
        tile.updatePosition(cell);
    }

    private void saveUndoState() {
        grid.saveTiles();
        canUndo = true;
        lastScore =  bufferScore;
        lastGameState = bufferGameState;
    }

    private void prepareUndoState() {
        grid.prepareSaveTiles();
        bufferScore = score;
        bufferGameState = gameState;
    }

    public void revertUndoState() {
        if (canUndo) {
            canUndo = false;
            aGrid.cancelAnimations();
            grid.revertTiles();
            score = lastScore;
            gameState = lastGameState;
            mView.refreshLastTime = true;
            mView.invalidate();
        }
    }

    public boolean gameWon() {
        return (gameState > 0 && gameState % 2 != 0);
    }

    public boolean gameLost() {
        return (gameState == GAME_LOST);
    }

    public boolean isActive() {
        return !(gameWon() || gameLost());
    }

    public void move(int direction) {
        aGrid.cancelAnimations();
        // 0: up, 1: right, 2: down, 3: left
        if (!isActive()) {
            return;
        }
        prepareUndoState();
        Cell vector = getVector(direction);
        List<Integer> traversalsX = buildTraversalsX(vector);
        List<Integer> traversalsY = buildTraversalsY(vector);
        boolean moved = false;

        prepareTiles();

        for (int xx: traversalsX) {
            for (int yy: traversalsY) {
                Cell cell = new Cell(xx, yy);
                Tile tile = grid.getCellContent(cell);

                if (tile != null) {
                    Cell[] positions = findFarthestPosition(cell, vector);
                    Tile next = grid.getCellContent(positions[1]);

                    if (next != null && next.getValue() == tile.getValue() && next.getMergedFrom() == null) {
                        Tile merged = new Tile(positions[1], tile.getValue() * 2);
                        Tile[] temp = {tile, next};
                        merged.setMergedFrom(temp);

                        grid.insertTile(merged);
                        grid.removeTile(tile);

                        // Converge the two tiles' positions
                        tile.updatePosition(positions[1]);

                        int[] extras = {xx, yy};
                        aGrid.startAnimation(merged.getX(), merged.getY(), MOVE_ANIMATION,
                                MOVE_ANIMATION_TIME, 0, extras); //Direction: 0 = MOVING MERGED
                        aGrid.startAnimation(merged.getX(), merged.getY(), MERGE_ANIMATION,
                                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null);

                        // Update the score
                        score = score + merged.getValue();
                        highScore = Math.max(score, highScore);

                        // The mighty 2048 tile
                        if (merged.getValue() >= winValue() && !gameWon()) {
                            gameState = gameState + GAME_WIN; // Set win state
                            endGame();
                        }
                    } else {
                        moveTile(tile, positions[0]);
                        int[] extras = {xx, yy, 0};
                        aGrid.startAnimation(positions[0].getX(), positions[0].getY(), MOVE_ANIMATION, MOVE_ANIMATION_TIME, 0, extras); //Direction: 1 = MOVING NO MERGE
                    }

                    if (!positionsEqual(cell, tile)) {
                        moved = true;
                    }
                }
            }
        }

        if (moved) {
            saveUndoState();
            addRandomTile();
            checkLose();
        }
        mView.resyncTime();
        mView.invalidate();
    }

    private boolean checkLose() {
        boolean isLose = true;
        if (movesAvailable()) {
            isLose = false;
            endGame();
        }else if(!grid.isCellsAvailable()){
            gameState = GAME_LOST;
            isLose = false;
            endGame();
        }
        return isLose;
    }

    private void endGame() {
        aGrid.startAnimation(-1, -1, FADE_GLOBAL_ANIMATION, NOTIFICATION_ANIMATION_TIME, NOTIFICATION_DELAY_TIME, null);
        if (score >= highScore) {
            highScore = score;
            recordHighScore();
        }
    }

    private Cell getVector(int direction) {
        Cell[] map = {
                new Cell(0, -1), // up
                new Cell(1, 0),  // right
                new Cell(0, 1),  // down
                new Cell(-1, 0)  // left
        };
        return map[direction];
    }

    private Cell[] getVectors(int direction){
        Cell [][] map = {
                {new Cell(0,-1),new Cell(0,1)},  //竖
                {new Cell(-1,-1),new Cell(1,1)}, //左斜杠
                {new Cell(-1,0),new Cell(1,0)},  //横
                {new Cell(1,-1),new Cell(-1,1)}  //右斜杠
        };
        return map[direction];
    }

    private Cell[][] getVertors(String key){
        Map<String,Cell [][]> cellMap = new HashMap<String,Cell [][]>();
        cellMap.put("0,0",  new Cell[][]{{new Cell(1,0),new Cell(2,0)},{new Cell(1,1),new Cell(2,2)},{new Cell(0,1),new Cell(0,2)}});  //左上
        cellMap.put("0,1",  new Cell[][]{{new Cell(1,1),new Cell(2,1)},{new Cell(0,0),new Cell(0,2)}});                                 //左中
        cellMap.put("0,2",  new Cell[][]{{new Cell(1,2),new Cell(2,2)},{new Cell(1,1),new Cell(2,0)},{new Cell(0,1),new Cell(0,0)}});  //左下

        cellMap.put("1,0",  new Cell[][]{{new Cell(0,0),new Cell(2,0)},{new Cell(1,1),new Cell(1,2)}});                                  //中上
        cellMap.put("1,1",  new Cell[][]{{new Cell(0,1),new Cell(2,1)},{new Cell(0,0),new Cell(2,2)},{new Cell(2,0),new Cell(0,2)},{new Cell(1,0),new Cell(1,2)}});   //中中
        cellMap.put("1,2",  new Cell[][]{{new Cell(0,2),new Cell(2,2)},{new Cell(1,1),new Cell(1,0)}});   //中下

        cellMap.put("2,0",  new Cell[][]{{new Cell(1,0),new Cell(0,0)},{new Cell(1,1),new Cell(0,2)},{new Cell(2,1),new Cell(2,2)}});   //右上
        cellMap.put("2,1",  new Cell[][]{{new Cell(1,1),new Cell(0,1)},{new Cell(2,0),new Cell(2,2)}});                                  //右中
        cellMap.put("2,2", new Cell[][]{{new Cell(1, 2), new Cell(0, 2)}, {new Cell(1, 1), new Cell(0, 0)}, {new Cell(2, 1), new Cell(2, 0)}});   //右下
        return cellMap.get(key);
    }

    private Cell randomAvailableCell(){
        ArrayList<Cell> cells = grid.getAvailableCells();
        if(cells!=null&&cells.size()>0){
            Cell cell = null;
            Cell[][] maybe = null;
            Tile prev_tile = null;
            int probability = 0;       // 发生的概率   最小为1  最大为 10
            List<Object [] > _c = new ArrayList<Object []>();
            for (int i = 0; i <cells.size() ; i++) {
                cell = cells.get(i);
                maybe = getVertors(cell.getX() + "," + cell.getY());
                probability = 0;
                for (int j = 0; j <maybe.length; j++) {
                    for (int k = 0; k <maybe[j].length ; k++) {
                        Cell temp = maybe[j][k];
                        if(grid.field[temp.getX()][temp.getY()]==null){     //为空
                            if(k==maybe[j].length-1){
                                if(prev_tile==null){
                                    probability += 1;
                                }else{
                                    if( prev_tile.getValue() == 2){
                                        probability += 5;
                                    }else{
                                        probability += 1;
                                    }
                                }
                            }
                        }else{                              //不为空
                            if(k==maybe[j].length-1){
                                if(prev_tile!=null){
                                    if(grid.field[temp.getX()][temp.getY()].getValue() ==2){
                                        if( prev_tile.getValue() == 2){
                                            probability += 100;
                                        }else{
                                            probability += 1;
                                        }
                                    }else{
                                        if(prev_tile.getValue() == 2){
                                            probability += 1;
                                        }else{
                                            probability += 2;
                                        }
                                    }
                                }else{
                                    if(grid.field[temp.getX()][temp.getY()].getValue() ==2){
                                        probability += 5;
                                    }else{
                                        probability += 1;
                                    }

                                }
                            }
                        }
                        prev_tile = grid.field[temp.getX()][temp.getY()];

                    }
                }
                _c.add(new Object[]{probability,cell});
            }
            for (int i = 0; i <_c.size() ; i++) {
                for (int j = i+1; j < _c.size() ; j++) {
                    if( Integer.parseInt(_c.get(i)[0].toString())   >  Integer.parseInt(_c.get(j)[0].toString())){
                        _c.add(i, _c.get(j));
                        _c.remove(j + 1);
                    }
                }
            }
            int temp = Integer.parseInt(_c.get(0)[0].toString());
            Cell temp_cell = (Cell)(_c.get(0)[1]);
            for (int i = 0; i <_c.size() ; i++) {
                if(temp==Integer.parseInt(_c.get(i)[0].toString())){

                }else{
                    int index =(int) Math.floor(Math.random() * i);
                    temp_cell = (Cell)(_c.get(index)[1]);
                    break;
                }
            }
            return temp_cell;
        }
        return null;
    }

    private List<Integer> buildTraversalsX(Cell vector) {
        List<Integer> traversals = new ArrayList<Integer>();

        for (int xx = 0; xx < numSquaresX; xx++) {
            traversals.add(xx);
        }
        if (vector.getX() == 1) {
            Collections.reverse(traversals);
        }

       return traversals;
    }

    private List<Integer> buildTraversalsY(Cell vector) {
        List<Integer> traversals = new ArrayList<Integer>();

        for (int xx = 0; xx <numSquaresY; xx++) {
            traversals.add(xx);
        }
        if (vector.getY() == 1) {
            Collections.reverse(traversals);
        }

        return traversals;
    }

    private Cell[] findFarthestPosition(Cell cell, Cell vector) {
        Cell previous;
        Cell nextCell = new Cell(cell.getX(), cell.getY());
        do {
            previous = nextCell;
            nextCell = new Cell(previous.getX() + vector.getX(),
                    previous.getY() + vector.getY());
        } while (grid.isCellWithinBounds(nextCell) && grid.isCellAvailable(nextCell));

        Cell[] answer = {previous, nextCell};
        return answer;
    }

    /**
     * 判断是否还可以移动
     * 是否已经完成匹配
     * */
    private boolean movesAvailable() {
        return  tilesMatchesAvailable();
    }

    private boolean tileMatchesAvailable() {
        Tile tile;

        for (int xx = 0; xx < numSquaresX; xx++) {
            for (int yy = 0; yy < numSquaresY; yy++) {
                tile = grid.getCellContent(new Cell(xx, yy));

                if (tile != null) {
                    for (int direction = 0; direction < 4; direction++) {
                        Cell vector = getVector(direction);
                        Cell cell = new Cell(xx + vector.getX(), yy + vector.getY());

                        Tile other = grid.getCellContent(cell);

                        if (other != null && other.getValue() == tile.getValue()) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean tilesMatchesAvailable(){
        Tile tile;
        for (int xx = 0; xx < numSquaresX; xx++) {
            for (int yy = 0; yy < numSquaresY; yy++) {
                tile = grid.getCellContent(new Cell(xx, yy));
                if (tile != null) {
                    for (int direction = 0; direction < 4; direction++) {
                        Cell[] vectors = getVectors(direction);
                        for (int i=0;i<vectors.length;i++){
                            Cell cell = new Cell(xx + vectors[i].getX(), yy + vectors[i].getY());
                            try {
                                Tile other = grid.getCellContent(cell);
                                if (other != null && other.getTitle().equals(tile.getTitle())) {
                                    if(i+1==vectors.length){
                                        if(tile.getTitle().equals(TILE_TITLE_O)){
                                            gameState = GAME_WIN;
                                        }else{
                                            gameState = GAME_LOST;
                                        }
                                        return true;
                                    }
                                }else{
                                    break;
                                }
                            }catch (NullPointerException e){
                                Log.i("exception",e.getMessage());
                                break;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }


    private boolean positionsEqual(Cell first, Cell second) {
        return first.getX() == second.getX() && first.getY() == second.getY();
    }

    private int winValue() {
        if (!canContinue()) {
            return endingMaxValue;
        } else {
            return startingMaxValue;
        }
    }

    public void setEndlessMode() {
        gameState = GAME_ENDLESS;
        mView.invalidate();
        mView.refreshLastTime = true;
    }

    public boolean canContinue() {
        return !(gameState == GAME_ENDLESS || gameState == GAME_ENDLESS_WON);
    }
}
