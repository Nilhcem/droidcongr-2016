package com.nilhcem.droidcongr.scraper

import com.nilhcem.droidcongr.scraper.model.Room
import com.nilhcem.droidcongr.scraper.model.Session
import com.nilhcem.droidcongr.scraper.model.Speaker
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Whitelist
import org.jsoup.select.Elements
import kotlin.text.RegexOption.IGNORE_CASE

class Scraper {

    val speakers = mutableListOf<Speaker>()
    val sessions = mutableListOf<Session>()

    companion object {
        val BASE_URL = "http://2016.droidcon.gr/speakers/"
    }

    init {
        println("Start scraping")
        val urlImgs = jsoup(BASE_URL)
                .select(".w-portfolio-list .w-portfolio-item")
                .map {
                    val url = it.select("a").attr("href")
                    val speakerImg = it.select("img").attr("src")
                    UrlImg(url, speakerImg)
                }
                .filter { it.url != "http://2016.droidcon.gr/speakers/uploading/" }
                .distinct()
                .sortedBy { it.url }

        urlImgs.forEachIndexed { index, urlImg ->
            println("Get speaker: #$index - url: ${urlImg.url}")
            with(jsoup(urlImg.url).body()) {
                speakers.add(parseSpeaker(index, urlImg.img, this))
                sessions.addAll(parseSessions(index, sessions.size, this))
            }
        }
    }

    private fun parseSpeaker(id: Int, photo: String, element: Element): Speaker {
        with (element) {
            val name = select(".l-titlebar-content h1").fmtText()
            val title = select(".l-titlebar-content p").fmtText()

            val section = select(".w-tabs-sections-h .w-tabs-section")[0]
            val bio = section.select(".wpb_text_column .wpb_wrapper p").fmtText()
            val website = section.select(".w-socials-list .custom a").attr("href")
            val twitter = section.select(".w-socials-list .twitter a").attr("href")
            val google = section.select(".w-socials-list .google a").attr("href")
            val github = section.select(".w-socials-list .github a").attr("href")

            return Speaker(
                    id + 1, name, title, photo, bio,
                    if (website.length == 0) (if (google.length == 0) null else google) else website,
                    getHandleFromUrl(twitter),
                    getHandleFromUrl(github)
            )
        }
    }

    private fun parseSessions(speakerId: Int, startId: Int, element: Element): List<Session> {
        with (element) {
            val sections = select(".w-tabs-sections-h .w-tabs-section")
            val nbSessions = sections[1].select(".wpb_wrapper h4").size

            return (0..nbSessions - 1).map { i ->
                val id = startId + i + 1
                val title = sections[1].select(".wpb_wrapper h4")[i].fmtText()
                val description = if (nbSessions == 1) {
                    sections[2].select(".wpb_wrapper").fmtText()
                } else {
                    sections[2].select(".wpb_wrapper p")[i].fmtText()
                }
                val speakersId = listOf(speakerId + 1)
                val duration = sections[3].select(".w-tabs-section-content-h .g-cols")[i]
                        .select(".one-quarter")[2].select("h4").fmtText().replace("~", "")
                        .replace(" minutes", "").replace("30-40", "40").toInt()

                Session(id, title, description, speakersId, "2016-07-07 10:00", duration, Room.HALL_A.id)
            }.toList()
        }
    }

    private fun getHandleFromUrl(url: String?): String? {
        if (url == null || url.length == 0) {
            return null
        }

        val urlWithoutLastSlash = if (url.last() == '/') url.substring(0, url.length - 1) else url
        return urlWithoutLastSlash.substring(urlWithoutLastSlash.lastIndexOf("/") + 1)
    }

    private fun jsoup(url: String, nbRetries: Int = 3): Document {
        (0..nbRetries).forEach {
            try {
                return Jsoup.connect(url).get()
            } catch (e: Exception) {
                System.err.println("Error: ${e.message}. Retry")
                if (it == nbRetries - 1) {
                    throw e
                }
            }
        }
        throw IllegalStateException("This should not happen")
    }

    private fun Element.fmtText() = formatText(html())
    private fun Elements.fmtText() = formatText(html())
    private fun formatText(html: String) = Jsoup.clean(html, "", Whitelist.basic(),
            Document.OutputSettings().prettyPrint(false))
            .replace(Regex("&nbsp;", IGNORE_CASE), " ")
            .replace(Regex("&amp;", IGNORE_CASE), "&")
            .replace(Regex("&gt;", IGNORE_CASE), ">").replace(Regex("&lt;", IGNORE_CASE), "<")
            .replace(Regex("<br[\\s/]*>", IGNORE_CASE), "\n")
            .replace(Regex("<p>", IGNORE_CASE), "").replace(Regex("</p>", IGNORE_CASE), "\n")
            .replace(Regex("</?ul>", IGNORE_CASE), "")
            .replace(Regex("<li>", IGNORE_CASE), "• ").replace(Regex("</li>", IGNORE_CASE), "\n")
            .replace(Regex("\n\n• ", IGNORE_CASE), "\n• ")
            .replace(Regex("<a\\s[^>]*>", IGNORE_CASE), "").replace(Regex("</a>", IGNORE_CASE), "")
            .replace(Regex("</?strong>", IGNORE_CASE), "")
            .replace(Regex("</?em>", IGNORE_CASE), "")
            .replace(Regex("\\s*\n\\s*"), "\n").replace(Regex("^\n"), "").replace(Regex("\n$"), "")

    data class UrlImg(val url: String, val img: String)
}
