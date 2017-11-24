package org.landroo.gridview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by rkovacs on 13/09/2017.
 */

public class CellClass {

    private static final String TAG = "CellClass";

    // visible item list
    public List<Cell> items = new ArrayList<>();
    // hashmap data list
    public List<Map> data = new ArrayList<>();

    public String[] header;
    public int[] isNum;

    public int xNum = 10;// x size piece number
    public int yNum = 100;// y size piece number
    public float width = 100;// cell width
    public float height = 60;// cell height

    private int ofx, ofy, mw;// for rebuild

    private Paint selectPaint;
    private Paint strokePaint;
    private Paint foreColor;
    private Paint backColor1;
    private Paint backColor2;
    private Paint headerColor;
    private Paint filterColor;
    private Paint highColor;

    private Context context;    // the app

    // for sorting
    private int array[];
    private String field;
    private int lastOrderId = -1;
    private boolean desc = false;

    /**
     * constructor
     * @param context   app reference
     */
    public CellClass(Context context)
    {
        this.context = context;

        strokePaint = new Paint();
        strokePaint.setColor(0xFF000000);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(3);

        selectPaint = new Paint();
        selectPaint.setColor(0xFFEC5940);
        selectPaint.setStyle(Paint.Style.FILL);

        foreColor = new Paint();
        foreColor.setTextSize(16);

        backColor1 = new Paint();
        backColor1.setColor(0xFFFFFFFF);

        backColor2 = new Paint();
        backColor2.setColor(0xFFAAAAAA);

        headerColor = new Paint();
        headerColor.setColor(0xFF1F77C9);

        filterColor = new Paint();
        filterColor.setColor(0xFF0DAD93);

        highColor = new Paint();
        highColor.setColor(0xFFD4E236);
    }

    /**
     * draw cells to the canvas
     * @param canvas    reference to the canvas
     * @param xPos      float x position
     * @param yPos      float y position
     * @param zx        float zoom x
     * @param zy        float zoom y
     * @param displayWidth  int screen width
     * @param displayHeight int screen height
     */
    public void draw(Canvas canvas, float xPos, float yPos, float zx, float zy, int displayWidth, int displayHeight) {
        int cnt1 = 0;
        int cnt2 = 0;
        for (Cell item : items) {

            if(item.mode != Cell.HIDDEN) {

                cnt1++;
                if((cnt1 - 1) % xNum == 0) {
                    cnt2++;
                }

                final double dx = xPos + item.px * zx;
                final double dy = yPos + item.py * zy;

                if (dx > -item.width * zx && dx < displayWidth + item.width * zx && dy > -item.height * zy && dy < displayHeight + item.height * zy) {

                    canvas.save();
                    canvas.translate(xPos + item.px * zx, yPos + item.py * zy);
                    canvas.scale(zx, zy);

                    canvas.clipRect(0, 0, item.width, item.height);
                    if (item.selected) {
                        canvas.drawRect(0, 0, item.width, item.height, selectPaint);
                    }
                    else {
                        if(item.mode == Cell.HEADER) {
                            canvas.drawRect(0, 0, item.width, item.height, headerColor);
                        }
                        else if(item.mode == Cell.FILTER) {
                            canvas.drawRect(0, 0, item.width, item.height, filterColor);
                        }
                        else if(item.lineColor || item.columnColor) {
                            canvas.drawRect(0, 0, item.width, item.height, highColor);
                        }
                        else if(cnt2 % 2 == 1) {
                            canvas.drawRect(0, 0, item.width, item.height, backColor1);
                        }
                        else {
                            canvas.drawRect(0, 0, item.width, item.height, backColor2);
                        }
                    }

                    canvas.drawRect(0, 0, item.width, item.height, strokePaint);

                    canvas.rotate(item.angle);
                    canvas.drawText("" + item.text, 10, foreColor.getTextSize(), foreColor);

                    canvas.restore();
                }
            }
        }
    }

