/*
 * GaugeProgress.java
 *
 * Copyright (c) 2012-2025 francitoshi@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.gauge;

public interface GaugeProgress
{
    boolean isStarted();
    boolean isPaused();
    void pause();
    void resume();
    void start();
    void start(int max);
    void start(int max, String prefix);
    void close();
    void setPrefix(String prefix);
    String getPrefix();
    
    /**
     * 
     * @return 
     */

    double getDone();
    /**
     * Gets the current value
     * <p>
     * Obtiene el valor actual
     * @return 
     */

    int getVal();
    /**
     * Gets the minimun value
     * <p>
     * Obtiene el valor mínimo
     * @return 
     */


    int getMax();
    /**
     * Sets the current value
     * <p>
     * Establece el valor actual
     * @param n 
     */

    void setVal(int n);
    /**
     * Sets the minimun value
     * <p>
     * Establece el valor mínimo
     * @param n 
     */

    void setMax(int n);
    void step();
    void step(int n);
    void setShow(boolean showPrev, boolean showNext, boolean showFull);
}
    
