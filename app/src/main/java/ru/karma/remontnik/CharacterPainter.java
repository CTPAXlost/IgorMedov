package ru.karma.remontnik;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

final class CharacterPainter {
    private CharacterPainter() {}

    private static final int OUTLINE = Color.rgb(22, 25, 31);
    private static final int SKIN = Color.rgb(232, 184, 154);
    private static final int SKIN_LIGHT = Color.rgb(249, 207, 178);
    private static final int HAIR = Color.rgb(45, 34, 31);
    private static final int SHIRT = Color.rgb(25, 28, 34);
    private static final int JEANS = Color.rgb(54, 67, 82);
    private static final int SHOE = Color.rgb(35, 39, 47);

    static void draw(Canvas c, Paint p, float x, float y, float scale, boolean facingRight,
                     float runPhase, boolean airborne, float hammerPhase, boolean victory) {
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(70, 0, 0, 0));
        c.drawOval(new RectF(x - 28 * scale, y - 6 * scale, x + 28 * scale, y + 7 * scale), p);

        c.save();
        c.translate(x, y);
        c.scale(facingRight ? scale : -scale, scale);

        float bob = airborne ? -3f : (float) Math.abs(Math.sin(runPhase)) * 2f;
        c.translate(0, bob);
        float legSwing = airborne ? 22f : (float) Math.sin(runPhase) * 25f;
        float armSwing = airborne ? -25f : -legSwing * .8f;

        // Back arm.
        drawArm(c, p, -13, -55, armSwing, false);
        // Back leg.
        drawLeg(c, p, -10, -28, -legSwing, false);

        // Torso shadow and shirt.
        p.setColor(OUTLINE);
        c.drawRoundRect(new RectF(-22, -70, 23, -26), 12, 12, p);
        p.setColor(SHIRT);
        c.drawRoundRect(new RectF(-19, -68, 20, -28), 10, 10, p);
        p.setColor(Color.rgb(43, 47, 55));
        c.drawRoundRect(new RectF(-15, -66, 15, -58), 5, 5, p);

        // Shirt logo.
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(6.8f);
        p.setColor(Color.WHITE);
        c.drawText("KAR", 0, -46, p);
        p.setColor(Color.rgb(204, 190, 40));
        c.drawText("MA", 8.8f, -46, p);

        // Belt.
        p.setColor(OUTLINE);
        c.drawRoundRect(new RectF(-21, -31, 22, -23), 3, 3, p);
        p.setColor(Color.rgb(104, 67, 42));
        c.drawRect(-20, -30, 21, -25, p);
        p.setColor(Color.rgb(211, 167, 79));
        c.drawRoundRect(new RectF(-4, -31, 5, -23), 2, 2, p);
        p.setColor(Color.rgb(77, 47, 31));
        c.drawRoundRect(new RectF(-28, -32, -18, -17), 3, 3, p);

        // Front leg.
        drawLeg(c, p, 10, -28, legSwing, true);

        // Neck.
        p.setColor(OUTLINE);
        c.drawRoundRect(new RectF(-8, -80, 9, -64), 5, 5, p);
        p.setColor(SKIN);
        c.drawRoundRect(new RectF(-6, -79, 7, -65), 4, 4, p);

        // Head outline and face.
        p.setColor(OUTLINE);
        c.drawOval(new RectF(-25, -113, 25, -72), p);
        p.setColor(SKIN);
        c.drawOval(new RectF(-22, -110, 22, -74), p);
        p.setColor(SKIN_LIGHT);
        c.drawOval(new RectF(-15, -105, 18, -78), p);

        // Ears.
        p.setColor(OUTLINE);
        c.drawOval(new RectF(-27, -99, -18, -84), p);
        c.drawOval(new RectF(18, -99, 27, -84), p);
        p.setColor(SKIN);
        c.drawOval(new RectF(-25, -97, -19, -86), p);
        c.drawOval(new RectF(19, -97, 25, -86), p);

        // Hair: close cropped, matching the reference.
        p.setColor(HAIR);
        Path hair = new Path();
        hair.moveTo(-21, -96);
        hair.cubicTo(-23, -111, -10, -117, 3, -115);
        hair.cubicTo(17, -115, 24, -106, 21, -96);
        hair.cubicTo(13, -103, 3, -104, -5, -103);
        hair.cubicTo(-12, -102, -17, -99, -21, -96);
        hair.close();
        c.drawPath(hair, p);
        p.setColor(Color.rgb(77, 57, 50));
        for (int i = 0; i < 7; i++) {
            float hx = -15 + i * 5f;
            c.drawLine(hx, -108, hx + 4, -103, p);
        }

        // Brows.
        p.setColor(Color.rgb(65, 47, 42));
        p.setStrokeWidth(2.2f);
        c.drawLine(-14, -95, -5, -97, p);
        c.drawLine(5, -97, 14, -95, p);

