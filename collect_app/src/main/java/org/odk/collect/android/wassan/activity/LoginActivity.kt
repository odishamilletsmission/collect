package org.odk.collect.android.wassan.activity

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.odk.collect.android.R
import org.odk.collect.android.activities.ActivityUtils
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.projects.ProjectsDataService
import org.odk.collect.android.wassan.app.ApiClient
import org.odk.collect.android.wassan.app.LoginResponse
import org.odk.collect.android.wassan.model.User
import org.odk.collect.android.wassan.model.UserProject
import org.odk.collect.projects.Project
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.settings.keys.MetaKeys
import org.odk.collect.settings.keys.ProjectKeys
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class LoginActivity : androidx.appcompat.app.AppCompatActivity() {

    @Inject
    lateinit var settingsProvider: SettingsProvider

    @Inject
    lateinit var projectsRepository: ProjectsRepository

    @Inject
    lateinit var projectsDataService: ProjectsDataService

    private lateinit var usernameEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var loginButton: MaterialButton
    private lateinit var LoginWithClap: MaterialCardView
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DaggerUtils.getComponent(this).inject(this)
        setContentView(R.layout.activity_login)

        lifecycleScope.launch(Dispatchers.IO) {
            ApiClient.create(applicationContext)
        }

        usernameEdit = findViewById(R.id.editTextUsername)
        passwordEdit = findViewById(R.id.editTextPassword)
        loginButton = findViewById(R.id.loginButton)
        LoginWithClap = findViewById(R.id.cardLoginWithClap)

        progressDialog = ProgressDialog(this).apply {
            setCanceledOnTouchOutside(false)
        }

        if (settingsProvider.getMetaSettings().getBoolean(MetaKeys.IS_LOGIN)) {
            launchDashboard()
            finish()
        }

        loginButton.setOnClickListener { loginRequest() }
        LoginWithClap.setOnClickListener { showClapLoginDialog() }
    }

    private fun loginRequest() {
        val username = usernameEdit.text.toString()
        val password = passwordEdit.text.toString()

        if (username.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.setMessage("Signing In...")
        progressDialog.show()

        val api = ApiClient.create(this)
        val call = api.login(username, password)

        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                progressDialog.dismiss()
                handleLoginResponse(response, username)
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                progressDialog.dismiss()
                Toast.makeText(applicationContext, t.localizedMessage ?: "Login failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showClapLoginDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_clap_login, null)
        val emailEt = dialogView.findViewById<TextInputEditText>(R.id.clapEmail)
        val passwordEt = dialogView.findViewById<TextInputEditText>(R.id.clapPassword)

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
            .also { dialog ->
                dialogView.findViewById<MaterialButton>(R.id.btnClapLogin).setOnClickListener {
                    val email = emailEt.text.toString()
                    val password = passwordEt.text.toString()

                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(this, "Enter both email and password", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    clapLoginRequest(email, password, dialog)
                }
                dialog.show()
            }
    }

    private fun clapLoginRequest(email: String, password: String, dialog: android.app.Dialog) {
        progressDialog.setMessage("Signing in with CLAP . . .")
        progressDialog.show()

        val api = ApiClient.create(this)

        val call = api.clapLogin(email, password)
        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                progressDialog.dismiss()
                dialog.dismiss()
                handleLoginResponse(response, email)
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                progressDialog.dismiss()
                Toast.makeText(applicationContext, t.localizedMessage ?: "Error", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun handleLoginResponse(response: Response<LoginResponse>, username: String) {
        val body = response.body()
        if (response.isSuccessful && body?.status == true && body.user != null) {
            val user: User = body.user

            val gson = Gson()
            val jsonUser = gson.toJson(user)
            settingsProvider.getMetaSettings().save(MetaKeys.KEY_USER, jsonUser)

            val defaultProject = user.defaultProject

            if (defaultProject == null || defaultProject.centralProjectId.isEmpty()) {
                Toast.makeText(applicationContext, "No default project assigned to your account.", Toast.LENGTH_LONG).show()
                return
            }

            lifecycleScope.launch {
                progressDialog.setMessage("Initializing projects...")
                withContext(Dispatchers.IO) {
                    initProject()
                }
                launchDashboard()
            }
        } else {
            Toast.makeText(this, body?.message ?: "Login failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initProject() {
        // Clear old projects
        projectsRepository.deleteAll()
        settingsProvider.getMetaSettings().save(MetaKeys.CURRENT_PROJECT_ID, null)

        val userJson = settingsProvider.getMetaSettings().getString(MetaKeys.KEY_USER) ?: return
        val user = Gson().fromJson(userJson, User::class.java)

        // Save all projects and their settings
        (user.userProjects ?: emptyList()).forEach { project ->
            // Save project
            projectsRepository.save(
                Project.Saved(project.centralProjectId, project.projectName, project.icon, project.color)
            )

            val serverUrl = "${project.serverUrl}/key/${project.centralUserToken}/projects/${project.centralProjectId}"

            settingsProvider.getUnprotectedSettings(project.centralProjectId).apply {
                save(ProjectKeys.KEY_METADATA_USERNAME, user.username)
                save(ProjectKeys.KEY_USERNAME, user.username)
                save(ProjectKeys.KEY_METADATA_PHONENUMBER, user.phone)
                save(ProjectKeys.KEY_METADATA_EMAIL, user.email)
                save(ProjectKeys.KEY_SERVER_URL, serverUrl)
                save(ProjectKeys.KEY_PROTOCOL, ProjectKeys.PROTOCOL_SERVER)
            }
        }

        // Set default project as current (if exists)
        user.defaultProject?.let {
            projectsDataService.setCurrentProject(it.centralProjectId)
        }
    }

    private fun launchDashboard() {
        settingsProvider.getMetaSettings().save(MetaKeys.IS_LOGIN, true)
        ActivityUtils.startActivityAndCloseAllOthers(this, MainActivity::class.java)
    }
}
