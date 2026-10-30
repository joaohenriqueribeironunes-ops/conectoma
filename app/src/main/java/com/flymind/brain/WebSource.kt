package com.flymind.brain

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.nio.charset.IllegalCharsetNameException
import java.nio.charset.UnsupportedCharsetException

data class WebSection(val title: String, val body: String)

data class WebPage(
    val url: String,
    val title: String,
    val host: String,
    val sections: List<WebSection>,
    val chars: Int,
)

/**
 * Busca uma página e a transforma em trechos aprendíveis.
 *
 * Só usa a internet para buscar a URL que a pessoa pediu: nada é enviado,
 * nada é rastreado e apenas a página informada é lida (os links dela não são
 * seguidos).
 */
object WebSource {

    const val MAX_BYTES = 2_500_000
    const val TIMEOUT_MS = 15_000
    const val MAX_SECTIONS = 12
    const val MIN_SECTION_CHARS = 60
    const val MAX_SECTION_CHARS = 700

    /** Marca o começo do título de um trecho enquanto o HTML é achatado em texto. */
    private const val MARKER = "\u0001"

    private val SCRIPT_BLOCK =
        Regex("(?is)<(script|style|noscript|svg|template|iframe|canvas|object|embed|form)\\b[^>]*>.*?</\\1>")
    private val COMMENT = Regex("(?s)<!--.*?-->")
    private val TITLE_TAG = Regex("(?is)<title[^>]*>(.*?)</title>")
    private val ANY_TAG = Regex("(?s)<[^>]+>")
    private val HEAD_START = Regex("(?i)<h[1-6][^>]*>")
    private val HEAD_END = Regex("(?i)</h[1-6]\\s*>")
    private val BLOCK_END =
        Regex("(?i)</?(p|div|li|ul|ol|tr|td|th|section|article|aside|header|blockquote|pre|br)\\b[^>]*>")

    /** Aceita URL com ou sem esquema; recusa fora de http/https. */
    fun normalize(raw: String): String {
        val value = raw.trim()
        require(value.isNotEmpty()) { "URL vazia" }
        val withScheme = if (value.contains("://")) value else "https://$value"
        val url = URL(withScheme)
        val protocol = url.protocol.lowercase()
        require(protocol == "http" || protocol == "https") { "só aceito URLs http/https" }
        require(url.host.isNotEmpty()) { "URL sem endereço" }
        return withScheme
    }

    fun fetch(rawUrl: String): WebPage {
        val url = normalize(rawUrl)
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty(
            "User-Agent",
            "Conectoma/1.3 (aprendizado de páginas; sem telemetria)",
        )
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,text/plain")

        val status = connection.responseCode
        require(status in 200..399) { "a página respondeu HTTP $status" }

        val declared = connection.contentType.orEmpty()
        val bytes = readCapped(connection)
        val html = String(bytes, charsetFor(declared, bytes))
        val host = URL(url).host.removePrefix("www.")
        val isHtml = declared.contains("html", ignoreCase = true) ||
            html.contains("<html", ignoreCase = true)

        val sections = if (isHtml) parseHtml(html) else parsePlain(html)
        return WebPage(
            url = url,
            title = pageTitle(html, sections, host),
            host = host,
            sections = sections.take(MAX_SECTIONS),
            chars = sections.sumOf { it.body.length },
        )
    }

    private fun readCapped(connection: HttpURLConnection): ByteArray {
        val stream = connection.inputStream ?: error("sem conteúdo")
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        stream.use {
            while (true) {
                val read = it.read(buffer)
                if (read <= 0) break
                val room = MAX_BYTES - total
                if (room <= 0) break
                out.write(buffer, 0, minOf(read, room))
                total += minOf(read, room)
            }
        }
        connection.disconnect()
        return out.toByteArray()
    }

    private fun charsetFor(contentType: String, bytes: ByteArray): Charset {
        val declared = Regex("(?i)charset\\s*=\\s*\"?([a-z0-9._-]+)")
            .find(contentType)?.groupValues?.get(1)
        val sniffed = declared ?: Regex("(?i)charset=[\"']?([a-z0-9._-]+)")
            .find(String(bytes, 0, minOf(bytes.size, 4096), Charsets.ISO_8859_1))
            ?.groupValues?.get(1)
        if (sniffed != null) {
            try {
                if (Charset.isSupported(sniffed)) return Charset.forName(sniffed)
            } catch (_: IllegalCharsetNameException) {
            } catch (_: UnsupportedCharsetException) {
            }
        }
        return Charsets.UTF_8
    }

