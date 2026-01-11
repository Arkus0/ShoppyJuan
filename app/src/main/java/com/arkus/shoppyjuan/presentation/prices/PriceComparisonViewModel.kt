package com.arkus.shoppyjuan.presentation.prices

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arkus.shoppyjuan.data.local.entity.PriceRecordEntity
import com.arkus.shoppyjuan.data.local.entity.ReceiptEntity
import com.arkus.shoppyjuan.data.ocr.ReceiptAnalyzer
import com.arkus.shoppyjuan.data.repository.PriceRepository
import com.arkus.shoppyjuan.domain.model.ListItem
import com.arkus.shoppyjuan.domain.price.*
import com.arkus.shoppyjuan.domain.repository.ShoppingListRepository
import com.arkus.shoppyjuan.domain.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PriceComparisonUiState(
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val listName: String = "",
    val items: List<ListItem> = emptyList(),
    val analysisResult: PriceAnalysisResult? = null,
    val optimalStrategy: OptimalShoppingStrategy? = null,
    val selectedView: PriceViewMode = PriceViewMode.BY_ITEM,
    val receipts: List<ReceiptEntity> = emptyList(),
    val recentPrices: List<PriceRecordEntity> = emptyList(),
    val error: String? = null,
    val uploadingReceipt: Boolean = false,
    val receiptAnalysisProgress: String? = null,
    // Open Prices integration
    val isOpenPricesAuthenticated: Boolean = false,
    val openPricesUsername: String? = null,
    val openPricesPricesContributed: Int = 0,
    val isContributing: Boolean = false,
    val contributionProgress: String? = null,
    val showOpenPricesLoginDialog: Boolean = false,
    val showContributeDialog: Boolean = false,
    val uncontributedReceiptsCount: Int = 0
)

enum class PriceViewMode {
    BY_ITEM,      // Show prices per item
    BY_STORE,     // Show recommendations by store
    OPTIMAL       // Show optimal multi-store strategy
}

