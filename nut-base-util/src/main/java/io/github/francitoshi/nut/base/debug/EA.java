/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.debug;

/**
 *
 * @author franci
 */
public class EA
{
    private enum Holder
    {
        INSTANCE;
        
        final boolean enabled = active();
        
        final boolean nop = false;
        
        private boolean active()
        {
            try
            {
                assert nop;
                return false;
            }
            catch(AssertionError ae)
            {
                return true;
            }
        }
    }
    
    public static boolean enabled()
    {
        return Holder.INSTANCE.enabled;
    }
    
}
