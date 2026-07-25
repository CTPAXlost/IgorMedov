package ru.karma.remontnik;

import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

final class LevelData {
    final int number;
    final int theme;
    final float worldWidth;
    final List<Platform> platforms = new ArrayList<>();
    final List<Coin> coins = new ArrayList<>();
    final List<Enemy> enemies = new ArrayList<>();
    final List<Hazard> hazards = new ArrayList<>();
    final List<Crate> crates = new ArrayList<>();
    final List<Float> checkpoints = new ArrayList<>();
    float finishX;

    LevelData(int number, int theme, float worldWidth) {
        this.number = number;
        this.theme = theme;
        this.worldWidth = worldWidth;
        this.finishX = worldWidth - 180f;
    }

    static final class Platform {
        final RectF rect;
        final int style;
        Platform(float x, float y, float w, float h, int style) {
            rect = new RectF(x, y, x + w, y + h);
            this.style = style;
        }
    }

    static final class Coin {
        final float x, y;
        boolean taken;
        Coin(float x, float y) { this.x = x; this.y = y; }
    }

    static final class Enemy {
        float x, y, minX, maxX, speed;
        int dir = 1;
        boolean alive = true;
        int type;
        Enemy(float x, float y, float minX, float maxX, float speed, int type) {
            this.x = x; this.y = y; this.minX = minX; this.maxX = maxX;
            this.speed = speed; this.type = type;
        }
    }

    static final class Hazard {
        final RectF rect;
        final int type;
        Hazard(float x, float y, float w, float h, int type) {
            rect = new RectF(x, y, x + w, y + h);
            this.type = type;
        }
    }

    static final class Crate {
        final RectF rect;
        boolean broken;
        Crate(float x, float y, float w, float h) {
            rect = new RectF(x, y, x + w, y + h);
        }
    }

    static LevelData create(int level) {
        if (level == 2) return createLevel2();
        if (level == 3) return createLevel3();
        return createLevel1();
    }

    private static void ground(LevelData d, float x, float w) {
        d.platforms.add(new Platform(x, 620, w, 120, 0));
    }

    private static void platform(LevelData d, float x, float y, float w) {
        d.platforms.add(new Platform(x, y, w, 34, 1));
    }

    private static void coins(LevelData d, float startX, float y, int count, float step) {
        for (int i = 0; i < count; i++) d.coins.add(new Coin(startX + i * step, y));
    }

    private static LevelData createLevel1() {
        LevelData d = new LevelData(1, 0, 3700);
        ground(d, 0, 920); ground(d, 1040, 900); ground(d, 2070, 780); ground(d, 2980, 720);
        platform(d, 280, 500, 220); platform(d, 610, 420, 190);
        platform(d, 900, 530, 160); platform(d, 1130, 470, 230); platform(d, 1460, 385, 200);
        platform(d, 1810, 505, 190); platform(d, 2040, 450, 180); platform(d, 2350, 355, 220);
        platform(d, 2720, 500, 190); platform(d, 2960, 440, 220); platform(d, 3320, 370, 200);
        coins(d, 310, 455, 4, 55); coins(d, 630, 375, 3, 55); coins(d, 930, 485, 3, 55);
        coins(d, 1150, 425, 4, 55); coins(d, 1480, 340, 3, 55); coins(d, 2070, 405, 4, 55);
        coins(d, 2370, 310, 4, 55); coins(d, 2990, 395, 4, 55); coins(d, 3340, 325, 3, 55);
        d.enemies.add(new Enemy(520, 620, 430, 800, 82, 0));
        d.enemies.add(new Enemy(1320, 620, 1120, 1780, 90, 0));
        d.enemies.add(new Enemy(2240, 620, 2110, 2760, 98, 1));
        d.enemies.add(new Enemy(3200, 620, 3030, 3520, 105, 0));
        d.hazards.add(new Hazard(820, 620, 75, 30, 0));
        d.hazards.add(new Hazard(1660, 620, 90, 30, 0));
        d.hazards.add(new Hazard(2600, 620, 100, 30, 0));
        d.crates.add(new Crate(760, 550, 70, 70));
        d.crates.add(new Crate(1725, 550, 70, 70));
        d.crates.add(new Crate(2780, 550, 70, 70));
        d.checkpoints.add(1250f); d.checkpoints.add(2500f);
        return d;
    }

