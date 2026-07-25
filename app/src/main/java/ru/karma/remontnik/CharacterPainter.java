package ru.karma.remontnik;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/**
 * Programmatically drawn cartoon character for the game.
 * The face is intentionally built from vector shapes so it stays crisp on all screens.
 */
final class CharacterPainter {
    private CharacterPainter() {}

    private static final int OUTLINE = Color.rgb(24, 25, 29);
    private static final int SKIN = Color.rgb(224, 169, 133);
    private static final int SKIN_LIGHT = Color.rgb(247, 202, 169);
    private static final int SKIN_SHADOW = Color.rgb(190, 126, 101);
    private static final int HAIR = Color.rgb(43, 32, 29);
    private static final int HAIR_LIGHT = Color.rgb(76, 54, 46);
    private static final int SHIRT = Color.rgb(23, 25, 29);
    private static final int JEANS = Color.rgb(47, 57, 68);
    private static final int SHOE = Color.rgb(31, 34, 40);

    static void draw(Canvas c, Paint p, float x, float y, float scale, boolean facingRight,
                     float runPhase, boolean airborne, float hammerPhase, boolean victory) {
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(75, 0, 0, 0));
        c.drawOval(new RectF(x - 30 * scale, y - 6 * scale, x + 30 * scale, y + 7 * scale), p);

        c.save();
        c.translate(x, y);
        c.scale(facingRight ? scale : -scale, scale);

        float bob = airborne ? -3f : (float) Math.abs(Math.sin(runPhase)) * 2f;
        c.translate(0, bob);
        float legSwing = airborne ? 23f : (float) Math.sin(runPhase) * 25f;
        float armSwing = airborne ? -25f : -legSwing * .8f;

        drawArm(c, p, -13, -56, armSwing, false);
        drawLeg(c, p, -10, -29, -legSwing, false);

        // Torso and shoulders.
        p.setColor(OUTLINE);
        c.drawRoundRect(new RectF(-23, -71, 24, -25), 12, 12, p);
        p.setColor(SHIRT);
        c.drawRoundRect(new RectF(-20, -69, 21, -28), 10, 10, p);
        p.setColor(Color.rgb(47, 50, 56));
        c.drawRoundRect(new RectF(-13, -68, 14, -61), 4, 4, p);
        p.setColor(Color.argb(34, 255, 255, 255));
        c.drawRoundRect(new RectF(-17, -66, -12, -34), 2, 2, p);

        // Exact shirt inscription requested by the user.
        p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(8.2f);
        p.setColor(Color.rgb(244, 244, 239));
        c.drawText("K", -8.2f, -45.5f, p);
        p.setColor(Color.rgb(190, 205, 61));
        c.drawText("arma", 4.7f, -45.5f, p);

        // Belt and tool pouch.
        p.setColor(OUTLINE);
        c.drawRoundRect(new RectF(-22, -31, 23, -22), 3, 3, p);
        p.setColor(Color.rgb(98, 62, 39));
        c.drawRect(-21, -30, 22, -25, p);
        p.setColor(Color.rgb(210, 166, 78));
        c.drawRoundRect(new RectF(-4, -31, 5, -22), 2, 2, p);
        p.setColor(Color.rgb(75, 45, 29));
        c.drawRoundRect(new RectF(-29, -33, -18, -16), 3, 3, p);
        p.setColor(Color.rgb(143, 88, 46));
        c.drawLine(-26, -28, -21, -20, p);

        drawLeg(c, p, 10, -29, legSwing, true);

        // Neck.
        p.setColor(OUTLINE);
        c.drawRoundRect(new RectF(-9, -82, 10, -64), 6, 6, p);
        p.setColor(SKIN);
        c.drawRoundRect(new RectF(-7, -81, 8, -65), 5, 5, p);
        p.setColor(SKIN_SHADOW);
        c.drawRoundRect(new RectF(-7, -79, -2, -66), 3, 3, p);

        drawRecognizableFace(c, p, victory);

        // Front arm / hammer action.
        if (hammerPhase > 0f) {
            float angle = -102f + Math.min(1f, hammerPhase) * 128f;
            drawHammerArm(c, p, 14, -60, angle);
        } else if (victory) {
            drawHammerArm(c, p, 14, -61, -108f);
        } else {
            drawArm(c, p, 14, -58, -armSwing, true);
        }

