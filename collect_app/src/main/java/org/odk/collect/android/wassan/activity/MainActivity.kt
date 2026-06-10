package org.odk.collect.android.wassan.activity

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.gson.Gson
import org.odk.collect.android.R
import org.odk.collect.android.activities.ActivityUtils
import org.odk.collect.android.activities.FormDownloadListActivity
import org.odk.collect.android.application.CollectComposeThemeProvider
import org.odk.collect.android.application.MapboxClassInstanceCreator
import org.odk.collect.android.formmanagement.FormFillingIntentFactory
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.mainmenu.CurrentProjectViewModel
import org.odk.collect.android.mainmenu.MainMenuActivity
import org.odk.collect.android.mainmenu.MainMenuViewModelFactory
import org.odk.collect.android.projects.ProjectIconView
import org.odk.collect.android.projects.ProjectSettingsDialog
import org.odk.collect.android.projects.ProjectsDataService
import org.odk.collect.android.wassan.app.ApiClient
import org.odk.collect.android.wassan.app.AppUpdater
import org.odk.collect.android.wassan.app.UserProjectsResponse
import org.odk.collect.android.wassan.fragments.AccountFragment
import org.odk.collect.android.wassan.fragments.CalculatorFragment
import org.odk.collect.android.wassan.fragments.CommunityFragment
import org.odk.collect.android.wassan.fragments.DashboardFragment
import org.odk.collect.android.wassan.fragments.OnFormSelectedListener
import org.odk.collect.android.wassan.model.User
import org.odk.collect.android.wassan.model.UserProject
import org.odk.collect.androidshared.system.IntentLauncher
import org.odk.collect.androidshared.ui.ToastUtils
import org.odk.collect.androidshared.ui.multiclicksafe.MultiClickGuard
import org.odk.collect.projects.Project
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.settings.keys.MetaKeys
import org.odk.collect.strings.localization.LocalizedActivity
import timber.log.Timber
import javax.inject.Inject

