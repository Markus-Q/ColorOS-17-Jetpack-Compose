package dev.irisui.catalog

enum class CatalogSection(val title: String) {
    Navigation("Navigation"),
    Buttons("Buttons"),
    Inputs("Inputs"),
    Content("Content"),
    Popups("Popups"),
    Effects("Effects"),
    Examples("Example screens"),
}

/** Every screen of the catalog, grouped by section. The order here is the order on the home screen. */
enum class CatalogDestination(val section: CatalogSection, val title: String) {
    BottomNavPill(CatalogSection.Navigation, "Bottom nav pill"),
    TopBar(CatalogSection.Navigation, "Top bar"),
    LargeTitle(CatalogSection.Navigation, "Collapsing large title"),
    SegmentedTabs(CatalogSection.Navigation, "Segmented tabs"),
    PageTransitions(CatalogSection.Navigation, "Page transitions"),

    PrimaryButton(CatalogSection.Buttons, "Primary button"),
    SecondaryButton(CatalogSection.Buttons, "Secondary button"),
    IconButton(CatalogSection.Buttons, "Icon button"),
    RoundPlusButton(CatalogSection.Buttons, "Round plus button"),
    TextButton(CatalogSection.Buttons, "Text button"),

    Switch(CatalogSection.Inputs, "Switch"),
    TextField(CatalogSection.Inputs, "Text field"),
    SearchBar(CatalogSection.Inputs, "Search bar"),
    Checkbox(CatalogSection.Inputs, "Checkbox"),
    Slider(CatalogSection.Inputs, "Slider"),
    Progress(CatalogSection.Inputs, "Progress"),

    Card(CatalogSection.Content, "Card"),
    ListItem(CatalogSection.Content, "List item"),
    GroupedList(CatalogSection.Content, "Grouped list"),
    SectionHeader(CatalogSection.Content, "Section header"),
    Divider(CatalogSection.Content, "Divider"),
    Badge(CatalogSection.Content, "Badge"),

    Dialog(CatalogSection.Popups, "Dialog"),
    BottomSheet(CatalogSection.Popups, "Bottom sheet"),
    PopupMenu(CatalogSection.Popups, "Popup menu"),

    BlurLab(CatalogSection.Effects, "Blur lab"),
    PressAnimation(CatalogSection.Effects, "Press animation"),
    Haptics(CatalogSection.Effects, "Haptics"),
    TouchGlow(CatalogSection.Effects, "Touch glow"),
    PersistentGlow(CatalogSection.Effects, "Persistent glow"),
    DragPhysics(CatalogSection.Effects, "Drag physics"),

    PreferencesExample(CatalogSection.Examples, "Preferences"),
    HabitsExample(CatalogSection.Examples, "Habits"),
    TypeScale(CatalogSection.Examples, "Type scale"),
}