        c.restore();
    }

    private static void drawRecognizableFace(Canvas c, Paint p, boolean victory) {
        // Ears sit slightly low, matching the reference proportions.
        p.setColor(OUTLINE);
        c.drawOval(new RectF(-28, -101, -18, -84), p);
        c.drawOval(new RectF(18, -101, 28, -84), p);
        p.setColor(SKIN);
        c.drawOval(new RectF(-26, -99, -19, -86), p);
        c.drawOval(new RectF(19, -99, 26, -86), p);
        p.setColor(SKIN_SHADOW);
        p.setStrokeWidth(1.2f);
        p.setStyle(Paint.Style.STROKE);
        c.drawArc(new RectF(-25, -96, -20, -88), 85, 190, false, p);
        c.drawArc(new RectF(20, -96, 25, -88), -95, 190, false, p);
        p.setStyle(Paint.Style.FILL);

        // Longer oval face with defined cheek and jaw structure.
        p.setColor(OUTLINE);
        Path outline = new Path();
        outline.moveTo(-22, -105);
        outline.cubicTo(-18, -116, -7, -121, 2, -120);
        outline.cubicTo(15, -119, 24, -112, 24, -100);
        outline.cubicTo(25, -91, 20, -78, 12, -72);
        outline.cubicTo(5, -67, -5, -67, -13, -73);
        outline.cubicTo(-21, -80, -25, -94, -22, -105);
        outline.close();
        c.drawPath(outline, p);

        p.setColor(SKIN);
        Path face = new Path();
        face.moveTo(-20, -104);
        face.cubicTo(-16, -113, -6, -118, 2, -117);
        face.cubicTo(13, -116, 21, -109, 21, -100);
        face.cubicTo(22, -92, 18, -81, 10, -75);
        face.cubicTo(4, -70, -4, -70, -11, -75);
        face.cubicTo(-18, -81, -22, -94, -20, -104);
        face.close();
        c.drawPath(face, p);

        // Forehead and right cheek light plane.
        p.setColor(SKIN_LIGHT);
        Path light = new Path();
        light.moveTo(-7, -113);
        light.cubicTo(4, -116, 14, -111, 17, -102);
        light.cubicTo(19, -94, 15, -83, 9, -78);
        light.cubicTo(5, -75, 1, -74, -2, -75);
        light.cubicTo(5, -85, 5, -99, -7, -113);
        light.close();
        c.drawPath(light, p);

        // Cheekbone and jaw shadows add recognizable facial structure.
        p.setColor(Color.argb(78, 150, 83, 67));
        Path cheek = new Path();
        cheek.moveTo(-17, -90);
        cheek.cubicTo(-13, -87, -10, -84, -9, -78);
        cheek.cubicTo(-14, -80, -18, -84, -19, -90);
        cheek.close();
        c.drawPath(cheek, p);
        Path jaw = new Path();
        jaw.moveTo(11, -77);
        jaw.cubicTo(4, -72, -5, -72, -11, -76);
        jaw.cubicTo(-5, -68, 5, -67, 12, -73);
        jaw.close();
        c.drawPath(jaw, p);

        // Close-cropped hair and fade.
        p.setColor(HAIR);
        Path hair = new Path();
        hair.moveTo(-20, -103);
        hair.cubicTo(-20, -116, -9, -122, 3, -120);
        hair.cubicTo(16, -120, 23, -111, 21, -101);
        hair.cubicTo(14, -105, 6, -107, -2, -106);
        hair.cubicTo(-10, -106, -16, -105, -20, -103);
        hair.close();
        c.drawPath(hair, p);
        p.setColor(Color.argb(135, 30, 24, 23));
        c.drawRoundRect(new RectF(-21, -105, -16, -92), 2, 2, p);
        c.drawRoundRect(new RectF(17, -106, 22, -93), 2, 2, p);
        p.setColor(HAIR_LIGHT);
        p.setStrokeWidth(1.15f);
        for (int i = 0; i < 9; i++) {
            float hx = -15 + i * 3.8f;
            c.drawLine(hx, -116 + (i % 2), hx + 3.5f, -109 + (i % 3), p);
        }

        // Straight brows.
        p.setColor(Color.rgb(61, 43, 38));
        p.setStrokeWidth(2.3f);
        c.drawLine(-15.2f, -98.1f, -5.4f, -99.6f, p);
        c.drawLine(4.3f, -99.4f, 14.8f, -98.0f, p);
        p.setColor(Color.argb(70, 110, 58, 48));
        p.setStrokeWidth(1.1f);
        c.drawLine(-14, -95.5f, -6, -96.2f, p);
        c.drawLine(5, -96.2f, 13.5f, -95.2f, p);

        // Eyes with upper lids; not oversized/chibi.
        p.setColor(Color.rgb(248, 245, 239));
        c.drawOval(new RectF(-14.2f, -95.8f, -4.1f, -88.4f), p);
        c.drawOval(new RectF(4.1f, -95.8f, 14.3f, -88.4f), p);
        p.setColor(Color.rgb(89, 86, 77));
        c.drawCircle(-8.6f, -92.1f, 2.7f, p);
        c.drawCircle(8.5f, -92.1f, 2.7f, p);
        p.setColor(Color.rgb(18, 20, 22));
        c.drawCircle(-8.3f, -92.1f, 1.35f, p);
        c.drawCircle(8.8f, -92.1f, 1.35f, p);
        p.setColor(Color.WHITE);
        c.drawCircle(-7.7f, -93, .62f, p);
        c.drawCircle(9.4f, -93, .62f, p);
        p.setColor(Color.rgb(57, 42, 38));
        p.setStrokeWidth(1.4f);
        c.drawArc(new RectF(-15, -98, -3, -88), 195, 150, false, p);
        c.drawArc(new RectF(3, -98, 15, -88), 195, 150, false, p);

        // Straight, compact nose with a defined bridge and nostrils.
        p.setColor(SKIN_SHADOW);
        p.setStrokeWidth(1.35f);
        c.drawLine(.2f, -93, -.8f, -85.3f, p);
        c.drawLine(-.8f, -85.3f, -3.2f, -82.8f, p);
        c.drawLine(-3.2f, -82.8f, 1.8f, -82.1f, p);
        p.setColor(Color.rgb(154, 91, 75));
        c.drawCircle(-2.1f, -82.4f, .75f, p);
        c.drawCircle(2.1f, -82.5f, .7f, p);

        // Subtle philtrum, lips and chin.
        p.setColor(Color.argb(75, 137, 77, 66));
        p.setStrokeWidth(.9f);
        c.drawLine(0, -80.5f, 0, -78.5f, p);
        p.setColor(Color.rgb(132, 69, 67));
        p.setStrokeWidth(1.55f);
        if (victory) {
            c.drawArc(new RectF(-6.5f, -80.8f, 7.2f, -73.8f), 5, 170, false, p);
            p.setColor(Color.WHITE);
            c.drawArc(new RectF(-5.5f, -79.7f, 6.2f, -75f), 5, 170, false, p);
        } else {
            c.drawLine(-5.9f, -77.6f, 5.8f, -77.3f, p);
            p.setColor(Color.rgb(174, 103, 94));
            c.drawArc(new RectF(-5.2f, -79.5f, 5.8f, -74.2f), 20, 140, false, p);
        }
        p.setColor(Color.argb(55, 125, 67, 59));
        p.setStrokeWidth(1.05f);
        c.drawArc(new RectF(-4.5f, -75.5f, 5.2f, -71.2f), 10, 160, false, p);
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
        p.setColor(Color.rgb(104, 67, 42));
        c.drawRoundRect(new RectF(-2, 24, 3, 62), 2, 2, p);
        p.setColor(OUTLINE);
        c.drawRoundRect(new RectF(-14, 55, 16, 68), 4, 4, p);
        p.setColor(Color.rgb(188, 197, 208));
        c.drawRoundRect(new RectF(-12, 57, 14, 66), 3, 3, p);
        p.setColor(Color.rgb(238, 242, 246));
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
        p.setColor(Color.rgb(67, 79, 92));
        c.drawRoundRect(new RectF(-5, 1, 1, 26), 3, 3, p);
        p.setColor(OUTLINE);
        c.drawRoundRect(new RectF(-9, 27, 16, 40), 6, 6, p);
        p.setColor(SHOE);
        c.drawRoundRect(new RectF(-7, 29, 14, 38), 5, 5, p);
        p.setColor(Color.rgb(220, 224, 228));
        c.drawRoundRect(new RectF(-5, 35, 15, 39), 2, 2, p);
        p.setColor(Color.rgb(190, 203, 55));
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
