package one.cafebabe.game;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ShootingGame extends JPanel implements Runnable {
    // Constants
    private static final int WINDOW_WIDTH = 600;
    private static final int WINDOW_HEIGHT = 600;
    private static final int FPS = 60;
    private static final long FRAME_TIME = 1000 / FPS;

    // Game states
    private enum GameState {
        TITLE, PLAYING, GAME_OVER
    }

    // Game variables
    private GameState gameState = GameState.TITLE;
    private Thread gameThread;
    private boolean running = false;
    private int score = 0;
    private long gameStartTime = 0;
    private long gameOverTime = 0;
    private Random random = new Random();

    // Input handling
    private boolean[] keys = new boolean[256];

    // Game objects
    private Fighter fighter;
    private List<Bullet> bullets = new ArrayList<>();
    private List<Enemy> enemies = new ArrayList<>();
    private List<EnemyBullet> enemyBullets = new ArrayList<>();
    private Boss boss;
    private List<Explosion> explosions = new ArrayList<>();

    // Resources
    private BufferedImage fighterImage;
    private BufferedImage enemyImage;
    private BufferedImage bossImage;
    private BufferedImage explosionImage1;
    private BufferedImage explosionImage2;

    // Title screen variables
    private float titleFighterX = WINDOW_WIDTH / 2.0f;
    private float titleFighterY = WINDOW_HEIGHT / 2.0f + 50;
    private float titleFighterVelocityX = 0;
    private boolean titleTransitioning = false;
    private long titleTransitionStartTime = 0;

    public ShootingGame() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        // Set up key listener
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                keys[e.getKeyCode()] = true;
                handleKeyPress(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                keys[e.getKeyCode()] = false;
            }
        });

        // Load resources
        loadResources();

        // Initialize game objects
        fighter = new Fighter(WINDOW_WIDTH / 4, WINDOW_HEIGHT / 2);
    }

    private void loadResources() {
        try {
            fighterImage = ImageIO.read(getClass().getResourceAsStream("/fighter.png"));
            enemyImage = ImageIO.read(getClass().getResourceAsStream("/enemy.png"));
            bossImage = ImageIO.read(getClass().getResourceAsStream("/boss.png"));
            explosionImage1 = ImageIO.read(getClass().getResourceAsStream("/explosion1.png"));
            explosionImage2 = ImageIO.read(getClass().getResourceAsStream("/explosion2.png"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void handleKeyPress(int keyCode) {
        if (keyCode == KeyEvent.VK_SPACE) {
            switch (gameState) {
                case TITLE:
                    if (!titleTransitioning) {
                        playSound("/jingle_original_interval_003.wav");
                        titleTransitioning = true;
                        titleTransitionStartTime = System.currentTimeMillis();
                    }
                    break;
                case PLAYING:
                    fighter.shoot();
                    break;
                case GAME_OVER:
                    if (System.currentTimeMillis() - gameOverTime > 3000) {
                        resetGame();
                    }
                    break;
            }
        }
    }

    private void resetGame() {
        gameState = GameState.TITLE;
        score = 0;
        titleFighterX = WINDOW_WIDTH / 2.0f;
        titleFighterY = WINDOW_HEIGHT / 2.0f + 50;
        titleFighterVelocityX = 0;
        titleTransitioning = false;
        fighter = new Fighter(WINDOW_WIDTH / 4, WINDOW_HEIGHT / 2);
        bullets.clear();
        enemies.clear();
        enemyBullets.clear();
        boss = null;
        explosions.clear();
    }

    public void start() {
        if (gameThread == null || !running) {
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    public void stop() {
        running = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long lastTime = System.currentTimeMillis();

        while (running) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastTime;

            update(elapsedTime);
            repaint();

            lastTime = currentTime;

            try {
                long sleepTime = FRAME_TIME - (System.currentTimeMillis() - currentTime);
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update(long elapsedTime) {
        float deltaTime = elapsedTime / 1000.0f;

        switch (gameState) {
            case TITLE:
                updateTitle(deltaTime);
                break;
            case PLAYING:
                updatePlaying(deltaTime);
                break;
            case GAME_OVER:
                updateGameOver(deltaTime);
                break;
        }
    }

    private void updateTitle(float deltaTime) {
        if (titleTransitioning) {
            titleFighterVelocityX += 200 * deltaTime;
            titleFighterX += titleFighterVelocityX * deltaTime;

            if (titleFighterX > WINDOW_WIDTH + 50) {
                gameState = GameState.PLAYING;
                gameStartTime = System.currentTimeMillis();
                fighter = new Fighter(WINDOW_WIDTH / 4, WINDOW_HEIGHT / 2);
            }
        } else {
            // Make the fighter float up and down slightly
            titleFighterY += Math.sin(System.currentTimeMillis() / 500.0) * 0.5;
        }
    }

    private void updatePlaying(float deltaTime) {
        long gameTime = System.currentTimeMillis() - gameStartTime;

        // Update fighter
        updateFighter(deltaTime);

        // Update bullets
        updateBullets(deltaTime);

        // Update enemies
        updateEnemies(deltaTime, gameTime);

        // Update boss
        updateBoss(deltaTime, gameTime);

        // Update enemy bullets
        updateEnemyBullets(deltaTime);

        // Update explosions
        updateExplosions(deltaTime);

        // Check collisions
        checkCollisions();
    }

    private void updateFighter(float deltaTime) {
        // Movement
        int dx = 0, dy = 0;

        if (keys[KeyEvent.VK_UP]) dy -= 1;
        if (keys[KeyEvent.VK_DOWN]) dy += 1;
        if (keys[KeyEvent.VK_LEFT]) dx -= 1;
        if (keys[KeyEvent.VK_RIGHT]) dx += 1;

        fighter.move(dx, dy, deltaTime);

        // Keep fighter within bounds
        fighter.x = Math.max(0, Math.min(WINDOW_WIDTH - fighter.width, fighter.x));
        fighter.y = Math.max(0, Math.min(WINDOW_HEIGHT - fighter.height, fighter.y));
    }

    private void updateBullets(float deltaTime) {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet bullet = it.next();
            bullet.update(deltaTime);

            if (bullet.x > WINDOW_WIDTH) {
                it.remove();
            }
        }
    }

    private void updateEnemies(float deltaTime, long gameTime) {
        // Spawn enemies
        if (gameTime < 10000 && enemies.size() < 5 && random.nextFloat() < 0.02) {
            int maxEnemies = (int) Math.min(5, 1 + gameTime / 2000);
            if (enemies.size() < maxEnemies) {
                enemies.add(new Enemy(WINDOW_WIDTH + 20, random.nextInt(WINDOW_HEIGHT - 40) + 20));
            }
        }

        // Update existing enemies
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy enemy = it.next();
            enemy.update(deltaTime);

            // Remove enemies that have left the screen
            if (enemy.x < -enemy.width) {
                it.remove();
                continue;
            }

            // Enemy shooting (after 5 seconds)
            if (gameTime > 5000 && random.nextFloat() < 0.005) {
                enemyBullets.add(new EnemyBullet(enemy.x, enemy.y + enemy.height / 2));
            }
        }
    }

    private void updateBoss(float deltaTime, long gameTime) {
        // Spawn boss after 10 seconds
        if (gameTime > 10000 && boss == null && enemies.isEmpty()) {
            boss = new Boss(WINDOW_WIDTH + 50, WINDOW_HEIGHT / 2);
        }

        if (boss != null) {
            boss.update(deltaTime);

            // Boss shooting
            if (random.nextFloat() < 0.05 && enemyBullets.size() < 5) {
                float targetX = fighter.x + random.nextInt(100) - 50;
                float targetY = fighter.y + random.nextInt(100) - 50;

                float dx = targetX - boss.x;
                float dy = targetY - boss.y;
                float length = (float) Math.sqrt(dx * dx + dy * dy);

                if (length > 0) {
                    dx /= length;
                    dy /= length;
                }

                enemyBullets.add(new EnemyBullet(boss.x, boss.y + boss.height / 2, dx, dy));
            }
        }
    }

    private void updateEnemyBullets(float deltaTime) {
        Iterator<EnemyBullet> it = enemyBullets.iterator();
        while (it.hasNext()) {
            EnemyBullet bullet = it.next();
            bullet.update(deltaTime);

            if (bullet.x < -bullet.radius || bullet.x > WINDOW_WIDTH + bullet.radius ||
                bullet.y < -bullet.radius || bullet.y > WINDOW_HEIGHT + bullet.radius) {
                it.remove();
            }
        }
    }

    private void updateExplosions(float deltaTime) {
        Iterator<Explosion> it = explosions.iterator();
        while (it.hasNext()) {
            Explosion explosion = it.next();
            explosion.update(deltaTime);

            if (explosion.isFinished()) {
                it.remove();
            }
        }
    }

    private void checkCollisions() {
        // Check fighter collision with enemy bullets
        for (EnemyBullet bullet : enemyBullets) {
            if (distance(fighter.x + fighter.width / 2, fighter.y + fighter.height / 2,
                    bullet.x, bullet.y) < bullet.radius) {
                gameOver();
                return;
            }
        }

        // Check fighter collision with enemies
        for (Enemy enemy : enemies) {
            if (distance(fighter.x + fighter.width / 2, fighter.y + fighter.height / 2,
                    enemy.x + enemy.width / 2, enemy.y + enemy.height / 2) < 5) {
                gameOver();
                return;
            }
        }

        // Check fighter collision with boss
        if (boss != null) {
            if (distance(fighter.x + fighter.width / 2, fighter.y + fighter.height / 2,
                    boss.x + boss.width / 2, boss.y + boss.height / 2) < 5) {
                gameOver();
                return;
            }
        }

        // Check bullet collision with enemies
        Iterator<Bullet> bulletIt = bullets.iterator();
        while (bulletIt.hasNext()) {
            Bullet bullet = bulletIt.next();

            Iterator<Enemy> enemyIt = enemies.iterator();
            boolean hit = false;

            while (enemyIt.hasNext() && !hit) {
                Enemy enemy = enemyIt.next();

                if (bullet.x + bullet.radius > enemy.x &&
                    bullet.x - bullet.radius < enemy.x + enemy.width &&
                    bullet.y + bullet.radius > enemy.y &&
                    bullet.y - bullet.radius < enemy.y + enemy.height) {

                    // Enemy hit
                    playSound("/se_hit_007.wav");
                    explosions.add(new Explosion(enemy.x, enemy.y, enemy.width, enemy.height, false));
                    enemyIt.remove();
                    bulletIt.remove();
                    score += 10;
                    hit = true;
                }
            }

            // Check bullet collision with boss
            if (!hit && boss != null) {
                if (bullet.x + bullet.radius > boss.x &&
                    bullet.x - bullet.radius < boss.x + boss.width &&
                    bullet.y + bullet.radius > boss.y &&
                    bullet.y - bullet.radius < boss.y + boss.height) {

                    // Boss hit
                    boss.hit();
                    bulletIt.remove();

                    if (boss.getHits() < 10) {
                        playSound("/se_shot_003.wav");
                    } else {
                        // Boss defeated
                        playBossDefeatSound();
                        explosions.add(new Explosion(boss.x, boss.y, boss.width, boss.height, true));
                        score += 100;
                        boss = null;
                        gameOver();
                    }
                }
            }

        }
    }

    private void gameOver() {
        gameState = GameState.GAME_OVER;
        gameOverTime = System.currentTimeMillis();

        // Clear all enemies and bullets
        enemies.clear();
        enemyBullets.clear();
        boss = null;
    }

    private void updateGameOver(float deltaTime) {
        // Update any remaining explosions
        updateExplosions(deltaTime);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (gameState) {
            case TITLE:
                drawTitle(g2d);
                break;
            case PLAYING:
                drawGame(g2d);
                break;
            case GAME_OVER:
                drawGameOver(g2d);
                break;
        }
    }

    private void drawTitle(Graphics2D g2d) {
        // Draw title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        FontMetrics fm = g2d.getFontMetrics();
        String title = "The Junie Fighter";
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, (WINDOW_WIDTH - titleWidth) / 2, 100);

        // Draw fighter
        g2d.drawImage(fighterImage,
                (int) titleFighterX - fighterImage.getWidth(),
                (int) titleFighterY - fighterImage.getHeight() / 2,
                fighterImage.getWidth() * 2,
                fighterImage.getHeight() * 2,
                null);

        // Draw instructions
        if (!titleTransitioning) {
            g2d.setFont(new Font("Arial", Font.PLAIN, 18));
            String instructions = "Press SPACE to start";
            int instructionsWidth = g2d.getFontMetrics().stringWidth(instructions);
            g2d.drawString(instructions, (WINDOW_WIDTH - instructionsWidth) / 2, WINDOW_HEIGHT - 100);
        }
    }

    private void drawGame(Graphics2D g2d) {
        // Draw score
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("Score: " + score, 20, 30);

        // Draw fighter
        g2d.drawImage(fighterImage,
                (int) fighter.x,
                (int) fighter.y,
                fighter.width,
                fighter.height,
                null);

        // Draw bullets
        g2d.setColor(Color.WHITE);
        for (Bullet bullet : bullets) {
            g2d.fillOval((int) (bullet.x - bullet.radius),
                    (int) (bullet.y - bullet.radius),
                    (int) (bullet.radius * 2),
                    (int) (bullet.radius * 2));
        }

        // Draw enemies
        for (Enemy enemy : enemies) {
            g2d.drawImage(enemyImage,
                    (int) enemy.x,
                    (int) enemy.y,
                    enemy.width,
                    enemy.height,
                    null);
        }

        // Draw boss
        if (boss != null) {
            g2d.drawImage(bossImage,
                    (int) boss.x,
                    (int) boss.y,
                    boss.width,
                    boss.height,
                    null);
        }

        // Draw enemy bullets
        g2d.setColor(Color.RED);
        for (EnemyBullet bullet : enemyBullets) {
            g2d.fillOval((int) (bullet.x - bullet.radius),
                    (int) (bullet.y - bullet.radius),
                    (int) (bullet.radius * 2),
                    (int) (bullet.radius * 2));
        }

        // Draw explosions
        for (Explosion explosion : explosions) {
            BufferedImage img = explosion.getCurrentFrame() == 0 ? explosionImage1 : explosionImage2;
            g2d.drawImage(img,
                    (int) explosion.x,
                    (int) explosion.y,
                    explosion.width,
                    explosion.height,
                    null);
        }
    }

    private void drawGameOver(Graphics2D g2d) {
        // Draw remaining explosions
        for (Explosion explosion : explosions) {
            BufferedImage img = explosion.getCurrentFrame() == 0 ? explosionImage1 : explosionImage2;
            g2d.drawImage(img,
                    (int) explosion.x,
                    (int) explosion.y,
                    explosion.width,
                    explosion.height,
                    null);
        }

        // Draw game over text
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g2d.getFontMetrics();
        String gameOverText = "GAME OVER";
        int textWidth = fm.stringWidth(gameOverText);
        g2d.drawString(gameOverText, (WINDOW_WIDTH - textWidth) / 2, WINDOW_HEIGHT / 2 - 50);

        // Draw score
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        fm = g2d.getFontMetrics();
        String scoreText = "Score: " + score;
        textWidth = fm.stringWidth(scoreText);
        g2d.drawString(scoreText, (WINDOW_WIDTH - textWidth) / 2, WINDOW_HEIGHT / 2 + 20);

        // Draw restart instructions (after 3 seconds)
        if (System.currentTimeMillis() - gameOverTime > 3000) {
            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            String restartText = "Press SPACE to restart";
            textWidth = fm.stringWidth(restartText);
            g2d.drawString(restartText, (WINDOW_WIDTH - textWidth) / 2, WINDOW_HEIGHT / 2 + 80);
        }
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void playSound(String soundFile) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                    getClass().getResource(soundFile));
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playBossDefeatSound() {
        new Thread(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    playSound("/se_hit_010.wav");
                    Thread.sleep(200);
                    playSound("/se_hit_012.wav");
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("The Junie Fighter");
            ShootingGame game = new ShootingGame();
            frame.add(game);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.start();
        });
    }

    // Game object classes

    private class Fighter {
        private float x, y;
        private int width, height;
        private float speed = 200;
        private long lastShotTime = 0;

        public Fighter(float x, float y) {
            this.x = x;
            this.y = y;
            this.width = fighterImage.getWidth() * 2;
            this.height = fighterImage.getHeight() * 2;
        }

        public void move(int dx, int dy, float deltaTime) {
            if (dx != 0 || dy != 0) {
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                dx /= length;
                dy /= length;

                x += dx * speed * deltaTime;
                y += dy * speed * deltaTime;
            }
        }

        public void shoot() {
            long currentTime = System.currentTimeMillis();

            // Limit shooting rate and max bullets
            if (currentTime - lastShotTime > 250 && bullets.size() < 2) {
                bullets.add(new Bullet(x + width, y + height / 2));
                lastShotTime = currentTime;
                playSound("/se_shot_001.wav");
            }
        }
    }

    private class Bullet {
        private float x, y;
        private float velocityX = 400;
        private float radius = 5;

        public Bullet(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public void update(float deltaTime) {
            x += velocityX * deltaTime;
        }
    }

    private class Enemy {
        private float x, y;
        private float baseY;
        private float time = 0;
        private float speed = 100;
        private int width, height;

        public Enemy(float x, float y) {
            this.x = x;
            this.y = y;
            this.baseY = y;
            this.width = enemyImage.getWidth() * 2;
            this.height = enemyImage.getHeight() * 2;
        }

        public void update(float deltaTime) {
            time += deltaTime;
            x -= speed * deltaTime;
            y = baseY + (float) Math.sin(time * 3) * 50;
        }
    }

    private class EnemyBullet {
        private float x, y;
        private float velocityX = -200;
        private float velocityY = 0;
        private float radius = 5;

        public EnemyBullet(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public EnemyBullet(float x, float y, float dirX, float dirY) {
            this.x = x;
            this.y = y;
            float speed = fighter.speed * 2;
            this.velocityX = dirX * speed;
            this.velocityY = dirY * speed;
        }

        public void update(float deltaTime) {
            x += velocityX * deltaTime;
            y += velocityY * deltaTime;
        }
    }

    private class Boss {
        private float x, y;
        private float targetY;
        private float speed = 150;
        private int width, height;
        private int hits = 0;
        private int state = 0; // 0: normal, 1: moving left, 2: waiting, 3: moving right
        private float stateTime = 0;

        public Boss(float x, float y) {
            this.x = x;
            this.y = y;
            this.targetY = y;
            this.width = bossImage.getWidth() * 8;
            this.height = bossImage.getHeight() * 8;
        }

        public void update(float deltaTime) {
            stateTime += deltaTime;

            switch (state) {
                case 0: // Normal state - move up and down
                    if (Math.abs(y - targetY) < 5) {
                        targetY = random.nextInt(WINDOW_HEIGHT - height - 100) + 50;
                    }

                    float dy = targetY - y;
                    y += Math.signum(dy) * Math.min(Math.abs(dy), speed * deltaTime);

                    // Occasionally move left
                    if (random.nextFloat() < 0.005) {
                        state = 1;
                        stateTime = 0;
                    }

                    // Move into screen at start
                    if (x > WINDOW_WIDTH - width - 20) {
                        x -= speed * deltaTime;
                    }
                    break;

                case 1: // Moving left
                    x -= speed * deltaTime;
                    if (x <= 0) {
                        state = 2;
                        stateTime = 0;
                    }
                    break;

                case 2: // Waiting at left
                    if (stateTime >= 1.0f) {
                        state = 3;
                        stateTime = 0;
                    }
                    break;

                case 3: // Moving right
                    x += speed * deltaTime;
                    if (x >= WINDOW_WIDTH - width - 20) {
                        x = WINDOW_WIDTH - width - 20;
                        state = 0;
                        stateTime = 0;
                    }
                    break;
            }
        }

        public void hit() {
            hits++;
        }

        public int getHits() {
            return hits;
        }
    }

    private class Explosion {
        private float x, y;
        private int width, height;
        private float time = 0;
        private float duration = 0.8f;
        private int currentFrame = 0;
        private float frameTime = 0;
        private boolean isBoss;

        public Explosion(float x, float y, int width, int height, boolean isBoss) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.isBoss = isBoss;
        }

        public void update(float deltaTime) {
            time += deltaTime;
            frameTime += deltaTime;

            if (frameTime >= 0.1f) { 
                currentFrame = (currentFrame + 1) % 2;
                frameTime = 0;
            }
        }

        public boolean isFinished() {
            return time >= duration;
        }

        public int getCurrentFrame() {
            return currentFrame;
        }
    }
}