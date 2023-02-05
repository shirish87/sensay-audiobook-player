package data

enum class InactiveReason(
    val label: String,
) {
    SCANNING(
        label = "Scanning",
    ),
    NOT_FOUND(
        label = "Not Found",
    );
}