    /**
     * add test cells
     * @param sx    float start x
     * @param sy    float start y
     */
    public void addCells(float sx, float sy) {
        int cnt = 0;
        for (int y = 0; y < yNum; y++) {
            for (int x = 0; x < xNum; x++) {
                Cell cell = new Cell(sx + x * width, sy + y * height, width, height, x, y, cnt++, null, "");
                items.add(cell);
            }
        }
    }

    /**
     * resize the selected column
     * @param dx    float delta x
     * @param id    int selected cell id
     * @return      boolean resizeable
     */
    public boolean sizeColumn(float dx, int id) {
        int fc = (int)(id % xNum);
        for(int i = fc; i < xNum * yNum + xNum; i += xNum) {
            Cell item = items.get(i);
            if (item.setWidth(dx)) {
                for (int j = i + 1; j < i + xNum - fc; j++) {
                    item = items.get(j);
                    item.px += dx;
                }
            }
            else {
                return false;
            }
        }
        return true;
    }

    /**
     * resize the selected line
     * @param dy    float delta y
     * @param id    int selected cell id
     * @return      boolean resizeable
     */
    public boolean sizeLine(float dy, int id) {
        int fc = id - (id % xNum);
        for(int i = fc; i < fc + xNum; i++) {
            Cell item = items.get(i);
            if(item.setHeight(dy)) {
                for (int j = i + xNum; j < xNum * yNum + xNum; j += xNum) {
                    item = items.get(j);
                    item.py += dy;
                }
            }
            else {
                return false;
            }
        }
        return true;
    }

    /**
     * parse csv string
     * @param csv   csv string
     * @param sx    int screen offset x
     * @param sy    int screen offset y
     * @param maxWidth  maximum cell width
     */
    public void parseCSV(String csv, int sx, int sy, int maxWidth) {

        if(csv.indexOf("\t") == -1) {
            Toast.makeText(context, "Not TAB separated data!", Toast.LENGTH_LONG).show();
            return;
        }

        data.clear();
        items.clear();

        String[] lines = csv.split("\n");
        lines[0] = "No.\t" + lines[0];
        header = lines[0].split("\t");

        isNum = new int[header.length];
        for(int i = 0; i < isNum.length; i++) {
            isNum[i] = 0;
        }

        xNum = header.length;
        yNum = lines.length;
        //yNum = 100;

        ofx = sx;
        ofy = sy;
        mw = maxWidth;

        // add header
        int cnt = 0, x, y = 0;
        cnt = addHeaders(sx, sy, y, cnt);
        y++;

        // add filter
        cnt = addFilters(sx, sy, y, cnt);
        y++;

        // add data
        data = new ArrayList<Map>();
        int lineNo = 1;
        for(int i = 1; i < lines.length; i++) {
            lines[i] = lineNo++ + "\t" + lines[i];
            x = 0;
            Map<String, Object> hm = new HashMap<>();
            for(int j = 0; j < xNum; j++) {
                String[] fields = lines[i].split("\t");
                if(j < fields.length) {
                    if(TextUtils.isDigitsOnly(fields[j]) && !fields[j].isEmpty()){
                        hm.put(header[j], Integer.parseInt(fields[j]));
                        isNum[j]++;
                    }
                    else {
                        hm.put(header[j], fields[j].trim());
                    }
                }

                Cell cell = new Cell(sx + x * width, sy + y * height, width, height, x, y, cnt, hm, header[x]);
                if(x == 0) {
                    cell.mode = Cell.HEADER;
                }
                cnt++;
                x++;
                if(j < fields.length) {
                    cell.text = fields[j].trim();
                }
                else {
                    cell.text = "";
                }
                items.add(cell);
            }
            data.add(hm);
            y++;
            //if(i > 100) break;
        }

        sizeColumns(maxWidth);
    }