    /**
     * Remove blocos ruidosos e devolve os trechos com título.
     *
     * Cada título abre um trecho novo: o texto até o próximo título vira o corpo
     * daquele trecho. Parágrafos curtos (menu, aviso de cookies) são descartados
     * para não ensinar lixo à mosca.
     */
    fun parseHtml(html: String): List<WebSection> {
        var text = SCRIPT_BLOCK.replace(html, " ")
        text = COMMENT.replace(text, " ")
        text = HEAD_END.replace(text, "\n")
        text = HEAD_START.replace(text, "\n" + MARKER)
        text = BLOCK_END.replace(text, "\n")
        text = ANY_TAG.replace(text, " ")
        text = decodeEntities(text)

        val sections = ArrayList<WebSection>()
        var title = ""
        val body = StringBuilder()

        fun flush() {
            val clean = collapse(body.toString())
            body.setLength(0)
            if (clean.length >= MIN_SECTION_CHARS) {
                sections.add(WebSection(collapse(title), clean.take(MAX_SECTION_CHARS)))
            }
            title = ""
        }

        for (line in text.lines()) {
            val trimmed = collapse(line)
            if (trimmed.startsWith(MARKER)) {
                val heading = collapse(trimmed.removePrefix(MARKER))
                if (heading.isNotEmpty()) {
                    if (body.isNotEmpty()) flush()
                    title = heading
                }
            } else if (trimmed.isNotEmpty()) {
                body.append(trimmed).append(' ')
            }
        }
        flush()
        if (sections.isEmpty()) {
            // Página sem blocos longos: guarda o texto inteiro como um trecho só.
            val whole = collapse(text)
            if (whole.length >= 40) {
                sections.add(WebSection(collapse(title), whole.take(MAX_SECTION_CHARS)))
            }
        }
        return sections.filter { it.body.isNotBlank() }
    }

    fun parsePlain(text: String): List<WebSection> {
        val paragraphs = text.lines()
            .map { collapse(it) }
            .filter { it.length >= MIN_SECTION_CHARS }
        if (paragraphs.isEmpty()) return emptyList()
        val title = paragraphs.first().split(Regex("[.!?]")).first().take(70)
        val body = paragraphs.take(MAX_SECTIONS).joinToString(" ").take(MAX_SECTION_CHARS)
        return listOf(WebSection(title, body))
    }

    private fun pageTitle(html: String, sections: List<WebSection>, host: String): String {
        val fromTag = TITLE_TAG.find(html)?.groupValues?.get(1).orEmpty()
        val clean = collapse(decodeEntities(ANY_TAG.replace(fromTag, " ")))
        return when {
            clean.isNotBlank() -> clean.take(90)
            sections.isNotEmpty() && sections.first().title.isNotBlank() ->
                sections.first().title.take(90)
            else -> host
        }
    }

    fun decodeEntities(text: String): String {
        var out = text
        val named = mapOf(
            "&nbsp;" to " ", "&amp;" to "&", "&lt;" to "<", "&gt;" to ">",
            "&quot;" to "\"", "&#39;" to "'", "&apos;" to "'",
            "&mdash;" to "—", "&ndash;" to "–", "&hellip;" to "...",
            "&aacute;" to "á", "&eacute;" to "é", "&iacute;" to "í", "&oacute;" to "ó",
            "&uacute;" to "ú", "&atilde;" to "ã", "&otilde;" to "õ", "&ccedil;" to "ç",
            "&Aacute;" to "Á", "&Eacute;" to "É", "&Iacute;" to "Í", "&Oacute;" to "Ó",
            "&Uacute;" to "Ú", "&Atilde;" to "Ã", "&Otilde;" to "Õ", "&Ccedil;" to "Ç",
        )
        for ((entity, value) in named) out = out.replace(entity, value, ignoreCase = true)
        out = Regex("&#(\\d{1,7});").replace(out) { m ->
            m.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: m.value
        }
        out = Regex("&#x([0-9a-fA-F]{1,6});").replace(out) { m ->
            m.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: m.value
        }
        return out
    }

    fun collapse(value: String): String = value.replace(Regex("\\s+"), " ").trim()
}
