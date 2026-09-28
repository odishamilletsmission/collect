package org.odk.collect.android.wassan.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.odk.collect.android.R
import org.odk.collect.android.activities.FormMapActivity
import org.odk.collect.android.activities.InstanceChooserList
import org.odk.collect.android.formentry.FormOpeningMode
import org.odk.collect.android.formlists.blankformlist.BlankFormListViewModel
import org.odk.collect.android.formlists.blankformlist.OnFormItemClickListener
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.instancemanagement.send.InstanceUploaderListActivity
import org.odk.collect.android.preferences.dialogs.ServerAuthDialogFragment
import org.odk.collect.android.projects.ProjectsDataService
import org.odk.collect.android.utilities.FormsRepositoryProvider
import org.odk.collect.android.utilities.InstancesRepositoryProvider
import org.odk.collect.android.wassan.app.FilterHelper
import org.odk.collect.android.wassan.listeners.FormActionListener
import org.odk.collect.android.wassan.model.DasboardFormListAdapter
import org.odk.collect.androidshared.ui.DialogFragmentUtils
import org.odk.collect.androidshared.ui.ObviousProgressBar
import org.odk.collect.androidshared.ui.SnackbarUtils
import org.odk.collect.androidshared.ui.SnackbarUtils.DURATION_SHORT
import org.odk.collect.lists.EmptyListView
import org.odk.collect.lists.RecyclerViewUtils
import org.odk.collect.permissions.PermissionListener
import org.odk.collect.permissions.PermissionsProvider
import javax.inject.Inject

class DashboardFragment : Fragment(), OnFormItemClickListener, FormActionListener {

    @Inject
    lateinit var viewModelFactory: BlankFormListViewModel.Factory

    @Inject
    lateinit var permissionsProvider: PermissionsProvider

    @Inject
    lateinit var formsRepositoryProvider: FormsRepositoryProvider

    @Inject
    lateinit var instancesRepositoryProvider: InstancesRepositoryProvider

    @Inject
    lateinit var projectsDataService: ProjectsDataService

    private val viewModel: BlankFormListViewModel by viewModels { viewModelFactory }
    private lateinit var adapter: DasboardFormListAdapter
    private var formSelectedListener: OnFormSelectedListener? = null

    private val formLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                // Optionally handle returned data if needed
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerUtils.getComponent(requireContext()).inject(this)
        adapter = DasboardFormListAdapter(this, this, instancesRepositoryProvider, projectsDataService)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        formSelectedListener = context as? OnFormSelectedListener
            ?: throw ClassCastException("$context must implement OnFormSelectedListener")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)

    }

    private fun initView(view: View) {
        val list = view.findViewById<RecyclerView>(R.id.dashboard_form_list)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.addItemDecoration(RecyclerViewUtils.verticalLineDivider(requireContext()))
        list.adapter = adapter

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            view.findViewById<ObviousProgressBar>(org.odk.collect.androidshared.R.id.progressBar)
                ?.apply { if (isLoading) show() else hide() }
        }

        viewModel.syncResult.observe(viewLifecycleOwner) { result ->
            result?.let { SnackbarUtils.showSnackbar(requireView(), it, DURATION_SHORT) }
        }

        viewModel.formsToDisplay.observe(viewLifecycleOwner) { forms ->
            view.findViewById<RecyclerView>(R.id.form_list)?.visibility =
                if (forms.isEmpty()) View.GONE else View.VISIBLE

            view.findViewById<EmptyListView>(R.id.empty_list_message)?.visibility =
                if (forms.isEmpty()) View.VISIBLE else View.GONE

            adapter.setData(forms)
        }

        viewModel.isAuthenticationRequired().observe(viewLifecycleOwner) { required ->
            if (required) {
                DialogFragmentUtils.showIfNotShowing(
                    ServerAuthDialogFragment::class.java,
                    childFragmentManager
                )
            } else {
                DialogFragmentUtils.dismissDialog(
                    ServerAuthDialogFragment::class.java,
                    childFragmentManager
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            // force RecyclerView to refresh
            adapter.notifyDataSetChanged()

        }

    }

    override fun onFormClick(formUri: Uri) {
        formSelectedListener?.onFormSelected(formUri)
    }

    override fun onMapButtonClick(id: Long) {
        permissionsProvider.requestEnabledLocationPermissions(requireActivity(), object : PermissionListener {
            override fun granted() {
                startActivity(Intent(requireContext(), FormMapActivity::class.java).apply {
                    putExtra(FormMapActivity.EXTRA_FORM_ID, id)
                })
            }
        })
    }

    override fun onDraftClick(formId: String) {
        FilterHelper.getInstance().setFilterId(formId)
        formLauncher.launch(
            Intent(requireActivity(), InstanceChooserList::class.java).apply {
                putExtra(FormOpeningMode.FORM_MODE_KEY, FormOpeningMode.EDIT_SAVED)
            }
        )
    }

    override fun onReadyClick(formId: String) {
        FilterHelper.getInstance().setFilterId(formId)
        formLauncher.launch(
            Intent(requireActivity(), InstanceUploaderListActivity::class.java).apply {
                putExtra("FILTER_ID", formId)
            }
        )
    }

    override fun onSentClick(formId: String) {
        FilterHelper.getInstance().setFilterId(formId)
        startActivity(
            Intent(requireActivity(), InstanceChooserList::class.java).apply {
                putExtra(FormOpeningMode.FORM_MODE_KEY, FormOpeningMode.VIEW_SENT)
            }
        )
    }
}

interface OnFormSelectedListener {
    fun onFormSelected(formUri: Uri)
}
