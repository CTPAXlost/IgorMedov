package ru.karma.remontnik;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.util.SparseIntArray;

import java.util.Locale;

public final class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final float W = 1280f;
    private static final float H = 720f;
    private static final float PLAYER_HALF_W = 24f;
    private static final float PLAYER_H = 112f;

    private enum Screen { MENU, SETTINGS, LEVEL_SELECT, GAME, PAUSE, REPAIR, VICTORY, GAME_OVER }

    private final Activity activity;
    private final SurfaceHolder surfaceHolder;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SharedPreferences prefs;
    private final SparseIntArray pointers = new SparseIntArray();
    private final RectF leftBtn = new RectF(34, 568, 142, 682);
    private final RectF rightBtn = new RectF(160, 568, 268, 682);
    private final RectF jumpBtn = new RectF(1004, 552, 1124, 682);
    private final RectF hammerBtn = new RectF(1138, 552, 1258, 682);
    private final RectF pauseBtn = new RectF(1192, 18, 1262, 82);

    private Thread loopThread;
    private volatile boolean running;
    private Screen screen = Screen.MENU;
    private Screen settingsReturn = Screen.MENU;
    private float time;
    private float repairTimer;
    private float cameraX;
    private float checkpointX = 90f;
    private int currentLevel = 1;
    private int levelCoins;
    private int totalCoins;
    private int unlockedLevel;
    private boolean hasSave;
    private boolean soundEnabled;
    private boolean musicEnabled;
    private boolean vibrationEnabled;
    private int controlSize;
    private boolean leftPressed, rightPressed, keyLeft, keyRight;
    private boolean jumpQueued, hammerQueued;
    private boolean menuTapLock;
    private String toastText = "";
    private float toastTimer;

    private LevelData level;
    private final Player player = new Player();

    private SoundPool soundPool;
    private int sndCoin, sndJump, sndHit, sndHammer, sndWin;
    private MediaPlayer music;
    private Vibrator vibrator;

    private final RectF tmpRect = new RectF();

    public GameView(Activity activity) {
        super(activity);
        this.activity = activity;
        this.surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        prefs = activity.getSharedPreferences("remontnik_save", Context.MODE_PRIVATE);
        loadPrefs();
        initAudio();
    }

    private void loadPrefs() {
        unlockedLevel = Math.max(1, Math.min(3, prefs.getInt("unlocked_level", 1)));
        totalCoins = Math.max(0, prefs.getInt("total_coins", 0));
        hasSave = prefs.getBoolean("has_save", false);
        soundEnabled = prefs.getBoolean("sound", true);
        musicEnabled = prefs.getBoolean("music", true);
        vibrationEnabled = prefs.getBoolean("vibration", true);
        controlSize = Math.max(0, Math.min(2, prefs.getInt("control_size", 1)));
    }

    private void initAudio() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attributes).build();
        sndCoin = soundPool.load(activity, R.raw.coin, 1);
        sndJump = soundPool.load(activity, R.raw.jump, 1);
        sndHit = soundPool.load(activity, R.raw.hit, 1);
        sndHammer = soundPool.load(activity, R.raw.hammer, 1);
        sndWin = soundPool.load(activity, R.raw.win, 1);
        music = MediaPlayer.create(activity, R.raw.theme);
        if (music != null) {
            music.setLooping(true);
            music.setVolume(.28f, .28f);
        }
        vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        updateMusic();
    }

    private void updateMusic() {
        if (music == null) return;
        try {
            if (musicEnabled && running && screen != Screen.PAUSE) {
                if (!music.isPlaying()) music.start();
            } else if (music.isPlaying()) {
                music.pause();
            }
        } catch (IllegalStateException ignored) { }
    }

    private void play(int id, float volume) {
        if (soundEnabled && soundPool != null) soundPool.play(id, volume, volume, 1, 0, 1f);
    }

    private void vibrate(int ms) {
        if (!vibrationEnabled || vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //noinspection deprecation
            vibrator.vibrate(ms);
        }
    }

    @Override public void surfaceCreated(SurfaceHolder holder) { resumeGame(); }
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }
    @Override public void surfaceDestroyed(SurfaceHolder holder) { pauseGame(); }

    public void resumeGame() {
        if (running) return;
        running = true;
        loopThread = new Thread(this, "RemontnikGameLoop");
        loopThread.start();
        updateMusic();
    }

    public void pauseGame() {
        if (!running) return;
        running = false;
        try {
            if (loopThread != null) loopThread.join(500);
        } catch (InterruptedException ignored) { }
        if (screen == Screen.GAME) screen = Screen.PAUSE;
        updateMusic();
    }

    @Override public void run() {
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = Math.min(.034f, (now - last) / 1_000_000_000f);
            last = now;
            update(dt);
            Canvas c = null;
            try {
                c = surfaceHolder.lockCanvas();
                if (c != null) render(c);
            } finally {
                if (c != null) surfaceHolder.unlockCanvasAndPost(c);
            }
            long used = System.nanoTime() - now;
            long sleep = 16_666_667L - used;
            if (sleep > 0) {
                try { Thread.sleep(sleep / 1_000_000L, (int) (sleep % 1_000_000L)); }
                catch (InterruptedException ignored) { }
            }
        }
    }

    private void update(float dt) {
        time += dt;
        if (toastTimer > 0) toastTimer -= dt;
        if (screen == Screen.GAME) updateGame(dt);
        else if (screen == Screen.REPAIR) repairTimer += dt;
    }

    private void updateGame(float dt) {
        if (level == null) return;
        player.invincible = Math.max(0, player.invincible - dt);
        player.hammerTime = Math.max(0, player.hammerTime - dt);

        boolean moveLeft = leftPressed || keyLeft;
        boolean moveRight = rightPressed || keyRight;
        float target = 0;
        if (moveLeft && !moveRight) target = -350f;
        if (moveRight && !moveLeft) target = 350f;
        float accel = player.grounded ? 2200f : 1300f;
        player.vx = approach(player.vx, target, accel * dt);
        if (target == 0) player.vx = approach(player.vx, 0, (player.grounded ? 2600f : 500f) * dt);
        if (Math.abs(player.vx) > 8) player.facingRight = player.vx > 0;

        if (jumpQueued) {
            jumpQueued = false;
            if (player.grounded || player.coyote > 0) {
                player.vy = -830f;
                player.grounded = false;
                player.coyote = 0;
                play(sndJump, .7f);
            }
        }
        if (hammerQueued) {
            hammerQueued = false;
            if (player.hammerTime <= 0) {
                player.hammerTime = .34f;
                play(sndHammer, .75f);
                vibrate(18);
            }
        }

        if (player.grounded) player.coyote = .1f;
        else player.coyote = Math.max(0, player.coyote - dt);
        player.vy += 1900f * dt;
        player.vy = Math.min(player.vy, 1250f);

        float prevX = player.x;
        player.x += player.vx * dt;
        resolveHorizontal(prevX);
        float prevY = player.y;
        player.y += player.vy * dt;
        resolveVertical(prevY);
        player.x = Math.max(PLAYER_HALF_W, Math.min(level.worldWidth - PLAYER_HALF_W, player.x));

        if (player.y > 820) damagePlayer(true);

        updateCoins();
        updateCrates();
        updateEnemies(dt);
        updateHazards();
        updateCheckpoints();

        if (player.x >= level.finishX) finishLevel();
        cameraX = approach(cameraX, clamp(player.x - 430f, 0, Math.max(0, level.worldWidth - W)), 900f * dt);
    }

    private float approach(float value, float target, float amount) {
        if (value < target) return Math.min(value + amount, target);
        return Math.max(value - amount, target);
    }

    private float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }

    private RectF playerRect() {
        tmpRect.set(player.x - PLAYER_HALF_W, player.y - PLAYER_H,
                player.x + PLAYER_HALF_W, player.y);
        return tmpRect;
    }

    private void resolveHorizontal(float prevX) {
        RectF r = playerRect();
        for (LevelData.Platform platform : level.platforms) {
            if (!RectF.intersects(r, platform.rect)) continue;
            if (player.vx > 0 && prevX + PLAYER_HALF_W <= platform.rect.left + 8) {
                player.x = platform.rect.left - PLAYER_HALF_W;
            } else if (player.vx < 0 && prevX - PLAYER_HALF_W >= platform.rect.right - 8) {
                player.x = platform.rect.right + PLAYER_HALF_W;
            }
            player.vx = 0;
            r = playerRect();
        }
        for (LevelData.Crate crate : level.crates) {
            if (crate.broken || !RectF.intersects(r, crate.rect)) continue;
            if (player.vx > 0 && prevX + PLAYER_HALF_W <= crate.rect.left + 8) player.x = crate.rect.left - PLAYER_HALF_W;
            else if (player.vx < 0 && prevX - PLAYER_HALF_W >= crate.rect.right - 8) player.x = crate.rect.right + PLAYER_HALF_W;
            player.vx = 0;
            r = playerRect();
        }
    }

    private void resolveVertical(float prevY) {
        player.grounded = false;
        RectF r = playerRect();
        for (LevelData.Platform platform : level.platforms) {
            if (!horizontalOverlap(r, platform.rect)) continue;
            if (player.vy >= 0 && prevY <= platform.rect.top + 7 && player.y >= platform.rect.top) {
                player.y = platform.rect.top;
                player.vy = 0;
                player.grounded = true;
                r = playerRect();
            } else if (player.vy < 0 && prevY - PLAYER_H >= platform.rect.bottom - 7
                    && player.y - PLAYER_H <= platform.rect.bottom) {
                player.y = platform.rect.bottom + PLAYER_H;
                player.vy = 0;
                r = playerRect();
            }
        }
        for (LevelData.Crate crate : level.crates) {
            if (crate.broken || !horizontalOverlap(r, crate.rect)) continue;
            if (player.vy >= 0 && prevY <= crate.rect.top + 7 && player.y >= crate.rect.top) {
                player.y = crate.rect.top;
                player.vy = 0;
                player.grounded = true;
                r = playerRect();
            } else if (player.vy < 0 && prevY - PLAYER_H >= crate.rect.bottom - 7
                    && player.y - PLAYER_H <= crate.rect.bottom) {
                player.y = crate.rect.bottom + PLAYER_H;
                player.vy = 0;
                r = playerRect();
            }
        }
    }

    private boolean horizontalOverlap(RectF a, RectF b) {
        return a.right > b.left + 3 && a.left < b.right - 3;
    }

    private void updateCoins() {
        for (LevelData.Coin coin : level.coins) {
            if (coin.taken) continue;
            float dx = coin.x - player.x;
            float dy = coin.y - (player.y - 56);
            if (dx * dx + dy * dy < 42 * 42) {
                coin.taken = true;
                levelCoins++;
                play(sndCoin, .75f);
                if (levelCoins % 10 == 0) showToast("Отлично! " + levelCoins + " монет");
            }
        }
    }

    private void updateCrates() {
        if (player.hammerTime <= .06f) return;
        float attackX = player.x + (player.facingRight ? 62 : -62);
        RectF attack = new RectF(attackX - 42, player.y - 94, attackX + 42, player.y - 18);
        for (LevelData.Crate crate : level.crates) {
            if (!crate.broken && RectF.intersects(attack, crate.rect)) {
                crate.broken = true;
                levelCoins += 2;
                vibrate(30);
            }
        }
    }

    private void updateEnemies(float dt) {
        RectF pr = playerRect();
        float attackX = player.x + (player.facingRight ? 58 : -58);
        RectF attack = new RectF(attackX - 46, player.y - 100, attackX + 46, player.y - 16);
        for (LevelData.Enemy e : level.enemies) {
            if (!e.alive) continue;
            e.x += e.dir * e.speed * dt;
            if (e.x < e.minX) { e.x = e.minX; e.dir = 1; }
            if (e.x > e.maxX) { e.x = e.maxX; e.dir = -1; }
            RectF er = new RectF(e.x - 27, e.y - 48, e.x + 27, e.y);
            if (player.hammerTime > .06f && RectF.intersects(attack, er)) {
                e.alive = false;
                levelCoins += 3;
                vibrate(25);
                continue;
            }
            if (!RectF.intersects(pr, er)) continue;
            if (player.vy > 160 && player.y - 12 < e.y - 28) {
                e.alive = false;
                player.vy = -520;
                levelCoins += 2;
                play(sndHit, .45f);
            } else damagePlayer(false);
        }
    }

    private void updateHazards() {
        RectF pr = playerRect();
        for (LevelData.Hazard h : level.hazards) {
            if (RectF.intersects(pr, h.rect)) {
                damagePlayer(false);
                return;
            }
        }
    }

    private void updateCheckpoints() {
        for (float cp : level.checkpoints) {
            if (player.x > cp && cp > checkpointX + 20) {
                checkpointX = cp;
                saveCurrent();
                showToast("Контрольная точка сохранена");
            }
        }
    }

    private void damagePlayer(boolean fall) {
        if (player.invincible > 0) return;
        player.lives--;
        play(sndHit, .85f);
        vibrate(90);
        if (player.lives <= 0) {
            saveCurrent();
            screen = Screen.GAME_OVER;
            updateMusic();
            return;
        }
        player.x = checkpointX;
        player.y = 560;
        player.vx = 0;
        player.vy = fall ? 0 : -340;
        player.invincible = 2f;
        cameraX = clamp(player.x - 430, 0, Math.max(0, level.worldWidth - W));
    }

    private void startNewGame() {
        prefs.edit().putInt("unlocked_level", 1).putInt("total_coins", 0)
                .putBoolean("has_save", false).remove("saved_checkpoint").remove("saved_level_coins").apply();
        unlockedLevel = 1; totalCoins = 0; hasSave = false;
        startLevel(1, false);
    }

    private void startLevel(int number, boolean loadSaved) {
        currentLevel = Math.max(1, Math.min(3, number));
        level = LevelData.create(currentLevel);
        levelCoins = 0;
        checkpointX = 90;
        if (loadSaved && prefs.getInt("saved_level", 1) == currentLevel) {
            checkpointX = Math.max(90, prefs.getFloat("saved_checkpoint", 90));
            levelCoins = Math.max(0, prefs.getInt("saved_level_coins", 0));
            // Remove coins before the checkpoint to prevent easy duplication.
            int remove = levelCoins;
            for (LevelData.Coin coin : level.coins) {
                if (coin.x < checkpointX && remove > 0) { coin.taken = true; remove--; }
            }
        }
        player.x = checkpointX;
        player.y = 560;
        player.vx = player.vy = 0;
        player.lives = 3;
        player.grounded = false;
        player.invincible = 1.2f;
        player.hammerTime = 0;
        player.facingRight = true;
        cameraX = clamp(player.x - 430, 0, Math.max(0, level.worldWidth - W));
        screen = Screen.GAME;
        saveCurrent();
        updateMusic();
    }

    private void saveCurrent() {
        if (level == null) return;
        hasSave = true;
        prefs.edit()
                .putBoolean("has_save", true)
                .putInt("saved_level", currentLevel)
                .putFloat("saved_checkpoint", checkpointX)
                .putInt("saved_level_coins", levelCoins)
                .putInt("unlocked_level", unlockedLevel)
                .putInt("total_coins", totalCoins)
                .apply();
    }

    private void finishLevel() {
        if (screen != Screen.GAME) return;
        totalCoins += levelCoins;
        unlockedLevel = Math.max(unlockedLevel, Math.min(3, currentLevel + 1));
        int next = Math.min(3, currentLevel + 1);
        prefs.edit().putInt("total_coins", totalCoins).putInt("unlocked_level", unlockedLevel)
                .putBoolean("has_save", currentLevel < 3).putInt("saved_level", next)
                .putFloat("saved_checkpoint", 90).putInt("saved_level_coins", 0).apply();
        hasSave = currentLevel < 3;
        repairTimer = 0;
        screen = Screen.REPAIR;
        play(sndWin, .9f);
        updateMusic();
    }

    private void continueAfterRepair() {
        if (currentLevel < 3) startLevel(currentLevel + 1, false);
        else {
            prefs.edit().putBoolean("has_save", false).apply();
            hasSave = false;
            screen = Screen.VICTORY;
            updateMusic();
        }
    }

    private void showToast(String text) {
        toastText = text;
        toastTimer = 2.2f;
    }

    private void render(Canvas c) {
        c.drawColor(Color.rgb(10, 13, 20));
        float scale = Math.min(c.getWidth() / W, c.getHeight() / H);
        float ox = (c.getWidth() - W * scale) * .5f;
        float oy = (c.getHeight() - H * scale) * .5f;
        c.save();
        c.translate(ox, oy);
        c.scale(scale, scale);
        switch (screen) {
            case MENU: drawMenu(c); break;
            case SETTINGS: drawSettings(c); break;
            case LEVEL_SELECT: drawLevelSelect(c); break;
            case GAME: drawGame(c); break;
            case PAUSE: drawGame(c); drawPause(c); break;
            case REPAIR: drawRepair(c); break;
            case VICTORY: drawVictory(c); break;
            case GAME_OVER: drawGame(c); drawGameOver(c); break;
        }
        if (toastTimer > 0) drawToast(c);
        c.restore();
    }

    private void drawMenu(Canvas c) {
        drawMenuBackground(c);
        // Title panel.
        p.setShader(new LinearGradient(70, 55, 620, 180, Color.rgb(29, 35, 47), Color.rgb(13, 17, 25), Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(58, 48, 660, 204), 30, 30, p); p.setShader(null);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(Color.rgb(255, 195, 47));
        c.drawRoundRect(new RectF(58, 48, 660, 204), 30, 30, p); p.setStyle(Paint.Style.FILL);
        text(c, "РЕМОНТНИК", 62, 92, 125, Color.WHITE, Paint.Align.LEFT, true);
        text(c, "СТАРЫЙ ДОМ", 42, 96, 171, Color.rgb(255, 198, 48), Paint.Align.LEFT, true);
        text(c, "Платформер • 3 уровня • ремонт дома", 20, 94, 196, Color.rgb(196, 207, 222), Paint.Align.LEFT, false);

        float y = 265;
        drawButton(c, new RectF(85, y, 505, y + 66), "НОВАЯ ИГРА", true, Color.rgb(196, 71, 45)); y += 78;
        drawButton(c, new RectF(85, y, 505, y + 66), "ПРОДОЛЖИТЬ", hasSave, Color.rgb(43, 126, 92)); y += 78;
        drawButton(c, new RectF(85, y, 505, y + 66), "ВЫБОР УРОВНЯ", true, Color.rgb(49, 99, 160)); y += 78;
        drawButton(c, new RectF(85, y, 505, y + 66), "НАСТРОЙКИ", true, Color.rgb(95, 78, 143)); y += 78;
        drawButton(c, new RectF(85, y, 505, y + 66), "ВЫХОД", true, Color.rgb(85, 88, 96));

        text(c, "Монеты: " + totalCoins, 24, 1190, 48, Color.rgb(255, 210, 50), Paint.Align.RIGHT, true);
        drawCoin(c, 1210, 38, 13, time);
        CharacterPainter.draw(c, p, 930, 650, 2.15f, false, time * 5, false, 0, false);
    }

    private void drawMenuBackground(Canvas c) {
        p.setShader(new LinearGradient(0, 0, 0, H, Color.rgb(122, 184, 224), Color.rgb(239, 222, 175), Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, p); p.setShader(null);
        drawCloud(c, 140, 100, .9f); drawCloud(c, 740, 78, 1.25f); drawCloud(c, 1080, 145, .7f);
        p.setColor(Color.rgb(89, 139, 88));
        Path hill = new Path(); hill.moveTo(0, 520); hill.cubicTo(250, 330, 520, 480, 760, 350); hill.cubicTo(1010, 250, 1190, 390, 1280, 330); hill.lineTo(1280,720); hill.lineTo(0,720); hill.close(); c.drawPath(hill,p);
        p.setColor(Color.rgb(61, 112, 68));
        Path hill2 = new Path(); hill2.moveTo(0, 610); hill2.cubicTo(300, 430, 590, 590, 840, 450); hill2.cubicTo(1030, 360, 1180, 480, 1280, 420); hill2.lineTo(1280,720); hill2.lineTo(0,720); hill2.close(); c.drawPath(hill2,p);
        drawHouse(c, 965, 510, 1.3f, 0, false);
        p.setColor(Color.rgb(70, 116, 62)); c.drawRect(0, 635, W, H, p);
        for (int i=0;i<18;i++) drawGrass(c, i*80 + 25, 640, .8f + (i%3)*.1f);
    }

    private void drawSettings(Canvas c) {
        drawSimpleBackdrop(c, "НАСТРОЙКИ");
        RectF panel = new RectF(260, 145, 1020, 610);
        panel(c, panel);
        drawSettingRow(c, 315, 210, "Звук", soundEnabled, 0);
        drawSettingRow(c, 315, 295, "Музыка", musicEnabled, 1);
        drawSettingRow(c, 315, 380, "Вибрация", vibrationEnabled, 2);
        text(c, "Размер кнопок", 30, 315, 470, Color.WHITE, Paint.Align.LEFT, true);
        String size = controlSize == 0 ? "Маленькие" : controlSize == 1 ? "Обычные" : "Большие";
        drawButton(c, new RectF(690, 430, 950, 490), size, true, Color.rgb(50, 100, 153));
        drawButton(c, new RectF(315, 520, 600, 580), "СБРОСИТЬ ПРОГРЕСС", true, Color.rgb(139, 54, 51));
        drawButton(c, new RectF(680, 520, 950, 580), "НАЗАД", true, Color.rgb(75, 83, 98));
    }

    private void drawSettingRow(Canvas c, float x, float y, String label, boolean enabled, int index) {
        text(c, label, 30, x, y + 36, Color.WHITE, Paint.Align.LEFT, true);
        RectF toggle = new RectF(790, y, 945, y + 58);
        p.setColor(enabled ? Color.rgb(51, 156, 103) : Color.rgb(91, 94, 104));
        c.drawRoundRect(toggle, 29, 29, p);
        p.setColor(Color.WHITE);
        float cx = enabled ? 916 : 819;
        c.drawCircle(cx, y + 29, 22, p);
        p.setColor(Color.argb(40,0,0,0)); c.drawCircle(cx, y + 33, 18, p);
    }

    private void drawLevelSelect(Canvas c) {
        drawSimpleBackdrop(c, "ВЫБОР УРОВНЯ");
        for (int i = 1; i <= 3; i++) {
            float x = 100 + (i - 1) * 390;
            RectF card = new RectF(x, 180, x + 320, 535);
            boolean unlocked = i <= unlockedLevel;
            p.setColor(unlocked ? Color.rgb(32, 42, 56) : Color.rgb(48, 49, 55));
            c.drawRoundRect(card, 24, 24, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(unlocked ? Color.rgb(255, 194, 45) : Color.rgb(90, 90, 95)); c.drawRoundRect(card,24,24,p); p.setStyle(Paint.Style.FILL);
            drawLevelPreview(c, x + 18, 205, 284, 180, i, unlocked);
            text(c, "УРОВЕНЬ " + i, 34, x + 160, 430, unlocked ? Color.WHITE : Color.LTGRAY, Paint.Align.CENTER, true);
            String subtitle = i == 1 ? "Дорога к дому" : i == 2 ? "Старый квартал" : "Последний участок";
            text(c, subtitle, 22, x + 160, 468, Color.rgb(190, 201, 215), Paint.Align.CENTER, false);
            drawButton(c, new RectF(x + 45, 485, x + 275, 535), unlocked ? "ИГРАТЬ" : "ЗАКРЫТО", unlocked, Color.rgb(49, 126, 88));
        }
        drawButton(c, new RectF(500, 620, 780, 682), "НАЗАД", true, Color.rgb(75, 83, 98));
    }

    private void drawLevelPreview(Canvas c, float x, float y, float w, float h, int n, boolean unlocked) {
        c.save(); c.clipRect(x,y,x+w,y+h);
        int top = n == 1 ? Color.rgb(112,184,229) : n == 2 ? Color.rgb(92,111,139) : Color.rgb(241,130,74);
        int bottom = n == 1 ? Color.rgb(230,220,164) : n == 2 ? Color.rgb(179,183,184) : Color.rgb(96,71,111);
        p.setShader(new LinearGradient(0,y,0,y+h,top,bottom,Shader.TileMode.CLAMP)); c.drawRect(x,y,x+w,y+h,p); p.setShader(null);
        p.setColor(n==1?Color.rgb(75,139,73):Color.rgb(69,77,83)); c.drawRect(x,y+h-45,x+w,y+h,p);
        drawHouse(c,x+w-75,y+h-52,.38f,n-1,true);
        CharacterPainter.draw(c,p,x+90,y+h-30,.58f,true,time*5,false,0,false);
        if (!unlocked) { p.setColor(Color.argb(160,20,20,24)); c.drawRect(x,y,x+w,y+h,p); drawLockIcon(c, x + w / 2, y + h / 2, 1f); }
        c.restore();
    }

    private void drawGame(Canvas c) {
        if (level == null) return;
        drawLevelBackground(c, level.theme, cameraX);
        c.save(); c.translate(-cameraX, 0);
        drawWorldDecor(c);
        for (LevelData.Platform platform : level.platforms) drawPlatform(c, platform);
        for (LevelData.Hazard h : level.hazards) drawHazard(c, h);
        for (LevelData.Crate crate : level.crates) if (!crate.broken) drawCrate(c, crate.rect);
        for (LevelData.Coin coin : level.coins) if (!coin.taken) drawCoin(c, coin.x, coin.y, 15, time + coin.x * .01f);
        for (LevelData.Enemy enemy : level.enemies) if (enemy.alive) drawEnemy(c, enemy);
        for (float cp : level.checkpoints) drawCheckpoint(c, cp, cp <= checkpointX + 1);
        drawHouse(c, level.finishX + 70, 620, .82f, currentLevel - 1, true);
        drawFinishFlag(c, level.finishX, 620);
        if (player.invincible <= 0 || ((int)(player.invincible * 10) % 2 == 0)) {
            float run = Math.abs(player.vx) > 25 && player.grounded ? time * 12 : 0;
            float hammerPhase = player.hammerTime > 0 ? 1f - player.hammerTime / .34f : 0f;
            CharacterPainter.draw(c, p, player.x, player.y, 1f, player.facingRight, run, !player.grounded, hammerPhase, false);
        }
        c.restore();
        drawHud(c);
        drawControls(c);
    }

    private void drawLevelBackground(Canvas c, int theme, float cam) {
        int top = theme == 0 ? Color.rgb(111,185,231) : theme == 1 ? Color.rgb(91,112,143) : Color.rgb(241,127,70);
        int bottom = theme == 0 ? Color.rgb(236,226,178) : theme == 1 ? Color.rgb(184,190,194) : Color.rgb(102,72,119);
        p.setShader(new LinearGradient(0,0,0,H,top,bottom,Shader.TileMode.CLAMP)); c.drawRect(0,0,W,H,p); p.setShader(null);
        if (theme == 2) { p.setColor(Color.rgb(255,211,87)); c.drawCircle(1040,130,58,p); p.setColor(Color.argb(40,255,238,170)); c.drawCircle(1040,130,94,p); }
        else { drawCloud(c, 160 - cam*.08f % 1450, 100, 1f); drawCloud(c, 720 - cam*.06f % 1500, 150, .7f); drawCloud(c, 1100 - cam*.1f % 1600, 75, 1.2f); }
        p.setColor(theme == 0 ? Color.rgb(101,150,99) : theme == 1 ? Color.rgb(91,102,110) : Color.rgb(92,73,104));
        Path far = new Path(); far.moveTo(0,500); for(int x=0;x<=1280;x+=160) far.lineTo(x,430+(float)Math.sin((x+cam*.18f)*.006f)*55); far.lineTo(1280,720); far.lineTo(0,720); far.close(); c.drawPath(far,p);
        p.setColor(theme == 0 ? Color.rgb(69,124,73) : theme == 1 ? Color.rgb(68,77,84) : Color.rgb(65,60,85));
        Path near = new Path(); near.moveTo(0,570); for(int x=0;x<=1280;x+=120) near.lineTo(x,505+(float)Math.sin((x+cam*.28f)*.008f)*45); near.lineTo(1280,720); near.lineTo(0,720); near.close(); c.drawPath(near,p);
        if(theme==1) { p.setColor(Color.argb(90,220,235,246)); p.setStrokeWidth(2); for(int i=0;i<70;i++){ float x=(i*173+time*260)%1400-60; float y=(i*97+time*410)%720; c.drawLine(x,y,x-8,y+18,p);} }
    }

    private void drawWorldDecor(Canvas c) {
        int theme = level.theme;
        for (int i = 0; i < (int)(level.worldWidth / 320); i++) {
            float x = 130 + i * 330;
            if (theme == 0) drawTree(c, x, 617, .72f + (i%3)*.08f);
            else if (theme == 1) drawLamp(c, x, 620);
            else drawRuinedWall(c, x, 620, i%2);
        }
    }

    private void drawPlatform(Canvas c, LevelData.Platform platform) {
        RectF r = platform.rect;
        if (platform.style == 0) {
            p.setColor(level.theme == 0 ? Color.rgb(107,75,48) : level.theme == 1 ? Color.rgb(87,75,66) : Color.rgb(83,58,55)); c.drawRect(r,p);
            p.setColor(level.theme == 0 ? Color.rgb(74,150,72) : level.theme == 1 ? Color.rgb(103,115,108) : Color.rgb(131,92,66)); c.drawRect(r.left,r.top,r.right,r.top+15,p);
            p.setColor(Color.argb(55,255,255,255)); c.drawRect(r.left,r.top,r.right,r.top+4,p);
            p.setColor(Color.argb(55,0,0,0)); p.setStrokeWidth(2); for(float x=r.left+22;x<r.right;x+=44) c.drawLine(x,r.top+22,x-15,r.bottom,p);
        } else {
            p.setColor(Color.rgb(70,49,38)); c.drawRoundRect(r,8,8,p);
            p.setColor(level.theme == 0 ? Color.rgb(206,113,58) : level.theme == 1 ? Color.rgb(122,126,129) : Color.rgb(174,78,53)); c.drawRoundRect(new RectF(r.left+3,r.top+3,r.right-3,r.bottom-4),7,7,p);
            p.setColor(Color.argb(70,255,255,255)); c.drawRect(r.left+9,r.top+5,r.right-9,r.top+9,p);
            p.setColor(Color.argb(80,40,20,15)); p.setStrokeWidth(2); for(float x=r.left+40;x<r.right;x+=55)c.drawLine(x,r.top+5,x,r.bottom-5,p);
        }
    }

    private void drawHazard(Canvas c, LevelData.Hazard h) {
        if (h.type == 1) {
            p.setColor(Color.rgb(74,79,85)); c.drawRect(h.rect,p);
            p.setColor(Color.rgb(222,93,47)); for(float x=h.rect.left+12;x<h.rect.right;x+=24)c.drawCircle(x,h.rect.centerY(),7,p);
        } else {
            p.setColor(Color.rgb(82,86,91));
            float step=24; for(float x=h.rect.left;x<h.rect.right;x+=step){ Path path=new Path(); path.moveTo(x,h.rect.bottom); path.lineTo(x+step/2,h.rect.top); path.lineTo(x+step,h.rect.bottom); path.close(); c.drawPath(path,p); }
            p.setColor(Color.rgb(215,220,225)); for(float x=h.rect.left+4;x<h.rect.right;x+=step)c.drawLine(x+step/2,h.rect.top+3,x+step/2,h.rect.bottom-3,p);
        }
    }

    private void drawCrate(Canvas c, RectF r) {
        p.setColor(Color.rgb(67,44,28)); c.drawRoundRect(r,5,5,p);
        p.setColor(Color.rgb(158,100,50)); c.drawRoundRect(new RectF(r.left+4,r.top+4,r.right-4,r.bottom-4),4,4,p);
        p.setColor(Color.rgb(207,142,71)); p.setStrokeWidth(7); c.drawLine(r.left+10,r.top+10,r.right-10,r.bottom-10,p); c.drawLine(r.right-10,r.top+10,r.left+10,r.bottom-10,p);
        p.setColor(Color.argb(70,255,255,255)); c.drawRect(r.left+6,r.top+6,r.right-6,r.top+10,p);
    }

    private void drawEnemy(Canvas c, LevelData.Enemy e) {
        float bob=(float)Math.sin(time*7+e.x*.02f)*2;
        c.save(); c.translate(e.x,e.y+bob); c.scale(e.dir,1);
        p.setColor(Color.argb(55,0,0,0)); c.drawOval(new RectF(-28,-5,28,7),p);
        if(e.type==0){
            p.setColor(Color.rgb(73,55,49)); c.drawOval(new RectF(-27,-44,27,0),p); p.setColor(Color.rgb(108,77,63)); c.drawOval(new RectF(-21,-39,21,-3),p);
            p.setColor(Color.rgb(239,189,151)); c.drawCircle(18,-28,7,p); p.setColor(Color.RED); c.drawCircle(13,-27,2.5f,p); p.setColor(Color.rgb(43,31,28)); c.drawCircle(24,-24,3,p);
            p.setStrokeWidth(2); c.drawLine(20,-21,34,-16,p); c.drawLine(20,-19,34,-20,p);
        }else if(e.type==1){
            p.setColor(Color.rgb(40,46,52)); c.drawRoundRect(new RectF(-25,-43,25,0),12,12,p); p.setColor(Color.rgb(219,145,52)); c.drawRoundRect(new RectF(-19,-37,19,-5),9,9,p);
            p.setColor(Color.WHITE); c.drawOval(new RectF(0,-30,15,-18),p); p.setColor(Color.rgb(25,25,25)); c.drawCircle(9,-24,3,p); p.setColor(Color.rgb(185,45,40)); c.drawRect(-15,-15,13,-10,p);
        }else{
            p.setColor(Color.rgb(38,42,50)); c.drawCircle(0,-24,25,p); p.setColor(Color.rgb(88,96,108)); c.drawCircle(0,-24,18,p); p.setColor(Color.WHITE); c.drawCircle(8,-29,5,p); p.setColor(Color.BLACK); c.drawCircle(10,-29,2,p);
            p.setColor(Color.rgb(190,190,196)); p.setStrokeWidth(5); c.drawLine(-18,-7,-27,4,p); c.drawLine(18,-7,27,4,p);
        }
        c.restore();
    }

    private void drawCheckpoint(Canvas c, float x, boolean active) {
        p.setColor(Color.rgb(87,61,42)); c.drawRect(x-3,500,x+4,620,p);
        p.setColor(active?Color.rgb(57,196,110):Color.rgb(205,169,49)); Path flag=new Path(); flag.moveTo(x+4,503); flag.lineTo(x+58,522); flag.lineTo(x+4,541); flag.close(); c.drawPath(flag,p);
        if(active){p.setColor(Color.argb(70,80,255,150)); c.drawCircle(x+20,522,48,p);}
    }

    private void drawFinishFlag(Canvas c, float x, float groundY) {
        p.setColor(Color.rgb(74,52,37)); c.drawRect(x-4,390,x+5,groundY,p);
        int cell=18; for(int yy=0;yy<4;yy++)for(int xx=0;xx<5;xx++){p.setColor((xx+yy)%2==0?Color.WHITE:Color.rgb(25,27,32)); c.drawRect(x+5+xx*cell,395+yy*cell,x+5+(xx+1)*cell,395+(yy+1)*cell,p);}
    }

    private void drawHud(Canvas c) {
        p.setColor(Color.argb(205,18,22,30)); c.drawRoundRect(new RectF(18,16,460,92),22,22,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(Color.argb(100,255,255,255)); c.drawRoundRect(new RectF(18,16,460,92),22,22,p); p.setStyle(Paint.Style.FILL);
        for(int i=0;i<3;i++) drawHeart(c,46+i*42,52,i<player.lives);
        drawCoin(c,205,53,15,time); text(c,"× " + levelCoins,30,232,64,Color.WHITE,Paint.Align.LEFT,true);
        text(c,"Уровень " + currentLevel,27,430,64,Color.rgb(255,203,50),Paint.Align.RIGHT,true);
        float progress=clamp(player.x/level.finishX,0,1); p.setColor(Color.argb(170,15,18,24)); c.drawRoundRect(new RectF(500,28,950,54),13,13,p); p.setColor(Color.rgb(255,194,44)); c.drawRoundRect(new RectF(504,32,504+438*progress,50),9,9,p);
        text(c,(int)(progress*100)+"%",18,725,76,Color.WHITE,Paint.Align.CENTER,true);
        drawRoundIcon(c,pauseBtn,"Ⅱ");
    }

    private void drawControls(Canvas c) {
        float mul = controlSize == 0 ? .86f : controlSize == 2 ? 1.12f : 1f;
        drawControl(c,scaledControl(leftBtn,mul,100,625),"◀",leftPressed||keyLeft);
        drawControl(c,scaledControl(rightBtn,mul,214,625),"▶",rightPressed||keyRight);
        drawControl(c,scaledControl(jumpBtn,mul,1064,617),"↑",false);
        drawControl(c,scaledControl(hammerBtn,mul,1198,617),"HAMMER",player.hammerTime>0);
    }

    private RectF scaledControl(RectF src,float mul,float cx,float cy){float hw=src.width()*.5f*mul,hh=src.height()*.5f*mul;return new RectF(cx-hw,cy-hh,cx+hw,cy+hh);}

    private void drawControl(Canvas c, RectF r, String symbol, boolean active) {
        p.setColor(active?Color.argb(210,255,186,43):Color.argb(145,20,26,36)); c.drawOval(r,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(active?Color.WHITE:Color.argb(160,255,255,255)); c.drawOval(r,p); p.setStyle(Paint.Style.FILL);
        if ("HAMMER".equals(symbol)) {
            drawHammerIcon(c, r.centerX(), r.centerY(), Math.min(r.width(), r.height()) / 95f);
        } else {
            text(c, symbol, 44, r.centerX(), r.centerY() + 15, Color.WHITE, Paint.Align.CENTER, true);
        }
    }

    private void drawPause(Canvas c) {
        p.setColor(Color.argb(190,5,7,12)); c.drawRect(0,0,W,H,p);
        panel(c,new RectF(380,105,900,625)); text(c,"ПАУЗА",52,640,180,Color.WHITE,Paint.Align.CENTER,true);
        drawButton(c,new RectF(455,225,825,290),"ПРОДОЛЖИТЬ",true,Color.rgb(43,133,91));
        drawButton(c,new RectF(455,315,825,380),"НАСТРОЙКИ",true,Color.rgb(78,82,153));
        drawButton(c,new RectF(455,405,825,470),"СОХРАНИТЬ И ВЫЙТИ",true,Color.rgb(175,103,44));
        drawButton(c,new RectF(455,495,825,560),"НАЧАТЬ УРОВЕНЬ ЗАНОВО",true,Color.rgb(126,57,55));
    }

    private void drawRepair(Canvas c) {
        int stage=currentLevel;
        int skyTop=stage==1?Color.rgb(118,183,222):stage==2?Color.rgb(236,169,92):Color.rgb(75,91,132);
        int skyBottom=stage==1?Color.rgb(239,220,174):stage==2?Color.rgb(126,89,111):Color.rgb(34,44,68);
        p.setShader(new LinearGradient(0,0,0,H,skyTop,skyBottom,Shader.TileMode.CLAMP));c.drawRect(0,0,W,H,p);p.setShader(null);
        text(c,"УРОВЕНЬ " + stage + " ПРОЙДЕН",42,640,70,Color.WHITE,Paint.Align.CENTER,true);
        text(c,"Ремонтируем старый дом",27,640,110,Color.rgb(255,215,88),Paint.Align.CENTER,false);
        p.setColor(Color.rgb(72,111,66));c.drawRect(0,610,W,H,p);
        int before=Math.max(0,stage-1); int after=repairTimer>3.6f?stage:before;
        drawHouse(c,810,585,1.65f,after,true);
        float strike=(repairTimer%1.05f)/1.05f;
        float hp=strike<.55f?strike/.55f:Math.max(0,1-(strike-.55f)/.45f);
        CharacterPainter.draw(c,p,390,620,1.65f,true,time*5,false,hp,false);
        if(strike>.48f&&strike<.68f&&repairTimer<4.2f){for(int i=0;i<8;i++){float a=(float)(i*Math.PI/4);p.setColor(i%2==0?Color.YELLOW:Color.WHITE);c.drawCircle(545+(float)Math.cos(a)*28,420+(float)Math.sin(a)*28,4,p);}}
        float bar=clamp(repairTimer/4.2f,0,1);p.setColor(Color.argb(150,20,22,30));c.drawRoundRect(new RectF(350,655,930,687),16,16,p);p.setColor(Color.rgb(255,192,43));c.drawRoundRect(new RectF(355,660,355+570*bar,682),11,11,p);
        if(repairTimer>4.2f){text(c,stage==3?"Дом полностью восстановлен!":"Часть дома восстановлена",30,640,150,Color.WHITE,Paint.Align.CENTER,true);drawButton(c,new RectF(500,545,780,610),stage==3?"ФИНАЛ":"ДАЛЬШЕ",true,Color.rgb(47,139,91));}
    }

    private void drawVictory(Canvas c) {
        p.setShader(new LinearGradient(0,0,0,H,Color.rgb(75,157,212),Color.rgb(241,207,130),Shader.TileMode.CLAMP));c.drawRect(0,0,W,H,p);p.setShader(null);
        drawCloud(c,120,100,1);drawCloud(c,840,80,1.2f);p.setColor(Color.rgb(72,142,72));c.drawRect(0,590,W,H,p);
        drawHouse(c,820,575,1.7f,3,true);CharacterPainter.draw(c,p,370,620,1.7f,true,time*4,false,0,true);
        p.setColor(Color.argb(210,18,23,32));c.drawRoundRect(new RectF(115,65,1165,195),30,30,p);
        text(c,"ДОМ ВОССТАНОВЛЕН!",56,640,130,Color.WHITE,Paint.Align.CENTER,true);
        text(c,"Все 3 уровня пройдены • собрано монет: " + totalCoins,26,640,174,Color.rgb(255,210,56),Paint.Align.CENTER,false);
        drawButton(c,new RectF(485,620,795,684),"В ГЛАВНОЕ МЕНЮ",true,Color.rgb(49,126,88));
    }

    private void drawGameOver(Canvas c) {
        p.setColor(Color.argb(205,5,7,12));c.drawRect(0,0,W,H,p);panel(c,new RectF(390,145,890,580));text(c,"ПОПРОБУЕМ ЕЩЁ",46,640,225,Color.WHITE,Paint.Align.CENTER,true);
        text(c,"Прогресс сохранён с контрольной точки",23,640,275,Color.rgb(196,205,217),Paint.Align.CENTER,false);
        drawButton(c,new RectF(455,330,825,398),"ПОВТОРИТЬ",true,Color.rgb(47,137,91));drawButton(c,new RectF(455,430,825,498),"В ГЛАВНОЕ МЕНЮ",true,Color.rgb(87,91,102));
    }

    private void drawSimpleBackdrop(Canvas c,String title){p.setShader(new LinearGradient(0,0,0,H,Color.rgb(37,54,75),Color.rgb(15,20,30),Shader.TileMode.CLAMP));c.drawRect(0,0,W,H,p);p.setShader(null);for(int i=0;i<14;i++){p.setColor(Color.argb(20,255,255,255));c.drawCircle(70+i*100,90+(i%3)*60,35+(i%4)*11,p);}text(c,title,52,640,92,Color.WHITE,Paint.Align.CENTER,true);}
    private void panel(Canvas c,RectF r){p.setColor(Color.argb(232,22,28,39));c.drawRoundRect(r,28,28,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.argb(110,255,255,255));c.drawRoundRect(r,28,28,p);p.setStyle(Paint.Style.FILL);}

    private void drawButton(Canvas c,RectF r,String label,boolean enabled,int color){p.setColor(Color.argb(100,0,0,0));c.drawRoundRect(new RectF(r.left+4,r.top+7,r.right+4,r.bottom+7),15,15,p);p.setColor(enabled?color:Color.rgb(72,75,82));c.drawRoundRect(r,15,15,p);p.setColor(enabled?Color.argb(75,255,255,255):Color.argb(30,255,255,255));c.drawRoundRect(new RectF(r.left+5,r.top+4,r.right-5,r.top+12),7,7,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(enabled?Color.argb(125,255,255,255):Color.argb(60,255,255,255));c.drawRoundRect(r,15,15,p);p.setStyle(Paint.Style.FILL);text(c,label,24,r.centerX(),r.centerY()+9,enabled?Color.WHITE:Color.rgb(155,158,164),Paint.Align.CENTER,true);}
    private void drawRoundIcon(Canvas c,RectF r,String label){p.setColor(Color.argb(180,22,27,37));c.drawRoundRect(r,16,16,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.argb(150,255,255,255));c.drawRoundRect(r,16,16,p);p.setStyle(Paint.Style.FILL);text(c,label,28,r.centerX(),r.centerY()+10,Color.WHITE,Paint.Align.CENTER,true);}

    private void text(Canvas c,String s,float size,float x,float y,int color,Paint.Align align,boolean bold){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(size);p.setTextAlign(align);p.setTypeface(android.graphics.Typeface.create("sans-serif-condensed",bold?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL));c.drawText(s,x,y,p);}

    private void drawCoin(Canvas c,float x,float y,float radius,float phase){float squash=.65f+.35f*Math.abs((float)Math.sin(phase*4));c.save();c.translate(x,y);c.scale(squash,1);p.setColor(Color.argb(60,255,211,42));c.drawCircle(0,0,radius+9,p);p.setColor(Color.rgb(154,93,10));c.drawCircle(0,0,radius+2,p);p.setColor(Color.rgb(255,193,28));c.drawCircle(0,0,radius,p);p.setColor(Color.rgb(255,230,92));c.drawCircle(-radius*.25f,-radius*.3f,radius*.45f,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.rgb(211,132,12));c.drawOval(new RectF(-radius*.4f,-radius*.7f,radius*.4f,radius*.7f),p);p.setStyle(Paint.Style.FILL);c.restore();}
    private void drawHeart(Canvas c,float x,float y,boolean full){p.setColor(full?Color.rgb(230,60,65):Color.rgb(77,80,88));Path path=new Path();path.moveTo(x,y+14);path.cubicTo(x-25,y-2,x-17,y-21,x,y-10);path.cubicTo(x+17,y-21,x+25,y-2,x,y+14);path.close();c.drawPath(path,p);if(full){p.setColor(Color.argb(100,255,255,255));c.drawCircle(x-7,y-9,4,p);}}
    private void drawToast(Canvas c){float alpha=clamp(toastTimer,0,1);p.setColor(Color.argb((int)(220*alpha),18,22,30));RectF r=new RectF(420,105,860,160);c.drawRoundRect(r,18,18,p);text(c,toastText,22,640,141,Color.argb((int)(255*alpha),255,255,255),Paint.Align.CENTER,true);}

    private void drawCloud(Canvas c,float x,float y,float s){p.setColor(Color.argb(205,255,255,255));c.drawOval(new RectF(x-55*s,y-18*s,x+65*s,y+24*s),p);c.drawCircle(x-26*s,y-24*s,29*s,p);c.drawCircle(x+12*s,y-35*s,38*s,p);c.drawCircle(x+47*s,y-20*s,27*s,p);}
    private void drawTree(Canvas c,float x,float y,float s){p.setColor(Color.rgb(90,59,39));c.drawRoundRect(new RectF(x-9*s,y-105*s,x+10*s,y),5,5,p);p.setColor(Color.rgb(49,112,58));c.drawCircle(x,y-115*s,44*s,p);p.setColor(Color.rgb(67,139,67));c.drawCircle(x-25*s,y-105*s,30*s,p);c.drawCircle(x+30*s,y-100*s,34*s,p);p.setColor(Color.argb(55,255,255,255));c.drawCircle(x-13*s,y-130*s,15*s,p);}
    private void drawGrass(Canvas c,float x,float y,float s){p.setColor(Color.rgb(48,105,51));p.setStrokeWidth(3*s);c.drawLine(x,y,x-9*s,y-21*s,p);c.drawLine(x,y,x+2*s,y-25*s,p);c.drawLine(x,y,x+12*s,y-18*s,p);}
    private void drawLamp(Canvas c,float x,float y){p.setColor(Color.rgb(54,59,65));c.drawRect(x-4,y-115,x+5,y,p);c.drawRoundRect(new RectF(x-18,y-145,x+19,y-108),8,8,p);p.setColor(Color.rgb(255,220,112));c.drawCircle(x,y-127,10,p);p.setColor(Color.argb(45,255,225,120));c.drawCircle(x,y-127,28,p);}
    private void drawRuinedWall(Canvas c,float x,float y,int variant){p.setColor(Color.rgb(92,66,58));float h=70+variant*25;c.drawRect(x-55,y-h,x+65,y,p);p.setColor(Color.rgb(150,91,65));for(int yy=0;yy<(int)h;yy+=20)for(int xx=-50;xx<60;xx+=34)c.drawRect(x+xx+(yy/20%2)*14,y-h+yy,x+xx+28+(yy/20%2)*14,y-h+yy+15,p);}

    private void drawHouse(Canvas c,float x,float ground,float s,int repairStage,boolean detail){c.save();c.translate(x,ground);c.scale(s,s);p.setColor(Color.argb(55,0,0,0));c.drawOval(new RectF(-130,-12,130,12),p);p.setColor(Color.rgb(72,48,37));c.drawRect(-105,-150,105,0,p);int wall=repairStage>=1?Color.rgb(231,205,154):Color.rgb(165,143,112);p.setColor(wall);c.drawRect(-98,-143,98,0,p);p.setColor(repairStage>=2?Color.rgb(176,61,43):Color.rgb(103,72,62));Path roof=new Path();roof.moveTo(-125,-142);roof.lineTo(0,-238);roof.lineTo(125,-142);roof.close();c.drawPath(roof,p);if(repairStage<2){p.setColor(Color.rgb(65,54,50));Path hole=new Path();hole.moveTo(18,-222);hole.lineTo(55,-193);hole.lineTo(35,-166);hole.lineTo(3,-190);hole.close();c.drawPath(hole,p);}p.setColor(Color.rgb(82,54,38));c.drawRect(-22,-86,25,0,p);p.setColor(repairStage>=3?Color.rgb(93,177,206):Color.rgb(70,91,98));c.drawRect(-78,-112,-37,-73,p);c.drawRect(42,-112,82,-73,p);p.setColor(Color.rgb(238,229,186));p.setStrokeWidth(4);c.drawLine(-57,-112,-57,-73,p);c.drawLine(-78,-92,-37,-92,p);c.drawLine(62,-112,62,-73,p);c.drawLine(42,-92,82,-92,p);if(repairStage<1){p.setColor(Color.rgb(80,69,58));p.setStrokeWidth(5);c.drawLine(-92,-45,-57,-61,p);c.drawLine(45,-28,90,-50,p);}if(repairStage>=3){p.setColor(Color.rgb(255,212,61));c.drawCircle(-57,-92,8,p);c.drawCircle(62,-92,8,p);p.setColor(Color.rgb(61,137,65));for(int i=0;i<5;i++)c.drawCircle(-100+i*50,3,15,p);}if(detail){p.setColor(Color.argb(45,255,255,255));c.drawRect(-91,-136,91,-128,p);}c.restore();}

    private void drawHammerIcon(Canvas c, float x, float y, float s) {
        c.save();
        c.translate(x, y);
        c.rotate(-38);
        p.setColor(Color.rgb(103, 68, 43));
        c.drawRoundRect(new RectF(-4 * s, -4 * s, 5 * s, 34 * s), 3 * s, 3 * s, p);
        p.setColor(Color.rgb(232, 237, 242));
        c.drawRoundRect(new RectF(-18 * s, -12 * s, 20 * s, 2 * s), 5 * s, 5 * s, p);
        p.setColor(Color.rgb(126, 135, 145));
        c.drawRoundRect(new RectF(-16 * s, -10 * s, 18 * s, -6 * s), 3 * s, 3 * s, p);
        c.restore();
    }

    private void drawLockIcon(Canvas c, float x, float y, float s) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(7 * s);
        p.setColor(Color.WHITE);
        c.drawArc(new RectF(x - 23 * s, y - 34 * s, x + 23 * s, y + 10 * s), 195, 150, false, p);
        p.setStyle(Paint.Style.FILL);
        c.drawRoundRect(new RectF(x - 31 * s, y - 4 * s, x + 31 * s, y + 40 * s), 8 * s, 8 * s, p);
        p.setColor(Color.rgb(60, 63, 70));
        c.drawCircle(x, y + 14 * s, 6 * s, p);
        c.drawRoundRect(new RectF(x - 3 * s, y + 14 * s, x + 3 * s, y + 29 * s), 2 * s, 2 * s, p);
    }

    private boolean contains(RectF r,float x,float y){return r.contains(x,y);}

    @Override public boolean onTouchEvent(MotionEvent event) {
        float scale=Math.min(getWidth()/W,getHeight()/H);float ox=(getWidth()-W*scale)*.5f;float oy=(getHeight()-H*scale)*.5f;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_CANCEL) {
            pointers.clear();
            recomputeControls();
            menuTapLock = false;
            return true;
        }
        int index = event.getActionIndex();
        int id = event.getPointerId(index);
        float x = (event.getX(index) - ox) / scale;
        float y = (event.getY(index) - oy) / scale;
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            handleTouchDown(id, x, y);
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            pointers.delete(id);
            recomputeControls();
            menuTapLock = false;
        }
        return true;
    }

    private void handleTouchDown(int id,float x,float y){
        if(screen==Screen.GAME){
            if(contains(pauseBtn,x,y)){screen=Screen.PAUSE;updateMusic();return;}
            int code=0;if(contains(scaledControl(leftBtn,controlSize==0?.86f:controlSize==2?1.12f:1f,100,625),x,y))code=1;else if(contains(scaledControl(rightBtn,controlSize==0?.86f:controlSize==2?1.12f:1f,214,625),x,y))code=2;else if(contains(scaledControl(jumpBtn,controlSize==0?.86f:controlSize==2?1.12f:1f,1064,617),x,y)){code=3;jumpQueued=true;}else if(contains(scaledControl(hammerBtn,controlSize==0?.86f:controlSize==2?1.12f:1f,1198,617),x,y)){code=4;hammerQueued=true;}if(code>0){pointers.put(id,code);recomputeControls();performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);return;}
        }
        if(menuTapLock)return;menuTapLock=true;
        switch(screen){
            case MENU: menuTouch(x,y); break;
            case SETTINGS: settingsTouch(x,y); break;
            case LEVEL_SELECT: levelSelectTouch(x,y); break;
            case PAUSE: pauseTouch(x,y); break;
            case REPAIR: if(repairTimer>4.2f&&contains(new RectF(500,545,780,610),x,y))continueAfterRepair(); break;
            case VICTORY: if(contains(new RectF(485,620,795,684),x,y))screen=Screen.MENU; break;
            case GAME_OVER: if(contains(new RectF(455,330,825,398),x,y))startLevel(currentLevel,true);else if(contains(new RectF(455,430,825,498),x,y))screen=Screen.MENU; break;
            default: break;
        }
    }

    private void recomputeControls(){leftPressed=false;rightPressed=false;for(int i=0;i<pointers.size();i++){int v=pointers.valueAt(i);if(v==1)leftPressed=true;if(v==2)rightPressed=true;}}

    private void menuTouch(float x,float y){float yy=265;RectF r=new RectF(85,yy,505,yy+66);if(contains(r,x,y)){startNewGame();return;}yy+=78;r.set(85,yy,505,yy+66);if(contains(r,x,y)&&hasSave){startLevel(prefs.getInt("saved_level",1),true);return;}yy+=78;r.set(85,yy,505,yy+66);if(contains(r,x,y)){screen=Screen.LEVEL_SELECT;return;}yy+=78;r.set(85,yy,505,yy+66);if(contains(r,x,y)){settingsReturn=Screen.MENU;screen=Screen.SETTINGS;return;}yy+=78;r.set(85,yy,505,yy+66);if(contains(r,x,y)){activity.finishAffinity();}}

    private void settingsTouch(float x,float y){if(contains(new RectF(790,210,945,268),x,y)){soundEnabled=!soundEnabled;saveSettings();}else if(contains(new RectF(790,295,945,353),x,y)){musicEnabled=!musicEnabled;saveSettings();updateMusic();}else if(contains(new RectF(790,380,945,438),x,y)){vibrationEnabled=!vibrationEnabled;saveSettings();}else if(contains(new RectF(690,430,950,490),x,y)){controlSize=(controlSize+1)%3;saveSettings();}else if(contains(new RectF(315,520,600,580),x,y)){prefs.edit().clear().apply();loadPrefs();showToast("Прогресс сброшен");updateMusic();}else if(contains(new RectF(680,520,950,580),x,y)){screen=settingsReturn;updateMusic();}}
    private void saveSettings(){prefs.edit().putBoolean("sound",soundEnabled).putBoolean("music",musicEnabled).putBoolean("vibration",vibrationEnabled).putInt("control_size",controlSize).apply();}

    private void levelSelectTouch(float x,float y){for(int i=1;i<=3;i++){float cx=100+(i-1)*390;if(i<=unlockedLevel&&contains(new RectF(cx+45,485,cx+275,535),x,y)){startLevel(i,false);return;}}if(contains(new RectF(500,620,780,682),x,y))screen=Screen.MENU;}
    private void pauseTouch(float x,float y){if(contains(new RectF(455,225,825,290),x,y)){screen=Screen.GAME;updateMusic();}else if(contains(new RectF(455,315,825,380),x,y)){settingsReturn=Screen.PAUSE;screen=Screen.SETTINGS;}else if(contains(new RectF(455,405,825,470),x,y)){saveCurrent();screen=Screen.MENU;updateMusic();}else if(contains(new RectF(455,495,825,560),x,y)){startLevel(currentLevel,false);}}

    @Override public boolean onKeyDown(int keyCode,KeyEvent event){if(keyCode==KeyEvent.KEYCODE_A||keyCode==KeyEvent.KEYCODE_DPAD_LEFT){keyLeft=true;return true;}if(keyCode==KeyEvent.KEYCODE_D||keyCode==KeyEvent.KEYCODE_DPAD_RIGHT){keyRight=true;return true;}if(keyCode==KeyEvent.KEYCODE_SPACE||keyCode==KeyEvent.KEYCODE_W||keyCode==KeyEvent.KEYCODE_DPAD_UP){jumpQueued=true;return true;}if(keyCode==KeyEvent.KEYCODE_E||keyCode==KeyEvent.KEYCODE_CTRL_LEFT){hammerQueued=true;return true;}if(keyCode==KeyEvent.KEYCODE_ESCAPE||keyCode==KeyEvent.KEYCODE_P){handleBack();return true;}return super.onKeyDown(keyCode,event);}
    @Override public boolean onKeyUp(int keyCode,KeyEvent event){if(keyCode==KeyEvent.KEYCODE_A||keyCode==KeyEvent.KEYCODE_DPAD_LEFT){keyLeft=false;return true;}if(keyCode==KeyEvent.KEYCODE_D||keyCode==KeyEvent.KEYCODE_DPAD_RIGHT){keyRight=false;return true;}return super.onKeyUp(keyCode,event);}

    public boolean handleBack(){if(screen==Screen.GAME){screen=Screen.PAUSE;updateMusic();return true;}if(screen==Screen.PAUSE){screen=Screen.GAME;updateMusic();return true;}if(screen==Screen.SETTINGS){screen=settingsReturn;updateMusic();return true;}if(screen==Screen.LEVEL_SELECT||screen==Screen.VICTORY||screen==Screen.GAME_OVER){screen=Screen.MENU;updateMusic();return true;}if(screen==Screen.REPAIR){return true;}return false;}

    @Override protected void onDetachedFromWindow(){running=false;if(soundPool!=null)soundPool.release();if(music!=null){music.stop();music.release();}super.onDetachedFromWindow();}

    private static final class Player {float x,y,vx,vy,invincible,hammerTime,coyote;int lives=3;boolean grounded,facingRight=true;}
}
