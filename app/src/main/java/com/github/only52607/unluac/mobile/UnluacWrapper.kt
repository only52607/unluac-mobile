package com.github.only52607.unluac.mobile

import java.io.ByteArrayOutputStream
import java.io.PrintStream

data class UnluacResult(val outputFilePath: String?, val error: String)

object UnluacWrapper {
    fun run(args: Array<String>): UnluacResult {
        val originalOut = System.out
        val originalErr = System.err

        val outStream = ByteArrayOutputStream()
        val errStream = ByteArrayOutputStream()
        
        var outputFilePath: String? = null
        for (i in args.indices) {
            if (args[i] == "-o" && i + 1 < args.size) {
                outputFilePath = args[i + 1]
                break
            }
        }

        try {
            System.setOut(PrintStream(outStream))
            System.setErr(PrintStream(errStream))

            unluac.Main.main(args)

        } catch (e: unluac.UnluacException) {
            // Error message is already printed to System.err by the Main.error method
        } catch (t: Throwable) {
            errStream.write(t.stackTraceToString().toByteArray())
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }

        val error = errStream.toString()
        if (error.isNotEmpty()) {
            return UnluacResult(null, error)
        }

        return UnluacResult(outputFilePath, "")
    }
}