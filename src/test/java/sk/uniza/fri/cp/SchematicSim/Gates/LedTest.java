package sk.uniza.fri.cp.SchematicSim.Gates;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class LedTest {

    @Test
    public void shouldKeepLedLitFor100msAfterInputFallsToLow() throws Exception {
        Led led = new Led();

        Field onField = Led.class.getDeclaredField("on");
        onField.setAccessible(true);
        onField.setBoolean(led, true);

        Field wasHighField = Led.class.getDeclaredField("wasHigh");
        wasHighField.setAccessible(true);
        wasHighField.setBoolean(led, true);

        Method applyInputState = Led.class.getDeclaredMethod("applyInputState", boolean.class, long.class);
        applyInputState.setAccessible(true);

        long now = System.currentTimeMillis();
        applyInputState.invoke(led, false, now);

        Assert.assertTrue("LED should remain lit immediately after the input falls to LOW.", onField.getBoolean(led));

        Field holdActiveField = Led.class.getDeclaredField("holdActive");
        holdActiveField.setAccessible(true);
        Assert.assertTrue("Hold timer should become active when the signal drops to LOW.", holdActiveField.getBoolean(led));

        Field holdDeadlineMsField = Led.class.getDeclaredField("holdDeadlineMs");
        holdDeadlineMsField.setAccessible(true);
        long deadline = holdDeadlineMsField.getLong(led);

        applyInputState.invoke(led, false, deadline + 1L);
        Assert.assertFalse("LED should turn off after the 100 ms hold expires.", onField.getBoolean(led));
    }
}
