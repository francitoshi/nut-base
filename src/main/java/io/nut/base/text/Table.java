/*
 * Table.java
 *
 * Copyright (c) 2024-2025 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.text;

import io.nut.base.util.Strings;
import io.nut.base.util.Utils;

/**
 *
 * @author franci
 */
public final class Table
{
    private static final char SPACE= ' ';
    private static final char HLINE= '-';
    private static final char CROSS= '+';
    private static final char VLINE= '|';
    
    public enum Paint
    {
        Space(SPACE,SPACE,SPACE), 
        Math('-','+','|'), 
        BoxLight('─','┼','│'), 
        BoxHeavy('━','╋','┃'),
        BoxDouble('═','╬','║');

        private Paint(char hline, char cross, char vline)
        {
            this.hline = hline;
            this.cross = cross;
            this.vline = vline;
        }
        
        final char hline;
        final char cross;
        final char vline;

    }
    
    
    final int cols;
    final int rows;
    private final String[][] cells;
    final boolean alignRight;
    private volatile String title;
    private final String[] colNames;
    private final String[] rowNames;
    
    private volatile Paint paint = Paint.Space;

    public Table(int rows, int cols, boolean alignRight)
    {
        this.rows = rows;
        this.cols = cols;
        this.cells = new String[rows][cols];
        this.alignRight = alignRight;
        this.colNames = new String[cols];
        this.rowNames = new String[rows];
    }
    public Table(int rows, int cols)
    {
        this(rows, cols, false);
    }

    public Table setBorder(Paint paint)
    {
        this.paint = paint!=null ?  paint : Paint.Space;
        return this;
    }
    
    private static int rowCount(String[] rows, String[][] cells)
    {
        int r1 = rows!=null ? rows.length : 0;
        int r2 = cells!=null ? cells.length : 0;
        return Math.max(r2, r1);
    }
    private static int colCount(String[] cols, String[][] cells)
    {
        int r1 = cols!=null ? cols.length : 0;
        int r2 = Utils.countColums(cells);
        return Math.max(r2, r1);
    }

    public Table(String[] rowNames, String[] colNames, String[][] cells, boolean alignRight)
    {
        this(rowCount(rowNames, cells), colCount(colNames, cells), alignRight);

        if(rowNames!=null)
        {
            for(int i=0;i<rowNames.length;i++)
            {
                this.setRowName(i, rowNames[i]);
            }
        }

        if(colNames!=null)
        {
            for(int i=0;i<colNames.length;i++)
            {
                this.setColName(i, colNames[i]);
            }
        }
        if(cells!=null)
        {
            for(int r=0;r<cells.length;r++)
            {
                if(cells[r]!=null)
                {
                    for(int c=0;c<cells[r].length;c++)
                    {
                        this.setCell(r, c, cells[r][c]);
                    }
                }
            }
        }
    }
    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTitle(String title)
    {
        return this.title;
    }
    
    public void setRowName(int r, String name)
    {
        this.rowNames[r] = name;
    }
    public void setColName(int c, String name)
    {
        this.colNames[c] = name;
    }
    
    public String getRowName(int r)
    {
        return this.rowNames[r];
    }
    public String getColName(int c)
    {
        return this.colNames[c];
    }
    
    public final void setCell(int r, int c, String value)
    {
        this.cells[r][c] = value;
    }

    @Override
    public String toString()
    {
        int tw = this.title!=null ? this.title.length() : 0;
        for (String name : this.rowNames)
        {
            tw = Math.max(tw, name!=null ? name.length(): 0);
        }

        boolean cn = this.title!=null;
        
        int[] cw = new int[this.cols];
        for (int c=0;c<this.cols;c++)
        {
            cn = cn || this.colNames[c]!=null;
            cw[c] = Math.max(cw[c], this.colNames[c]!=null ? this.colNames[c].length(): 0);
            for(int r=0;r<this.rows;r++)
            {
                String value = this.cells[r][c];
                if(value!=null)
                {
                    cw[c] = Math.max(cw[c], value.length());
                }
            }
        }
        
        StringBuilder sb = new StringBuilder();

        String hr = "";
                
        if(tw>0)
        {
            String s = Strings.fill(this.title!=null?this.title:"", ' ', tw);
            sb.append(s).append(paint.vline);
            hr += Strings.repeat(paint.hline, tw)+paint.cross;
        }
        for (int c=0;c<this.cols;c++)
        {
            hr += Strings.repeat(paint.hline, cw[c])+paint.cross;
        }

        if(cn)
        {
            for (int c=0;c<this.cols;c++)
            {
                String s = Strings.fill(this.colNames[c]!=null ? this.colNames[c] : "", ' ', cw[c], alignRight);
                sb.append(s).append(paint.vline);
            }
            sb.append('\n');
        }
        sb.append(hr).append('\n');
        
        for (int r=0;r<this.rows;r++)
        {
            if(tw>0)
            {
                String s = Strings.fill(this.rowNames[r]!=null?this.rowNames[r]:"", ' ', tw);
                sb.append(s).append(paint.vline);
            }

            for (int c=0;c<this.cols;c++)
            {
                String s = Strings.fill(this.cells[r][c]!=null ? this.cells[r][c] : "", ' ', cw[c], alignRight);
                sb.append(s).append(paint.vline);
            }
            sb.append('\n').append(hr).append('\n');
        }        
        return sb.toString();
    }
    
}
