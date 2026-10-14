package com.unera.social.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.unera.social.model.Product
import com.unera.social.model.User
import com.unera.social.ui.components.rememberRestoredLazyGridState
import com.unera.social.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

// Data structures matching constants.ts on web
data class MarketCategoryItem(val id: String, val name: String)
data class MarketCountryItem(val code: String, val name: String, val currency: String, val symbol: String, val flag: String)

val WEB_MARKETPLACE_CATEGORIES = listOf(
    MarketCategoryItem("all", "All Products"),
    MarketCategoryItem("electronics", "Electronics"),
    MarketCategoryItem("books", "Books"),
    MarketCategoryItem("services", "Services"),
    MarketCategoryItem("real_estate", "Real Estate"),
    MarketCategoryItem("vehicles", "Vehicles"),
    MarketCategoryItem("furniture", "Furniture"),
    MarketCategoryItem("clothing", "Clothing"),
    MarketCategoryItem("sports", "Sports & Fitness"),
    MarketCategoryItem("home_garden", "Home & Garden"),
    MarketCategoryItem("business", "Business & Industrial")
)

val WEB_MARKETPLACE_COUNTRIES = listOf(
    MarketCountryItem("all", "All Countries", "", "$", "🌍"),
    MarketCountryItem("TZ", "Tanzania", "TZS", "TSh", "🇹🇿"),
    MarketCountryItem("KE", "Kenya", "KES", "KSh", "🇰🇪"),
    MarketCountryItem("UG", "Uganda", "UGX", "USh", "🇺🇬"),
    MarketCountryItem("NG", "Nigeria", "NGN", "₦", "🇳🇬"),
    MarketCountryItem("ZA", "South Africa", "ZAR", "R", "🇿🇦"),
    MarketCountryItem("ET", "Ethiopia", "ETB", "Br", "🇪🇹"),
    MarketCountryItem("EG", "Egypt", "EGP", "E£", "🇪🇬"),
    MarketCountryItem("GH", "Ghana", "GHS", "GH₵", "🇬🇭"),
    MarketCountryItem("US", "United States", "USD", "$", "🇺🇸"),
    MarketCountryItem("GB", "United Kingdom", "GBP", "£", "🇬🇧"),
    MarketCountryItem("CN", "China", "CNY", "¥", "🇨🇳"),
    MarketCountryItem("IN", "India", "INR", "₹", "🇮🇳"),
    MarketCountryItem("AE", "UAE", "AED", "AED", "🇦🇪")
)

fun getCurrencyLabel(countryCode: String): String {
    return when (countryCode.uppercase()) {
        "TZ", "TZS" -> "TSh"
        "KE", "KES" -> "KSh"
        "UG", "UGX" -> "USh"
        "NG", "NGN" -> "₦"
        "ZA", "ZAR" -> "R"
        "ET", "ETB" -> "Br"
        "EG", "EGP" -> "E£"
        "GH", "GHS" -> "GH₵"
        "US", "USD" -> "$"
        "GB", "GBP" -> "£"
        "EU", "EUR" -> "€"
        "CN", "CNY" -> "¥"
        "IN", "INR" -> "₹"
        "AE", "AED" -> "AED"
        else -> "$"
    }
}

fun formatMarketPrice(value: Double, countryCode: String): String {
    val symbol = getCurrencyLabel(countryCode)
    val isZeroDecimal = countryCode.uppercase() in listOf("TZ", "TZS", "KE", "KES", "UG", "UGX", "RW", "RWF", "BI", "BIF")
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = if (isZeroDecimal) 0 else 2
        minimumFractionDigits = if (isZeroDecimal) 0 else 0
    }
    return "$symbol${formatter.format(value)}"
}

