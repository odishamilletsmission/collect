package org.odk.collect.android.wassan.model

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import org.odk.collect.android.R
import org.odk.collect.android.database.instances.DatabaseInstanceColumns
import org.odk.collect.android.formlists.blankformlist.BlankFormListItem
import org.odk.collect.android.formlists.blankformlist.OnFormItemClickListener
import org.odk.collect.android.projects.ProjectsDataService
import org.odk.collect.android.utilities.InstancesRepositoryProvider
import org.odk.collect.android.wassan.app.InstanceCountHelper
import org.odk.collect.android.wassan.listeners.FormActionListener
import org.odk.collect.androidshared.ui.multiclicksafe.MultiClickGuard
import org.odk.collect.forms.instances.Instance

class DasboardFormListAdapter(
    val listener: OnFormItemClickListener,
    val formActionListener: FormActionListener,
    private val instancesRepositoryProvider: InstancesRepositoryProvider,
    private val projectsDataService: ProjectsDataService
) : RecyclerView.Adapter<DashboardFormListItemViewHolder>() {

    private var formItems = emptyList<BlankFormListItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardFormListItemViewHolder {
        return DashboardFormListItemViewHolder(parent).also {
            it.setTrailingView(R.layout.map_button)
        }
    }

    override fun onBindViewHolder(holder: DashboardFormListItemViewHolder, position: Int) {
        bindCounts(holder, position)
    }

    override fun onBindViewHolder(
        holder: DashboardFormListItemViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            // Only update counts
            bindCounts(holder, position)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun bindCounts(holder: DashboardFormListItemViewHolder, position: Int) {
        val item = formItems[position]
        holder.dashboardFormListItem = item

        val draftCount = holder.itemView.findViewById<TextView>(R.id.draftCount)
        val readyCount = holder.itemView.findViewById<TextView>(R.id.readyCount)
        val sentCount = holder.itemView.findViewById<TextView>(R.id.sentCount)

        val mapButton = holder.itemView.findViewById<Button>(R.id.map_button)
        mapButton.visibility = if (item.geometryPath.isNotBlank()) View.VISIBLE else View.GONE

        val currentProject = projectsDataService.requireCurrentProject()

        // Update counts dynamically
        draftCount.text = InstanceCountHelper.getInstanceCount(
            instancesRepositoryProvider,
            currentProject.uuid,
            "${DatabaseInstanceColumns.JR_FORM_ID} = ? AND ${DatabaseInstanceColumns.STATUS} IN (?, ?, ?)",
            arrayOf(item.formId, Instance.STATUS_INCOMPLETE, Instance.STATUS_INVALID, Instance.STATUS_VALID)
        ).toString()

        readyCount.text = InstanceCountHelper.getInstanceCount(
            instancesRepositoryProvider,
            currentProject.uuid,
            "${DatabaseInstanceColumns.JR_FORM_ID} = ? AND ${DatabaseInstanceColumns.STATUS} IN (?, ?)",
            arrayOf(item.formId, Instance.STATUS_COMPLETE, Instance.STATUS_SUBMISSION_FAILED)
        ).toString()

        sentCount.text = InstanceCountHelper.getInstanceCount(
            instancesRepositoryProvider,
            currentProject.uuid,
            "${DatabaseInstanceColumns.JR_FORM_ID} = ? AND ${DatabaseInstanceColumns.STATUS} = ?",
            arrayOf(item.formId, Instance.STATUS_SUBMITTED)
        ).toString()

        // Click listeners
        holder.itemView.setOnClickListener {
            if (MultiClickGuard.allowClick(javaClass.name)) {
                listener.onFormClick(item.contentUri)
            }
        }

        mapButton.setOnClickListener {
            if (MultiClickGuard.allowClick(javaClass.name)) {
                listener.onMapButtonClick(item.databaseId)
            }
        }

        holder.itemView.findViewById<LinearLayout>(R.id.btnDraft).setOnClickListener {
            if (MultiClickGuard.allowClick(javaClass.name)) {
                formActionListener.onDraftClick(item.formId)
            }
        }

        holder.itemView.findViewById<LinearLayout>(R.id.btnReady).setOnClickListener {
            if (MultiClickGuard.allowClick(javaClass.name)) {
                formActionListener.onReadyClick(item.formId)
            }
        }

        holder.itemView.findViewById<LinearLayout>(R.id.btnSent).setOnClickListener {
            if (MultiClickGuard.allowClick(javaClass.name)) {
                formActionListener.onSentClick(item.formId)
            }
        }
    }

    override fun getItemCount() = formItems.size

    /** Set full list of forms (initial load or refresh) */
    @SuppressLint("NotifyDataSetChanged")
    fun setData(blankFormItems: List<BlankFormListItem>) {
        this.formItems = blankFormItems.toList()
        notifyDataSetChanged()
    }

    /** Refresh only counts for visible items */
    fun refreshCounts() {
        notifyItemRangeChanged(0, formItems.size)
    }

    /** Return current adapter data */
    fun getData(): List<BlankFormListItem> = formItems

    /** Add new forms dynamically after download */
    fun addData(newForms: List<BlankFormListItem>) {
        formItems = formItems + newForms
        notifyDataSetChanged()
    }
}
