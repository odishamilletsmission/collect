package org.odk.collect.android.wassan.activity

import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.odk.collect.android.R
import org.odk.collect.android.activities.ActivityUtils
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.projects.ProjectsDataService
import org.odk.collect.android.wassan.app.ApiClient
import org.odk.collect.android.wassan.app.LoginResponse
import org.odk.collect.android.wassan.app.UserProject
import org.odk.collect.android.wassan.model.User
import org.odk.collect.projects.Project
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.settings.keys.MetaKeys
import org.odk.collect.settings.keys.MetaKeys.CURRENT_PROJECT_ID
import org.odk.collect.settings.keys.ProjectKeys
import org.odk.collect.strings.localization.LocalizedActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import javax.inject.Inject


class LoginActivity : LocalizedActivity() {
    @Inject
    lateinit var settingsProvider: SettingsProvider

    @Inject
    lateinit var projectsRepository: ProjectsRepository

    @Inject
    lateinit var projectsDataService: ProjectsDataService

    lateinit var pd: ProgressDialog

    lateinit var webServerURL: String
    lateinit var username: EditText
    lateinit var password:EditText
    lateinit var loginButton: MaterialButton
    lateinit var cardLoginWithClap: MaterialCardView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        DaggerUtils.getComponent(this).inject(this)
        setContentView(R.layout.activity_login)

