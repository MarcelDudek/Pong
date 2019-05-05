package am.lista5.pong

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    /**
     * On pause save game to shared preferences
     */
    override fun onPause() {
        super.onPause()

        //Save GameView to shared preferences
        val gameView = findViewById<GameView>(R.id.gameView)
        gameView.saveToSharedPreferences(this)
    }

    /**
     * On resume load game from shared preferences
     */
    override fun onResume() {
        super.onResume()

        //Load GameView from shared preferences
        val gameView = findViewById<GameView>(R.id.gameView)
        gameView.readFromSharedPreferences(this)
    }

    /**
     * On back pressed return to start screen
     */
    override fun onBackPressed() {

        //Return to start screen
        val gameView = findViewById<GameView>(R.id.gameView)
        gameView.returnToStartScreen()
    }

    /**
     * Makes app fullscreen
     */
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

}
