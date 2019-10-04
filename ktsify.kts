#!/usr/bin/env kscript

import java.io.File
import java.lang.Exception
import kotlin.system.exitProcess

if (args.size == 0 || args.size > 1) {
    println("""
        ktsify.kts: convert from build.gradle to build.gradle.kts
            
            ktsify helps you convert your gradle files to kotlin. As Groovy and Kotlin are
            quite different languages, it will miss some details but it can still save you 
            time for the most frequent transformations.

        ktsify.kts [file]: converts the given file. 
        
        ktsify.kts [directory]: converts all settings.gradle and build.gradle recursively 
            in directory.
    """.trimIndent())
    exitProcess(1)
}

fun traverse(file: File) {
    when {
        file.isDirectory -> {
            file.listFiles()?.forEach {
                traverse(it)
            }
        }
        !file.isFile -> Unit
        file.name == "build.gradle" || file.name == "build.gradle.old" || file.name == "settings.gradle" || file.name == "settings.gradle.old" -> {
            convert(file)
        }
    }
}

fun String.isExtra() = when {
    startsWith("dep") -> true
    startsWith("androidConfig") -> true
    this == "isCi" -> true
    else -> false
}

fun String.escapeExtra() = when {
    isExtra() -> "groovy.util.Eval.x(project, \"x.$this\")"
    else -> this
}


class Replacement(val from: String, val to: (List<String>) -> String, val import: String? = null, val escapeExtra: Boolean = false)

class Processor {
    private val replacements = mutableListOf<Replacement>()

    fun replace(from: String,  import: String? = null, to: (List<String>) -> String): Processor {
        replacements.add(Replacement(from, to, import))
        return this
    }

    fun replaceCheckExtra(from: String,  import: String? = null, to: (List<String>) -> String): Processor {
        replacements.add(Replacement(from, to, null, true))
        return this
    }

    fun process(text: String): String {
        return replacements.fold(text) { t, replacement ->
            var replaced = false
            val newText = t.replace(Regex(replacement.from)) {
                replaced = true
                replacement.to(it.groupValues.map {
                    if (replacement.escapeExtra) {
                        it.escapeExtra()
                    } else {
                        it
                    }
                })
            }

            if (replaced && replacement.import != null) {
                "import ${replacement.import}\n$newText"
            } else {
                newText
            }
        }
    }
}

val processor = Processor()
        .replace("'") {
            "\""
        }
        .replace("android \\{", "com.android.build.gradle.BaseExtension") {
            "extensions.findByType(BaseExtension::class.java)!!.apply {"
        }
        .replace("apply from\\: (.*)") {
            """
                apply {
                  from(${it[1]})
                }
            """.trimIndent()
        }
        .replaceCheckExtra("classpath (.*)") {
            "classpath(${it[1]})"
        }
        .replaceCheckExtra("apply plugin\\: *\"(.*)\"") {
            "apply(plugin = \"${it[1]}\")"
        }
        .replaceCheckExtra("compileSdkVersion (.*)") {
            "compileSdkVersion(${it[1]}.toString().toInt())"
        }
        .replaceCheckExtra("minSdkVersion (.*)") {
            "minSdkVersion(${it[1]}.toString())"
        }
        .replaceCheckExtra("targetSdkVersion (.*)") {
            "targetSdkVersion(${it[1]}.toString())"
        }
        .replaceCheckExtra("sourceCompatibility ([^=]+)") {
            "sourceCompatibility = ${it[1]}"
        }
        .replaceCheckExtra("targetCompatibility ([^=]+)") {
            "targetCompatibility = ${it[1]}"
        }
        .replaceCheckExtra("testInstrumentationRunner ([^=]+)") {
            "testInstrumentationRunner = ${it[1]}"
        }
        .replaceCheckExtra("preDexLibraries = (.*)") {
            "preDexLibraries = ${it[1]} as Boolean"
        }
        .replace("([^ ]*(?:ompileOnly|ompile|mplementation|api|rovided)) (.*)") {
            "add(\"${it[1]}\", ${it[2].escapeExtra()})"
        }

fun convert(file: File) {
    val ktsFile: File
    val oldFile: File?
    if (file.name == "build.gradle" || file.name == "settings.gradle") {
        ktsFile = File(file.parentFile, "${file.name}.kts")
        oldFile = File(file.parentFile, "${file.name}.old")
    } else if (file.name == "build.gradle.old" || file.name == "settings.gradle.old") {
        ktsFile = File(file.parentFile, "${file.name.substringBeforeLast(".")}.kts")
        oldFile = null
    } else {
        throw Exception("cannot convert from ${file.name}")
    }

    println("Converting ${file.path} to ${ktsFile.path}")
    val ktsText = processor.process(file.readText())
    ktsFile.writeText(ktsText)

    if (oldFile != null) {
        oldFile.writeText(file.readText())
        file.delete()
    }
}


val file = File(args[0])
if (!file.exists()) {
    println("${file.absolutePath} does not exist")
    exitProcess(1)
}
traverse(file)
