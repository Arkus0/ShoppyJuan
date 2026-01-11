package com.arkus.shoppyjuan.data.remote.mapper

import com.arkus.shoppyjuan.data.remote.dto.MealDetailDto
import com.arkus.shoppyjuan.data.remote.dto.MealDto
import com.arkus.shoppyjuan.domain.model.Recipe
import com.arkus.shoppyjuan.domain.model.RecipeIngredient

fun MealDto.toRecipe(): Recipe {
    return Recipe(
        id = id,
        name = name,
        category = category ?: "",
        area = area ?: "",
        instructions = "",
        imageUrl = thumbnail,
        ingredients = emptyList(),
        isFavorite = false
    )
}

fun MealDetailDto.toRecipe(): Recipe {
    return Recipe(
        id = id,
        name = name,
        category = category ?: "",
        area = area ?: "",
        instructions = instructions ?: "",
        imageUrl = thumbnail,
        youtubeUrl = youtubeUrl,
        ingredients = getIngredients().map { (ingredient, measure) ->
            RecipeIngredient(
                name = translateIngredient(ingredient),
                quantity = extractQuantity(measure),
                unit = extractUnit(measure)
            )
        },
        isFavorite = false
    )
}

// Traducciones de ingredientes más comunes de inglés a español
private fun translateIngredient(ingredient: String): String {
    val translations = mapOf(
        "chicken" to "pollo",
        "beef" to "carne de res",
        "pork" to "cerdo",
        "fish" to "pescado",
        "salmon" to "salmón",
        "tuna" to "atún",
        "shrimp" to "camarones",
        "eggs" to "huevos",
        "milk" to "leche",
        "butter" to "mantequilla",
        "cheese" to "queso",
        "cream" to "crema",
        "yogurt" to "yogur",
        "rice" to "arroz",
        "pasta" to "pasta",
        "bread" to "pan",
        "flour" to "harina",
        "sugar" to "azúcar",
        "salt" to "sal",
        "pepper" to "pimienta",
        "garlic" to "ajo",
        "onion" to "cebolla",
        "tomato" to "tomate",
        "potato" to "patata",
        "carrot" to "zanahoria",
        "lettuce" to "lechuga",
        "spinach" to "espinaca",
        "broccoli" to "brócoli",
        "mushroom" to "champiñones",
        "olive oil" to "aceite de oliva",
        "oil" to "aceite",
        "vinegar" to "vinagre",
        "lemon" to "limón",
        "lime" to "lima",
        "orange" to "naranja",
        "apple" to "manzana",
        "banana" to "plátano",
        "strawberry" to "fresa",
        "parsley" to "perejil",
        "cilantro" to "cilantro",
        "basil" to "albahaca",
        "oregano" to "orégano",
        "thyme" to "tomillo",
        "rosemary" to "romero",
        "cumin" to "comino",
        "paprika" to "pimentón",
        "cinnamon" to "canela",
        "ginger" to "jengibre",
        "soy sauce" to "salsa de soja",
        "honey" to "miel",
        "chocolate" to "chocolate",
        "vanilla" to "vainilla"
    )

    val lowerIngredient = ingredient.lowercase()
    return translations.entries.firstOrNull { lowerIngredient.contains(it.key) }?.value
        ?: ingredient
}

private fun extractQuantity(measure: String): String {
    if (measure.isBlank()) return "1"

    // Extraer números y fracciones
    val numberRegex = """(\d+(?:/\d+)?|\d+\.\d+)""".toRegex()
    val match = numberRegex.find(measure)
    return match?.value ?: "1"
}

private fun extractUnit(measure: String): String {
    if (measure.isBlank()) return "ud"

    val unitTranslations = mapOf(
        "cup" to "taza",
        "cups" to "tazas",
        "tbsp" to "cucharada",
        "tablespoon" to "cucharada",
        "tablespoons" to "cucharadas",
        "tsp" to "cucharadita",
        "teaspoon" to "cucharadita",
        "teaspoons" to "cucharaditas",
        "oz" to "onza",
        "ounce" to "onza",
        "ounces" to "onzas",
        "lb" to "libra",
        "pound" to "libra",
        "pounds" to "libras",
        "g" to "g",
        "gram" to "g",
        "grams" to "g",
        "kg" to "kg",
        "ml" to "ml",
        "milliliter" to "ml",
        "milliliters" to "ml",
        "l" to "l",
        "liter" to "l",
        "liters" to "l",
        "pinch" to "pizca",
        "dash" to "pizca",
        "clove" to "diente",
        "cloves" to "dientes"
    )

    val lowerMeasure = measure.lowercase()
    return unitTranslations.entries.firstOrNull { lowerMeasure.contains(it.key) }?.value
        ?: "ud"
}
