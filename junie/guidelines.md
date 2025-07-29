# The Junie Fighter - Development Guidelines

## Project Overview

"The Junie Fighter" is a simple 2D shooting game implemented in Java using Swing. The player controls a spaceship that can move in four directions and shoot bullets to destroy enemies and ultimately defeat a boss.

## Game Structure

The game consists of three main states:
1. **Title Screen**: Displays the game title and waits for player input to start
2. **Playing**: The main gameplay where the player fights enemies and the boss
3. **Game Over**: Displayed when the player is hit by an enemy or enemy bullet, or when the boss is defeated

## Code Organization

The game is implemented in a single Java class `ShootingGame` that extends `JPanel` and implements `Runnable`. The game objects are implemented as inner classes:

- **Fighter**: The player-controlled spaceship
- **Bullet**: Projectiles fired by the player
- **Enemy**: Basic enemy spaceships that move across the screen
- **EnemyBullet**: Projectiles fired by enemies and the boss
- **Boss**: A larger enemy that appears after a certain time
- **Explosion**: Visual effect when enemies or the boss are destroyed

## Game Mechanics

### Game Loop
- The game runs at 60 FPS (frames per second)
- Each frame, the game updates all game objects and then redraws the screen

### Player Controls
- Arrow keys to move the fighter
- Space bar to shoot bullets
- Space bar to start the game from the title screen
- Space bar to restart after game over (after a 3-second delay)

### Enemy Behavior
- Regular enemies move from right to left in a sine wave pattern
- Enemies can shoot bullets at the player
- The boss appears after 10 seconds and has more complex movement patterns
- The boss requires 10 hits to defeat

### Collision Detection
- Collisions are checked between:
  - Player bullets and enemies
  - Player bullets and the boss
  - Player and enemy bullets
  - Player and enemies
  - Player and the boss

## Resources

The game uses the following resources:
- Images: fighter.png, enemy.png, boss.png, explosion1.png, explosion2.png
- Sound effects: Various .wav files for shooting, explosions, and game events

## Extension Guidelines

### Adding New Enemy Types
To add a new enemy type:
1. Create a new inner class similar to the `Enemy` class
2. Implement the `update` method to define its movement pattern
3. Add code to spawn the new enemy type in the `updateEnemies` method
4. Add collision detection in the `checkCollisions` method

### Adding Power-ups
To add power-ups:
1. Create a new inner class for the power-up
2. Implement spawning logic in the `updatePlaying` method
3. Add collision detection with the player in the `checkCollisions` method
4. Implement the power-up effect when collected

### Improving Graphics
To enhance the visual appearance:
1. Replace the image resources with higher quality versions
2. Add background images or animations
3. Implement particle effects for explosions and engine thrust

## Performance Considerations

- The game uses simple collision detection based on distance calculations
- All game objects are stored in ArrayLists and updated each frame
- Unused objects (bullets that leave the screen, destroyed enemies) are removed to conserve memory

## Testing

When making changes to the game:
1. Test all game states (title, playing, game over)
2. Verify that collision detection works correctly
3. Check that the game runs at a consistent frame rate
4. Ensure that sound effects play at the appropriate times

## Building and Running

The game uses Gradle as its build system. To build and run the game:
1. Ensure you have Java installed
2. Run `./gradlew build` to build the project
3. Run `./gradlew run` to start the game

## License

Please refer to the LICENSE file in the project root for licensing information.