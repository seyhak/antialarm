package com.antialarm.alarm

import kotlin.random.Random

data class MathProblem(
    val question: String,
    val answer: Int
)

object MathProblemGenerator {

    fun generate(difficulty: Int): MathProblem {
        return when (difficulty) {
            1 -> generateEasy()
            2 -> generateMedium()
            3 -> generateHard()
            else -> generateEasy()
        }
    }

    private fun generateEasy(): MathProblem {
        val a = Random.nextInt(1, 21)
        val b = Random.nextInt(1, 21)
        return if (Random.nextBoolean()) {
            MathProblem("$a + $b = ?", a + b)
        } else {
            val max = maxOf(a, b)
            val min = minOf(a, b)
            MathProblem("$max - $min = ?", max - min)
        }
    }

    private fun generateMedium(): MathProblem {
        val a = Random.nextInt(10, 100)
        val b = Random.nextInt(2, 16)
        return MathProblem("$a × $b = ?", a * b)
    }

    private fun generateHard(): MathProblem {
        return when (Random.nextInt(3)) {
            0 -> {
                val a = Random.nextInt(10, 50)
                val b = Random.nextInt(2, 10)
                val c = Random.nextInt(10, 100)
                MathProblem("$a × $b + $c = ?", a * b + c)
            }
            1 -> {
                val a = Random.nextInt(5, 20)
                val b = Random.nextInt(1, a)
                MathProblem("$a² - $b = ?", a * a - b)
            }
            else -> {
                val a = Random.nextInt(10, 50)
                val b = Random.nextInt(10, 50)
                val c = Random.nextInt(1, 20)
                MathProblem("$a + $b × $c = ?", a + b * c)
            }
        }
    }
}
