/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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
    
    double getDone();
    
    /**
     * Gets the current value
     * @return 
     */
    int getVal();
    
    /**
     * Gets the maximum value
     * @return 
     */
    int getMax();
    
    /**
     * Sets the current value
     * @param n 
     */
    void setVal(int n);
    
    /**
     * Sets the maximun value
     * @param n 
     */
    void setMax(int n);

    void step();
    void step(int n);
    void setShow(boolean showPrev, boolean showNext, boolean showFull);
}
    
