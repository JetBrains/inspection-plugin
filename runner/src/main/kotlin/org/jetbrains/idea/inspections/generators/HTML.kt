package org.jetbrains.idea.inspections.generators

import org.apache.commons.lang.StringEscapeUtils

class HTML private constructor(private val builder: StringBuilder, private val indent: Int) {

    constructor() : this(StringBuilder(), 0)

    class LineHTML(private val builder: StringBuilder) {
        fun text(string: String) {
            val text = StringEscapeUtils.escapeHtml(string)
            builder.append(text)
        }

        fun tag(name: String, generate: LineHTML.() -> Unit) {
            builder.append("<$name>")
            generate()
            builder.append("</$name>")
        }

        fun tagIf(condition: Boolean, name: String, generate: LineHTML.() -> Unit) {
            when (condition) {
                true -> tag(name, generate)
                false -> generate()
            }
        }
    }

    val result: String
        get() = builder.toString()

    private val prefix: String
        get() = "    ".repeat(indent)

    fun snapshot(): HTML {
        return HTML(StringBuilder(), indent)
    }

    fun merge(html: HTML, force: Boolean = false) {
        if (!force && indent != html.indent) {
            throw IllegalArgumentException("Cannot merge a html code with different indents.")
        }
        builder.append(html.builder)
    }

    fun line(generate: LineHTML.() -> Unit) {
        builder.append(prefix)
        raw(generate)
    }

    fun raw(generate: LineHTML.() -> Unit) {
        LineHTML(builder).generate()
        builder.appendln()
    }

    fun text(string: String) {
        val text = StringEscapeUtils.escapeHtml(string)
        builder.appendln(text)
    }

    fun tag(name: String, generate: HTML.() -> Unit) {
        builder.appendln("$prefix<$name>")
        HTML(builder, indent + 1).generate()
        builder.appendln("$prefix</$name>")
    }

    fun tagIf(condition: Boolean, name: String, generate: HTML.() -> Unit) {
        when (condition) {
            true -> tag(name, generate)
            false -> generate()
        }
    }
}
