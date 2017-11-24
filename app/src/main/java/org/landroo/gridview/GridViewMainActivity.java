package org.landroo.gridview;
/*
GridView
Simple csv table application.
- Show a content of the csv file in a table view.
- The table scrollable and resizeable.
- The rows and the columns resizeable by long tapping the end of a cell.
- You can select a cell by tapping on and edit the text on second tap.
- You can select a line or a column by tapping the header of the line or column.
- You can filter the table by typing in the green filter cell.
- You can reorder the table by third tap on a filter cell.
- You can add orr delete a line in the table.

v 1.0

tasks:
numeric ordering the column OK  more line text if longer    __  create new table     __

bugs:
file open from SD card      __  size increase error         OK

*/
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import org.landroo.ui.UI;
import org.landroo.ui.UIInterface;
import org.landroo.view.ScaleView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class GridViewMainActivity extends Activity implements UIInterface {

    private static final int SCROLL_INTERVAL = 10;
    private static final int SCROLL_ALPHA = 500;
    private static final int SCROLL_SIZE = 16;
    private static final int GAP = 10;
    private static final int FILE_SELECT_CODE = 1;

    private String TAG;

    // virtual desktop
    private GridView gridView;
    private int displayWidth;
    private int displayHeight;
    private int deskWidth;
    private int deskHeight;

    // scroll plane
    private ScaleView scaleView;
    private float sX = 0;
    private float sY = 0;
    private float mX = 0;
    private float mY = 0;
    private float zoomX = 1;
    private float zoomY = 1;
    private float xPos;
    private float yPos;
    private boolean afterMove = false;
    private boolean paused = false;

    // user event handler
    private UI ui = null;
    private Timer scrollTimer = null;
    private Paint scrollPaint1 = new Paint();
    private Paint scrollPaint2 = new Paint();
    private int scrollAlpha = SCROLL_ALPHA;
    private int scrollBar = 0;
    private float barPosX = 0;
    private float barPosY= 0;

    // background
    private int tileSize = 80;
    private Bitmap backBitmap;
    private Drawable backDrawable;// background bitmap drawable
    private boolean staticBack = true;// fix or scrollable background
    private int backColor = Color.LTGRAY;// background color
    private float rotation = 0;
    private float rx = 0;
    private float ry = 0;
    private int longPress = 0;
    private String back = "";//"grid";

    // cells
    private Bitmap fillBMP = null;
    private CellClass cellClass;
    private Cell selItem = null;
    private Cell lastItem = null;
    private float hBar = -1;
    private float vBar = -1;
    private Paint barPaint = new Paint();
    private String fName;

    private TextView drawerText;
    private EditText drawerEdit;
    private SlidingDrawer bottomView;
    public Rect bottomViewRect;

    /**
     * main view
     */
    private class GridView extends ViewGroup {

        public GridView(Context context) {
            super(context);
        }

        // draw items
        @Override
        protected void dispatchDraw(@NonNull Canvas canvas) {
            drawBack(canvas);
            drawItems(canvas);
            drawScrollBars(canvas);

            super.dispatchDraw(canvas);
        }

        @Override
        protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
            // main
            View child = this.getChildAt(0);
            if (child != null) {
                //child.layout(0, 0, displayWidth, displayHeight);
                child.layout(bottomViewRect.left, bottomViewRect.top, bottomViewRect.right, bottomViewRect.bottom);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(displayWidth, displayHeight);
            // main
            View child = this.getChildAt(0);
            if (child != null)
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
        }
    }

    /**
     * entry point
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_grid_view_main);

        TAG = getResources().getString(R.string.app_name);

        Display display = getWindowManager().getDefaultDisplay();
        displayWidth = display.getWidth();
        displayHeight = display.getHeight();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        gridView = new GridView(this);
        setContentView(gridView);

        ui = new UI(this);

        cellClass = new CellClass(this);

        // Get filename
        String sFile = "";
        Intent intent = getIntent();
        Uri data = intent.getData();
        if(data != null) {
            sFile = data.getEncodedPath();
            try {
                sFile = URLDecoder.decode(sFile, "UTF-8");
            }
            catch (Exception e) {
            }
        }

        SharedPreferences settings = getSharedPreferences("org.landroo.gridview_preferences", MODE_PRIVATE);
        fName = settings.getString("lastUri", "");
        if(!sFile.equals("")) {
            fName = sFile;
        }
        String csv = cellClass.loadUri(Uri.parse(fName));
        if(csv.equals("")) {
            csv = cellClass.loadFile(fName, false);
        }
        if(csv.equals("")) {
            showFileChooser();
        }
        else {
            cellClass.parseCSV(csv, 80, 80, displayWidth / 3);

            deskWidth = cellClass.getWidth() + 160;
            deskHeight = cellClass.getHeight() + 160;
            scaleView = new ScaleView(displayWidth, displayHeight, deskWidth, deskHeight, gridView);
        }

        scrollPaint1.setColor(Color.GRAY);
        scrollPaint1.setAntiAlias(true);
        scrollPaint1.setDither(true);
        scrollPaint1.setStyle(Paint.Style.STROKE);
        scrollPaint1.setStrokeJoin(Paint.Join.ROUND);
        scrollPaint1.setStrokeCap(Paint.Cap.ROUND);
        scrollPaint1.setStrokeWidth(SCROLL_SIZE);

        scrollPaint2.setColor(0xFF4AE2E7);
        scrollPaint2.setAntiAlias(true);
        scrollPaint2.setDither(true);
        scrollPaint2.setStyle(Paint.Style.STROKE);
        scrollPaint2.setStrokeJoin(Paint.Join.ROUND);
        scrollPaint2.setStrokeCap(Paint.Cap.ROUND);
        scrollPaint2.setStrokeWidth(SCROLL_SIZE);

        barPaint.setColor(Color.GRAY);
        barPaint.setAntiAlias(true);
        barPaint.setDither(true);
        barPaint.setStyle(Paint.Style.STROKE);
        barPaint.setStrokeJoin(Paint.Join.ROUND);
        barPaint.setStrokeCap(Paint.Cap.ROUND);
        barPaint.setStrokeWidth(SCROLL_SIZE);

        if (!back.equals("")) {
            int resId = getResources().getIdentifier(back, "drawable", getPackageName());
            backBitmap = BitmapFactory.decodeResource(getResources(), resId);
            backDrawable = new BitmapDrawable(backBitmap);
            backDrawable.setBounds(0, 0, backBitmap.getWidth(), backBitmap.getHeight());
        }

        bottomView = (SlidingDrawer)getBottomView();
        gridView.addView(bottomView);
    }

    @Override
    public void onBackPressed() {
        if(bottomView.isOpened()) {
            bottomView.animateClose();
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        paused = true;

        cellClass.saveCSV(fName);

        if(scrollTimer != null)
        {
            scrollTimer.cancel();
            scrollTimer = null;
        }
        //Log.i(TAG, "paused");
        super.onPause();
    }

    @Override
    protected void onResume() {
        paused = false;

        scrollTimer = new Timer();
        scrollTimer.scheduleAtFixedRate(new ScrollTask(), 0, SCROLL_INTERVAL);

        //Log.i(TAG, "resumed");
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    fName = uri.toString();

                    SharedPreferences settings = getSharedPreferences("org.landroo.gridview_preferences", MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("lastUri", fName);
                    editor.apply();

                    String csv = cellClass.loadUri(uri);
                    cellClass.parseCSV(csv, 80, 80, displayWidth / 3);

                    if(scaleView == null) {
                        deskWidth = cellClass.getWidth() + 160;
                        deskHeight = cellClass.getHeight() + 160;
                        scaleView = new ScaleView(displayWidth, displayHeight, deskWidth, deskHeight, gridView);
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return ui.tapEvent(event);
    }

    @Override
    public void onDown(float x, float y) {
        afterMove = false;
        scrollAlpha = SCROLL_ALPHA;

        scaleView.onDown(x, y);

        sX = x / zoomX;
        sY = y / zoomY;

        mX = x / zoomX;
        mY = y / zoomY;

        xPos = scaleView.xPos();
        yPos = scaleView.yPos();

        scrollBar = checkBars(x, y);
        if(scrollBar == 1) {
            barPosX = x - barPosX;
        }
        else if(scrollBar == 2) {
            barPosY = y - barPosY;
        }
        else if(hBar == -1 && vBar == -1) {
            for (int i = cellClass.items.size() - 1; i >= 0; i--) {
                Cell item = cellClass.items.get(i);
                if(item.mode != Cell.HIDDEN) {
                    if (item.onRight(x - xPos, y - yPos, zoomX, zoomY)) {
                        hBar = xPos + (item.px + item.width) * zoomX;
                        longPress = 0;
                        lastItem = item;
                        break;
                    }
                    if (item.onBottom(x - xPos, y - yPos, zoomX, zoomY)) {
                        vBar = yPos + (item.py + item.height) * zoomY;
                        longPress = 0;
                        lastItem = item;
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onUp(float x, float y) {
        scaleView.onUp(x, y);

        scrollBar = 0;

        longPress = 0;
        if (hBar != -1) {
            hBar = -1;
            resizeDesk();
        }
        if (vBar != -1) {
            vBar = -1;
            resizeDesk();
        }

        if (selItem != null) {
            //afterMove = true;
            //gridView.postInvalidate();
        }
    }

    /**
     * resize virtual desktop
     */
    private void resizeDesk() {
        deskWidth = cellClass.getWidth() + 160;
        deskHeight = cellClass.getHeight() + 160;
        scaleView.setSize(displayWidth, displayHeight, deskWidth, deskHeight);
        scaleView.setPos(xPos, yPos);
        scaleView.setZoom(zoomX, zoomY);
        gridView.postInvalidate();
    }

    @Override
    public void onTap(float x, float y) {
        scrollAlpha = SCROLL_ALPHA;
        hBar = -1;
        vBar = -1;
        longPress = 60;
        scrollBar = 0;

        xPos = scaleView.xPos();
        yPos = scaleView.yPos();

        for (int i = cellClass.items.size() - 1; i >= 0; i--) {
            Cell item = cellClass.items.get(i);
            if (item.isInside(x - xPos, y - yPos, zoomX, zoomY)) {
                if (selItem != null) {
                    if (selItem == item) {
                        if (bottomView.isOpened()) {
                            bottomView.animateClose();
                            if (selItem.mode == Cell.FILTER) {
                                cellClass.clearHigh();
                                cellClass.sortItems(selItem.id);
                            }
                        } else {
                            if (selItem.mode != Cell.HEADER)
                                bottomView.animateOpen();
                        }
                    } else {
                        selItem.selected = false;
                    }
                }
                item.selected = true;
                selItem = item;
                if (selItem.mode != Cell.HEADER)
                    drawerEdit.setText(item.text);

                cellClass.checkLine(item.id);
                cellClass.checkColumn(item.id);

                gridView.postInvalidate();
                break;
            }
        }

        return;
    }

    @Override
    public void onHold(float x, float y) {
        //Log.i(TAG, "onHold");
    }

    @Override
    public void onMove(float x, float y) {
        scrollAlpha = SCROLL_ALPHA;

        mX = x / zoomX;
        mY = y / zoomY;

        float dx = mX - sX;
        float dy = mY - sY;

        boolean bMove = true;

        if(scrollBar != 0) {
            // vertical scroll
            if(scrollBar == 1) {
                float xp = -(x - barPosX) / (displayWidth / (deskWidth * zoomX));
                Log.i(TAG, "" + xp);
                if(xp < 0 && xp > displayWidth - deskWidth * zoomX) {
                    xPos = xp;
                }
            }
            else {
                float yp = -(y - barPosY) / (displayHeight / (deskHeight * zoomY));
                Log.i(TAG, "" + yp);
                if(yp < 0 && yp > displayHeight - deskHeight * zoomY) {
                    yPos = yp;
                }
            }
            bMove = false;
            scaleView.setPos(xPos, yPos);
            gridView.postInvalidate();
        }

        if((dx != 0 || dy != 0) && longPress < 50) {
            longPress = 60;
            //Log.i(TAG, "cancel longpress");
        }

        if(longPress == 50) {
            if (hBar != -1) {
                if (cellClass.sizeColumn(dx, lastItem.id)) {
                    hBar += dx * zoomX;
                    gridView.postInvalidate();
                }
                bMove = false;
            }
            else if (vBar != -1) {
                if (cellClass.sizeLine(dy, lastItem.id)) {
                    vBar += dy * zoomY;
                    gridView.postInvalidate();
                }
                bMove = false;
            }
        }

        if(bMove) {
            scaleView.onMove(x, y);
        }

        sX = mX;
        sY = mY;
    }

    @Override
    public void onSwipe(int direction, float velocity, float x1, float y1, float x2, float y2) {
        hBar = -1;
        vBar = -1;
        longPress = 0;

        if (!afterMove)
            scaleView.onSwipe(direction, velocity, x1, y1, x2, y2);
    }

    @Override
    public void onDoubleTap(float x, float y) {
        gridView.postInvalidate();
    }

    @Override
    public void onZoom(int mode, float x, float y, float distance, float xdiff, float ydiff) {
        hBar = -1;
        vBar = -1;
        longPress = 0;

        scaleView.onZoom(mode, x, y, distance, xdiff, ydiff);

        zoomX = scaleView.getZoomX();
        zoomY = scaleView.getZoomY();
    }

    @Override
    public void onRotate(int mode, float x, float y, float angle) {

    }

    @Override
    public void onFingerChange() {

    }

    /**
     * draw background
     * @param canvas Canvas canvas
     */
    private void drawBack(Canvas canvas) {
        if (backDrawable != null) {
            // static back or tiles
            if (staticBack) {
                if(backDrawable != null) {
                    backDrawable.setBounds(0, 0, displayWidth, displayHeight);
                    backDrawable.draw(canvas);
                }
                else {
                    canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
                }
            }
            else {
                if (scaleView != null) {
                    xPos = scaleView.xPos();
                    yPos = scaleView.yPos();
                }
                for (float x = 0; x < deskWidth; x += tileSize) {
                    for (float y = 0; y < deskHeight; y += tileSize) {
                        // distance of the tile center from the rotation center
                        final float dis = (float) Utils.getDist(rx * zoomX, ry * zoomY, (x + tileSize / 2) * zoomX, (y + tileSize / 2) * zoomY);
                        // angle of the tile center from the rotation center
                        final float ang = (float) Utils.getAng(rx * zoomX, ry * zoomY, (x + tileSize / 2) * zoomX, (y + tileSize / 2) * zoomY);

                        // coordinates of the block after rotation
                        final float cx = dis * (float) Math.cos((rotation + ang) * Utils.DEGTORAD) + rx * zoomX + xPos;
                        final float cy = dis * (float) Math.sin((rotation + ang) * Utils.DEGTORAD) + ry * zoomY + yPos;

                        if (cx >= -tileSize && cx <= displayWidth + tileSize && cy >= -tileSize && cy <= displayHeight + tileSize) {
                            backDrawable.setBounds(0, 0, (int) (tileSize * zoomX) + 1, (int) (tileSize * zoomY) + 1);

                            canvas.save();
                            canvas.rotate(rotation, rx * zoomX + xPos, ry * zoomY + yPos);
                            canvas.translate(x * zoomX + xPos, y * zoomY + yPos);
                            backDrawable.draw(canvas);
                            canvas.restore();
                        }
                    }
                }
            }
        } else {
            canvas.drawColor(backColor);
        }
    }

    /**
     * draw cells and resize bars
     * @param canvas    Canvas canvas
     */
    private void drawItems(Canvas canvas) {
        if (scaleView != null) {
            xPos = scaleView.xPos();
            yPos = scaleView.yPos();
        }
        if(cellClass != null) {
            cellClass.draw(canvas, xPos, yPos, zoomX, zoomY, displayWidth, displayHeight);
        }
        if(hBar != -1 && longPress == 50) {
            canvas.drawLine(hBar, 0, hBar, displayHeight, barPaint);
        }
        if(vBar != -1 && longPress == 50) {
            canvas.drawLine(0, vBar, displayWidth, vBar, barPaint);
        }
    }

    /**
     * draw scroll bars
     * @param canvas    Canvas canvas
     */
    private void drawScrollBars(Canvas canvas)
    {
        float x, y;
        float xSize = displayWidth / ((deskWidth * zoomX) / displayWidth);
        float ySize = displayHeight / ((deskHeight * zoomY) / displayHeight);

        x = (displayWidth / (deskWidth * zoomX)) * -xPos;
        y = displayHeight - SCROLL_SIZE - 2;
        if(xSize < displayWidth) {
            if (scrollBar == 1) {
                canvas.drawLine(x, y, x + xSize, y, scrollPaint2);
            }
            else {
                canvas.drawLine(x, y, x + xSize, y, scrollPaint1);
            }
        }

        x = displayWidth - SCROLL_SIZE - 2;
        y = (displayHeight / (deskHeight * zoomY)) * -yPos;
        if(ySize < displayHeight) {
            if (scrollBar == 2) {
                canvas.drawLine(x, y, x, y + ySize, scrollPaint2);
            }
            else {
                canvas.drawLine(x, y, x, y + ySize, scrollPaint1);
            }
        }
    }

    /**
     * long press and alpha timer
     */
    class ScrollTask extends TimerTask
    {
        public void run()
        {
            if(paused)
                return;

            if(longPress < 50) {
                longPress++;
            }

            if (scrollAlpha > 32) {
                scrollAlpha--;
                if (scrollAlpha > 255) scrollPaint1.setAlpha(255);
                else scrollPaint1.setAlpha(scrollAlpha);
                gridView.postInvalidate();
            }
        }
    }

    /**
     * add bottom drawer view
     * @return
     */
    private View getBottomView()
    {
        Display display = getWindowManager().getDefaultDisplay();
        int displayWidth = display.getWidth();
        int displayHeight = display.getHeight();

        LayoutInflater inflater = getLayoutInflater();

        int w = displayWidth;
        int h = displayHeight / 4;
        //ViewGroup view = (ViewGroup) inflater.inflate(R.layout.bottomview, (ViewGroup)this.findViewById(R.id.slidingDrawer));
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.bottomview, null);
        bottomView = (SlidingDrawer) view;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(w, h);
        bottomView.setLayoutParams(params);
        //bottomView.setTranslationX(10);
        bottomView.setTranslationY(displayHeight * 3 / 4);

        Bitmap back = BitmapFactory.decodeResource(getResources(), R.drawable.drawerbg);
        BitmapDrawable drawable = new BitmapDrawable(back);
        drawable.setBounds(0, 0, w, h);

        bottomViewRect = new Rect(0, 0, w, h);

        LinearLayout ll = (LinearLayout) view.getChildAt(1);
        ll.setBackgroundDrawable(drawable);

        drawerEdit = (EditText)view.findViewById(R.id.drawerEditView);
        drawerEdit.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(selItem.text.length() > drawerEdit.getText().toString().length()) {
                    cellClass.clearFilter();
                }
                selItem.text = drawerEdit.getText().toString();
                selItem.updateText();
                if(selItem.mode == Cell.FILTER && selItem.text.length() > 2) {
                    int num = cellClass.setFilter(selItem.id, selItem.text);
                    Toast.makeText(GridViewMainActivity.this, num + " found", Toast.LENGTH_LONG).show();

                    //Log.i(TAG, selItem.text);

                    Display display = getWindowManager().getDefaultDisplay();
                    int displayWidth = display.getWidth();
                    int displayHeight = display.getHeight();

                    deskWidth = cellClass.getWidth() + 160;
                    deskHeight = cellClass.getHeight() + 160;
                    scaleView.setSize(displayWidth, displayHeight, deskWidth, deskHeight);
                    scaleView.setPos(xPos, yPos);
                    scaleView.setZoom(zoomX, zoomY);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        drawerEdit.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    if (v != null) {
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }

                    /*
                    selItem.text = ((EditText) v).getText().toString();
                    float w = selItem.foreColor.measureText(selItem.text);
                    w -= selItem.width;
                    cellClass.sizeColumn(w + 20, selItem.id);*/
                }

                return false;
            }
        });

        drawerText = (TextView)view.findViewById(R.id.drawerTextView);

        ImageView imageView = (ImageView) view.findViewById(R.id.addImageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomView.animateClose();
                cellClass.addLine();
                resizeDesk();
            }
        });

        imageView = (ImageView) view.findViewById(R.id.deleteImageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomView.animateClose();
                if(selItem != null) {
                    cellClass.deleteLine(selItem);
                    selItem = null;
                    resizeDesk();
                }
                else {
                    Toast.makeText(GridViewMainActivity.this, "No selected line!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        imageView = (ImageView) view.findViewById(R.id.loadImageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomView.animateClose();
                showFileChooser();
            }
        });

        return bottomView;
    }

    /**
     * call default file browser
     */
    private void showFileChooser() {

        cellClass.saveCSV(fName);

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * get file path from uri
     * @param uri   Uri uri
     * @return      string file path
     */
    public String getPath(Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = this.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                Log.i(TAG, "" + e);
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * check tap on scroll bars
     * @param x float position x
     * @param y float position y
     * @return  int 1 vertical scroll bar 2 horizontal scroll bar
     */
    private int checkBars(float x, float y) {
        float px, py;
        float xSize = displayWidth / ((deskWidth * zoomX) / displayWidth);
        float ySize = displayHeight / ((deskHeight * zoomY) / displayHeight);
        px = (displayWidth / (deskWidth * zoomX)) * -xPos;
        py = displayHeight - SCROLL_SIZE - 2;
        //Log.i(TAG, "" + x + " " + xp + " " + (x+ xSize) + " " + y + " " + yp + " " + (y + SCROLL_SIZE));
        if(x > px && y > py - GAP && x < px + xSize && y < py + SCROLL_SIZE + GAP && xSize < displayWidth) {
            barPosX = px;
            return 1;
        }

        px = displayWidth - SCROLL_SIZE - 2;
        py = (displayHeight / (deskHeight * zoomY)) * -yPos;
        if(x > px - GAP && y > py && x < px + SCROLL_SIZE + GAP && y < py + ySize && ySize < displayHeight) {
            barPosY = py;
            return 2;
        }

        return 0;
    }
}
