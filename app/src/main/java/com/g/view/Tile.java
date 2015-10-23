package com.g.view;

public class Tile extends Cell {
    private int value;
    private String title;
    private Tile[] mergedFrom = null;

    public Tile(int x, int y, int value) {
        super(x, y);
        this.value = value;
    }

    public Tile(int x, int y, String title) {
        super(x, y);
        this.title = title;
    }
    public Tile(int x, int y,int value, String title) {
        super(x, y);
        this.value = value;
        this.title = title;
    }

    public Tile(Cell cell, int value) {
        super(cell.getX(), cell.getY());
        this.value = value;
    }

    public Tile(Cell cell, String title) {
        super(cell.getX(), cell.getY());
        this.title = title;
    }

    public Tile(Cell cell,int value, String title) {
        super(cell.getX(), cell.getY());
        this.value = value;
        this.title = title;
    }

    public void updatePosition(Cell cell) {
        this.setX(cell.getX());
        this.setY(cell.getY());
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getValue() {
        return this.value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public Tile[] getMergedFrom() {
       return mergedFrom;
    }

    public void setMergedFrom(Tile[] tile) {
        mergedFrom = tile;
    }
}
