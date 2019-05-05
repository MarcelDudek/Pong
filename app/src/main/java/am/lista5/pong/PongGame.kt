package am.lista5.pong

import android.content.SharedPreferences
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class PongGame(_platformSpeed: Float, _platformWidth: Float, _ballSpeed: Float, _ballMaxAngle: Float, _freezeTicks: Int) {

    /** Game settings */
    private val platformSpeed = _platformSpeed
    private val platformWidth = _platformWidth
    private val ballSpeed = _ballSpeed
    private val ballMaxAngle = _ballMaxAngle    //In radians
    private val freezeTicksAfterScore = _freezeTicks
    var bot = false

    /** Positions of platforms */
    private var platformPosP1 = 0.5f
    private var platformPosP2 = 0.5f

    /** Position of ball */
    private var ballPosX = 0.5f
    private var ballPosY = 0.5f
    private var ballDir = true
    private var ballAngle = 0.0f
    private var ballFreeze = freezeTicksAfterScore

    /** Score */
    private var scoreP1 = 0
    private var scoreP2 = 0

    /**
     * Possible movement of a player
     */
    enum class Movement {
        NONE, LEFT, RIGHT
    }

    fun tickGame(movementP1: Movement, movementP2: Movement) {
        var movP2 = movementP2

        //If playing against bot, calculate its movement
        if(bot) {
            val platformCenterPos = platformPosP2 + platformWidth / 2.0f
            val ballCenterPos = ballPosX * (1.0f + platformWidth)

            //Follow the ball
            movP2 = when(platformCenterPos < ballCenterPos) {
                true    -> Movement.RIGHT
                false   -> Movement.LEFT
            }

            //If ball close enough to platform center, don't move
            if(abs(platformCenterPos - ballCenterPos) < platformWidth * 0.3f)
                movP2 = Movement.NONE
        }

        //Calculate movement for player 1
        when(movementP1) {
            Movement.LEFT   -> {
                platformPosP1 -= platformSpeed
                if(platformPosP1 < 0.0f)
                    platformPosP1 = 0.0f
            }
            Movement.RIGHT  -> {
                platformPosP1 += platformSpeed
                if(platformPosP1 > 1.0f)
                    platformPosP1 = 1.0f
            }
            Movement.NONE   -> {}
        }

        //Calculate movement for player 2
        when(movP2) {
            Movement.LEFT   -> {
                platformPosP2 -= platformSpeed
                if(platformPosP2 < 0.0f)
                    platformPosP2 = 0.0f
            }
            Movement.RIGHT  -> {
                platformPosP2 += platformSpeed
                if(platformPosP2 > 1.0f)
                    platformPosP2 = 1.0f
            }
            Movement.NONE   -> {}
        }

        //If game freeze
        if(ballFreeze > 0) {
            ballFreeze--
            return
        }

        //Calculate ball position X
        ballPosX += ballSpeed * sin(ballAngle)
        if(ballPosX > 1.0f) {
            ballPosX = 1.0f - (ballPosX - 1.0f)
            ballAngle *= -1.0f
        }
        if(ballPosX < 0.0f) {
            ballPosX = 0.0f - ballPosX
            ballAngle *= -1.0f
        }

        //Calculate ball position Y
        when(ballDir) {
            true    ->  ballPosY += ballSpeed * cos(ballAngle)
            false   ->  ballPosY -= ballSpeed * cos(ballAngle)
        }
        if(ballPosY > 1.0f) {   //If ball over edge P1
            ballAngle = calculateAngle(true)

            if(abs(ballAngle) > ballMaxAngle) {
                pointScored(false)
                ballFreeze = freezeTicksAfterScore
            } else {
                ballPosY = 1.0f - (ballPosY - 1.0f)
                ballDir = false
            }
        }
        if(ballPosY < 0.0f) {  //If ball over edge P2
            ballAngle = calculateAngle(false)

            //Player 1 scores
            if(abs(ballAngle) > ballMaxAngle) {
                pointScored(true)
                ballFreeze = freezeTicksAfterScore
            } else {
                ballPosY = 0.0f - ballPosY
                ballDir = true
            }
        }
    }

    /**
     * Calculates angle of bounced ball
     */
    private fun calculateAngle(player1: Boolean): Float {

        return when(player1) {
            true    -> {
                val platPos = (1.0f - platformWidth) * platformPosP1
                var ballAng = ballPosX - (platPos + platformWidth / 2.0f)
                ballAng /= (platformWidth / 2.0f)

                ballAng * ballMaxAngle
            }

            false   ->  {
                val platPos = (1.0f - platformWidth) * platformPosP2
                var ballAng = ballPosX - (platPos + platformWidth / 2.0f)
                ballAng /= (platformWidth / 2.0f)

                ballAng * ballMaxAngle
            }
        }
    }

    /**
     * Handles the game after the point has been scored
     */
    private fun pointScored(player1: Boolean) {

        //Add point
        when(player1) {
            true    ->  scoreP1++
            false   ->  scoreP2++
        }

        //Restart the game
        platformPosP1 = 0.5f
        platformPosP2 = 0.5f
        ballPosX = 0.5f
        ballPosY = 0.5f
        ballAngle = 0.0f
        ballDir = !player1
    }

    /**
     * Returns platform position for player 1
     */
    fun getPlatformPositionP1(): Float {
        return platformPosP1
    }

    /**
     * Returns platform position for player 2
     */
    fun getPlatformPositionP2(): Float {
        return platformPosP2
    }

    /**
     * Returns ball position X
     */
    fun getBallPositionX(): Float {
        return ballPosX
    }

    /**
     * Returns ball position Y
     */
    fun getBallPositionY(): Float {
        return ballPosY
    }

    /**
     * Returns score of player 1
     */
    fun getScoreP1(): Int {
        return scoreP1
    }

    /**
     * Returns score of player 2
     */
    fun getScoreP2(): Int {
        return scoreP2
    }

    /**
     * Resets the game to default values
     */
    fun resetGame() {
        scoreP1 = 0
        scoreP2 = 0
        platformPosP1 = 0.5f
        platformPosP2 = 0.5f
        ballPosY = 0.5f
        ballPosX = 0.5f
        ballAngle = 0.0f
        ballDir = true
        ballFreeze = freezeTicksAfterScore
    }

    /**
     * Saves the game status to shared preferences editor
     */
    fun saveToSharedPrefEditor(editor: SharedPreferences.Editor) {

        //Save game variables
        editor.putFloat("plat_pos_p1", platformPosP1)
        editor.putFloat("plat_pos_p2", platformPosP2)
        editor.putFloat("ball_pos_x", ballPosX)
        editor.putFloat("ball_pos_y", ballPosY)
        editor.putBoolean("ball_dir", ballDir)
        editor.putFloat("ball_angle", ballAngle)
        editor.putInt("ball_freeze", ballFreeze)
        editor.putInt("score_p1", scoreP1)
        editor.putInt("score_p2", scoreP2)
        editor.putBoolean("bot", bot)
    }

    fun loadFromSharedPref(sharedPref: SharedPreferences) {

        //Load game variables
        platformPosP1 = sharedPref.getFloat("plat_pos_p1", 0.5f)
        platformPosP2 = sharedPref.getFloat("plat_pos_p2", 0.5f)
        ballPosX = sharedPref.getFloat("ball_pos_x", 0.5f)
        ballPosY = sharedPref.getFloat("ball_pos_y", 0.5f)
        ballDir = sharedPref.getBoolean("ball_dir", true)
        ballAngle = sharedPref.getFloat("ball_angle", 0.0f)
        ballFreeze = sharedPref.getInt("ball_freeze", freezeTicksAfterScore)
        scoreP1 = sharedPref.getInt("score_p1", 0)
        scoreP2 = sharedPref.getInt("score_p2", 0)
        bot = sharedPref.getBoolean("bot", true)
    }
}