package data

enum class BookCategory(
    val label: String,
) {
    CURRENT(
        label = "Current",
    ),
    NOT_STARTED(
        label = "Not Started",
    ),
    FINISHED(
        label = "Finished",
    );
}
