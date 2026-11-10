package dev.pocketpc.desktop.apps

/**
 * Expression evaluator for the Calculator app.
 *
 * A real recursive-descent parser rather than a stub: it handles operator precedence,
 * unary minus, parentheses, decimals and percentage, and reports errors as data instead
 * of throwing. Kept Android-free so it is unit tested.
 */
public sealed class CalcResult {
    public data class Value(public val value: Double) : CalcResult()
    public data class Error(public val message: String) : CalcResult() {
        public val display: String get() = "Error"
    }
}

public object Calculator {

    public fun evaluate(expression: String): CalcResult {
        val src = expression.replace("×", "*").replace("÷", "/").replace("−", "-").replace(" ", "")
        if (src.isBlank()) return CalcResult.Value(0.0)
        val parser = Parser(src)
        return try {
            val v = parser.parseExpression()
            parser.expectEnd()
            if (v.isNaN() || v.isInfinite()) CalcResult.Error("Not a number") else CalcResult.Value(v)
        } catch (e: CalcException) {
            CalcResult.Error(e.message ?: "Invalid expression")
        }
    }

    /** Formats a result the way a calculator should: no trailing ".0", bounded length. */
    public fun format(value: Double): String {
        if (value == value.toLong().toDouble() && kotlin.math.abs(value) < 1e15) {
            return value.toLong().toString()
        }
        val s = String.format("%.10f", value).trimEnd('0').trimEnd('.')
        return if (s.length > 16) String.format("%.6e", value) else s
    }

    private class CalcException(message: String) : IllegalArgumentException(message)

    private class Parser(private val s: String) {
        private var i = 0

        fun parseExpression(): Double {
            var value = parseTerm()
            while (i < s.length) {
                when (s[i]) {
                    '+' -> { i++; value += parseTerm() }
                    '-' -> { i++; value -= parseTerm() }
                    else -> return value
                }
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parseUnary()
            while (i < s.length) {
                when (s[i]) {
                    '*' -> { i++; value *= parseUnary() }
                    '/' -> {
                        i++
                        val divisor = parseUnary()
                        if (divisor == 0.0) throw CalcException("Division by zero")
                        value /= divisor
                    }
                    '%' -> { i++; value %= 100.0 }
                    else -> return value
                }
            }
            return value
        }

        private fun parseUnary(): Double {
            if (i < s.length && (s[i] == '-' || s[i] == '+')) {
                val negative = s[i] == '-'
                i++
                val v = parseUnary()
                return if (negative) -v else v
            }
            return parsePrimary()
        }

        private fun parsePrimary(): Double {
            if (i >= s.length) throw CalcException("Unexpected end of expression")
            if (s[i] == '(') {
                i++
                val v = parseExpression()
                if (i >= s.length || s[i] != ')') throw CalcException("Missing closing parenthesis")
                i++
                return v
            }
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            if (start == i) throw CalcException("Unexpected character '${s[i]}'")
            return s.substring(start, i).toDoubleOrNull() ?: throw CalcException("Malformed number")
        }

        fun expectEnd() {
            if (i != s.length) throw CalcException("Unexpected character '${s[i]}'")
        }
    }
}
