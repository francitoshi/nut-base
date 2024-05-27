/*
 * Table.java
 *
 * Copyright (c) 2024 francitoshi@gmail.com
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

/**
 *
 * @author franci
 */
public class Table
{
    
    private final int cols;
    private final int rows;
    private final String[][] cells;
    private final boolean alignRight;
    private volatile String title;
    private final String[] colNames;
    private final String[] rowNames;

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
    
    public void setCell(int r, int c, String value)
    {
        this.cells[r][c] = value;
    }

    static final char SEP = '|';
    
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
            sb.append(s).append(SEP);
            hr += Strings.repeat('-', tw)+'+';
        }
        for (int c=0;c<this.cols;c++)
        {
            hr += Strings.repeat('-', cw[c])+'+';
        }

        if(cn)
        {
            for (int c=0;c<this.cols;c++)
            {
                String s = Strings.fill(this.colNames[c]!=null ? this.colNames[c] : "", ' ', cw[c], alignRight);
                sb.append(s).append(SEP);
            }
            sb.append('\n');
        }
        sb.append(hr).append('\n');
        
        for (int r=0;r<this.rows;r++)
        {
            if(tw>0)
            {
                String s = Strings.fill(this.rowNames[r]!=null?this.rowNames[r]:"", ' ', tw);
                sb.append(s).append(SEP);
            }

            for (int c=0;c<this.cols;c++)
            {
                String s = Strings.fill(this.cells[r][c]!=null ? this.cells[r][c] : "", ' ', cw[c], alignRight);
                sb.append(s).append(SEP);
            }
            sb.append('\n').append(hr).append('\n');
        }        
        return sb.toString();
    }
    
}
