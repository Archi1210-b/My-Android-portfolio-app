package com.example.portfolioapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Shows each project as a card in the list
class ProjectAdapter(
    private val onOpen: (Project) -> Unit,
    private val onShare: (Project) -> Unit,
    private val onEdit: (Project) -> Unit,
    private val onDelete: (Project) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.VH>() {

    private val items = mutableListOf<Project>()

    fun submit(list: List<Project>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val category: TextView = v.findViewById(R.id.tvCategory)
        val title: TextView = v.findViewById(R.id.tvTitle)
        val tagline: TextView = v.findViewById(R.id.tvTagline)
        val tech: TextView = v.findViewById(R.id.tvTech)
        val links: TextView = v.findViewById(R.id.tvLinks)
        val btnCard: Button = v.findViewById(R.id.btnCard)
        val btnShare: Button = v.findViewById(R.id.btnShare)
        val btnEdit: Button = v.findViewById(R.id.btnEdit)
        val btnDelete: Button = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val p = items[position]
        h.category.text = p.category
        h.title.text = p.title
        h.tagline.text = p.tagline
        h.tagline.visibility = if (p.tagline.isBlank()) View.GONE else View.VISIBLE
        h.tech.text = if (p.techStack.isBlank()) "" else "🛠 ${p.techStack}"
        h.tech.visibility = if (p.techStack.isBlank()) View.GONE else View.VISIBLE

        val links = mutableListOf<String>()
        if (p.hasPdf || p.pdfLink.isNotBlank()) links.add("📄 PDF")
        if (p.github.isNotBlank()) links.add("GitHub")
        if (p.demoLink.isNotBlank()) links.add("Live demo")
        h.links.text = links.joinToString("  ·  ")
        h.links.visibility = if (links.isEmpty()) View.GONE else View.VISIBLE

        h.itemView.setOnClickListener { onOpen(p) }
        h.btnCard.setOnClickListener { onOpen(p) }
        h.btnShare.setOnClickListener { onShare(p) }
        h.btnEdit.setOnClickListener { onEdit(p) }
        h.btnDelete.setOnClickListener { onDelete(p) }
    }

    override fun getItemCount() = items.size
}