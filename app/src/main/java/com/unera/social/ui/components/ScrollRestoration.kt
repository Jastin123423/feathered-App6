package com.unera.social.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import kotlinx.coroutines.launch

/**
 * ScrollRestorationController - Manages and coordinates scroll positions across screens and routes.
 * Mirrors native Facebook / mobile web scroll restoration behavior:
 * 1. Resets scroll position to (0, 0) upon navigating to a new route.
 * 2. Allows instant or animated scroll-to-top when tapping the active bottom navigation tab.
 * 3. Keeps registries for LazyListState, LazyGridState, and ScrollState.
 */
class ScrollRestorationController {
    private val listStates = mutableMapOf<String, LazyListState>()
    private val gridStates = mutableMapOf<String, LazyGridState>()
    private val scrollStates = mutableMapOf<String, ScrollState>()

    fun register(key: String, state: LazyListState) {
        listStates[key] = state
    }

    fun register(key: String, state: LazyGridState) {
        gridStates[key] = state
    }

    fun register(key: String, state: ScrollState) {
        scrollStates[key] = state
    }

    fun unregister(key: String) {
        listStates.remove(key)
        gridStates.remove(key)
        scrollStates.remove(key)
    }

    suspend fun scrollToTop(key: String, animate: Boolean = true) {
        listStates[key]?.let {
            if (animate) it.animateScrollToItem(0, 0) else it.scrollToItem(0, 0)
        }
        gridStates[key]?.let {
            if (animate) it.animateScrollToItem(0, 0) else it.scrollToItem(0, 0)
        }
        scrollStates[key]?.let {
            if (animate) it.animateScrollTo(0) else it.scrollTo(0)
        }
    }

    suspend fun onRouteChanged(route: String) {
        // Scroll to top on route change (like Facebook / window.scrollTo(0, 0))
        scrollToTop(route, animate = false)
    }
}

/**
 * LocalScrollRestoration - CompositionLocal providing the app's ScrollRestorationController.
 */
val LocalScrollRestoration = staticCompositionLocalOf {
    ScrollRestorationController()
}

/**
 * ProvideScrollRestoration - Injects ScrollRestorationController into the Compose tree.
 */
@Composable
fun ProvideScrollRestoration(
    controller: ScrollRestorationController = remember { ScrollRestorationController() },
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalScrollRestoration provides controller) {
        content()
    }
}

/**
 * ScrollRestoration - Native Jetpack Compose equivalent of React Router's ScrollRestoration.tsx:
 *
 * export default function ScrollRestoration() {
 *   const { pathname } = useLocation();
 *   useEffect(() => {
 *     // Scroll to top on route change (like Facebook)
 *     window.scrollTo(0, 0);
 *   }, [pathname]);
 *   return null;
 * }
 */
@Composable
fun ScrollRestoration(
    currentRoute: Any,
    controller: ScrollRestorationController = LocalScrollRestoration.current
) {
    val routeKey = currentRoute.toString()
    LaunchedEffect(routeKey) {
        // Scroll to top on route change (like Facebook / window.scrollTo(0, 0))
        controller.onRouteChanged(routeKey)
    }
}

/**
 * ScrollRestoration overload for a specific LazyListState.
 */
@Composable
fun ScrollRestoration(
    currentRoute: Any,
    lazyListState: LazyListState
) {
    val routeKey = currentRoute.toString()
    LaunchedEffect(routeKey) {
        lazyListState.scrollToItem(0, 0)
    }
}

/**
 * ScrollRestoration overload for a specific LazyGridState.
 */
@Composable
fun ScrollRestoration(
    currentRoute: Any,
    gridState: LazyGridState
) {
    val routeKey = currentRoute.toString()
    LaunchedEffect(routeKey) {
        gridState.scrollToItem(0, 0)
    }
}

/**
 * ScrollRestoration overload for a specific ScrollState.
 */
@Composable
fun ScrollRestoration(
    currentRoute: Any,
    scrollState: ScrollState
) {
    val routeKey = currentRoute.toString()
    LaunchedEffect(routeKey) {
        scrollState.scrollTo(0)
    }
}

/**
 * rememberRestoredLazyListState - Remembers a LazyListState and registers it with ScrollRestoration.
 */
@Composable
fun rememberRestoredLazyListState(
    key: String,
    controller: ScrollRestorationController = LocalScrollRestoration.current
): LazyListState {
    val state = rememberLazyListState()
    DisposableEffect(key, state) {
        controller.register(key, state)
        onDispose {
            controller.unregister(key)
        }
    }
    return state
}

/**
 * rememberRestoredLazyGridState - Remembers a LazyGridState and registers it with ScrollRestoration.
 */
@Composable
fun rememberRestoredLazyGridState(
    key: String,
    controller: ScrollRestorationController = LocalScrollRestoration.current
): LazyGridState {
    val state = rememberLazyGridState()
    DisposableEffect(key, state) {
        controller.register(key, state)
        onDispose {
            controller.unregister(key)
        }
    }
    return state
}

/**
 * rememberRestoredScrollState - Remembers a ScrollState and registers it with ScrollRestoration.
 */
@Composable
fun rememberRestoredScrollState(
    key: String,
    controller: ScrollRestorationController = LocalScrollRestoration.current
): ScrollState {
    val state = rememberScrollState()
    DisposableEffect(key, state) {
        controller.register(key, state)
        onDispose {
            controller.unregister(key)
        }
    }
    return state
}
