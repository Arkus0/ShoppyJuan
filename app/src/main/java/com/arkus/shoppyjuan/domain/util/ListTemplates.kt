package com.arkus.shoppyjuan.domain.util

data class ListTemplate(
    val name: String,
    val items: List<TemplateItem>
)

data class TemplateItem(
    val name: String,
    val quantity: Double = 1.0,
    val unit: String? = null
)

object ListTemplates {
    val WEEKLY_SHOPPING = ListTemplate(
        name = "Compra Semanal",
        items = listOf(
            TemplateItem("Leche", 2.0, "l"),
            TemplateItem("Pan", 1.0, "ud"),
            TemplateItem("Huevos", 12.0, "ud"),
            TemplateItem("Pollo", 1.0, "kg"),
            TemplateItem("Arroz", 1.0, "kg"),
            TemplateItem("Pasta", 500.0, "g"),
            TemplateItem("Tomate", 1.0, "kg"),
            TemplateItem("Lechuga", 1.0, "ud"),
            TemplateItem("Manzanas", 1.0, "kg"),
            TemplateItem("Yogur", 8.0, "ud")
        )
    )

    val BBQ = ListTemplate(
        name = "Barbacoa",
        items = listOf(
            TemplateItem("Carne de res", 2.0, "kg"),
            TemplateItem("Chorizo", 1.0, "kg"),
            TemplateItem("Pollo", 1.5, "kg"),
            TemplateItem("Carbón", 5.0, "kg"),
            TemplateItem("Pan para hamburguesa", 8.0, "ud"),
            TemplateItem("Lechuga", 1.0, "ud"),
            TemplateItem("Tomate", 1.0, "kg"),
            TemplateItem("Cebolla", 0.5, "kg"),
            TemplateItem("Salsa BBQ", 1.0, "ud"),
            TemplateItem("Bebidas", 6.0, "ud")
        )
    )

    val BREAKFAST = ListTemplate(
        name = "Desayuno",
        items = listOf(
            TemplateItem("Leche", 1.0, "l"),
            TemplateItem("Cereales", 1.0, "ud"),
            TemplateItem("Pan de molde", 1.0, "ud"),
            TemplateItem("Mantequilla", 250.0, "g"),
            TemplateItem("Mermelada", 1.0, "ud"),
            TemplateItem("Jugo de naranja", 1.0, "l"),
            TemplateItem("Café", 250.0, "g"),
            TemplateItem("Huevos", 6.0, "ud"),
            TemplateItem("Jamón", 200.0, "g"),
            TemplateItem("Queso", 200.0, "g")
        )
    )

    val PARTY = ListTemplate(
        name = "Fiesta",
        items = listOf(
            TemplateItem("Papas fritas", 3.0, "ud"),
            TemplateItem("Nachos", 2.0, "ud"),
            TemplateItem("Salsa", 2.0, "ud"),
            TemplateItem("Refrescos", 12.0, "ud"),
            TemplateItem("Pizza", 3.0, "ud"),
            TemplateItem("Alitas de pollo", 1.0, "kg"),
            TemplateItem("Helado", 2.0, "l"),
            TemplateItem("Vasos desechables", 1.0, "paq"),
            TemplateItem("Platos desechables", 1.0, "paq"),
            TemplateItem("Servilletas", 1.0, "paq")
        )
    )

    fun getAllTemplates() = listOf(WEEKLY_SHOPPING, BBQ, BREAKFAST, PARTY)
}
