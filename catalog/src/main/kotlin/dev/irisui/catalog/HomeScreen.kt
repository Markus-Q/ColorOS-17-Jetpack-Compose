package dev.irisui.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.irisui.content.IrisGroupedList
import dev.irisui.content.IrisListItem
import dev.irisui.content.IrisSectionHeader
import dev.irisui.navigation.IrisLargeTitleScaffold
import dev.irisui.navigation.titleScrollOffset

/** Home: large title, then one grouped list per section. Same structure as the reference catalog. */
@Composable
fun HomeScreen(onOpen: (CatalogDestination) -> Unit) {
    val listState = rememberLazyListState()
    IrisLargeTitleScaffold(
        title = "Iris Catalog",
        subtitle = "Components for a ColorOS 17 look",
        scrollOffset = listState.titleScrollOffset(),
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = padding) {
            item { LargeTitle() }
            CatalogSection.entries.forEach { section ->
                item(key = section.name) {
                    IrisSectionHeader(section.title)
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        IrisGroupedList {
                            CatalogDestination.entries.filter { it.section == section }.forEach { destination ->
                                item {
                                    IrisListItem(
                                        headline = destination.title,
                                        showChevron = true,
                                        onClick = { onOpen(destination) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
