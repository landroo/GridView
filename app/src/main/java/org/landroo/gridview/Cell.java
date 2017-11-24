package org.landroo.gridview;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by rkovacs on 13/09/2017.
 */

public class Cell {

    private static final String TAG = "Cell";
    private static final int GAP = 10;
    public static final int NORMAL = 0;
    public static final int HIDDEN = 1;
    public static final int LINE = 2;
    public static final int COLUMN = 3;
    public static final int HEADER = 4;
    public static final int FILTER = 5;

    public int mode = 0;

    public float px;// X position
    public float py;// Y position

    public float ox;// original X position
    public float oy;// original Y position

    public int sx;// start X
    public int sy;// start Y

    public int id;

    public boolean selected = false;

    public float angle = 0;

    public float width;
    public float height;

    public String text = "";

    public boolean filtered = false;
    public boolean lineColor = false;
    public boolean columnColor = false;

    public Map<String, Object> lineData;
    public String key;

    /**
     * constructor
     * @param px    float position x
     * @param py    float position y
     * @param w     float width
     * @param h     float height
     * @param sx    int start x
     * @param sy    int start y
     * @param id    int id
     * @param hm    reference to line data
     * @param key   data key
     */
    public Cell(float px, float py, float w, float h, int sx, int sy, int id, Map<String, Object> hm, String key) {
        this.px = px;
        this.py = py;

        this.ox = px;
        this.oy = py;

        this.width = w;
        this.height = h;

        this.sx = sx;
        this.sy = sy;

        this.id = id;

        this.lineData = hm;
        this.key = key;
    }

    /**
     * tap inside the cell
     * @param posx  float x position
     * @param posy  float y position
     * @param zx    float x zoom factor
     * @param zy    float y zoom factor
     * @return  boolean is inside
     */
    public boolean isInside(float posx, float posy, float zx, float zy) {
        if(posx > px * zx && posx < (px + width) * zx && posy > py * zy && posy < (py + height) * zy) {
            return true;
        }
        return false;
    }

    /**
     * tap on bottom of te cell
     * @param posx  float x position
     * @param posy  float y position
     * @param zx    float x zoom factor
     * @param zy    float y zoom factor
     * @return  boolean tap on cell bottom
     */
    public boolean onBottom(float posx, float posy, float zx, float zy) {
        if(posy - GAP < (py + height) * zy && posy + GAP > (py + height) * zy && posx > px * zx && posx < (px + width) * zx) {
            return true;
        }
        return false;
    }

    /**
     * tap on right of te cell
     * @param posx  float x position
     * @param posy  float y position
     * @param zx    float x zoom factor
     * @param zy    float y zoom factor
     * @return  boolean tap on cell right
     */
    public boolean onRight(float posx, float posy, float zx, float zy) {
        if(posx - GAP < (px + width) * zx && posx + GAP > (px + width) * zx && posy > py * zy && posy < (py + height) * zy) {
            return true;
        }
        return false;
    }

    /**
     * set the cell width
     * @param dx    float delta x
     * @return      boolean set new width
     */
    public boolean setWidth(float dx) {
        if(width + dx < GAP)
            return false;
        width += dx;
        return true;
    }

    /**
     * set the cell height
     * @param dy    float delta y
     * @return      boolean set new height
     */
    public boolean setHeight(float dy) {
        if(height + dy < GAP)
            return false;
        height += dy;
        return true;
    }

    /**
     * update cell data
     */
    public void updateText() {
        if(lineData != null) {
            lineData.put(key, text);
        }
    }

}