@HiltViewModel
class PriceComparisonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val shoppingListRepository: ShoppingListRepository,
    private val priceRepository: PriceRepository,
    private val priceAnalyzer: PriceAnalyzer,
    private val receiptAnalyzer: ReceiptAnalyzer,
    private val userManager: UserManager
) : ViewModel() {

    private val listId: String = savedStateHandle.get<String>("listId") ?: ""

    private val _uiState = MutableStateFlow(PriceComparisonUiState())
    val uiState: StateFlow<PriceComparisonUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = userManager.currentUserId

    init {
        loadUserReceipts()
        loadListAndAnalyze()
        observeOpenPricesAuth()
    }

    /**
     * Observe Open Prices authentication state
     */
    private fun observeOpenPricesAuth() {
        viewModelScope.launch {
            priceRepository.isOpenPricesAuthenticated.collect { isAuth ->
                _uiState.update { it.copy(isOpenPricesAuthenticated = isAuth) }

                if (isAuth) {
                    // Load username and uncontributed count
                    priceRepository.openPricesUsername.collect { username ->
                        _uiState.update { it.copy(openPricesUsername = username) }
                    }
                }
            }
        }

        viewModelScope.launch {
            // Count uncontributed receipts
            val count = priceRepository.getUncontributedReceipts(currentUserId).size
            _uiState.update { it.copy(uncontributedReceiptsCount = count) }
        }
    }

    /**
     * Load list items and start price analysis
     */
    private fun loadListAndAnalyze() {
        if (listId.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Load list info
                shoppingListRepository.getListById(listId).collect { list ->
                    if (list != null) {
                        _uiState.update { it.copy(listName = list.name) }
                    }
                }
            } catch (e: Exception) {
                // Ignore list name errors
            }

            // Load items and analyze
            shoppingListRepository.getItemsByListId(listId).collect { items ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = items
                    )
                }

                // Auto-analyze when items are loaded
                if (items.isNotEmpty()) {
                    analyzeList(items)
                }
            }
        }
    }

    /**
     * Analyze prices for a shopping list
     */
    fun analyzeList(items: List<ListItem>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null) }

            try {
                val analysis = priceAnalyzer.analyzeList(items)
                val optimalStrategy = priceAnalyzer.calculateOptimalStrategy(items)

                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        analysisResult = analysis,
                        optimalStrategy = optimalStrategy
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        error = "Error al analizar precios: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Change view mode
     */
    fun setViewMode(mode: PriceViewMode) {
        _uiState.update { it.copy(selectedView = mode) }
    }

    /**
     * Submit a price manually
     */
    fun submitPrice(
        productName: String,
        price: Double,
        storeChain: String,
        storeName: String = storeChain,
        barcode: String? = null
    ) {
        viewModelScope.launch {
            try {
                priceRepository.submitPrice(
                    productName = productName,
                    price = price,
                    storeChain = storeChain,
                    storeName = storeName,
                    userId = currentUserId,
                    barcode = barcode
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al guardar precio: ${e.message}") }
            }
        }
    }

    /**
     * Upload and analyze a receipt
     */
    fun uploadReceipt(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    uploadingReceipt = true,
                    receiptAnalysisProgress = "Subiendo imagen..."
                )
            }

            try {
                // Create receipt record
                val receipt = priceRepository.uploadReceipt(
                    userId = currentUserId,
                    imageUri = imageUri.toString()
                )

                _uiState.update {
                    it.copy(receiptAnalysisProgress = "Analizando texto...")
                }

                // Analyze with OCR
                val analysis = receiptAnalyzer.analyzeReceipt(imageUri, receipt.id)

                _uiState.update {
                    it.copy(receiptAnalysisProgress = "Extrayendo productos...")
                }

                // Save extracted items and prices
                priceRepository.updateReceiptWithExtractedItems(
                    receiptId = receipt.id,
                    items = analysis.items,
                    storeChain = analysis.storeChain,
                    totalAmount = analysis.totalAmount
                )

                _uiState.update {
                    it.copy(
                        uploadingReceipt = false,
                        receiptAnalysisProgress = null
                    )
                }

                // Refresh receipts list
                loadUserReceipts()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        uploadingReceipt = false,
                        receiptAnalysisProgress = null,
                        error = "Error al procesar ticket: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Load user's uploaded receipts
     */
    private fun loadUserReceipts() {
        viewModelScope.launch {
            priceRepository.getReceiptsByUser(currentUserId)
                .collect { receipts ->
                    _uiState.update { it.copy(receipts = receipts) }
                }
        }
    }

    /**
     * Verify a price (confirm it's correct)
     */
    fun verifyPrice(priceId: String) {
        viewModelScope.launch {
            try {
                priceRepository.verifyPrice(priceId)
            } catch (e: Exception) {
                // Silent fail for verification
            }
        }
    }

    /**
     * Search for prices
     */
    fun searchPrices(query: String) {
        viewModelScope.launch {
            try {
                val prices = priceRepository.searchPrices(query)
                _uiState.update { it.copy(recentPrices = prices) }
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ==================== OPEN PRICES INTEGRATION ====================

    /**
     * Show/hide Open Prices login dialog
     */
    fun showOpenPricesLogin(show: Boolean) {
        _uiState.update { it.copy(showOpenPricesLoginDialog = show) }
    }

    /**
     * Show/hide contribute dialog
     */
    fun showContributeDialog(show: Boolean) {
        _uiState.update { it.copy(showContributeDialog = show) }
    }

    /**
     * Login to Open Prices (Open Food Facts account)
     */
    fun loginToOpenPrices(username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isContributing = true, contributionProgress = "Iniciando sesion...") }

            val result = priceRepository.authenticateOpenPrices(username, password)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isContributing = false,
                            contributionProgress = null,
                            showOpenPricesLoginDialog = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isContributing = false,
                            contributionProgress = null,
                            error = "Error de autenticacion: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Logout from Open Prices
     */
    fun logoutFromOpenPrices() {
        viewModelScope.launch {
            priceRepository.logoutOpenPrices()
            _uiState.update { it.copy(openPricesUsername = null) }
        }
    }

    /**
     * Contribute a single receipt's prices to Open Prices
     */
    fun contributeReceipt(receiptId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isContributing = true, contributionProgress = "Compartiendo precios...")
            }

            val result = priceRepository.contributeReceiptPrices(
                receiptId = receiptId,
                uploadProof = true
            )

            result.fold(
                onSuccess = { summary ->
                    _uiState.update {
                        it.copy(
                            isContributing = false,
                            contributionProgress = null,
                            openPricesPricesContributed = it.openPricesPricesContributed + summary.successCount,
                            uncontributedReceiptsCount = maxOf(0, it.uncontributedReceiptsCount - 1),
                            error = if (summary.failCount > 0)
                                "Compartidos ${summary.successCount} precios, ${summary.failCount} fallidos"
                            else null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isContributing = false,
                            contributionProgress = null,
                            error = "Error al compartir: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Contribute all unshared receipts to Open Prices
     */
    fun contributeAllReceipts() {
        viewModelScope.launch {
            val uncontributedReceipts = priceRepository.getUncontributedReceipts(currentUserId)

            if (uncontributedReceipts.isEmpty()) {
                _uiState.update { it.copy(error = "No hay tickets pendientes de compartir") }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isContributing = true,
                    contributionProgress = "Compartiendo ${uncontributedReceipts.size} tickets..."
                )
            }

            var totalSuccess = 0
            var totalFail = 0

            uncontributedReceipts.forEachIndexed { index, receipt ->
                _uiState.update {
                    it.copy(
                        contributionProgress = "Compartiendo ticket ${index + 1} de ${uncontributedReceipts.size}..."
                    )
                }

                val result = priceRepository.contributeReceiptPrices(
                    receiptId = receipt.id,
                    uploadProof = true
                )

                result.fold(
                    onSuccess = { summary ->
                        totalSuccess += summary.successCount
                        totalFail += summary.failCount
                    },
                    onFailure = {
                        totalFail++
                    }
                )
            }

            _uiState.update {
                it.copy(
                    isContributing = false,
                    contributionProgress = null,
                    openPricesPricesContributed = it.openPricesPricesContributed + totalSuccess,
                    uncontributedReceiptsCount = 0,
                    showContributeDialog = false,
                    error = if (totalFail > 0)
                        "Compartidos $totalSuccess precios de ${uncontributedReceipts.size} tickets ($totalFail errores)"
                    else
                        "Compartidos $totalSuccess precios de ${uncontributedReceipts.size} tickets a Open Prices"
                )
            }
        }
    }
}
