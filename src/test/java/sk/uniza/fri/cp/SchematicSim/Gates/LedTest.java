package sk.uniza.fri.cp.SchematicSim.Gates;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class LedTest {

    private static final long HOLD = 100L;

    @Test
    public void shouldKeepLedLitFor100msAfterShortHighPulse() throws Exception {
        Led led = new Led();
        long t0 = System.currentTimeMillis();
        applyInput(led, true, t0);
        Assert.assertTrue("LED should be lit while input is HIGH.", lit(led));

        applyInput(led, false, t0 + 30L);
        Assert.assertTrue("LED should stay lit after a short HIGH pulse (< 100 ms).", lit(led));

        applyInput(led, false, t0 + 99L);
        Assert.assertTrue("LED should still be lit before the 100 ms minimum elapses.", lit(led));

        applyInput(led, false, t0 + HOLD);
        Assert.assertFalse("LED should turn off once the 100 ms minimum is reached.", lit(led));
    }

    @Test
    public void shouldTurnOffImmediatelyWhenAlreadyLitFor100ms() throws Exception {
        Led led = new Led();
        long t0 = System.currentTimeMillis();
        applyInput(led, true, t0);
        Assert.assertTrue(lit(led));

        applyInput(led, false, t0 + HOLD);
        Assert.assertFalse("LED lit for >= 100 ms should turn off immediately on falling edge.", lit(led));
    }

    @Test
    public void repeatedHighShouldRestartTheCounting() throws Exception {
        Led led = new Led();
        long t0 = System.currentTimeMillis();

        applyInput(led, true, t0);
        applyInput(led, false, t0 + 30L);
        Assert.assertTrue("LED lit by first pulse before hold expires.", lit(led));

        applyInput(led, true, t0 + 60L);
        applyInput(led, false, t0 + 90L);
        Assert.assertTrue("After repeated HIGH the 100 ms window restarts.", lit(led));

        applyInput(led, false, t0 + 90L + HOLD);
        Assert.assertFalse("LED should turn off once the restarted 100 ms window expires.", lit(led));
    }

    private static boolean lit(Led led) throws Exception {
        Field onField = Led.class.getDeclaredField("on");
        onField.setAccessible(true);
        return onField.getBoolean(led);
    }

    private static void applyInput(Led led, boolean high, long now) throws Exception {
        Method applyInputState = Led.class.getDeclaredMethod("applyInputState", boolean.class, long.class);
        applyInputState.setAccessible(true);
        applyInputState.invoke(led, high, now);
    }
}