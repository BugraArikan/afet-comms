package com.example.afetcomms.util

object MessageLocationParser {

    private val LOCATION_REGEX = Regex(
        """\[konum:\s*([-\d.]+)\s*,\s*([-\d.]+)\s*]""",
        RegexOption.IGNORE_CASE
    )

    fun parse(content: String): LocationHelper.Coordinates? {
        val match = LOCATION_REGEX.find(content) ?: return null
        return try {
            LocationHelper.Coordinates(
                latitude = match.groupValues[1].toDouble(),
                longitude = match.groupValues[2].toDouble()
            )
        } catch (_: NumberFormatException) {
            null
        }
    }

    fun formatMapReference(coords: LocationHelper.Coordinates): String {
        return "${"%.5f".format(coords.latitude)}, ${"%.5f".format(coords.longitude)}"
    }
}
