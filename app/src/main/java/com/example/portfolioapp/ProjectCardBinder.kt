package com.example.portfolioapp

import android.view.View
import android.widget.TextView

// Fills the compact project card (view_project_card.xml) with a project's data.
// Used by the Flashcard screen AND when the card is shared as an image.
object ProjectCardBinder {

    fun bind(card: View, p: Project, owner: String) {
        card.findViewById<TextView>(R.id.tvCCategory).text = p.category
        card.findViewById<TextView>(R.id.tvCTitle).text = p.title

        val tagline = card.findViewById<TextView>(R.id.tvCTagline)
        tagline.text = p.tagline
        tagline.visibility = if (p.tagline.isBlank()) View.GONE else View.VISIBLE

        section(card, R.id.boxOverview, R.id.tvCOverview, p.description)
        section(card, R.id.boxTech, R.id.tvCTech, p.techStack)
        section(card, R.id.boxProblem, R.id.tvCProblem, p.problem)
        section(card, R.id.boxSolution, R.id.tvCSolution, p.solution)
        section(card, R.id.boxRole, R.id.tvCRole, p.role)
        section(card, R.id.boxOutcome, R.id.tvCOutcome, p.outcome)

        val links = mutableListOf<String>()
        if (p.github.isNotBlank()) links.add("GitHub: ${p.github}")
        if (p.demoLink.isNotBlank()) links.add("Demo: ${p.demoLink}")
        if (p.pdfLink.isNotBlank()) links.add("Report: ${p.pdfLink}")
        else if (p.hasPdf) links.add("Report: PDF attached in the portfolio")
        section(card, R.id.boxLinks, R.id.tvCLinks, links.joinToString("\n"))

        card.findViewById<TextView>(R.id.tvCOwner).text =
            if (owner.isBlank()) "Shared from My Portfolio app" else "$owner  •  My Portfolio app"
    }

    private fun section(root: View, boxId: Int, textId: Int, value: String) {
        val box = root.findViewById<View>(boxId)
        if (value.isBlank()) {
            box.visibility = View.GONE
        } else {
            box.visibility = View.VISIBLE
            root.findViewById<TextView>(textId).text = value
        }
    }
}