    /**
     * add column headers
     * @param sx    float start x
     * @param sy    float star y
     * @param l     int line counter
     * @param c     int id counter
     * @return      int line counter
     */
    private int addHeaders(float sx, float sy, int l, int c){
        int x = 0, y = l, cnt = c;
        for(int i = 0; i < xNum; i++) {
            Cell cell = new Cell(sx + x * width, sy + y * height, width, height, x, y, cnt++, null, "" + i);
            cell.text = header[i].toString();
            cell.mode = Cell.HEADER;
            items.add(cell);
            x++;
        }

        return cnt;
    }

    /**
     * add column filters
     * @param sx    float start x
     * @param sy    float star y
     * @param l     int line counter
     * @param c     int id counter
     * @return      int line counter
     */
    private int addFilters(float sx, float sy, int l, int c){
        int x = 0, y = l, cnt = c;
        for(int i = 0; i < xNum; i++) {
            Cell cell = new Cell(sx + x * width, sy + y * height, width, height, x, y, cnt++, null, header[i]);
            cell.text = "";
            if(i == 0) {
                cell.mode = Cell.HEADER;
            }
            else {
                cell.mode = Cell.FILTER;
            }
            items.add(cell);
            x++;
        }
        return cnt;
    }

    /**
     * resize columns by contents
     * @param maxWidth
     */
    private void sizeColumns(float maxWidth) {
        String w;
        float tw;
        foreColor.setTextSize(height - 16);
        for(int i = 0; i < xNum; i++) {
            w = getMaxColWidth(i);
            tw = foreColor.measureText(w);
            if(tw < maxWidth) {
                sizeColumn(tw, i);
            }
            else {
                sizeColumn(maxWidth, i);
            }
        }
    }

