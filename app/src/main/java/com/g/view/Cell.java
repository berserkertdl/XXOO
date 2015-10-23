package com.g.view;

public class Cell {
    private int x;
    private int y;

    private int startingX;
    private int startingY;
    private int endingX;
    private int endingY;

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Cell( int startingX, int startingY, int endingX, int endingY) {
        this.startingX = startingX;
        this.startingY = startingY;
        this.endingX = endingX;
        this.endingY = endingY;
    }

    public Cell(int x, int y, int startingX, int startingY, int endingX, int endingY) {
        this.x = x;
        this.y = y;
        this.startingX = startingX;
        this.startingY = startingY;
        this.endingX = endingX;
        this.endingY = endingY;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getStartingX() {
        return startingX;
    }

    public void setStartingX(int startingX) {
        this.startingX = startingX;
    }

    public int getStartingY() {
        return startingY;
    }

    public void setStartingY(int startingY) {
        this.startingY = startingY;
    }

    public int getEndingX() {
        return endingX;
    }

    public void setEndingX(int endingX) {
        this.endingX = endingX;
    }

    public int getEndingY() {
        return endingY;
    }

    public void setEndingY(int endingY) {
        this.endingY = endingY;
    }
}