class MainActivity : LocalizedActivity(),CollectComposeThemeProvider,
    NavigationView.OnNavigationItemSelectedListener,
    OnFormSelectedListener {

    @Inject
    lateinit var settingsProvider: SettingsProvider

    @Inject
    lateinit var projectsRepository: ProjectsRepository

    @Inject
    lateinit var projectsDataService: ProjectsDataService

    @Inject
    lateinit var viewModelFactory: MainMenuViewModelFactory

    @Inject
    lateinit var intentLauncher: IntentLauncher

    private lateinit var currentProjectViewModel: CurrentProjectViewModel

    private val GOOGLE_PLAY_URL = "https://play.google.com/store/apps/details?id="

    private lateinit var progressDialog: ProgressDialog


    private val formLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            setResult(RESULT_OK, it.data)
            //finish()
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerUtils.getComponent(this).inject(this)
        setContentView(R.layout.activity_main)

        val updater = AppUpdater(
            activity = this,
            updateJsonUrl = "https://ecocollect.wassan.org/version.json",
            forceUpdate = false // or true to block until updated
        )
        updater.checkForUpdate()

        progressDialog = ProgressDialog(this).apply {
            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }

        currentProjectViewModel = ViewModelProvider(this, viewModelFactory)
            .get(CurrentProjectViewModel::class.java)
        initUI()
    }


    private fun initUI() {
        initMapbox()
        initToolbar()
        initBottomNavigation()
        initSyncButton()
    }

    private fun initMapbox() {
        if (MapboxClassInstanceCreator.isMapboxAvailable()) {
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.map_box_initialization_fragment,
                    MapboxClassInstanceCreator.createMapBoxInitializationFragment()
                )
                .commit()
        }
    }

    private fun initToolbar() {
        val toolbar = findViewById<Toolbar>(org.odk.collect.androidshared.R.id.toolbar)
        setSupportActionBar(toolbar)

        currentProjectViewModel = ViewModelProvider(this).get(CurrentProjectViewModel::class.java)

        // Observe the current project
        currentProjectViewModel.currentProject.observe(this) { project ->
            project?.let {
                val (_, name) = it
                toolbar.subtitle = name
            }
        }

        val drawerLayout = findViewById<DrawerLayout>(R.id.main)
        val navView = findViewById<NavigationView>(R.id.nav_sidebar)
       // applyWaveDrawer(navView)
        navView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set header info
        val header = navView.getHeaderView(0)
        val fullNameTv = header.findViewById<TextView>(R.id.fullName)
        val roleTv = header.findViewById<TextView>(R.id.role)
        val photoIv = header.findViewById<ImageView>(R.id.imageView)
        val syncIcon = header.findViewById<ImageView>(R.id.syncIcon)

        // Apply safe area (status bar padding)
        ViewCompat.setOnApplyWindowInsetsListener(header) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = statusBarHeight)
            insets
        }


        try {
            val jsonUser = settingsProvider.getMetaSettings().getString(MetaKeys.KEY_USER)
            val user: User = Gson().fromJson(jsonUser, User::class.java)

            fullNameTv.text = user.fullname.ifEmpty { user.username }
            roleTv.text = user.position.ifEmpty { "Employee" }

            Glide.with(this)
                .load(user.image)
                .apply(RequestOptions.circleCropTransform().placeholder(R.drawable.profile))
                .into(photoIv)
        } catch (e: Exception) {
            Timber.e(e, "Error loading user info")
        }

        // --- FOOTER ---
        val footerVersion = navView.findViewById<TextView>(R.id.app_version)

        footerVersion?.let {
            try {
                val versionName = packageManager.getPackageInfo(packageName, 0).versionName
                it.text = "Version $versionName"
            } catch (e: PackageManager.NameNotFoundException) {
                it.text = "Version Unknown"
            }
        }

        syncIcon.setOnClickListener {
            syncUserProjects()
        }
    }

    private fun syncUserProjects() {
        val gson = Gson()
        val userJson = settingsProvider.getMetaSettings().getString(MetaKeys.KEY_USER)
        if (userJson.isNullOrEmpty()) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            return
        }

        val user: User = gson.fromJson(userJson, User::class.java)
        val api = ApiClient.create(this)
        val call = api.getUserProjects(user.id)

        // Show progress dialog
        progressDialog.setMessage("Syncing projects...")
        progressDialog.show()

        call.enqueue(object : retrofit2.Callback<UserProjectsResponse> {
            override fun onResponse(call: retrofit2.Call<UserProjectsResponse>, response: retrofit2.Response<UserProjectsResponse>) {
                progressDialog.dismiss()
                if (response.isSuccessful && response.body()?.status == true) {
                    val projects = response.body()?.userProjects ?: emptyList()
                    handleProjectResponse(projects, user)
                } else {
                    Toast.makeText(applicationContext, "Failed to sync projects", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<UserProjectsResponse>, t: Throwable) {
                progressDialog.dismiss()
                Toast.makeText(applicationContext, t.localizedMessage ?: "Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleProjectResponse(projects: List<UserProject>, user: User) {
        if (projects.isEmpty()) {
            Toast.makeText(this, "No projects available", Toast.LENGTH_SHORT).show()
            return
        }

        // Clear existing projects
        projectsRepository.deleteAll()

        // Save all projects
        projects.forEach { project ->
            projectsRepository.save(
                Project.Saved(
                    project.centralProjectId,
                    project.projectName,
                    project.icon,
                    project.color
                )
            )
        }

        // Set default project as current if exists, else first project
        val defaultProject = user.defaultProject ?: projects.first()
        projectsDataService.setCurrentProject(defaultProject.centralProjectId)

        // Relaunch or refresh dashboard
        //launchDashboard()
    }
    private fun initBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.background = null
        bottomNav.menu.getItem(2).isEnabled = false
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.dashboard -> loadFragment(DashboardFragment(), "Dashboard")
                R.id.community -> loadFragment(CommunityFragment(), "Community")
                R.id.crop -> loadFragment(CalculatorFragment(), "Crop Calculator")
                R.id.account -> loadFragment(AccountFragment(), "Account")
                else -> false
            }
        }
        loadFragment(DashboardFragment(), "Dashboard")
    }

    private fun initSyncButton() {
        findViewById<FloatingActionButton>(R.id.newform).setOnClickListener {
            startActivity(Intent(this, FormDownloadListActivity::class.java))
        }
    }

    private fun applyWaveDrawer(navView: NavigationView) {
        val radius = resources.getDimension(R.dimen.drawer_curve_radius) // e.g., 60dp
        val shapeDrawable = MaterialShapeDrawable().apply {
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setTopRightCorner(CornerFamily.ROUNDED, radius)
                .setBottomRightCorner(CornerFamily.ROUNDED, radius)
                .build()
            fillColor = ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50"))
        }
        navView.background = shapeDrawable
    }

    private fun loadFragment(fragment: Fragment, title: String): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.framecontainer, fragment)
            .commit()
        setTitle(title)
        return true
    }

    override fun onFormSelected(formUri: Uri) {
        if (Intent.ACTION_PICK == intent.action) {
            // Caller is waiting on a picked form
            setResult(Activity.RESULT_OK, Intent().setData(formUri))
            finish()
        } else {
            // Caller wants to view/edit a form, so launch FormFillingActivity
            formLauncher.launch(FormFillingIntentFactory.newFormIntent(this, formUri))
        }
    }

    override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
        val drawerLayout = findViewById<DrawerLayout>(R.id.main)
        when (item.itemId) {
            R.id.nav_logout -> {
                settingsProvider.getMetaSettings().save(MetaKeys.IS_LOGIN, false)
                ActivityUtils.startActivityAndCloseAllOthers(this, LoginActivity::class.java)
            }
            R.id.nav_form -> {
                val intent = Intent(this, MainMenuActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) // Prevent multiple instances
                startActivity(intent)
            }
            R.id.nav_share -> { shareApp() }
            R.id.nav_about -> { aboutApp() }
            R.id.nav_rate -> { addReview() }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                getString(org.odk.collect.strings.R.string.tell_your_friends_msg) + " " + GOOGLE_PLAY_URL + packageName
            )
        }
        startActivity(
            Intent.createChooser(
                shareIntent,
                getString(org.odk.collect.strings.R.string.tell_your_friends)
            )
        )
    }

    private fun addReview() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        )
        intentLauncher.launch(this, intent) {
            // Show a list of all available browsers if user doesn't have a default browser
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(GOOGLE_PLAY_URL + packageName)
                )
            )
        }
    }

    private fun aboutApp(){
        val aboutServerURL = getString(org.odk.collect.strings.R.string.web_server_url)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(aboutServerURL))
        startActivity(browserIntent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!MultiClickGuard.allowClick(javaClass.name)) {
            return true
        }
        if (item.itemId == R.id.projects) {
            val dialog = ProjectSettingsDialog(viewModelFactory)  // Now you can pass the viewModelFactory here
            dialog.show(supportFragmentManager, ProjectSettingsDialog::class.java.simpleName)
            dialog.onProjectSwitchListener = { project ->
                switchProject(project)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun switchProject(project: Project.Saved) {
        // Switch project logic
        currentProjectViewModel.setCurrentProject(project)

        ActivityUtils.startActivityAndCloseAllOthers(this, MainActivity::class.java)
        ToastUtils.showLongToast(
            getString(org.odk.collect.strings.R.string.switched_project, project.name)
        )
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val projectsMenuItem = menu?.findItem(R.id.projects)
        (projectsMenuItem?.actionView as ProjectIconView).apply {
            project = currentProjectViewModel.currentProject.value
            setOnClickListener { onOptionsItemSelected(projectsMenuItem) }
            contentDescription = getString(org.odk.collect.strings.R.string.projects)
        }
        return super.onPrepareOptionsMenu(menu)
    }
}

