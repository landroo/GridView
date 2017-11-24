package org.landroo.gridview;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

/**
 * Created by rkovacs on 2015.07.06..
 */
public class Utils
{
    public static final float RADTODEG = 57.295779513082320876f;
    public static final float DEGTORAD = 0.0174532925199432957f;

    public class PointD
    {
        double x;
        double y;
    }

    // rotate a point (x, y) around the center (u, v) with a radian
    public static PointF rotatePnt(double u, double v, double x, double y, double ang)
    {
        PointF pnt = new PointF();
        pnt.x = (float) ((x - u) * Math.cos(ang) - (y - v) * Math.sin(ang) + u);
        pnt.y = (float) ((x - u) * Math.sin(ang) + (y - v) * Math.cos(ang) + v);

        return pnt;
    }

    // return the distance between the two point
    public static double getDist(double x1, double y1, double x2, double y2)
    {
        double nDelX = x2 - x1;
        double nDelY = y2 - y1;

        return Math.sqrt(nDelY * nDelY + nDelX * nDelX);
    }

    // return the angle degree
    public static double getAng(double x1, double y1, double x2, double y2)
    {
        double nDelX = x2 - x1;
        double nDelY = y2 - y1;
        double nDe = 0;

        if (nDelX != 0)
        {
            nDe = 2 * Math.PI;
            nDe = nDe + Math.atan(nDelY / nDelX);
            if (nDelX <= 0)
            {
                nDe = Math.PI;
                nDe = nDe + Math.atan(nDelY / nDelX);
            }
            else
            {
                if (nDelY >= 0)
                {
                    nDe = 0;
                    nDe = nDe + Math.atan(nDelY / nDelX);
                }
            }
        }
        else
        {
            if (nDelY == 0) nDe = 0;
            else
            {
                if (nDelY < 0) nDe = Math.PI;
                nDe = nDe + Math.PI / 2;
            }
        }

        return nDe / Math.PI * 180;
    }

    // point inside polygon
    public static boolean ponitInPoly(int nVert, float[] vertX, float[] vertY, float testX, float testY)
    {
        int i, j;
        boolean b = false;
        for (i = 0, j = nVert - 1; i < nVert; j = i++)
        {
            if (((vertY[i] > testY) != (vertY[j] > testY))
                    && (testX < (vertX[j] - vertX[i]) * (testY - vertY[i]) / (vertY[j] - vertY[i]) + vertX[i]))
            {
                b = !b;
            }
        }

        return b;
    }

    // square root
    public static double sqrt(double c)
    {
        if (c < 0) return Double.NaN;
        double err = 1e-15;
        double t = c;
        while (Math.abs(t - c/t) > err * t)
            t = (c/t + t) / 2.0;

        return t;
    }

    // recursive implementation of binary search
    public static int rank(int key, int[] a)
    {
        return rank(key, a, 0, a.length - 1);
    }

    public static int rank(int key, int[] a, int lo, int hi)
    {
        // Index of key in a[], if present, is not smaller than lo and not larger than hi.
        if (lo > hi) return -1;
        int mid = lo + (hi - lo) / 2;
        if (key < a[mid]) return rank(key, a, lo, mid - 1);
        else if (key > a[mid]) return rank(key, a, mid + 1, hi);
        else return mid;
    }

    // point line distance
    public static float pointLineDist(PointF A, PointF B, PointF pnt)
    {
        PointF p2 = new PointF(B.x - A.x, B.y - A.y);
        float f = p2.x * p2.x + p2.y * p2.y;
        float u = ((pnt.x - A.x) * p2.x + (pnt.y - A.y) * p2.y) / f;

        if (u > 1) u = 1;
        else if (u < 0) u = 0;

        float x = A.x + u * p2.x;
        float y = A.y + u * p2.y;

        float dx = x - pnt.x;
        float dy = y - pnt.y;

        float dist = (float)Math.sqrt(dx * dx + dy * dy);

        return dist;
    }

    // create base64 string from the bitmap
    public static String bitmapToBase64(Bitmap bmp)
    {
        String encodedImage = "";

        if(bmp != null)
        {
            ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 10, byteArrayBitmapStream);
            byte[] b = byteArrayBitmapStream.toByteArray();
            encodedImage = Base64.encodeToString(b, Base64.NO_WRAP);
        }

        return encodedImage;
    }

    // create a random number between minimum and maximum: Utils.random(0, 9, 1)
    public static int random(int nMinimum, int nMaximum, int nRoundToInterval)
    {
        if (nMinimum > nMaximum)
        {
            int nTemp = nMinimum;
            nMinimum = nMaximum;
            nMaximum = nTemp;
        }

        int nDeltaRange = (nMaximum - nMinimum) + (1 * nRoundToInterval);
        double nRandomNumber = Math.random() * nDeltaRange;

        nRandomNumber += nMinimum;

        int nRet = (int) (Math.floor(nRandomNumber / nRoundToInterval) * nRoundToInterval);

        return nRet;
    }

    private static void transpose(float[][] m)
    {

        for (int i = 0; i < m.length; i++)
        {
            for (int j = i; j < m[0].length; j++)
            {
                float x = m[i][j];
                m[i][j] = m[j][i];
                m[j][i] = x;
            }
        }
    }


    public static void rotateByNinetyToLeft(float[][] m)
    {
        // transpose
        transpose(m);

        //  swap rows
        for (int  i = 0; i < m.length / 2; i++)
        {
            for (int j = 0; j < m[0].length; j++)
            {
                float x = m[i][j];
                m[i][j] = m[m.length -1 -i][j];
                m[m.length -1 -i][j] = x;
            }
        }
    }


    public static void rotateByNinetyToRight(float[][] m)
    {
        // transpose
        transpose(m);

        // swap columns
        for (int  j = 0; j < m[0].length / 2; j++) {
            for (int i = 0; i < m.length; i++) {
                float x = m[i][j];
                m[i][j] = m[i][m[0].length -1 -j];
                m[i][m[0].length -1 -j] = x;
            }
        }
    }
}
