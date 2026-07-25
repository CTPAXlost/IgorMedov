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
        switch (level) {
            case 2: return createLevel2();
            case 3: return createLevel3();
            case 4: return createLevel4();
            case 5: return createLevel5();
            default: return createLevel1();
        }
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
        d.hazards.add(new Hazard(820, 590, 75, 30, 0));
        d.hazards.add(new Hazard(1660, 590, 90, 30, 0));
        d.hazards.add(new Hazard(2600, 590, 100, 30, 0));
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

    /** Level 4: smaller safe zones, mixed fire/saw hazards and elevated enemies. */
    private static LevelData createLevel4() {
        LevelData d = new LevelData(4, 3, 5900);
        ground(d, 0, 650); ground(d, 820, 600); ground(d, 1570, 620); ground(d, 2360, 540);
        ground(d, 3060, 620); ground(d, 3850, 630); ground(d, 4660, 570); ground(d, 5410, 490);

        platform(d, 210, 490, 180); platform(d, 470, 390, 170); platform(d, 690, 510, 150);
        platform(d, 900, 430, 170); platform(d, 1180, 330, 190); platform(d, 1410, 500, 180);
        platform(d, 1680, 410, 160); platform(d, 1940, 300, 200); platform(d, 2200, 490, 180);
        platform(d, 2460, 390, 170); platform(d, 2730, 280, 200); platform(d, 2960, 500, 150);
        platform(d, 3160, 420, 180); platform(d, 3440, 310, 180); platform(d, 3710, 500, 170);
        platform(d, 3970, 390, 190); platform(d, 4260, 280, 180); platform(d, 4510, 500, 170);
        platform(d, 4760, 410, 160); platform(d, 5020, 300, 190); platform(d, 5280, 495, 160);
        platform(d, 5520, 380, 170);

        coins(d, 225, 445, 3, 52); coins(d, 485, 345, 3, 52); coins(d, 915, 385, 3, 52);
        coins(d, 1195, 285, 4, 50); coins(d, 1695, 365, 3, 50); coins(d, 1955, 255, 4, 50);
        coins(d, 2475, 345, 3, 50); coins(d, 2745, 235, 4, 50); coins(d, 3175, 375, 3, 50);
        coins(d, 3455, 265, 3, 50); coins(d, 3985, 345, 4, 50); coins(d, 4275, 235, 3, 50);
        coins(d, 4775, 365, 3, 50); coins(d, 5035, 255, 4, 50); coins(d, 5535, 335, 3, 50);

        d.enemies.add(new Enemy(500, 620, 400, 610, 150, 2));
        d.enemies.add(new Enemy(1010, 620, 870, 1340, 155, 1));
        d.enemies.add(new Enemy(1750, 620, 1600, 2140, 162, 3));
        d.enemies.add(new Enemy(2010, 300, 1955, 2100, 138, 1));
        d.enemies.add(new Enemy(2520, 620, 2400, 2820, 168, 2));
        d.enemies.add(new Enemy(3270, 620, 3100, 3600, 174, 3));
        d.enemies.add(new Enemy(4050, 620, 3890, 4410, 180, 1));
        d.enemies.add(new Enemy(5090, 620, 4700, 5260, 185, 2));
        d.enemies.add(new Enemy(5590, 620, 5450, 5750, 190, 3));

        d.hazards.add(new Hazard(560, 580, 82, 40, 2));
        d.hazards.add(new Hazard(1110, 582, 110, 38, 3));
        d.hazards.add(new Hazard(1820, 580, 105, 40, 0));
        d.hazards.add(new Hazard(2600, 580, 120, 40, 2));
        d.hazards.add(new Hazard(3330, 580, 115, 40, 3));
        d.hazards.add(new Hazard(4100, 580, 130, 40, 0));
        d.hazards.add(new Hazard(4880, 580, 110, 40, 2));
        d.hazards.add(new Hazard(5600, 580, 105, 40, 3));

        d.crates.add(new Crate(600, 550, 70, 70));
        d.crates.add(new Crate(1325, 550, 70, 70));
        d.crates.add(new Crate(2140, 550, 70, 70));
        d.crates.add(new Crate(2820, 550, 70, 70));
        d.crates.add(new Crate(3590, 550, 70, 70));
        d.crates.add(new Crate(4410, 550, 70, 70));
        d.crates.add(new Crate(5260, 550, 70, 70));
        d.checkpoints.add(1500f); d.checkpoints.add(3150f); d.checkpoints.add(4750f);
        return d;
    }

    /** Level 5: final long gauntlet with the fastest enemies and shortest platforms. */
    private static LevelData createLevel5() {
        LevelData d = new LevelData(5, 4, 6900);
        ground(d, 0, 600); ground(d, 790, 530); ground(d, 1510, 490); ground(d, 2210, 490);
        ground(d, 2920, 480); ground(d, 3630, 510); ground(d, 4370, 480); ground(d, 5080, 480);
        ground(d, 5800, 460); ground(d, 6480, 420);

        platform(d, 180, 500, 150); platform(d, 420, 385, 150); platform(d, 620, 510, 150);
        platform(d, 850, 440, 150); platform(d, 1080, 320, 170); platform(d, 1320, 500, 170);
        platform(d, 1580, 410, 145); platform(d, 1810, 285, 165); platform(d, 2030, 495, 170);
        platform(d, 2280, 390, 145); platform(d, 2500, 270, 170); platform(d, 2730, 500, 170);
        platform(d, 2980, 420, 140); platform(d, 3200, 300, 160); platform(d, 3420, 495, 170);
        platform(d, 3700, 390, 145); platform(d, 3930, 265, 165); platform(d, 4160, 500, 170);
        platform(d, 4440, 410, 140); platform(d, 4660, 285, 165); platform(d, 4880, 495, 170);
        platform(d, 5150, 390, 145); platform(d, 5370, 260, 165); platform(d, 5590, 500, 170);
        platform(d, 5870, 410, 140); platform(d, 6090, 290, 165); platform(d, 6310, 495, 170);
        platform(d, 6530, 390, 145); platform(d, 6720, 275, 140);

        coins(d, 195, 455, 3, 48); coins(d, 435, 340, 3, 48); coins(d, 865, 395, 3, 48);
        coins(d, 1095, 275, 4, 46); coins(d, 1595, 365, 3, 48); coins(d, 1825, 240, 4, 46);
        coins(d, 2295, 345, 3, 48); coins(d, 2515, 225, 4, 46); coins(d, 2995, 375, 3, 48);
        coins(d, 3215, 255, 4, 46); coins(d, 3715, 345, 3, 48); coins(d, 3945, 220, 4, 46);
        coins(d, 4455, 365, 3, 48); coins(d, 4675, 240, 4, 46); coins(d, 5165, 345, 3, 48);
        coins(d, 5385, 215, 4, 46); coins(d, 5885, 365, 3, 48); coins(d, 6105, 245, 4, 46);
        coins(d, 6545, 345, 3, 48); coins(d, 6730, 230, 3, 46);

        d.enemies.add(new Enemy(480, 620, 350, 565, 175, 3));
        d.enemies.add(new Enemy(900, 620, 820, 1270, 182, 2));
        d.enemies.add(new Enemy(1120, 320, 1090, 1215, 155, 1));
        d.enemies.add(new Enemy(1650, 620, 1540, 1940, 188, 3));
        d.enemies.add(new Enemy(1860, 285, 1820, 1930, 162, 2));
        d.enemies.add(new Enemy(2330, 620, 2240, 2630, 194, 1));
        d.enemies.add(new Enemy(3030, 620, 2960, 3330, 198, 3));
        d.enemies.add(new Enemy(3740, 620, 3670, 4050, 202, 2));
        d.enemies.add(new Enemy(3990, 265, 3940, 4050, 170, 1));
        d.enemies.add(new Enemy(4480, 620, 4410, 4780, 206, 3));
        d.enemies.add(new Enemy(5180, 620, 5120, 5480, 210, 2));
        d.enemies.add(new Enemy(5910, 620, 5840, 6180, 214, 3));
        d.enemies.add(new Enemy(6580, 620, 6520, 6800, 220, 1));

        d.hazards.add(new Hazard(500, 580, 92, 40, 3));
        d.hazards.add(new Hazard(980, 580, 100, 40, 2));
        d.hazards.add(new Hazard(1700, 580, 95, 40, 0));
        d.hazards.add(new Hazard(2380, 580, 105, 40, 3));
        d.hazards.add(new Hazard(3060, 580, 110, 40, 2));
        d.hazards.add(new Hazard(3790, 580, 115, 40, 0));
        d.hazards.add(new Hazard(4510, 580, 105, 40, 3));
        d.hazards.add(new Hazard(5210, 580, 110, 40, 2));
        d.hazards.add(new Hazard(5930, 580, 105, 40, 0));
        d.hazards.add(new Hazard(6600, 580, 100, 40, 3));
        d.hazards.add(new Hazard(2535, 238, 95, 32, 2));
        d.hazards.add(new Hazard(5395, 228, 95, 32, 3));

        d.crates.add(new Crate(550, 550, 70, 70));
        d.crates.add(new Crate(1240, 550, 70, 70));
        d.crates.add(new Crate(1940, 550, 70, 70));
        d.crates.add(new Crate(2630, 550, 70, 70));
        d.crates.add(new Crate(3330, 550, 70, 70));
        d.crates.add(new Crate(4050, 550, 70, 70));
        d.crates.add(new Crate(4780, 550, 70, 70));
        d.crates.add(new Crate(5480, 550, 70, 70));
        d.crates.add(new Crate(6180, 550, 70, 70));

        d.checkpoints.add(1700f); d.checkpoints.add(3500f); d.checkpoints.add(5400f);
        return d;
    }
}
