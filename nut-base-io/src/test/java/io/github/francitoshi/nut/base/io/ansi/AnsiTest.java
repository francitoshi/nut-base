package io.github.francitoshi.nut.base.io.ansi;

import io.github.francitoshi.nut.base.io.ansi.Ansi;
import io.github.francitoshi.nut.base.io.ansi.Ansi.Color;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class AnsiTest
{

    @Test
    public void testAnsi_0args()
    {
        Ansi result = Ansi.ansi().fg(Color.YELLOW).a("hola mundo").bg(Color.CYAN).a("hola mundo");

        System.out.println(result.toString());
    }
    
}
