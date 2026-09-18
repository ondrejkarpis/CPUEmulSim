package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.application.Platform;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SevenSegmentDisplayTest {

    @Test
    public void shouldUpdateSegmentStateImmediatelyWhenInputChanges() throws Exception {
        SevenSegmentDisplay display = new SevenSegmentDisplay();

        Shape[] segmentShapes = new Shape[8];
        for (int i = 0; i < segmentShapes.length; i++) {
            segmentShapes[i] = new Rectangle();
        }
        Field segmentShapesField = SevenSegmentDisplay.class.getDeclaredField("segmentShapes");
        segmentShapesField.setAccessible(true);
        segmentShapesField.set(display, segmentShapes);

        boolean[] litSegments = new boolean[8];
        Field litSegmentsField = SevenSegmentDisplay.class.getDeclaredField("litSegments");
        litSegmentsField.setAccessible(true);
        litSegmentsField.set(display, litSegments);

        Field holdSnapshotField = SevenSegmentDisplay.class.getDeclaredField("holdSnapshot");
        holdSnapshotField.setAccessible(true);
        holdSnapshotField.set(display, new boolean[8]);

        Field caHoldActiveField = SevenSegmentDisplay.class.getDeclaredField("caHoldActive");
        caHoldActiveField.setAccessible(true);
        caHoldActiveField.setBoolean(display, false);

        Field caHoldDeadlineMsField = SevenSegmentDisplay.class.getDeclaredField("caHoldDeadlineMs");
        caHoldDeadlineMsField.setAccessible(true);
        caHoldDeadlineMsField.setLong(display, 0L);

        Field visualUpdateScheduledField = SevenSegmentDisplay.class.getDeclaredField("visualUpdateScheduled");
        visualUpdateScheduledField.setAccessible(true);
        visualUpdateScheduledField.setBoolean(display, true);

        Method applyLitState = SevenSegmentDisplay.class.getDeclaredMethod("applyLitState", boolean[].class, boolean.class);
        applyLitState.setAccessible(true);

        boolean[] enabledSegments = new boolean[8];
        enabledSegments[0] = true;
        applyLitState.invoke(display, enabledSegments, false);
        Assert.assertTrue("Segment should turn on immediately when enabled.", ((boolean[]) litSegmentsField.get(display))[0]);

        boolean[] disabledSegments = new boolean[8];
        applyLitState.invoke(display, disabledSegments, false);
        Assert.assertFalse("Segment should turn off immediately when the signal changes; there must be no forced delay.", ((boolean[]) litSegmentsField.get(display))[0]);

        Assert.assertFalse("CA hold should not remain active after a normal input change.", caHoldActiveField.getBoolean(display));
    }
}
