package com.arkus.shoppyjuan.domain.util

import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

/**
 * Utility for fuzzy string matching and product name normalization
 */
object FuzzySearch {

    /**
     * Normalize a string for comparison:
     * - Lowercase
     * - Remove accents
     * - Remove special characters
     * - Trim whitespace
     */
    fun normalize(input: String): String {
        return Normalizer.normalize(input.lowercase().trim(), Normalizer.Form.NFD)
            .replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[m][n]
    }

    /**
     * Calculate similarity ratio (0-1) between two strings
     * 1.0 = identical, 0.0 = completely different
     */
    fun similarityRatio(s1: String, s2: String): Double {
        val n1 = normalize(s1)
        val n2 = normalize(s2)

        if (n1 == n2) return 1.0
        if (n1.isEmpty() || n2.isEmpty()) return 0.0

        val distance = levenshteinDistance(n1, n2)
        val maxLen = max(n1.length, n2.length)
        return 1.0 - (distance.toDouble() / maxLen)
    }

    /**
     * Check if two product names are likely the same product
     * using multiple matching strategies
     */
    fun isMatch(query: String, candidate: String, threshold: Double = 0.7): Boolean {
        val nQuery = normalize(query)
        val nCandidate = normalize(candidate)

        // Exact match
        if (nQuery == nCandidate) return true

        // Contains match
        if (nCandidate.contains(nQuery) || nQuery.contains(nCandidate)) return true

        // Similarity match
        if (similarityRatio(nQuery, nCandidate) >= threshold) return true

        // Token match (all query words present in candidate)
        val queryTokens = nQuery.split(" ").filter { it.length > 2 }
        val candidateTokens = nCandidate.split(" ")
        if (queryTokens.isNotEmpty() && queryTokens.all { qt ->
            candidateTokens.any { ct -> ct.contains(qt) || qt.contains(ct) }
        }) return true

        return false
    }

    /**
     * Calculate a match score for ranking results
     */
    fun matchScore(query: String, candidate: String): Double {
        val nQuery = normalize(query)
        val nCandidate = normalize(candidate)

        var score = 0.0

        // Exact match bonus
        if (nQuery == nCandidate) return 1.0

        // Starts with bonus
        if (nCandidate.startsWith(nQuery)) {
            score += 0.3
        }

        // Contains bonus
        if (nCandidate.contains(nQuery)) {
            score += 0.2
        }

        // Similarity score
        score += similarityRatio(nQuery, nCandidate) * 0.5

        return min(1.0, score)
    }

    /**
     * Find best matches from a list of candidates
     */
    fun <T> findBestMatches(
        query: String,
        candidates: List<T>,
        nameExtractor: (T) -> String,
        threshold: Double = 0.5,
        maxResults: Int = 10
    ): List<Pair<T, Double>> {
        return candidates
            .map { candidate -> candidate to matchScore(query, nameExtractor(candidate)) }
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }
            .take(maxResults)
    }

    /**
     * Extract common product keywords from a name
     */
    fun extractKeywords(productName: String): List<String> {
        val stopWords = setOf(
            "de", "el", "la", "los", "las", "un", "una", "unos", "unas",
            "con", "sin", "para", "por", "en", "al", "del",
            "kg", "g", "ml", "l", "ud", "uds", "pack", "unidad", "unidades"
        )

        return normalize(productName)
            .split(" ")
            .filter { it.length > 2 && it !in stopWords }
    }

    /**
     * Common product name variations mapping
     */
    private val productAliases = mapOf(
        "leche" to listOf("leche", "milk"),
        "pan" to listOf("pan", "barra", "hogaza", "chapata"),
        "huevos" to listOf("huevos", "huevo", "eggs"),
        "aceite" to listOf("aceite", "oil", "oliva"),
        "arroz" to listOf("arroz", "rice"),
        "pasta" to listOf("pasta", "espaguetis", "macarrones", "fideos"),
        "tomate" to listOf("tomate", "tomates", "tomato"),
        "pollo" to listOf("pollo", "chicken", "pechuga"),
        "carne" to listOf("carne", "ternera", "cerdo", "meat"),
        "pescado" to listOf("pescado", "fish", "salmon", "merluza"),
        "yogur" to listOf("yogur", "yogurt", "yogures"),
        "queso" to listOf("queso", "cheese"),
        "jamon" to listOf("jamon", "ham", "serrano", "york"),
        "agua" to listOf("agua", "water"),
        "cerveza" to listOf("cerveza", "beer", "cervezas"),
        "vino" to listOf("vino", "wine")
    )

    /**
     * Expand a query with known aliases
     */
    fun expandWithAliases(query: String): List<String> {
        val normalized = normalize(query)
        val expanded = mutableSetOf(normalized)

        productAliases.forEach { (key, aliases) ->
            if (aliases.any { normalized.contains(it) }) {
                expanded.addAll(aliases)
            }
        }

        return expanded.toList()
    }
}