        // Eyes.
        p.setColor(Color.WHITE);
        c.drawOval(new RectF(-14, -94, -4, -86), p);
        c.drawOval(new RectF(4, -94, 14, -86), p);
        p.setColor(Color.rgb(91, 104, 102));
        c.drawCircle(-8.5f, -90, 2.8f, p);
        c.drawCircle(8.5f, -90, 2.8f, p);
        p.setColor(Color.rgb(20, 24, 27));
        c.drawCircle(-8.1f, -90, 1.35f, p);
        c.drawCircle(8.9f, -90, 1.35f, p);
        p.setColor(Color.WHITE);
        c.drawCircle(-7.5f, -91, .65f, p);
        c.drawCircle(9.5f, -91, .65f, p);

        // Nose and mouth.
        p.setColor(Color.rgb(188, 133, 110));
        p.setStrokeWidth(1.4f);
        c.drawLine(1, -90, -1, -84, p);
        c.drawLine(-1, -84, 3, -84, p);
        p.setColor(Color.rgb(130, 70, 66));
        p.setStrokeWidth(1.8f);
        RectF mouth = new RectF(-6, -83, 7, -76);
        c.drawArc(mouth, 15, 150, false, p);
        if (victory) {
            p.setColor(Color.WHITE);
            c.drawArc(new RectF(-6, -82, 7, -76), 15, 150, false, p);
        }

        // Front arm / hammer action.
        if (hammerPhase > 0f) {
            float angle = -100f + Math.min(1f, hammerPhase) * 125f;
            drawHammerArm(c, p, 14, -59, angle);
        } else if (victory) {
            drawHammerArm(c, p, 14, -60, -105f);
        } else {
            drawArm(c, p, 14, -57, -armSwing, true);
        }

        c.restore();
    }

    private static void drawArm(Canvas c, Paint p, float x, float y, float angle, boolean front) {
        c.save();
        c.translate(x, y);
        c.rotate(angle);
        p.setColor(OUTLINE);
        c.drawRoundRect(new RectF(-6, -4, 7, 30), 6, 6, p);
        p.setColor(SHIRT);
        c.drawRoundRect(new RectF(-4, -3, 5, 14), 4, 4, p);
        p.setColor(SKIN);
        c.drawRoundRect(new RectF(-4, 10, 5, 29), 4, 4, p);
        p.setColor(OUTLINE);
        c.drawCircle(.5f, 30, 6, p);
        p.setColor(SKIN_LIGHT);
        c.drawCircle(.5f, 29, 4.2f, p);
        if (front) {
            p.setColor(Color.argb(55, 255, 255, 255));
            c.drawLine(-2, 13, -2, 24, p);
        }
        c.restore();
    }

    private static void drawHammerArm(Canvas c, Paint p, float x, float y, float angle) {
        c.save();
        c.translate(x, y);
        c.rotate(angle);
        p.setColor(OUTLINE);
        c.drawRoundRect(new RectF(-6, -3, 7, 30), 6, 6, p);
        p.setColor(SHIRT);
        c.drawRoundRect(new RectF(-4, -2, 5, 13), 4, 4, p);
        p.setColor(SKIN);
        c.drawRoundRect(new RectF(-4, 10, 5, 29), 4, 4, p);
        p.setColor(Color.rgb(112, 72, 43));
        c.drawRoundRect(new RectF(-2, 24, 3, 62), 2, 2, p);
        p.setColor(OUTLINE);
        c.drawRoundRect(new RectF(-14, 55, 16, 68), 4, 4, p);
        p.setColor(Color.rgb(190, 198, 208));
        c.drawRoundRect(new RectF(-12, 57, 14, 66), 3, 3, p);
        p.setColor(Color.rgb(237, 241, 245));
        c.drawRoundRect(new RectF(-9, 58, 8, 61), 2, 2, p);
        p.setColor(OUTLINE);
        c.drawCircle(.5f, 29, 6, p);
        p.setColor(SKIN_LIGHT);
        c.drawCircle(.5f, 29, 4.2f, p);
        c.restore();
    }

    private static void drawLeg(Canvas c, Paint p, float x, float y, float angle, boolean front) {
        c.save();
        c.translate(x, y);
        c.rotate(angle);
        p.setColor(OUTLINE);
        c.drawRoundRect(new RectF(-9, -3, 10, 34), 7, 7, p);
        p.setColor(JEANS);
        c.drawRoundRect(new RectF(-7, -2, 8, 31), 6, 6, p);
        p.setColor(Color.rgb(71, 87, 104));
        c.drawRoundRect(new RectF(-5, 1, 1, 26), 3, 3, p);
        p.setColor(OUTLINE);
        c.drawRoundRect(new RectF(-9, 27, 16, 40), 6, 6, p);
        p.setColor(SHOE);
        c.drawRoundRect(new RectF(-7, 29, 14, 38), 5, 5, p);
        p.setColor(Color.rgb(220, 224, 228));
        c.drawRoundRect(new RectF(-5, 35, 15, 39), 2, 2, p);
        p.setColor(Color.rgb(191, 201, 50));
        c.drawCircle(6, 32, 1.5f, p);
        if (front) {
            p.setColor(Color.rgb(170, 177, 184));
            p.setStrokeWidth(1.1f);
            c.drawLine(-3, 31, 8, 34, p);
            c.drawLine(-1, 29, 10, 32, p);
        }
        c.restore();
    }
}