        // Adjust insets for keyboard and system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            view.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                if (imeVisible) imeInsets.bottom else systemBarsInsets.bottom
            )
            insets
        }

        val isLogged = settingsProvider.getMetaSettings().getBoolean(MetaKeys.IS_LOGIN)

        if (isLogged) {
            launchDashboard()
            finish()
        }

        username = findViewById<EditText>(R.id.editTextUsername)
        password = findViewById<EditText>(R.id.editTextPassword)
        loginButton = findViewById(R.id.loginButton)
        cardLoginWithClap = findViewById(R.id.cardLoginWithClap)

        pd = ProgressDialog(this)
        pd.setCanceledOnTouchOutside(false)
        loginButton.setOnClickListener(View.OnClickListener { loginRequest() })

        cardLoginWithClap.setOnClickListener {
            showClapLoginDialog()
        }
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
        pd.setMessage("Signing in with CLAP . . .")
        pd.show()

        val api = ApiClient.create(this)

        val call = api.clapLogin(email, password)
        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                pd.dismiss()
                dialog.dismiss()
                handleLoginResponse(response, email)
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                pd.dismiss()
                Toast.makeText(applicationContext, t.localizedMessage ?: "Error", Toast.LENGTH_SHORT).show()
            }
        })
        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: retrofit2.Response<LoginResponse>) {
                pd.dismiss()
                if (response.isSuccessful && response.body() != null) {
                    val clapUser = response.body()!!

                    // ✅ Save into Meta settings (like default login does)
                    val gson = Gson()
                    val jsonuser = gson.toJson(clapUser)
                    settingsProvider.getMetaSettings().save(MetaKeys.KEY_USER, jsonuser)

                    val generalSettings = settingsProvider.getUnprotectedSettings("clap_project")
                    /*generalSettings.save(ProjectKeys.KEY_METADATA_USERNAME, clapUser.employee_name)
                    generalSettings.save(ProjectKeys.KEY_USERNAME, clapUser.email_id ?: email)
                    generalSettings.save(ProjectKeys.KEY_METADATA_PHONENUMBER, clapUser.mobile_num)
                    generalSettings.save(ProjectKeys.KEY_METADATA_EMAIL, clapUser.email_id)*/

                    // Clap API doesn’t return project tokens/URLs like ODK does,
                    // so you may want to set a fixed CLAP server URL or skip it.
                    generalSettings.save(ProjectKeys.KEY_SERVER_URL, "https://clap.wassan.org")

                    dialog.dismiss()

                    initProject()
                    launchDashboard()
                } else {
                    Toast.makeText(applicationContext, "Clap Login Failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                pd.dismiss()
                Toast.makeText(applicationContext, t.localizedMessage ?: "Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loginRequest() {
        val username1 = findViewById<EditText>(R.id.editTextUsername).text.toString()
        val password1 = findViewById<EditText>(R.id.editTextPassword).text.toString()
        pd.setMessage("Signing In . . .")
        pd.show()

        val api = ApiClient.create(this)

        val call = api.login(username1, password1)
        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                pd.dismiss()
                handleLoginResponse(response, username1)
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                pd.dismiss()
                Toast.makeText(applicationContext, t.localizedMessage ?: "Error", Toast.LENGTH_SHORT).show()
            }
        })
        /*call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: retrofit2.Response<LoginResponse>) {
                pd.dismiss()
                if (response.isSuccessful && response.body()?.status == true) {
                    val userJson = response.body()?.user ?: return
                    val defaultProjectJson = userJson.default_project

                    if (defaultProjectJson == null || defaultProjectJson.central_project_id.isNullOrEmpty()) {
                        Toast.makeText(applicationContext, "No project assigned you", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val serverUrl = "${defaultProjectJson.server_url}/key/${defaultProjectJson.central_user_token}/projects/${defaultProjectJson.central_project_id}"
                    val user = User(
                        userJson.id,
                        userJson.username,
                        userJson.email,
                        userJson.fullname,
                        userJson.phone,
                        userJson.position,
                        userJson.district_id,
                        userJson.block_id,
                        userJson.gp_id,
                        userJson.user_group_id,
                        userJson.image,
                        userJson.user_project,
                        defaultProjectJson
                    )

                    val gson = Gson()
                    val jsonuser = gson.toJson(user)
                    val generalSettings = settingsProvider.getUnprotectedSettings(defaultProjectJson.central_project_id)
                    settingsProvider.getMetaSettings().save(MetaKeys.KEY_USER, jsonuser)
                    generalSettings.save(ProjectKeys.KEY_METADATA_USERNAME, username1)
                    generalSettings.save(ProjectKeys.KEY_USERNAME, username1)
                    generalSettings.save(ProjectKeys.KEY_METADATA_PHONENUMBER, userJson.phone)
                    generalSettings.save(ProjectKeys.KEY_METADATA_EMAIL, userJson.email)
                    generalSettings.save(ProjectKeys.KEY_SERVER_URL, serverUrl)

                    initProject()
                    launchDashboard()

                } else {
                    val msg = response.body()?.message ?: "Login failed"
                    showSnackbar(msg)
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                pd.dismiss()
                Toast.makeText(applicationContext, t.localizedMessage ?: "Error", Toast.LENGTH_SHORT).show()
            }
        })*/
    }

    private fun handleLoginResponse(response: Response<LoginResponse>, usernameOrEmail: String) {
        if (response.isSuccessful && response.body()?.status == true) {
            val userJson = response.body()?.user ?: return
            val defaultProjectJson = userJson.default_project

            if (defaultProjectJson == null || defaultProjectJson.central_project_id.isNullOrEmpty()) {
                Toast.makeText(applicationContext, "No project assigned to you", Toast.LENGTH_SHORT).show()
                return
            }

            val serverUrl = "${defaultProjectJson.server_url}/key/${defaultProjectJson.central_user_token}/projects/${defaultProjectJson.central_project_id}"

            val user = User(
                userJson.id,
                userJson.username,
                userJson.email,
                userJson.fullname,
                userJson.phone,
                userJson.position,
                userJson.district_id,
                userJson.block_id,
                userJson.gp_id,
                userJson.user_group_id,
                userJson.image,
                userJson.user_project,
                defaultProjectJson
            )

            val gson = Gson()
            val jsonuser = gson.toJson(user)

            val generalSettings = settingsProvider.getUnprotectedSettings(defaultProjectJson.central_project_id)
            settingsProvider.getMetaSettings().save(MetaKeys.KEY_USER, jsonuser)
            generalSettings.save(ProjectKeys.KEY_METADATA_USERNAME, usernameOrEmail)
            generalSettings.save(ProjectKeys.KEY_USERNAME, usernameOrEmail)
            generalSettings.save(ProjectKeys.KEY_METADATA_PHONENUMBER, userJson.phone)
            generalSettings.save(ProjectKeys.KEY_METADATA_EMAIL, userJson.email)
            generalSettings.save(ProjectKeys.KEY_SERVER_URL, serverUrl)

            initProject()
            launchDashboard()

        } else {
            val msg = response.body()?.message ?: "Login failed"
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchDashboard() {
        finish()
        settingsProvider.getMetaSettings().save(MetaKeys.IS_LOGIN, true)
        ActivityUtils.startActivityAndCloseAllOthers(this, MainActivity::class.java)
    }

    private fun initProject() {
        settingsProvider.getMetaSettings().save(CURRENT_PROJECT_ID, null)
        val gson = Gson()
        val userJson = settingsProvider.getMetaSettings().getString(MetaKeys.KEY_USER)
        //val user: User = gson.fromJson(userJson, User::class.java)
        // val projectsArray: JsonArray = JsonParser.parseString(userJson).asJsonObject.getAsJsonArray("projects")

        val root = JsonParser.parseString(userJson).asJsonObject

        val projectsJson = root.get("userProjects").asJsonArray // Step 1
        val userProjects = projectsJson.map { gson.fromJson(it, UserProject::class.java) }

        val defaultProjectJson = root.get("defaultProject").asJsonObject
        val defaultProject = gson.fromJson(defaultProjectJson, UserProject::class.java)
        projectsRepository.deleteAll();
        userProjects.forEach { projectObject ->
            val projectId = projectObject.central_project_id
            val projectName = projectObject.project_name
            val projectIcon = projectObject.icon
            val projectColor = projectObject.color
            val serverAddress = projectObject.server_url
            val centralUserToken = projectObject.central_user_token
            val serverUrl=serverAddress+"/key/"+centralUserToken+"/projects/"+projectId


            projectsRepository.save(
                Project.Saved(
                    projectId,
                    projectName,
                    projectIcon,
                    projectColor
                )
            )

            /*val generalSettings = settingsProvider.getUnprotectedSettings(projectId)
            generalSettings.save(ProjectKeys.KEY_METADATA_USERNAME, user.username)
            generalSettings.save(ProjectKeys.KEY_USERNAME, user.username)
            generalSettings.save(ProjectKeys.KEY_METADATA_PHONENUMBER, user.phone)
            generalSettings.save(ProjectKeys.KEY_METADATA_EMAIL, user.email)
            generalSettings.save(ProjectKeys.KEY_SERVER_URL, serverUrl)*/

        }

        //set current project
        val uuid = defaultProject.central_project_id
        val projectName=defaultProject.project_name
        val projectIcon = defaultProject.icon
        val projectColor = defaultProject.color
        projectsRepository.save(
            Project.Saved(
                uuid,
                projectName,
                projectIcon,
                projectColor
            )
        )
        projectsDataService.setCurrentProject(uuid)


    }


    private fun showSnackbar(stringSnackbar: String?) {
        Snackbar.make(
            findViewById(android.R.id.content),
            stringSnackbar!!, Snackbar.LENGTH_SHORT
        )
            .setActionTextColor(getResources().getColor(R.color.colorError))
            .show()
    }
}