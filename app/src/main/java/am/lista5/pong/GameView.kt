package am.lista5.pong

import android.content.Context
import android.graphics.*
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.concurrent.thread

class GameView : View {

    /** Game settings */
    private val platformSpeed = 0.026f
    private val platformWidth = 0.22f
    private val platformHeight = 0.02f
    private val ballSpeed = 0.03f
    private val ballRadius = 0.01f
    private val ballMaxAngle = 1.48f    //In radians
    private val scoreMax = 10
    private val freezeTicksAfterScore = 120
    private var singlePlayer = true

    /** Game object*/
    private val game = PongGame(platformSpeed, platformWidth, ballSpeed, ballMaxAngle, freezeTicksAfterScore)
    private var gameState = GameState.START_SCREEN
    private var stopGame = false
    private var gamePaused = false

    /**Paint object*/
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val typeFace: Typeface

    /** Movements of players*/
    private var movementP1 = PongGame.Movement.NONE
    private var movementP2 = PongGame.Movement.NONE

    /** Touches */
    private var touchArray = ArrayList<PongGame.Movement>()
    private var touchArrayPlayer = ArrayList<Boolean>()


    constructor(ctx: Context) : super(ctx) {
        typeFace = ResourcesCompat.getFont(ctx, R.font.gang_wolfik) as Typeface
    }

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs) {
        typeFace = ResourcesCompat.getFont(ctx, R.font.gang_wolfik) as Typeface
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //Make background black
        canvas.drawARGB(255, 0, 0, 0)

        //Draw on screen depending on game state
        when(gameState) {

            GameState.START_SCREEN  -> {
                drawStartScreen(canvas)
            }

            GameState.GAME          -> {
                synchronized(game) {
                    drawLine(canvas)
                    drawScore(canvas, game.getScoreP1(), game.getScoreP2(), !singlePlayer)
                    drawPlatform(canvas, true, game.getPlatformPositionP1())
                    drawPlatform(canvas, false, game.getPlatformPositionP2())
                    drawBall(canvas, game.getBallPositionX(), game.getBallPositionY())
                }
            }

            GameState.FINISH_SCREEN -> {
                drawFinishScreen(canvas, !singlePlayer)
            }
        }
    }

    /**
     * Draws line in the middle of screen
     */
    private fun drawLine(canvas: Canvas) {

        //Get the middle of the screen
        val height = this.height / 2f
        val width = this.width / 1f

        //Set parameters
        paint.color = Color.GRAY
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.pathEffect = DashPathEffect(floatArrayOf(width * 0.07f, width * 0.046f), 0f)

        //draw line in the middle of screen
        val line = Path()
        line.moveTo(0f, height)
        line.lineTo(width, height)
        canvas.drawPath(line, paint)    /*drawPath not drawLine because drawLine with pathEffect is bugged under hardware acceleration */
    }

    /**
     * Draws player's platforms
     */
    private fun drawPlatform(canvas: Canvas, player: Boolean, position: Float) {

        //Set parameters
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.pathEffect = null

        val height = this.height * platformHeight
        val width = this.width * platformWidth
        val x: Float
        val y: Float

        //Set Y to player's platform
        y = when(player) {
            true    -> this.height - height
            false   -> 0f
        }

        //Set X to player's platform position
        x = (this.width - width) * position

        //Draw platform on screen
        canvas.drawRect(x, y, x + width, y + height, paint)
    }

    /**
     * Draws the ball
     */
    private fun drawBall(canvas: Canvas, positionX: Float, positionY: Float) {

        //Set parameters
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.pathEffect = null

        //Calculate x and y
        val x = this.width * positionX
        val y = this.height * ((1.0f - platformHeight * 2.0f) * positionY + platformHeight)

        //Draw the ball
        canvas.drawCircle(x, y, this.height * ballRadius, paint)
    }

    /**
     * Draws the score
     */
    private fun drawScore(canvas: Canvas, scoreP1: Int, scoreP2: Int, addInverted: Boolean) {

        //Set parameters
        paint.color = Color.GRAY
        paint.style = Paint.Style.FILL
        paint.pathEffect = null
        paint.textSize = this.height * 0.2f
        paint.typeface = typeFace

        //Draw score P1
        val x = this.width * 0.15f
        var y = this.height * 0.67f
        canvas.drawText(scoreP1.toString(), x, y, paint)

        //Draw score P2
        y = this.height * 0.4f
        canvas.drawText(scoreP2.toString(), x, y, paint)

        //Draw rotated score if required
        if(addInverted) {
            //Rotate the canvas
            canvas.rotate(180.0f, this.width / 2.0f, this.height / 2.0f)

            //Draw score P2
            y = this.height * 0.67f
            canvas.drawText(scoreP2.toString(), x, y, paint)

            //Draw score P1
            y = this.height * 0.4f
            canvas.drawText(scoreP1.toString(), x, y, paint)

            //Rotate the canvas to normal position
            canvas.rotate(-180.0f, this.width / 2.0f, this.height / 2.0f)
        }
    }

    /**
     * Draws the start screen
     */
    private fun drawStartScreen(canvas: Canvas) {

        //Set parameters
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.pathEffect = null
        paint.textSize = this.height * 0.1f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = typeFace

        //Draw single player text
        var x = this.width * 0.5f
        var y = this.height * 0.45f
        canvas.drawText("Single player", x, y, paint)

        //Draw multi player text
        x = this.width * 0.5f
        y = this.height * 0.6f
        canvas.drawText("Multi player", x, y, paint)

        //If the game is paused draw resume text
        if(gamePaused) {
            x = this.width * 0.5f
            y = this.height * 0.75f
            canvas.drawText("Resume", x, y, paint)
        }

        //Set parameters for title
        paint.color = Color.DKGRAY
        paint.textSize = this.height * 0.2f

        x = this.width * 0.5f
        y = this.height * 0.2f
        canvas.drawText("PONG", x, y, paint)
    }

    /**
     * Draws the finish screen
     */
    private fun drawFinishScreen(canvas: Canvas, addInverted: Boolean) {

        //Set parameters
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.pathEffect = null
        paint.textSize = this.height * 0.1f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = typeFace

        //Check the winner
        val winner = when(game.getScoreP1() > game.getScoreP2()) {
            true    -> "Player 1 wins!"
            false   -> "Player 2 wins!"
        }

        //Draw the winner text
        canvas.drawText(winner, this.width * 0.5f, this.height * 0.4f, paint)

        //If needed draw the inverted winner text
        if(addInverted) {
            //Rotate the canvas
            canvas.rotate(180.0f, this.width / 2.0f, this.height / 2.0f)

            canvas.drawText(winner, this.width * 0.5f, this.height * 0.4f, paint)

            //Rotate the canvas to normal position
            canvas.rotate(-180.0f, this.width / 2.0f, this.height / 2.0f)
        }
    }


    /**
     * Touch handler
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {

        //Use different handlers for different game states
        return when(gameState) {
            GameState.GAME          -> gameOnTouchEvent(event)
            GameState.START_SCREEN  -> startScreenOnTouchEvent(event)
            GameState.FINISH_SCREEN -> finishScreenOnTouchEvent(event)
        }
    }

    /**
     * Handles touch input during game
     */
    private fun gameOnTouchEvent(event: MotionEvent): Boolean {
        val index = event.actionIndex
        val player1 = touchP1(event.getY(index))
        val side  = touchSide(event.getX(index))

        synchronized(touchArray) {
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    touchArray.add(index, side)
                    touchArrayPlayer.add(index, player1)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    touchArray.removeAt(index)
                    touchArrayPlayer.removeAt(index)
                }

                else    -> {}
            }
        }

        return true     //Return true for multitouch
    }

    /**
     * Handles touch input during start screen
     */
    private fun startScreenOnTouchEvent(event: MotionEvent): Boolean {
        if(event.actionMasked == MotionEvent.ACTION_DOWN) {

            //Single player
            if(event.y > this.height * 0.4f && event.y < this.height * 0.5f) {

                //Clear the touchArrays
                touchArray.clear()
                touchArrayPlayer.clear()

                //Start the game
                gameState = GameState.GAME
                singlePlayer = true
                game.resetGame()
                game.bot = true
                stopGame = false
                runGame()
            }

            //Multi player
            if(event.y > this.height * 0.5f && event.y < this.height * 0.6f) {

                //Clear the touchArrays
                touchArray.clear()
                touchArrayPlayer.clear()

                //Start the game
                gameState = GameState.GAME
                singlePlayer = false
                game.resetGame()
                game.bot = false
                stopGame = false
                runGame()
            }

            //Resume
            if(gamePaused && event.y > this.height * 0.7f && event.y < this.height * 0.8f) {
                //Clear the touchArrays
                touchArray.clear()
                touchArrayPlayer.clear()

                //Start the game
                gameState = GameState.GAME
                stopGame = false
                gamePaused = false
                runGame()
            }
        }

        return false
    }

    /**
     * Handles touch input during finish screen
     */
    private fun finishScreenOnTouchEvent(event: MotionEvent): Boolean {

        //Go to start screen
        if(event.actionMasked == MotionEvent.ACTION_DOWN &&
                event.y > this.height * 0.4f && event.y < this.height * 0.6f) {

            gameState = GameState.START_SCREEN
            this.invalidate()   //Redraw
        }

        return false
    }

    /**
     * Checks if touch on position y has been made by player 1
     */
    private fun touchP1(y: Float): Boolean {
        return y > (this.height / 2.0f)
    }

    /**
     * Checks if touch on position x has been made on left or right side of a screen
     */
    private fun touchSide(x: Float): PongGame.Movement {
        return when(x > this.width / 2.0f) {
            true    ->  PongGame.Movement.RIGHT
            false   ->  PongGame.Movement.LEFT
        }
    }

    /**
     * Calculates movement of players depending on touches
     */
    private fun calculateMovement() {

        movementP1 = PongGame.Movement.NONE
        movementP2 = PongGame.Movement.NONE

        //Check all touches
        for(i in 0 until touchArray.size) {
            when(touchArray[i]) {
                PongGame.Movement.LEFT -> {
                    if(touchArrayPlayer[i]) {
                        movementP1 = PongGame.Movement.LEFT
                    } else {
                        movementP2 = PongGame.Movement.LEFT
                    }
                }

                PongGame.Movement.RIGHT -> {
                    if(touchArrayPlayer[i]) {
                        movementP1 = PongGame.Movement.RIGHT
                    } else {
                        movementP2 = PongGame.Movement.RIGHT
                    }
                }

                PongGame.Movement.NONE -> {}
            }
        }
    }

    /**
     * Runs the game loop
     */
    private fun runGame() {
        thread {
            while(game.getScoreP1() < scoreMax && game.getScoreP2() < scoreMax && !stopGame) {
                //Run game
                synchronized(touchArray) {
                    calculateMovement()
                }
                synchronized(game) {
                    game.tickGame(movementP1, movementP2)
                }

                //Invalidate the view so it redraws itself
                this.invalidate()

                //Apply delay
                Thread.sleep(16)
            }

            //Draw the finish screen
            if(!stopGame)
                gameState = GameState.FINISH_SCREEN
            this.invalidate()
        }
    }

    /**
     * Returns to start screen
     */
    fun returnToStartScreen() {

        //If during the game, pause
        if(gameState == GameState.GAME)
            gamePaused = true

        gameState = GameState.START_SCREEN
        stopGame = true
        this.invalidate()
    }

    /**
     * Saves the game to shared preferences
     */
    fun saveToSharedPreferences(mAct: MainActivity) {
        val sharedPref = mAct.getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        editor.putInt("game_state", gameState.toInt())
        editor.putBoolean("game_paused", gamePaused)

        //If the game is currently played, save it to editor
        if(gameState == GameState.GAME || gameState == GameState.FINISH_SCREEN || gamePaused) {
            editor.putBoolean("single_player", singlePlayer)
            game.saveToSharedPrefEditor(editor)
            editor.putBoolean("game_paused", true)
        }

        editor.apply()
        stopGame = true
    }

    /**
     * Loads the game from shared preferences
     */
    fun readFromSharedPreferences(mAct: MainActivity) {
        val sharedPref = mAct.getPreferences(Context.MODE_PRIVATE)
        gameState = GameState.fromInt(sharedPref.getInt("game_state", 0))
        gamePaused = sharedPref.getBoolean("game_paused", false)

        //If the game was played, load it
        if(gameState == GameState.GAME || gameState == GameState.FINISH_SCREEN || gamePaused) {
            gameState = GameState.START_SCREEN
            singlePlayer = sharedPref.getBoolean("single_player", true)
            game.loadFromSharedPref(sharedPref)
            stopGame = true
        }
    }

    /**
     * Possible game states
     */
    private enum class GameState{
        START_SCREEN, GAME, FINISH_SCREEN;

        fun toInt(): Int {
            return when(this) {
                START_SCREEN    ->  0
                GAME            ->  1
                FINISH_SCREEN   ->  2
            }
        }

        companion object {
            fun fromInt(x: Int): GameState {
                return when(x) {
                    0       ->  START_SCREEN
                    1       ->  GAME
                    2       ->  FINISH_SCREEN
                    else    ->  START_SCREEN
                }
            }
        }
    }
}