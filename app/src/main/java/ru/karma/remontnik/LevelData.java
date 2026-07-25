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
        LevelData d = new LevelData(1, 0, 3950);
        ground(d, 0, 760); ground(d, 920, 760); ground(d, 1830, 740); ground(d, 2710, 1000);

        platform(d, 170, 510, 190); platform(d, 430, 425, 180); platform(d, 670, 355, 170);
        platform(d, 990, 490, 160); platform(d, 1220, 420, 180); platform(d, 1470, 345, 180);
        platform(d, 1940, 505, 180); platform(d, 2200, 430, 200); platform(d, 2480, 350, 180);
        platform(d, 2830, 500, 170); platform(d, 3080, 420, 190); platform(d, 3360, 345, 180);

        coins(d, 190, 465, 4, 50); coins(d, 450, 380, 3, 52); coins(d, 690, 310, 3, 52);
        coins(d, 1010, 445, 3, 52); coins(d, 1240, 375, 3, 52); coins(d, 1490, 300, 3, 52);
        coins(d, 1960, 460, 3, 52); coins(d, 2230, 385, 4, 48); coins(d, 2500, 305, 3, 52);
        coins(d, 2850, 455, 3, 52); coins(d, 3100, 375, 3, 52); coins(d, 3380, 300, 4, 50);

        d.enemies.add(new Enemy(520, 620, 390, 700, 90, 0));
        d.enemies.add(new Enemy(1160, 620, 980, 1490, 95, 4));
        d.enemies.add(new Enemy(2080, 620, 1900, 2360, 100, 1));
        d.enemies.add(new Enemy(3180, 620, 2880, 3500, 110, 4));

        d.hazards.add(new Hazard(690, 590, 70, 30, 0));
        d.hazards.add(new Hazard(1600, 590, 80, 30, 0));
        d.hazards.add(new Hazard(2500, 590, 90, 30, 0));

        d.crates.add(new Crate(580, 550, 70, 70));
        d.crates.add(new Crate(1710, 550, 70, 70));
        d.crates.add(new Crate(2600, 550, 70, 70));
        d.checkpoints.add(1260f); d.checkpoints.add(2610f);
        return d;
    }

    private static LevelData createLevel2() {
        LevelData d = new LevelData(2, 1, 4900);
        ground(d, 0, 700); ground(d, 850, 650); ground(d, 1660, 720); ground(d, 2540, 620); ground(d, 3320, 1380);

        platform(d, 150, 500, 160); platform(d, 370, 405, 180); platform(d, 630, 335, 170);
        platform(d, 930, 485, 150); platform(d, 1160, 395, 180); platform(d, 1420, 325, 180);
        platform(d, 1730, 500, 160); platform(d, 1980, 400, 190); platform(d, 2240, 320, 180);
        platform(d, 2630, 490, 150); platform(d, 2860, 405, 180); platform(d, 3110, 330, 180);
        platform(d, 3470, 485, 165); platform(d, 3720, 395, 180); platform(d, 3980, 315, 180); platform(d, 4260, 430, 170);

        coins(d, 170, 455, 3, 52); coins(d, 390, 360, 3, 50); coins(d, 650, 290, 3, 50);
        coins(d, 950, 440, 3, 52); coins(d, 1180, 350, 3, 50); coins(d, 1440, 280, 4, 48);
        coins(d, 1750, 455, 3, 52); coins(d, 2000, 355, 3, 50); coins(d, 2260, 275, 4, 48);
        coins(d, 2650, 445, 3, 52); coins(d, 2880, 360, 3, 50); coins(d, 3130, 285, 4, 48);
        coins(d, 3490, 440, 3, 52); coins(d, 3740, 350, 3, 50); coins(d, 4000, 270, 4, 48); coins(d, 4280, 385, 3, 50);

        d.enemies.add(new Enemy(480, 620, 320, 620, 100, 5));
        d.enemies.add(new Enemy(1010, 620, 900, 1400, 112, 1));
        d.enemies.add(new Enemy(1840, 620, 1720, 2300, 120, 4));
        d.enemies.add(new Enemy(2720, 620, 2610, 3200, 128, 2));
        d.enemies.add(new Enemy(3620, 620, 3400, 4500, 135, 5));

        d.hazards.add(new Hazard(670, 585, 70, 35, 1));
        d.hazards.add(new Hazard(1510, 585, 90, 35, 1));
        d.hazards.add(new Hazard(2390, 585, 100, 35, 1));
        d.hazards.add(new Hazard(3230, 585, 110, 35, 1));

        d.crates.add(new Crate(610, 550, 70, 70)); d.crates.add(new Crate(1600, 550, 70, 70));
        d.crates.add(new Crate(2490, 550, 70, 70)); d.crates.add(new Crate(3330, 550, 70, 70));
        d.checkpoints.add(1450f); d.checkpoints.add(2980f);
        return d;
    }

    private static LevelData createLevel3() {
        LevelData d = new LevelData(3, 2, 5600);
        ground(d, 0, 640); ground(d, 800, 570); ground(d, 1540, 650); ground(d, 2360, 560); ground(d, 3090, 620); ground(d, 3910, 1490);

        platform(d, 130, 510, 170); platform(d, 370, 420, 170); platform(d, 600, 340, 160);
        platform(d, 870, 485, 150); platform(d, 1110, 395, 180); platform(d, 1360, 315, 180);
        platform(d, 1610, 500, 170); platform(d, 1870, 405, 190); platform(d, 2130, 320, 170);
        platform(d, 2430, 485, 150); platform(d, 2660, 395, 190); platform(d, 2920, 320, 170);
        platform(d, 3160, 500, 150); platform(d, 3400, 410, 180); platform(d, 3660, 330, 180);
        platform(d, 4000, 490, 160); platform(d, 4250, 395, 190); platform(d, 4520, 315, 190); platform(d, 4800, 435, 180);

        coins(d, 150, 465, 3, 50); coins(d, 390, 375, 3, 50); coins(d, 620, 295, 3, 50);
        coins(d, 890, 440, 3, 50); coins(d, 1130, 350, 3, 50); coins(d, 1380, 270, 4, 48);
        coins(d, 1630, 455, 3, 50); coins(d, 1890, 360, 3, 50); coins(d, 2150, 275, 4, 48);
        coins(d, 2450, 440, 3, 50); coins(d, 2680, 350, 3, 50); coins(d, 2940, 275, 4, 48);
        coins(d, 3180, 455, 3, 50); coins(d, 3420, 365, 3, 50); coins(d, 3680, 285, 4, 48);
        coins(d, 4020, 445, 3, 50); coins(d, 4270, 350, 3, 50); coins(d, 4540, 270, 4, 48); coins(d, 4820, 390, 3, 50);

        d.enemies.add(new Enemy(430, 620, 300, 570, 112, 0));
        d.enemies.add(new Enemy(1010, 620, 860, 1290, 118, 4));
        d.enemies.add(new Enemy(1750, 620, 1610, 2100, 128, 2));
        d.enemies.add(new Enemy(2550, 620, 2410, 2900, 136, 5));
        d.enemies.add(new Enemy(3300, 620, 3140, 3600, 144, 1));
        d.enemies.add(new Enemy(4200, 620, 3980, 4950, 152, 4));

        d.hazards.add(new Hazard(610, 580, 82, 40, 0));
        d.hazards.add(new Hazard(1440, 580, 95, 40, 2));
        d.hazards.add(new Hazard(2260, 580, 90, 40, 0));
        d.hazards.add(new Hazard(3000, 580, 100, 40, 2));
        d.hazards.add(new Hazard(3830, 580, 110, 40, 0));

        d.crates.add(new Crate(560, 550, 70, 70)); d.crates.add(new Crate(1490, 550, 70, 70)); d.crates.add(new Crate(2320, 550, 70, 70));
        d.crates.add(new Crate(3070, 550, 70, 70)); d.crates.add(new Crate(3890, 550, 70, 70));
        d.checkpoints.add(1600f); d.checkpoints.add(3250f);
        return d;
    }

    private static LevelData createLevel4() {
        LevelData d = new LevelData(4, 3, 6350);
        ground(d, 0, 620); ground(d, 780, 530); ground(d, 1470, 560); ground(d, 2200, 500); ground(d, 2880, 540); ground(d, 3620, 560); ground(d, 4380, 540); ground(d, 5100, 1080);

        platform(d, 150, 500, 150); platform(d, 380, 410, 160); platform(d, 590, 320, 150);
        platform(d, 860, 485, 145); platform(d, 1080, 395, 165); platform(d, 1300, 305, 165);
        platform(d, 1540, 495, 150); platform(d, 1780, 405, 175); platform(d, 2030, 315, 165);
        platform(d, 2260, 490, 145); platform(d, 2470, 400, 165); platform(d, 2690, 305, 165);
        platform(d, 2950, 500, 150); platform(d, 3190, 410, 170); platform(d, 3420, 320, 160);
        platform(d, 3690, 495, 145); platform(d, 3920, 405, 165); platform(d, 4150, 315, 165);
        platform(d, 4440, 500, 145); platform(d, 4660, 410, 170); platform(d, 4890, 320, 160);
        platform(d, 5170, 500, 145); platform(d, 5410, 410, 170); platform(d, 5660, 320, 170); platform(d, 5900, 455, 180);

        coins(d, 170, 455, 3, 48); coins(d, 400, 365, 3, 48); coins(d, 610, 275, 3, 48);
        coins(d, 880, 440, 3, 48); coins(d, 1100, 350, 3, 48); coins(d, 1320, 260, 4, 46);
        coins(d, 1560, 450, 3, 48); coins(d, 1800, 360, 3, 48); coins(d, 2050, 270, 4, 46);
        coins(d, 2280, 445, 3, 48); coins(d, 2490, 355, 3, 48); coins(d, 2710, 260, 4, 46);
        coins(d, 2970, 455, 3, 48); coins(d, 3210, 365, 3, 48); coins(d, 3440, 275, 4, 46);
        coins(d, 3710, 450, 3, 48); coins(d, 3940, 360, 3, 48); coins(d, 4170, 270, 4, 46);
        coins(d, 4460, 455, 3, 48); coins(d, 4680, 365, 3, 48); coins(d, 4910, 275, 4, 46);
        coins(d, 5190, 455, 3, 48); coins(d, 5430, 365, 3, 48); coins(d, 5680, 275, 4, 46); coins(d, 5920, 410, 3, 48);

        d.enemies.add(new Enemy(460, 620, 300, 540, 130, 5));
        d.enemies.add(new Enemy(920, 620, 820, 1230, 138, 2));
        d.enemies.add(new Enemy(1640, 620, 1520, 2010, 145, 4));
        d.enemies.add(new Enemy(2340, 620, 2240, 2680, 150, 3));
        d.enemies.add(new Enemy(3050, 620, 2930, 3410, 158, 5));
        d.enemies.add(new Enemy(3770, 620, 3650, 4140, 165, 2));
        d.enemies.add(new Enemy(4500, 620, 4410, 4870, 172, 4));
        d.enemies.add(new Enemy(5240, 620, 5130, 5960, 180, 3));

        d.hazards.add(new Hazard(640, 582, 90, 38, 3));
        d.hazards.add(new Hazard(1385, 580, 95, 40, 2));
        d.hazards.add(new Hazard(2100, 582, 95, 38, 3));
        d.hazards.add(new Hazard(2790, 580, 95, 40, 2));
        d.hazards.add(new Hazard(3540, 582, 110, 38, 3));
        d.hazards.add(new Hazard(4300, 580, 105, 40, 2));
        d.hazards.add(new Hazard(5030, 582, 110, 38, 3));
        d.hazards.add(new Hazard(5750, 580, 105, 40, 2));

        d.crates.add(new Crate(580, 550, 70, 70)); d.crates.add(new Crate(1460, 550, 70, 70)); d.crates.add(new Crate(2200, 550, 70, 70));
        d.crates.add(new Crate(2880, 550, 70, 70)); d.crates.add(new Crate(3620, 550, 70, 70)); d.crates.add(new Crate(4370, 550, 70, 70)); d.crates.add(new Crate(5100, 550, 70, 70));
        d.checkpoints.add(1550f); d.checkpoints.add(3220f); d.checkpoints.add(5030f);
        return d;
    }

    private static LevelData createLevel5() {
        LevelData d = new LevelData(5, 4, 7350);
        ground(d, 0, 560); ground(d, 730, 460); ground(d, 1350, 520); ground(d, 2020, 460); ground(d, 2650, 500);
        ground(d, 3340, 450); ground(d, 3960, 470); ground(d, 4610, 460); ground(d, 5240, 470); ground(d, 5900, 1240);

        platform(d, 120, 500, 145); platform(d, 330, 405, 150); platform(d, 540, 315, 145);
        platform(d, 810, 490, 135); platform(d, 1010, 400, 150); platform(d, 1210, 305, 150);
        platform(d, 1440, 495, 145); platform(d, 1660, 405, 160); platform(d, 1880, 315, 150);
        platform(d, 2110, 495, 145); platform(d, 2330, 405, 160); platform(d, 2550, 315, 150);
        platform(d, 2760, 500, 145); platform(d, 2980, 410, 160); platform(d, 3200, 320, 150);
        platform(d, 3450, 495, 140); platform(d, 3660, 405, 160); platform(d, 3870, 315, 150);
        platform(d, 4080, 495, 145); platform(d, 4300, 405, 155); platform(d, 4510, 315, 150);
        platform(d, 4720, 495, 145); platform(d, 4940, 405, 160); platform(d, 5160, 315, 150);
        platform(d, 5370, 500, 145); platform(d, 5590, 410, 160); platform(d, 5810, 320, 150); platform(d, 6060, 430, 160); platform(d, 6330, 345, 150); platform(d, 6580, 270, 140);

        coins(d, 140, 455, 3, 46); coins(d, 350, 360, 3, 46); coins(d, 560, 270, 3, 46);
        coins(d, 830, 445, 3, 46); coins(d, 1030, 355, 3, 46); coins(d, 1230, 260, 4, 44);
        coins(d, 1460, 450, 3, 46); coins(d, 1680, 360, 3, 46); coins(d, 1900, 270, 4, 44);
        coins(d, 2130, 450, 3, 46); coins(d, 2350, 360, 3, 46); coins(d, 2570, 270, 4, 44);
        coins(d, 2780, 455, 3, 46); coins(d, 3000, 365, 3, 46); coins(d, 3220, 275, 4, 44);
        coins(d, 3470, 450, 3, 46); coins(d, 3680, 360, 3, 46); coins(d, 3890, 270, 4, 44);
        coins(d, 4100, 450, 3, 46); coins(d, 4320, 360, 3, 46); coins(d, 4530, 270, 4, 44);
        coins(d, 4740, 450, 3, 46); coins(d, 4960, 360, 3, 46); coins(d, 5180, 270, 4, 44);
        coins(d, 5390, 455, 3, 46); coins(d, 5610, 365, 3, 46); coins(d, 5830, 275, 4, 44); coins(d, 6080, 385, 3, 46); coins(d, 6350, 300, 3, 46); coins(d, 6600, 225, 3, 46);

        d.enemies.add(new Enemy(420, 620, 280, 510, 150, 4));
        d.enemies.add(new Enemy(860, 620, 770, 1160, 160, 3));
        d.enemies.add(new Enemy(1500, 620, 1390, 1900, 168, 5));
        d.enemies.add(new Enemy(2180, 620, 2080, 2530, 176, 2));
        d.enemies.add(new Enemy(2840, 620, 2740, 3180, 182, 4));
        d.enemies.add(new Enemy(3520, 620, 3420, 3860, 188, 3));
        d.enemies.add(new Enemy(4140, 620, 4050, 4500, 194, 5));
        d.enemies.add(new Enemy(4780, 620, 4690, 5130, 200, 2));
        d.enemies.add(new Enemy(5440, 620, 5350, 5800, 208, 4));
        d.enemies.add(new Enemy(6120, 620, 5950, 7000, 215, 3));

        d.hazards.add(new Hazard(560, 582, 90, 38, 3));
        d.hazards.add(new Hazard(1270, 580, 95, 40, 2));
        d.hazards.add(new Hazard(1920, 582, 95, 38, 3));
        d.hazards.add(new Hazard(2580, 580, 100, 40, 2));
        d.hazards.add(new Hazard(3260, 582, 100, 38, 3));
        d.hazards.add(new Hazard(3900, 580, 100, 40, 2));
        d.hazards.add(new Hazard(4560, 582, 100, 38, 3));
        d.hazards.add(new Hazard(5200, 580, 105, 40, 2));
        d.hazards.add(new Hazard(5860, 582, 100, 38, 3));
        d.hazards.add(new Hazard(6600, 580, 120, 40, 2));
        d.hazards.add(new Hazard(5850, 288, 90, 32, 3));
        d.hazards.add(new Hazard(6620, 238, 90, 32, 3));

        d.crates.add(new Crate(520, 550, 70, 70)); d.crates.add(new Crate(1320, 550, 70, 70)); d.crates.add(new Crate(1980, 550, 70, 70));
        d.crates.add(new Crate(2640, 550, 70, 70)); d.crates.add(new Crate(3310, 550, 70, 70)); d.crates.add(new Crate(3980, 550, 70, 70));
        d.crates.add(new Crate(4630, 550, 70, 70)); d.crates.add(new Crate(5290, 550, 70, 70)); d.crates.add(new Crate(5950, 550, 70, 70));
        d.checkpoints.add(1650f); d.checkpoints.add(3500f); d.checkpoints.add(5550f);
        return d;
    }
}
