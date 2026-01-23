package app.matjlergame.domain.usecase

import kotlin.math.abs

class ValidateExpressionUseCase {
    operator fun invoke(expression: String, target: Int): Boolean {
        if (Regex("[+\\-*/]{2,}").containsMatchIn(expression)) return false
        if (expression.startsWith("*") || expression.startsWith("/")) return false
        if (expression.endsWith("+") || expression.endsWith("-") ||
            expression.endsWith("*") || expression.endsWith("/")) return false

        return try {
            val result = evaluateExpression(expression)
            abs(result - target.toDouble()) < 0.001
        } catch (e: Exception) {
            false
        }
    }

    private fun evaluateExpression(expr: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0.toChar()

            fun nextChar() {
                ch = if (++pos < expr.length) expr[pos] else 0.toChar()
            }

            fun eat(charToEat: Char): Boolean {
                while (ch == ' ') nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < expr.length) throw RuntimeException("Unexpected: $ch")
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+') -> x += parseTerm()
                        eat('-') -> x -= parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*') -> x *= parseFactor()
                        eat('/') -> x /= parseFactor()
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+')) return parseFactor()
                if (eat('-')) return -parseFactor()

                var x: Double
                val startPos = pos

                if (eat('(')) {
                    x = parseExpression()
                    eat(')')
                } else if (ch in '0'..'9') {
                    while (ch in '0'..'9') nextChar()
                    x = expr.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: $ch")
                }

                return x
            }
        }.parse()
    }
}
