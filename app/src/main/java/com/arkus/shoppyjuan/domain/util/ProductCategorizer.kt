package com.arkus.shoppyjuan.domain.util

import java.text.Normalizer
import java.util.Locale

enum class ProductCategory(
    val id: String,
    val label: String,
    val emoji: String,
    val keywords: List<String>
) {
    FRUITS_VEG(
        id = "fruits-veg",
        label = "Frutas y Verduras",
        emoji = "🥕",
        keywords = listOf(
            // Frutas
            "manzana", "platano", "banana", "naranja", "mandarina", "clementina", "limon", "lima",
            "fresa", "fresón", "frambuesa", "arandano", "mora", "cereza", "melocoton", "nectarina",
            "ciruela", "albaricoque", "uva", "pera", "kiwi", "mango", "papaya", "piña", "coco",
            "sandia", "melon", "higo", "granada", "caqui", "chirimoya", "maracuya", "fruta pasion",
            "pomelo", "aguacate", "datil", "membrillo",
            // Verduras y hortalizas
            "tomate", "lechuga", "escarola", "rucula", "canonigo", "espinaca", "acelga", "col",
            "repollo", "lombarda", "brocoli", "coliflor", "cebolla", "cebolleta", "puerro",
            "ajo", "patata", "boniato", "batata", "zanahoria", "pimiento", "pimientos", "calabacin",
            "berenjena", "pepino", "calabaza", "rabano", "nabo", "remolacha", "apio", "hinojo",
            "esparrago", "alcachofa", "champiñon", "seta", "judias verdes", "guisante", "haba",
            "maiz", "endivia", "berro", "perejil", "cilantro", "albahaca", "romero", "tomillo",
            "oregano", "menta", "hierbabuena", "jengibre", "fruta", "verdura", "hortaliza",
            "ensalada", "vegetal", "vegetales"
        )
    ),

    MEAT_FISH(
        id = "meat-fish",
        label = "Carnes y Pescados",
        emoji = "🥩",
        keywords = listOf(
            // Carnes
            "pollo", "pavo", "ternera", "vaca", "cerdo", "cordero", "conejo", "pato",
            "carne", "carnes", "filete", "bistec", "entrecot", "chuleton", "solomillo",
            "chuleta", "costilla", "hamburguesa", "albondiga", "carne picada", "picada",
            "higado", "riñones", "lengua", "callos", "menudo",
            // Embutidos y fiambres
            "jamon", "serrano", "iberico", "york", "cocido", "pechuga pavo", "lacón",
            "chorizo", "salchichon", "fuet", "longaniza", "morcilla", "butifarra",
            "mortadela", "salchicha", "frankfurt", "bacon", "panceta", "tocino",
            "sobrasada", "lomo embuchado", "cecina", "embutido", "fiambre",
            // Pescados
            "pescado", "pescados", "salmon", "trucha", "merluza", "bacalao", "lubina",
            "dorada", "rape", "lenguado", "rodaballo", "besugo", "sardina", "boqueron",
            "anchoa", "atun", "bonito", "caballa", "jurel", "pez espada", "emperador",
            // Mariscos
            "marisco", "gamba", "langostino", "camaron", "cigala", "bogavante", "langosta",
            "mejillon", "almeja", "berberecho", "navaja", "vieira", "ostra", "percebe",
            "pulpo", "calamar", "chipirón", "sepia", "cangrejo", "centollo", "nécora"
        )
    ),

    DAIRY(
        id = "dairy",
        label = "Lácteos y Huevos",
        emoji = "🥛",
        keywords = listOf(
            // Leche
            "leche", "lacteo", "lacteos", "desnatada", "semidesnatada", "entera",
            "sin lactosa", "evaporada", "condensada",
            // Huevos
            "huevo", "huevos", "clara", "yema",
            // Yogures
            "yogur", "yogures", "yogurt", "kefir", "actimel", "danone", "bifidus",
            "griego", "skyr", "cuajada", "flan", "natillas",
            // Quesos
            "queso", "quesos", "mozzarella", "parmesano", "manchego", "cheddar",
            "brie", "camembert", "roquefort", "azul", "gouda", "edam", "emmental",
            "gruyere", "provolone", "feta", "burgos", "fresco", "rallado", "fundido",
            "curado", "semicurado", "tierno", "cremoso", "mascarpone", "ricotta",
            "requesón", "cottage",
            // Otros lácteos
            "mantequilla", "margarina", "nata", "crema", "bechamel", "batido",
            "horchata", "leche almendra", "leche soja", "leche avena", "leche coco",
            "bebida vegetal"
        )
    ),

    PANTRY(
        id = "pantry",
        label = "Despensa",
        emoji = "🍪",
        keywords = listOf(
            // Pan y bollería
            "pan", "barra", "baguette", "chapata", "molde", "integral", "cereales pan",
            "tostada", "rebanada", "panecillo", "bollo", "croissant", "ensaimada",
            "magdalena", "muffin", "donut", "berlina", "palmera", "pastel",
            // Arroz y cereales
            "arroz", "basmati", "integral arroz", "bomba", "paella", "risotto",
            "quinoa", "cuscus", "bulgur", "avena", "muesli", "corn flakes",
            "cereales", "copos", "granola",
            // Pasta
            "pasta", "espagueti", "macarron", "tallarín", "lasaña", "canelón",
            "ravioli", "tortellini", "ñoqui", "fideo", "penne", "fusilli",
            "rigatoni", "farfalle", "spaghetti", "macarrones",
            // Legumbres
            "legumbre", "garbanzo", "lenteja", "alubia", "judion", "azuki",
            "soja", "frijol", "habichuela",
            // Aceites y vinagres
            "aceite", "oliva", "girasol", "coco aceite", "vinagre", "vinagreta",
            "balsamico", "modena",
            // Salsas y condimentos
            "tomate frito", "salsa tomate", "ketchup", "mostaza", "mayonesa",
            "mahonesa", "alioli", "salsa rosa", "salsa barbacoa", "soja salsa",
            "tabasco", "sriracha", "pesto",
            // Especias y condimentos secos
            "sal", "pimienta", "pimenton", "comino", "curry", "canela", "nuez moscada",
            "clavo", "laurel", "azafran", "especias", "sazonador", "caldo", "pastilla",
            "concentrado",
            // Conservas
            "conserva", "lata", "atun lata", "sardina lata", "mejillon lata",
            "tomate triturado", "tomate natural", "pimiento lata", "esparrago lata",
            "aceituna", "pepinillo", "maiz lata", "pimiento piquillo",
            // Dulces y snacks
            "azucar", "sacarina", "edulcorante", "stevia", "miel", "sirope", "melaza",
            "mermelada", "confitura", "nocilla", "nutella", "crema cacao",
            "chocolate", "cacao", "tableta", "bombón", "galleta", "galletas",
            "maria", "digestive", "oreo", "cookies", "barquillo", "bizcocho",
            "pastas te", "rosquilla", "polvoron", "turron", "mazapan",
            "caramelo", "gominola", "chicle", "regaliz", "palomitas", "patatas chip",
            "nachos", "frutos secos", "almendra", "nuez", "avellana", "pistacho",
            "cacahuete", "anacardo", "pipa", "girasol pipa",
            // Harinas y repostería
            "harina", "trigo", "maicena", "maizena", "levadura", "bicarbonato",
            "impulsor", "royal", "gelatina", "fondant",
            // Otros
            "sopas", "pure", "croquetas preparadas", "preparado"
        )
    ),

    FROZEN(
        id = "frozen",
        label = "Congelados",
        emoji = "❄️",
        keywords = listOf(
            "congelado", "congelados", "hielo", "helado", "polo", "tarrina",
            "pizza congelada", "lasaña congelada", "croquetas congeladas",
            "nuggets", "fingers", "empanadilla", "san jacobo", "cordon bleu",
            "verduras congeladas", "guisantes congelados", "judias congeladas",
            "menestra", "salteado", "wok", "patatas congeladas", "carne congelada",
            "pescado congelado", "marisco congelado", "langostino congelado",
            "gamba congelada", "calamar congelado", "merluza congelada",
            "frozen", "ultracongelado", "cubito", "sorbete", "granizado"
        )
    ),

    BEVERAGES(
        id = "beverages",
        label = "Bebidas",
        emoji = "☕",
        keywords = listOf(
            // Agua
            "agua", "mineral", "con gas", "sin gas", "fontvella", "bezoya", "aquarius",
            // Refrescos
            "refresco", "cola", "coca cola", "pepsi", "fanta", "sprite", "7up",
            "naranjada", "limonada", "tonica", "schweppes", "gaseosa", "casera",
            "isotonica", "energetica", "red bull", "monster",
            // Zumos
            "zumo", "nectar", "mosto", "smoothie", "batido frutas", "piña zumo",
            "naranja zumo", "manzana zumo", "melocoton zumo", "multifrutas",
            // Cafe y te
            "cafe", "cappuccino", "latte", "espresso", "descafeinado", "soluble",
            "nescafe", "capsulas", "nespresso", "dolce gusto", "cafetera",
            "te", "infusion", "manzanilla", "tila", "poleo", "verde te", "rojo te",
            // Alcohol
            "cerveza", "sidra", "vino", "tinto", "blanco", "rosado", "cava",
            "champagne", "vermut", "sangria", "tinto verano", "licor", "ron",
            "whisky", "vodka", "ginebra", "gin", "brandy", "coñac", "orujo",
            "bebida", "bebidas", "botella", "lata bebida", "brick"
        )
    ),

    HOUSEHOLD(
        id = "household",
        label = "Hogar y Limpieza",
        emoji = "🧹",
        keywords = listOf(
            // Limpieza ropa
            "detergente", "detergente ropa", "lavadora", "suavizante", "quitamanchas",
            "blanqueador", "lejia ropa", "ariel", "skip", "wipp", "vernel", "mimosin",
            // Limpieza hogar
            "limpiador", "multiusos", "desengrasante", "antical", "lejia", "amoniaco",
            "cristales", "cristasol", "limpiacristales", "fregasuelos", "cif", "cillit",
            "vim", "don limpio", "fairy", "mistol", "ajax", "lavavajillas", "finish",
            "somat", "pastillas lavavajillas", "sal lavavajillas", "abrillantador",
            // Baño
            "desatascador", "wc", "inodoro", "pato", "harpic", "sanitario", "antihongos",
            // Utensilios limpieza
            "estropajo", "esponja", "bayeta", "trapo", "fregona", "mopa", "escoba",
            "recogedor", "cubo", "guantes limpieza", "cepillo", "desechable",
            // Bolsas y papel
            "bolsa basura", "basura", "rollo cocina", "papel cocina", "papel aluminio",
            "film", "plastico transparente", "papel horno", "servilleta", "servilletas",
            "papel higienico", "pañuelo papel", "clinex", "kleenex", "scottex",
            // Insecticidas
            "insecticida", "antimosquitos", "raid", "cucaracha", "hormiga", "polilla",
            "naftalina", "ambientador", "incienso", "vela aromatica",
            // Otros hogar
            "pilas", "bombilla", "cerilla", "mechero", "encendedor", "vela",
            "cinta adhesiva", "pegamento", "super glue"
        )
    ),

    HYGIENE(
        id = "hygiene",
        label = "Higiene Personal",
        emoji = "✨",
        keywords = listOf(
            // Ducha y baño
            "champu", "shampoo", "gel ducha", "jabon", "pastilla jabon", "esponja baño",
            "exfoliante", "locion", "acondicionador", "suavizante pelo", "mascarilla pelo",
            // Cabello
            "tinte", "pelo", "cabello", "gomina", "cera pelo", "laca", "espuma pelo",
            "secador", "plancha pelo", "peine", "cepillo pelo",
            // Dental
            "pasta dientes", "dentrifico", "cepillo dientes", "enjuague bucal",
            "colutorio", "hilo dental", "seda dental", "blanqueador dental",
            // Desodorante
            "desodorante", "antitranspirante", "roll on", "spray corporal",
            // Afeitado
            "cuchilla", "maquinilla", "afeitado", "espuma afeitar", "gel afeitar",
            "after shave", "gillette", "wilkinson",
            // Cuidado facial
            "crema", "hidratante", "facial", "contorno ojos", "serum", "mascarilla facial",
            "limpiador facial", "tonico", "desmaquillante", "protector solar",
            "bronceador", "aftersun",
            // Cuidado corporal
            "body", "corporal", "manos crema", "pies crema", "callos", "depilar",
            "depilacion", "cera depilatoria", "crema depilatoria",
            // Maquillaje
            "maquillaje", "base", "corrector", "polvos", "colorete", "rimel",
            "mascara", "sombra ojos", "perfilador", "labial", "pintalabios", "gloss",
            "esmalte uñas", "quitaesmalte", "lima",
            // Higiene intima
            "compresa", "tampon", "salvaslip", "copa menstrual", "intimo gel",
            // Bebe
            "pañal", "toallita", "bebe", "colonia bebe", "crema pañal",
            // Otros
            "preservativo", "condon", "lubricante", "pañuelo", "pañuelos",
            "bastoncillo", "algodon", "tirita", "venda", "gasa", "esparadrapo",
            "termometro", "mascarilla", "gel hidroalcoholico", "hidroalcohol"
        )
    ),

    PETS(
        id = "pets",
        label = "Mascotas",
        emoji = "🐕",
        keywords = listOf(
            // Perros
            "perro", "cachorro", "can", "pienso perro", "comida perro", "lata perro",
            "snack perro", "premio perro", "hueso perro", "galleta perro",
            "correa", "collar perro", "arnes", "bozal", "cama perro", "caseta",
            "juguete perro", "pelota perro", "champú perro",
            // Gatos
            "gato", "gatito", "felino", "pienso gato", "comida gato", "lata gato",
            "snack gato", "arena gato", "arenero", "rascador", "juguete gato",
            // General mascotas
            "mascota", "pienso", "comedero", "bebedero", "transportin",
            "antiparasitario", "pulga", "garrapata", "desparasitar", "veterinario",
            // Otros animales
            "pajaro", "canario", "periquito", "loro", "alpiste", "semilla pajaro",
            "jaula", "pez", "acuario", "pecera", "comida peces",
            "hamster", "conejo", "conejillo", "roedor", "heno", "viruta"
        )
    ),

    OTHER(
        id = "other",
        label = "Otros",
        emoji = "📦",
        keywords = emptyList()
    );

    companion object {
        /**
         * Normalizes text by removing accents and converting to lowercase
         */
        private fun normalizeText(text: String): String {
            val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
            return normalized.replace("\\p{M}".toRegex(), "")
                .lowercase(Locale.getDefault())
                .trim()
        }

        /**
         * Detects the category for a given product name
         */
        fun detectCategory(productName: String): ProductCategory {
            val normalized = normalizeText(productName)

            // Priority order to avoid confusion
            val priorityOrder = listOf(
                HOUSEHOLD, HYGIENE, PETS,  // Specific categories first
                FROZEN, BEVERAGES,
                MEAT_FISH, DAIRY, PANTRY,
                FRUITS_VEG,
                OTHER  // Default last
            )

            for (category in priorityOrder) {
                if (category.keywords.any { keyword ->
                        normalized.contains(normalizeText(keyword))
                    }) {
                    return category
                }
            }

            return OTHER
        }

        /**
         * Gets emoji for a product name
         */
        fun getProductEmoji(productName: String): String {
            return detectCategory(productName).emoji
        }
    }
}