    private static LevelData createLevel2() {
        LevelData d = new LevelData(2, 1, 4500);
        ground(d, 0, 760); ground(d, 900, 700); ground(d, 1730, 850); ground(d, 2710, 620); ground(d, 3480, 1020);
        platform(d, 210, 490, 170); platform(d, 480, 400, 210); platform(d, 740, 520, 170);
        platform(d, 980, 455, 210); platform(d, 1300, 355, 210); platform(d, 1600, 500, 180);
        platform(d, 1900, 430, 200); platform(d, 2230, 330, 220); platform(d, 2530, 500, 190);
        platform(d, 2800, 420, 200); platform(d, 3140, 320, 210); platform(d, 3420, 500, 160);
        platform(d, 3700, 410, 210); platform(d, 4040, 320, 220);
        coins(d, 230, 445, 3, 55); coins(d, 500, 355, 4, 55); coins(d, 990, 410, 4, 55);
        coins(d, 1320, 310, 4, 55); coins(d, 1920, 385, 4, 55); coins(d, 2250, 285, 4, 55);
        coins(d, 2810, 375, 4, 55); coins(d, 3160, 275, 4, 55); coins(d, 3710, 365, 4, 55); coins(d, 4060, 275, 4, 55);
        d.enemies.add(new Enemy(560, 620, 390, 700, 105, 1));
        d.enemies.add(new Enemy(1050, 620, 940, 1480, 110, 2));
        d.enemies.add(new Enemy(2040, 620, 1790, 2440, 115, 1));
        d.enemies.add(new Enemy(2860, 620, 2750, 3250, 120, 2));
        d.enemies.add(new Enemy(3650, 620, 3520, 4200, 128, 1));
        d.hazards.add(new Hazard(690, 585, 70, 35, 1));
        d.hazards.add(new Hazard(1450, 585, 120, 35, 1));
        d.hazards.add(new Hazard(2400, 585, 120, 35, 1));
        d.hazards.add(new Hazard(3200, 585, 120, 35, 1));
        d.hazards.add(new Hazard(3900, 585, 120, 35, 1));
        d.crates.add(new Crate(690, 550, 70, 70));
        d.crates.add(new Crate(1510, 550, 70, 70));
        d.crates.add(new Crate(2490, 550, 70, 70));
        d.crates.add(new Crate(3310, 550, 70, 70));
        d.checkpoints.add(1500f); d.checkpoints.add(3000f);
        return d;
    }

    private static LevelData createLevel3() {
        LevelData d = new LevelData(3, 2, 5200);
        ground(d, 0, 690); ground(d, 840, 650); ground(d, 1640, 650); ground(d, 2470, 590);
        ground(d, 3210, 760); ground(d, 4130, 1070);
        platform(d, 180, 495, 180); platform(d, 440, 390, 180); platform(d, 690, 510, 170);
        platform(d, 920, 435, 180); platform(d, 1190, 325, 210); platform(d, 1480, 500, 170);
        platform(d, 1740, 410, 210); platform(d, 2050, 305, 210); platform(d, 2310, 495, 170);
        platform(d, 2590, 400, 190); platform(d, 2860, 290, 210); platform(d, 3070, 505, 170);
        platform(d, 3340, 430, 210); platform(d, 3670, 320, 220); platform(d, 3970, 505, 180);
        platform(d, 4260, 410, 200); platform(d, 4590, 300, 220); platform(d, 4900, 470, 180);
        coins(d, 195, 450, 3, 55); coins(d, 455, 345, 3, 55); coins(d, 930, 390, 3, 55);
        coins(d, 1210, 280, 4, 55); coins(d, 1750, 365, 4, 55); coins(d, 2070, 260, 4, 55);
        coins(d, 2600, 355, 4, 55); coins(d, 2880, 245, 4, 55); coins(d, 3360, 385, 4, 55);
        coins(d, 3690, 275, 4, 55); coins(d, 4280, 365, 4, 55); coins(d, 4610, 255, 4, 55); coins(d, 4920, 425, 3, 55);
        d.enemies.add(new Enemy(500, 620, 370, 650, 120, 2));
        d.enemies.add(new Enemy(1020, 620, 890, 1420, 125, 1));
        d.enemies.add(new Enemy(1840, 620, 1680, 2220, 132, 2));
        d.enemies.add(new Enemy(2640, 620, 2510, 3000, 140, 1));
        d.enemies.add(new Enemy(3440, 620, 3260, 3900, 145, 2));
        d.enemies.add(new Enemy(4370, 620, 4180, 5000, 155, 1));
        d.hazards.add(new Hazard(610, 580, 80, 40, 0));
        d.hazards.add(new Hazard(1360, 580, 110, 40, 0));
        d.hazards.add(new Hazard(2170, 580, 100, 40, 0));
        d.hazards.add(new Hazard(2940, 580, 110, 40, 0));
        d.hazards.add(new Hazard(3820, 580, 130, 40, 0));
        d.hazards.add(new Hazard(4750, 580, 120, 40, 0));
        d.crates.add(new Crate(620, 550, 70, 70));
        d.crates.add(new Crate(1430, 550, 70, 70));
        d.crates.add(new Crate(2240, 550, 70, 70));
        d.crates.add(new Crate(3010, 550, 70, 70));
        d.crates.add(new Crate(3910, 550, 70, 70));
        d.checkpoints.add(1700f); d.checkpoints.add(3500f);
        return d;
    }
}
