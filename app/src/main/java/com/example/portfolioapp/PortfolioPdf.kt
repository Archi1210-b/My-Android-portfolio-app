package com.example.portfolioapp

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Chunk
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.Image
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.ColumnText
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfPageEventHelper
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.draw.LineSeparator
import java.io.File
import java.io.FileOutputStream

// Builds a formal, recruiter-ready PDF of the whole portfolio (uses the iText library).
// - links to GitHub / LinkedIn / demo / email are clickable
// - project list entries jump to the project's detail page inside the PDF
// - the uploaded resume and project report PDFs are added at the end and linked from the project pages
object PortfolioPdf {
    private val BLUE = BaseColor(29, 78, 216)
    private val DARK = BaseColor(30, 58, 138)
    private val GREY = BaseColor(107, 114, 128)
    private val TEXT = BaseColor(17, 24, 39)
    private val LIGHT = BaseColor(219, 234, 254)
    private val WHITE = BaseColor(255, 255, 255)
    private val LINE = BaseColor(229, 231, 235)

    // ---------- step 1: collect the uploaded PDFs, then build on a background thread ----------
    fun create(
        context: Context,
        profile: DocumentSnapshot?,
        projects: List<Project>,
        onDone: (File?, String?) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val ui = Handler(Looper.getMainLooper())
        val attachments = HashMap<String, ByteArray>()

        // attachment id -> firestore document key
        val wanted = ArrayList<Pair<String, String>>()
        if (profile?.getBoolean("hasResume") == true) wanted.add("resume" to "resume")
        for (p in projects) if (p.hasPdf) wanted.add(p.id to "project_${p.id}")

        fun build() {
            Thread {
                try {
                    val name = (profile?.getString("name") ?: "").replace(Regex("[^A-Za-z0-9]"), "_")
                    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                    val file = File(dir, if (name.isBlank()) "Portfolio.pdf" else "Portfolio_$name.pdf")
                    write(file, profile, projects, attachments)
                    ui.post { onDone(file, null) }
                } catch (e: Throwable) {
                    ui.post { onDone(null, e.message) }
                }
            }.start()
        }

        if (wanted.isEmpty()) {
            build()
            return
        }
        var remaining = wanted.size
        for ((id, key) in wanted) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("files").document(key).get()
                .addOnSuccessListener { d ->
                    val data = d.getString("data")
                    if (data != null) attachments[id] = Base64.decode(data, Base64.NO_WRAP)
                }
                .addOnCompleteListener {
                    remaining--
                    if (remaining == 0) build()
                }
        }
    }

    // ---------- small helpers ----------
    private fun font(size: Float, style: Int = Font.NORMAL, color: BaseColor = TEXT) =
        Font(Font.FontFamily.HELVETICA, size, style, color)

    // the PDF base font only supports basic Latin letters, so emojis etc. are removed
    private fun clean(s: String?): String {
        if (s == null) return ""
        return s.filter { it == '\n' || it.code in 32..126 || it.code in 160..255 }
    }

    private fun fixUrl(u: String) = if (u.startsWith("http", true)) u else "https://$u"

    private fun anchor(label: String, url: String, size: Float = 10f): Chunk =
        Chunk(label, font(size, Font.UNDERLINE, BLUE)).setAnchor(url)

    private fun jump(label: String, dest: String, size: Float = 10f): Chunk =
        Chunk(label, font(size, Font.UNDERLINE, BLUE)).setLocalGoto(dest)

    private fun joinChunks(items: List<Chunk>, spacingBefore: Float): Paragraph {
        val p = Paragraph()
        p.setSpacingBefore(spacingBefore)
        items.forEachIndexed { i, c ->
            if (i > 0) p.add(Chunk("   |   ", font(10f, Font.NORMAL, GREY)))
            p.add(c)
        }
        return p
    }

    private fun heading(doc: Document, title: String, dest: String? = null) {
        val c = Chunk(title.uppercase(), font(12f, Font.BOLD, DARK))
        if (dest != null) c.setLocalDestination(dest)
        val p = Paragraph(c)
        p.setSpacingBefore(16f)
        doc.add(p)
        doc.add(Chunk(LineSeparator(1f, 100f, BLUE, Element.ALIGN_CENTER, -2f)))
        doc.add(Paragraph(" ", font(4f)))
    }

    private fun labeled(doc: Document, label: String, value: String) {
        if (value.isBlank()) return
        val p = Paragraph()
        p.setSpacingBefore(4f)
        p.setLeading(13f)
        p.add(Chunk("$label: ", font(10f, Font.BOLD, DARK)))
        p.add(Chunk(value, font(10f)))
        doc.add(p)
    }

    private fun cell(content: Phrase): PdfPCell {
        val c = PdfPCell(content)
        c.setBorderColor(LINE)
        c.setPadding(6f)
        return c
    }

    private fun linksPhrase(p: Project, i: Int, attachments: Map<String, ByteArray>): Phrase {
        val items = mutableListOf<Chunk>()
        if (p.github.isNotBlank()) items.add(anchor("GitHub", fixUrl(p.github), 9f))
        if (p.demoLink.isNotBlank()) items.add(anchor("Live demo", fixUrl(p.demoLink), 9f))
        if (p.pdfLink.isNotBlank()) items.add(anchor("Report (link)", fixUrl(p.pdfLink), 9f))
        if (attachments.containsKey(p.id)) items.add(jump("Report (PDF)", "rep$i", 9f))
        val ph = Phrase()
        items.forEachIndexed { k, c ->
            if (k > 0) ph.add(Chunk("\n"))
            ph.add(c)
        }
        if (items.isEmpty()) ph.add(Chunk("-", font(9f)))
        return ph
    }

    // ---------- step 2: write the PDF ----------
    private fun write(
        file: File,
        profile: DocumentSnapshot?,
        projects: List<Project>,
        attachments: Map<String, ByteArray>
    ) {
        fun g(key: String) = clean(profile?.getString(key)).trim()

        val name = g("name").ifBlank { "Student" }
        val doc = Document(PageSize.A4, 40f, 40f, 40f, 55f)
        val writer = PdfWriter.getInstance(doc, FileOutputStream(file))
        writer.setPageEvent(FooterEvent())
        doc.addTitle("Portfolio - $name")
        doc.addAuthor(name)
        doc.open()

        // ----- header band: photo + name -----
        val band = PdfPTable(floatArrayOf(1.4f, 4f))
        band.setWidthPercentage(100f)

        val photoCell = PdfPCell()
        photoCell.setBorder(Rectangle.NO_BORDER)
        photoCell.setBackgroundColor(DARK)
        photoCell.setPadding(14f)
        photoCell.setVerticalAlignment(Element.ALIGN_MIDDLE)
        var photoAdded = false
        val photoText = profile?.getString("photo")
        if (!photoText.isNullOrBlank()) {
            try {
                val img = Image.getInstance(Base64.decode(photoText, Base64.NO_WRAP))
                img.scaleToFit(84f, 84f)
                photoCell.addElement(img)
                photoAdded = true
            } catch (e: Exception) {
                photoAdded = false
            }
        }
        if (!photoAdded) photoCell.addElement(Paragraph(Utils.initials(name), font(30f, Font.BOLD, WHITE)))

        val infoCell = PdfPCell()
        infoCell.setBorder(Rectangle.NO_BORDER)
        infoCell.setBackgroundColor(DARK)
        infoCell.setPadding(14f)
        infoCell.setVerticalAlignment(Element.ALIGN_MIDDLE)
        infoCell.addElement(Paragraph(name, font(22f, Font.BOLD, WHITE)))
        if (g("headline").isNotEmpty()) infoCell.addElement(Paragraph(g("headline"), font(11.5f, Font.NORMAL, WHITE)))
        val edu = listOf(g("college"), g("branch")).filter { it.isNotEmpty() }.joinToString("  |  ")
        if (edu.isNotEmpty()) infoCell.addElement(Paragraph(edu, font(10f, Font.NORMAL, LIGHT)))

        band.addCell(photoCell)
        band.addCell(infoCell)
        doc.add(band)

        // ----- contact + clickable links -----
        val contact = mutableListOf<Chunk>()
        if (g("email").isNotEmpty()) contact.add(anchor(g("email"), "mailto:" + g("email")))
        if (g("phone").isNotEmpty()) contact.add(Chunk(g("phone"), font(10f)))
        if (g("location").isNotEmpty()) contact.add(Chunk(g("location"), font(10f)))
        if (contact.isNotEmpty()) doc.add(joinChunks(contact, 10f))

        val links = mutableListOf<Chunk>()
        if (g("github").isNotEmpty()) links.add(anchor("GitHub", fixUrl(g("github"))))
        if (g("linkedin").isNotEmpty()) links.add(anchor("LinkedIn", fixUrl(g("linkedin"))))
        if (attachments.containsKey("resume")) links.add(jump("Resume (PDF attached)", "resume_pdf"))
        else if (g("resumeLink").isNotEmpty()) links.add(anchor("Resume", fixUrl(g("resumeLink"))))
        if (links.isNotEmpty()) doc.add(joinChunks(links, 4f))

        // ----- about -----
        if (g("bio").isNotEmpty()) {
            heading(doc, "About me")
            val p = Paragraph(g("bio"), font(10.5f))
            p.setLeading(14f)
            doc.add(p)
        }

        // ----- education -----
        val hasEdu = listOf(g("college"), g("branch"), g("gradYear"), g("cgpa")).any { it.isNotEmpty() }
        if (hasEdu) {
            heading(doc, "Education")
            labeled(doc, "College", g("college"))
            labeled(doc, "Branch", g("branch"))
            labeled(doc, "Graduation year", g("gradYear"))
            labeled(doc, "CGPA", g("cgpa"))
        }

        // ----- skills -----
        val skills = g("skills").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (skills.isNotEmpty()) {
            heading(doc, "Skills")
            doc.add(Paragraph(skills.joinToString("   |   "), font(10.5f)))
        }

        // ----- project list (table) -----
        heading(doc, "Projects (${projects.size})", "projects")
        if (projects.isEmpty()) {
            doc.add(Paragraph("No projects added yet.", font(10f, Font.NORMAL, GREY)))
        } else {
            val note = Paragraph("Tap a project name to jump to its details. Tap a link to open it.", font(8.5f, Font.ITALIC, GREY))
            note.setSpacingAfter(6f)
            doc.add(note)

            val table = PdfPTable(floatArrayOf(0.6f, 3.2f, 2.8f, 2.6f))
            table.setWidthPercentage(100f)
            for (h in listOf("#", "Project", "Tech stack", "Links")) {
                val c = PdfPCell(Phrase(h, font(9.5f, Font.BOLD, WHITE)))
                c.setBackgroundColor(BLUE)
                c.setBorderColor(BLUE)
                c.setPadding(6f)
                table.addCell(c)
            }
            table.setHeaderRows(1)
            projects.forEachIndexed { i, p ->
                table.addCell(cell(Phrase("${i + 1}", font(9.5f))))
                val titlePhrase = Phrase()
                titlePhrase.add(jump(clean(p.title), "proj$i", 10.5f))
                if (p.category.isNotBlank()) titlePhrase.add(Chunk("\n" + clean(p.category), font(8.5f, Font.NORMAL, GREY)))
                table.addCell(cell(titlePhrase))
                table.addCell(cell(Phrase(clean(p.techStack).ifEmpty { "-" }, font(9f))))
                table.addCell(cell(linksPhrase(p, i, attachments)))
            }
            doc.add(table)

            // ----- project details -----
            heading(doc, "Project details")
            projects.forEachIndexed { i, p ->
                val title = Chunk("${i + 1}. ${clean(p.title)}", font(14f, Font.BOLD, DARK))
                title.setLocalDestination("proj$i")
                val tp = Paragraph(title)
                tp.setSpacingBefore(12f)
                doc.add(tp)

                if (p.tagline.isNotBlank()) doc.add(Paragraph(clean(p.tagline), font(10.5f, Font.ITALIC, GREY)))
                val meta = listOf(clean(p.category), clean(p.techStack)).filter { it.isNotBlank() }.joinToString("   |   ")
                if (meta.isNotEmpty()) doc.add(Paragraph(meta, font(9.5f, Font.BOLD, BLUE)))

                labeled(doc, "Overview", clean(p.description))
                labeled(doc, "Problem", clean(p.problem))
                labeled(doc, "Solution", clean(p.solution))
                labeled(doc, "My role", clean(p.role))
                labeled(doc, "Outcome", clean(p.outcome))

                val pl = mutableListOf<Chunk>()
                if (p.github.isNotBlank()) pl.add(anchor("GitHub", fixUrl(p.github)))
                if (p.demoLink.isNotBlank()) pl.add(anchor("Live demo", fixUrl(p.demoLink)))
                if (p.pdfLink.isNotBlank()) pl.add(anchor("Report (link)", fixUrl(p.pdfLink)))
                if (attachments.containsKey(p.id)) pl.add(jump("Open report (PDF)", "rep$i"))
                if (pl.isNotEmpty()) doc.add(joinChunks(pl, 6f))

                val back = Paragraph(jump("< Back to project list", "projects", 8.5f))
                back.setSpacingBefore(4f)
                doc.add(back)
                doc.add(Chunk(LineSeparator(0.5f, 100f, LINE, Element.ALIGN_CENTER, -4f)))
            }
        }

        // ----- appendix: resume + project report PDFs -----
        val appendix = mutableListOf<Triple<String, String, ByteArray>>()
        attachments["resume"]?.let { appendix.add(Triple("resume_pdf", "Resume", it)) }
        projects.forEachIndexed { i, p ->
            attachments[p.id]?.let { appendix.add(Triple("rep$i", "Project report: ${clean(p.title)}", it)) }
        }
        for ((dest, title, bytes) in appendix) {
            try {
                appendPdf(doc, writer, dest, title, bytes)
            } catch (e: Exception) {
                // a broken or protected PDF is simply skipped
            }
        }

        doc.close()
    }

    // Draws every page of an uploaded PDF onto a new page of the portfolio
    private fun appendPdf(doc: Document, writer: PdfWriter, dest: String, title: String, bytes: ByteArray) {
        val reader = PdfReader(bytes)
        try {
            for (page in 1..reader.numberOfPages) {
                doc.newPage()
                val label = if (page == 1) title else "$title (page $page)"
                val head = Chunk(label, font(11f, Font.BOLD, DARK))
                if (page == 1) head.setLocalDestination(dest)
                doc.add(Paragraph(head))
                doc.add(Paragraph(jump("< Back to project list", "projects", 8.5f)))

                val size = reader.getPageSize(page)
                val availW = doc.right() - doc.left()
                val availH = doc.top() - doc.bottom() - 55f
                val scale = minOf(availW / size.width, availH / size.height, 1f)
                val imported = writer.getImportedPage(reader, page)
                val x = doc.left() + (availW - size.width * scale) / 2
                writer.directContent.addTemplate(imported, scale, 0f, 0f, scale, x, doc.bottom())
            }
        } finally {
            writer.freeReader(reader)
            reader.close()
        }
    }
}

// Footer on every page: "Created with My Portfolio app | Page n"
private class FooterEvent : PdfPageEventHelper() {
    override fun onEndPage(writer: PdfWriter, document: Document) {
        val f = Font(Font.FontFamily.HELVETICA, 8f, Font.NORMAL, BaseColor(107, 114, 128))
        ColumnText.showTextAligned(
            writer.directContent, Element.ALIGN_CENTER,
            Phrase("Created with My Portfolio app   |   Page ${writer.pageNumber}", f),
            (document.right() + document.left()) / 2, document.bottom() - 20, 0f
        )
    }
}