@Composable
fun MarketplaceScreen(
    products: List<Product>,
    selectedCategory: String,
    selectedCountry: String,
    marketMode: String,
    isLoading: Boolean,
    onCategorySelected: (String) -> Unit,
    onCountrySelected: (String) -> Unit,
    onMarketModeSelected: (String) -> Unit,
    onProductClick: (Product) -> Unit,
    onOpenCreateProduct: () -> Unit,
    onToggleSaveProduct: (Long) -> Unit,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSearchOverlay by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showCountryDialog by remember { mutableStateOf(false) }

    val activeCountry = WEB_MARKETPLACE_COUNTRIES.find { it.code.equals(selectedCountry, ignoreCase = true) }
        ?: WEB_MARKETPLACE_COUNTRIES[0]

    // Filter products matching web logic
    val filteredProducts = remember(products, selectedCategory, selectedCountry, marketMode, searchQuery) {
        products.filter { p ->
            // Category filter
            if (selectedCategory != "all") {
                val catMatches = p.category.equals(selectedCategory, ignoreCase = true) ||
                                 p.category.replace(" ", "_").equals(selectedCategory, ignoreCase = true)
                if (!catMatches) return@filter false
            }

            // Country filter
            if (selectedCountry != "all") {
                if (!p.country.equals(selectedCountry, ignoreCase = true)) return@filter false
            }

            // Market mode
            if (marketMode == "local" && selectedCountry != "all") {
                if (!p.country.equals(selectedCountry, ignoreCase = true)) return@filter false
            }

            // Search query
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                val match = p.title.lowercase().contains(q) ||
                            p.description.lowercase().contains(q) ||
                            p.location.lowercase().contains(q)
                if (!match) return@filter false
            }

            true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050B18))
            .testTag("marketplace_screen")
    ) {
        // Sticky Header matching web `h-14 px-3 bg-[#0B1120] border-b border-[#1E293B]`
        Surface(
            color = Color(0xFF0B1120),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Back button + Title "MarketPoint"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFFF8FAFC)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "MarketPoint",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                    }

                    // Right: Refresh button, Search button, Country flag button, Sell button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Refresh Live Data Button
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .testTag("marketplace_refresh_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh live listings",
                                tint = if (isLoading) Color(0xFF1877F2) else Color(0xFFF8FAFC),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Search Button
                        IconButton(
                            onClick = { showSearchOverlay = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .testTag("marketplace_search_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFFF8FAFC),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Country Flag button (cycles or opens selector)
                        Surface(
                            onClick = { showCountryDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("marketplace_country_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeCountry.flag,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        // Sell Button (Primary Blue on web: #1877F2)
                        Button(
                            onClick = onOpenCreateProduct,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1877F2)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("sell_product_btn")
                        ) {
                            Text(
                                text = "Sell",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Mode switch pills: "For You", "Local", "Worldwide"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        "for_you" to "For You",
                        "local" to "Local",
                        "worldwide" to "Worldwide"
                    )
                    modes.forEach { (modeId, label) ->
                        val active = marketMode == modeId
                        Surface(
                            onClick = { onMarketModeSelected(modeId) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (active) Color(0xFFE7F3FF) else Color(0xFF1E293B),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (active) Color(0xFF1877F2) else Color(0xFFF8FAFC)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sticky Categories Row matching web `top-[104px] bg-[#0B1120] border-b border-[#1E293B]`
        Surface(
            color = Color(0xFF0B1120),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(WEB_MARKETPLACE_CATEGORIES) { cat ->
                    val active = selectedCategory.equals(cat.id, ignoreCase = true)
                    Surface(
                        onClick = { onCategorySelected(cat.id) },
                        shape = RoundedCornerShape(18.dp),
                        color = if (active) Color(0xFF1877F2) else Color(0xFF1E293B),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = cat.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) Color.White else Color(0xFFF8FAFC)
                            )
                        }
                    }
                }
            }
        }

        // Live syncing / Loading indicator
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1877F2),
                trackColor = Color(0xFF0B1120)
            )
        }

        val gridState = rememberRestoredLazyGridState(key = "MARKETPLACE")

        // Main 2-Column Product Grid matching web `grid grid-cols-2 gap-[2px] px-[2px] pt-[2px]`
        if (filteredProducts.isNotEmpty()) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("marketplace_product_grid")
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    WebMarketProductCard(
                        product = product,
                        onClick = { onProductClick(product) }
                    )
                }
            }
        } else {
            // Empty State matching web
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF1E293B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No items found",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFFF8FAFC)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Try changing your category, market view, or search.",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            onCategorySelected("all")
                            onCountrySelected("all")
                            onMarketModeSelected("for_you")
                            searchQuery = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Reset",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Search Overlay Dialog matching web
    if (showSearchOverlay) {
        Dialog(
            onDismissRequest = { showSearchOverlay = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B1120))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showSearchOverlay = false },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFFF8FAFC)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search MarketPoint…", color = Color(0xFF94A3B8)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Cancel,
                                            contentDescription = "Clear",
                                            tint = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF1877F2),
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedTextColor = Color(0xFFF8FAFC),
                                unfocusedTextColor = Color(0xFFF8FAFC)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("market_search_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (searchQuery.isNotBlank()) "${filteredProducts.size} results found" else "Type to search products",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }

    // Country Picker Dialog
    if (showCountryDialog) {
        Dialog(onDismissRequest = { showCountryDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Country",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFFF8FAFC)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    WEB_MARKETPLACE_COUNTRIES.forEach { item ->
                        val isSelected = selectedCountry.equals(item.code, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onCountrySelected(item.code)
                                    showCountryDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = item.flag, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = item.name,
                                color = if (isSelected) Color(0xFF1877F2) else Color(0xFFF8FAFC),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Exactly matching Web Marketplace Card: aspect-[0.86] with bottom gradient overlay
@Composable
fun WebMarketProductCard(
    product: Product,
    onClick: () -> Unit
) {
    val displayPrice = product.discountPrice ?: product.price
    val formattedPrice = formatMarketPrice(displayPrice, product.country)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.86f)
            .background(Color(0xFF0F172A))
            .clickable(onClick = onClick)
            .testTag("product_card_${product.id}")
    ) {
        // Full Image
        AsyncImage(
            model = product.imageUrl,
            contentDescription = product.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Bottom gradient overlay: from black/75 via black/20 to transparent
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x33000000),
                            Color(0xBF000000)
                        ),
                        startY = 140f
                    )
                )
        )

        // Price text at bottom left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = formattedPrice,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Sponsored "Ad" Badge top-left: bg-[#F97316] text-white text-[11px] font-bold px-2.5 py-0.5 rounded shadow-md
        if (product.isSponsored) {
            Surface(
                color = Color(0xFFF97316),
                shape = RoundedCornerShape(4.dp),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = "Ad",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        // "New" Badge top-right: bg-[#1877F2] text-white text-[10px] font-bold px-2 py-1 rounded-full
        if (product.isFresh && !product.isSponsored) {
            Surface(
                color = Color(0xFF1877F2),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = "New",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