    /**
     * load a text file
     * @param fName string file name
     * @param asset boolean form assests
     * @return      string file content
     */
    public String loadFile(String fName, boolean asset)
    {
        String fileName = "";
        if(fName.indexOf(":") != -1) {
            fileName = Uri.decode(fName);
            String[] arr = fileName.split(":");
            if(arr.length > 1) {
                fileName = context.getExternalCacheDir().getAbsolutePath();
                fileName = fileName.substring(0, fileName.indexOf("Android"));
                fileName += arr[2];
                //Log.i(TAG, fileName);
            }
        }

        File file = new File(fName);
        if(!file.exists()) {
            file = new File(fileName);
        }

        StringBuffer sb = new StringBuffer();
        BufferedReader reader = null;
        try
        {
            if(asset) {
                reader = new BufferedReader(new InputStreamReader(context.getAssets().open(fName), "UTF-8"));
            }
            else {
                FileInputStream inStrem = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(inStrem, "UTF-8"));
            }

            // do reading, usually loop until end of file reading
            String line;
            while ((line = reader.readLine()) != null) {
                //process line
                sb.append(line);
                sb.append("\n");
            }
        }
        catch (Exception e) {
            Log.e(TAG, "" + e);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (Exception e) {
                    Log.e(TAG, "" + e);
                }
            }
        }

        return sb.toString();
    }

    /**
     * load a file by uri
     * @param uri   Uri uri
     * @return      string file content
     */
    public String loadUri(Uri uri) {
        StringBuffer sb = new StringBuffer();
        try {
            InputStream inStrem = context.getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inStrem, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                //process line
                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            inStrem.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    /**
     * calculate table width
     * @return  int table width
     */
    public int getWidth() {
        int w = 0;
        for(int i = 0; i < xNum; i++) {
            w += items.get(i).width;
        }

        return w;
    }

    /**
     * calculate table height
     * @return  table height
     */
    public int getHeight() {
        int h = 0;
        for(int i = 0; i < items.size(); i += xNum) {
            if(items.get(i).mode != Cell.HIDDEN) {
                h += items.get(i).height;
            }
        }

        return h;
    }

    /**
     * select the longest text in a column
     * @param col   int column number
     * @return      string the longest text
     */
    public String getMaxColWidth(int col) {
        String w = "";
        for(int i = xNum; i < items.size() - xNum; i += xNum) {
            if(items.get(i + col).text.length() > w.length()) {
                w = items.get(i + col).text;
            }
        }

        return w;
    }

    /**
     * set the column filter
     * @param id    int column id
     * @param text  string filter
     * @return      filtered line number
     */
    public int setFilter(final int id, final String text) {
        int cnt = 0;
        float h = items.get(0).py + items.get(0).height + items.get((int)xNum).height;
        // get the column
        int fc = (int) (id % xNum);
        //Log.i(TAG, "col: " + fc);
        for (int i = fc; i < xNum * yNum; i += xNum) {
            Cell item = items.get(i);
            if(item.mode == Cell.NORMAL) {
                int fl = item.id - (item.id % xNum);
                // find text
                if (item.text.toLowerCase().indexOf(text.toLowerCase()) != -1) {
                    // if found move line up
                    for (int j = fl; j < fl + xNum; j++) {
                        items.get(j).py = h;
                        items.get(j).filtered = true;
                    }
                    h += item.height;
                    cnt++;
                }
                else {
                    // hide a line
                    for (int j = fl; j < fl + xNum; j++) {
                        items.get(j).mode = Cell.HIDDEN;
                    }
                }
            }
        }
        //Log.i(TAG, "" + cnt);
        return cnt;
    }

    /**
     * clear cell mode
     */
    public void clearFilter() {
        for (int j = 0; j < xNum * yNum; j++) {
            if(items.get(j).mode == Cell.HIDDEN) {
                items.get(j).mode = Cell.NORMAL;
            }
            if(items.get(j).filtered) {
                items.get(j).filtered = false;
                items.get(j).py = items.get(j).oy;
            }
        }
    }

    /**
     * sort data
     * @param id    int sort column id
     */
    public void sortItems(int id) {
        int fc = (int) (id % xNum);
        field = header[fc];
        array = new int[data.size()];
        int cnt = 0;
        for(int i = 0; i < data.size(); i++) {
            array[i] = i;
        }

        quickSort(0, data.size()- 1, isNum[fc] == data.size());

        items = new ArrayList<>();

        // add header
        int x, y = 0;
        cnt = addHeaders(ofx, ofy, y, cnt);
        y++;

        // add filter
        cnt = addFilters(ofx, ofy, y, cnt);
        y++;

        if(id == lastOrderId) {
            desc = !desc;
        }

        Map map;
        int line = 1;
        if(desc) {
            for(int i = array.length - 1; i >= 0 ; i--) {
                map = data.get(array[i]);
                x = 0;
                if(map == null)
                    continue;
                for(int j = 0; j < xNum; j++) {
                    Cell cell = new Cell(ofx + x * width, ofy + y * height, width, height, x, y, cnt++, map, header[j]);
                    if(x == 0) {
                        cell.mode = Cell.HEADER;
                        cell.text = "" + line++;
                    }
                    else {
                        cell.text = map.get(header[j]).toString();
                    }
                    items.add(cell);
                    x++;
                }
                y++;
            }
        }
        else {
            for(int i = 0; i < array.length; i++) {
                map = data.get(array[i]);
                x = 0;
                if(map == null)
                    continue;
                for(int j = 0; j < xNum; j++) {
                    Cell cell = new Cell(ofx + x * width, ofy + y * height, width, height, x, y, cnt++, map, header[j]);
                    if(x == 0) {
                        cell.mode = Cell.HEADER;
                        cell.text = "" + line++;
                    }
                    else {
                        cell.text = map.get(header[j]).toString();
                    }
                    items.add(cell);
                    x++;
                }
                y++;
            }
        }

        sizeColumns(mw);

        lastOrderId = id;
    }

    /**
     * change up two item in an array
     * @param i int first item id
     * @param j int second item id
     */
    private void exchangeNumbers(int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    /**
     * quick sort algorithm
     * @param lowerIndex    int start index
     * @param higherIndex   int end index
     * @param number        boolean is number
     */
    private void quickSort(int lowerIndex, int higherIndex, boolean number) {

        int i = lowerIndex;
        int j = higherIndex;
        // calculate pivot number, I am taking pivot as middle index number
        //int pivot = array[lowerIndex + (higherIndex - lowerIndex) / 2];
        int pivotNum = 0;
        String pivot = "";
        if(number) {
            pivotNum = (int)data.get(array[lowerIndex + (higherIndex - lowerIndex) / 2]).get(field);
        }
        else {
            pivot = data.get(array[lowerIndex + (higherIndex - lowerIndex) / 2]).get(field).toString().toLowerCase();
        }
        // Divide into two arrays
        while (i <= j) {
            /**
             * In each iteration, we will identify a number from left side which
             * is greater then the pivot value, and also we will identify a number
             * from right side which is less then the pivot value. Once the search
             * is done, then we exchange both numbers.
             */
            if(number) {
                while ((int)data.get(array[i]).get(field) < pivotNum) {
                    i++;
                }
                while ((int)data.get(array[j]).get(field) > pivotNum) {
                    j--;
                }
            }
            else {
                while (data.get(array[i]).get(field).toString().toLowerCase(Locale.getDefault()).compareTo(pivot) < 0) {
                    //while(compFunc(data.get(array[i]).get(field).toString(), pivot) < 0){
                    i++;
                }
                while (data.get(array[j]).get(field).toString().toLowerCase(Locale.getDefault()).compareTo(pivot) > 0) {
                    //while(compFunc(data.get(array[i]).get(field).toString(), pivot) > 0){
                    j--;
                }
            }
            if (i <= j) {
                exchangeNumbers(i, j);
                //move index to next position on both sides
                i++;
                j--;
            }
        }
        // call quickSort() method recursively
        if (lowerIndex < j)
            quickSort(lowerIndex, j, number);
        if (i < higherIndex)
            quickSort(i, higherIndex, number);
    }

    /**
     * highlight line
     * @param id    int line id
     */
    public void checkLine(int id) {
        int fc = id % xNum;
        if(fc == 0) {
            if(!items.get(id).lineColor)
                clearHigh();
            for(int i = 0; i < xNum; i++) {
                items.get(id + i).lineColor = !items.get(id + i).lineColor;
            }
        }
    }

    /**
     * highlight column
     * @param id    int column id
     */
    public void checkColumn(int id) {
        int fc = (int) (id % xNum);
        if(id < xNum) {
            if (!items.get(id).lineColor)
                clearHigh();
            for (int i = fc; i < items.size(); i += xNum) {
                items.get(i).lineColor = !items.get(i).lineColor;
            }
        }
    }

    /**
     * clear highlight
     */
    public void clearHigh() {
        for(Cell item: items) {
            item.lineColor = false;
            item.columnColor = false;
        }
    }

    /**
     * string compare function
     * @param a string first
     * @param b string second
     * @return  int -1 less, 0 equal, 1 bigger
     */
    private int compFunc(String a, String b) {
        int r;
        if(a.length() < b.length())
            r = compStr(a, b, true);
        else
            r = compStr(b, a, false);
        return r;
    }

    private final String abc = "aábcdeéfghiíjklmnoóöőpqrstuúüűvxyz";

    /**
     * compare two string
     * @param a string first
     * @param b string second
     * @param c string order
     * @return  int -1 less, 0 equal, 1 bigger
     */
    private int compStr(String a, String b, boolean c) {
        int r = 0;
        a = a.toLowerCase(Locale.getDefault());
        b = b.toLowerCase(Locale.getDefault());
        for(int i = 0; i < a.length(); i++) {
            char a1 = a.charAt(i);
            char b1 = b.charAt(i);
            if(a1 == b1) {
                continue;
            }
            else {
                int a2 = -1;
                int b2 = -1;
                for(int j = 0; j < abc.length(); j++) {
                    if(a1 == abc.charAt(j) && a2 == -1) a2 = j;
                    if(b1 == abc.charAt(j) && b2 == -1) b2 = j;
                }
                if( a2 == -1 || b2 == -1) {
                    if(a1 > b1) r = 1;
                    else r = -1;
                    break;
                }
                if(a2 > b2) r = 1;
                else r = -1;
                break;
            }
        }

        if(!c && r != 0) {
            if(r == 1) r = -1;
            else r = 1;
        }

        return r;
    }

    /**
     * save csv file
     * @param fName string file name
     */
    public void saveCSV(final String fName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String fileName = "";
                if(fName.indexOf(":") != -1) {
                    fileName = Uri.decode(fName);
                    String[] arr = fileName.split(":");
                    fileName = context.getExternalCacheDir().getAbsolutePath();
                    fileName = fileName.substring(0, fileName.indexOf("Android"));
                    if(arr.length > 1) {
                        fileName += arr[2];
                        //Log.i(TAG, fileName);
                    }
                    else {
                        fileName += "backup.csv";
                    }
                }
                else {
                    fileName = fName;
                }

                File fout = new File(fileName);
                FileOutputStream fos;
                BufferedWriter bw;
                try {
                    fos = new FileOutputStream(fout);
                    bw = new BufferedWriter(new OutputStreamWriter(fos));

                    // write header
                    StringBuilder sb = new StringBuilder();
                    //sb.append("\ufeff");
                    for(int i = 1; i < header.length; i++) {
                        if(i > 1) {
                            sb.append("\t");
                        }
                        sb.append(header[i]);
                    }
                    sb.append("\r\n");
                    bw.write(sb.toString());

                    // write data
                    for(Map map: data) {
                        sb = new StringBuilder();
                        for(int i = 1; i < header.length; i++) {
                            if(i > 1) {
                                sb.append("\t");
                            }
                            sb.append(map.get(header[i]));
                        }
                        sb.append("\r\n");
                        bw.write(sb.toString());
                    }

                    bw.flush();
                    bw.close();
                    fos.close();
                } catch (Exception ex) {
                    Log.e(context.getResources().getString(R.string.app_name), "" + ex);
                }
                Log.e(context.getResources().getString(R.string.app_name), fileName + " saved.");
            }
        }).start();
    }

    /**
     * Add a new empty line to the end of the table
     */
    public void addLine() {
        Map<String, Object> hm = new HashMap<>();
        int x = 0;
        int cnt = xNum * yNum + xNum;
        int sx = ofx;
        int sy = getHeight() + ofy;
        for (int j = 0; j < xNum; j++) {
            if (isNum[j] == data.size()) {
                hm.put(header[j], 0);
                isNum[j]++;
            }
            else {
                hm.put(header[j], "");
            }

            Cell cell = new Cell(sx, sy, items.get(j).width, height, x, yNum + 1, cnt, hm, header[x]);
            if(j == 0) {
                cell.mode = Cell.HEADER;
                cell.text = "" + (data.size() + 1);
            }
            items.add(cell);
            x++;
            sx += items.get(j).width;
        }
        data.add(hm);
        yNum++;
    }

    /**
     * delete the selected line
     * @param cell    selected cell
     */
    public void deleteLine(Cell cell) {
        int fc = cell.id % xNum;
        if(fc == 0) {
            Map map = cell.lineData;
            data.remove(map);

            yNum--;

            items = new ArrayList<>();

            // add header
            int x, y = 0, cnt = 0;
            cnt = addHeaders(ofx, ofy, y, cnt);
            y++;

            // add filter
            cnt = addFilters(ofx, ofy, y, cnt);
            y++;

            int line = 1;
            for(int i = 0; i < data.size(); i++) {
                map = data.get(i);
                x = 0;
                for(int j = 0; j < xNum; j++) {
                    cell = new Cell(ofx + x * width, ofy + y * height, width, height, x, y, cnt++, map, header[j]);
                    if(x == 0) {
                        cell.mode = Cell.HEADER;
                        cell.text = "" + line++;
                    }
                    else {
                        cell.text = map.get(header[j]).toString();
                    }
                    items.add(cell);
                    x++;
                }
                y++;
            }

            sizeColumns(mw);
        }
        else {
            Toast.makeText(context, "No selected line!", Toast.LENGTH_SHORT).show();
        }
    }
}
