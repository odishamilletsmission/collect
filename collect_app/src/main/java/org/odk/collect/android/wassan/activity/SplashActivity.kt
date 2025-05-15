package org.odk.collect.android.wassan.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.odk.collect.android.R
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.settings.keys.MetaKeys
import org.odk.collect.strings.localization.LocalizedActivity
import javax.inject.Inject

class SplashActivity : LocalizedActivity() {
    @Inject
    lateinit var settingsProvider: SettingsProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DaggerUtils.getComponent(this).inject(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Delay before redirecting (3 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            val nextActivity = if (isUserLoggedIn()) MainActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this, nextActivity))
            finish()
        }, 3000)
    }

    private fun isUserLoggedIn(): Boolean {
        return settingsProvider.getMetaSettings().getBoolean(MetaKeys.IS_LOGIN)
    }
}
