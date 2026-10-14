package com.unera.social.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.unera.social.model.Product

/**
 * MarketplaceProductData - Matches the getProductData return type:
 * {
 *   price: number;
 *   location: string;
 *   currency?: string;
 * } | null
 */
data class MarketplaceProductData(
    val price: Double,
    val location: String,
    val currency: String? = null,
    val title: String? = null,
    val imageUrl: String? = null
)

/**
 * MarketplaceContext - Jetpack Compose equivalent of React MarketplaceContext:
 *
 * export const MarketplaceContext = React.createContext<{
 *   onViewProduct: (productId: number) => void;
 *   getProductData: (productId: number) => {
 *     price: number;
 *     location: string;
 *     currency?: string;
 *   } | null;
 * }>({
 *   onViewProduct: () => {},
 *   getProductData: () => null,
 * });
 */
data class MarketplaceContext(
    val onViewProduct: (productId: Long) -> Unit = {},
    val getProductData: (productId: Long) -> MarketplaceProductData? = { null }
) {
    /**
     * Number overload to match Javascript number type (Int, Long, etc.)
     */
    fun onViewProduct(productId: Number) {
        onViewProduct(productId.toLong())
    }

    /**
     * Number overload to match Javascript number type (Int, Long, etc.)
     */
    fun getProductData(productId: Number): MarketplaceProductData? {
        return getProductData(productId.toLong())
    }
}

/**
 * LocalMarketplaceContext - CompositionLocal representing MarketplaceContext.
 * Default provides safe no-op handlers matching React.createContext defaults.
 */
val LocalMarketplaceContext = staticCompositionLocalOf {
    MarketplaceContext(
        onViewProduct = {},
        getProductData = { null }
    )
}

/**
 * useMarketplaceContext - Jetpack Compose hook equivalent to React's useContext(MarketplaceContext).
 */
@Composable
fun useMarketplaceContext(): MarketplaceContext = LocalMarketplaceContext.current

/**
 * ProvideMarketplaceContext - Convenience provider binding products list to MarketplaceContext.
 */
@Composable
fun ProvideMarketplaceContext(
    products: List<Product>,
    onViewProduct: (Long) -> Unit,
    content: @Composable () -> Unit
) {
    val contextValue = remember(products, onViewProduct) {
        MarketplaceContext(
            onViewProduct = onViewProduct,
            getProductData = { productId ->
                val product = products.find { it.id == productId }
                if (product != null) {
                    MarketplaceProductData(
                        price = product.price,
                        location = product.location,
                        currency = product.currency,
                        title = product.title,
                        imageUrl = product.imageUrl
                    )
                } else null
            }
        )
    }

    CompositionLocalProvider(LocalMarketplaceContext provides contextValue) {
        content()
    }
}

/**
 * ProvideMarketplaceContext - Directly supplies a MarketplaceContext instance.
 */
@Composable
fun ProvideMarketplaceContext(
    context: MarketplaceContext,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalMarketplaceContext provides context) {
        content()
    }
}

/**
 * ProvideMarketplaceContext - Supplies custom onViewProduct and getProductData implementations.
 */
@Composable
fun ProvideMarketplaceContext(
    onViewProduct: (Long) -> Unit = {},
    getProductData: (Long) -> MarketplaceProductData? = { null },
    content: @Composable () -> Unit
) {
    val contextValue = remember(onViewProduct, getProductData) {
        MarketplaceContext(
            onViewProduct = onViewProduct,
            getProductData = getProductData
        )
    }

    CompositionLocalProvider(LocalMarketplaceContext provides contextValue) {
        content()
    }
}
