package com.empyreanlabs.omnitouch.model

/**
 * Enum representing the different menu layout types.
 */
enum class MenuLayoutType(val id: String, val displayName: String) {
    GRID("grid", "Grid Popup"),
    RADIAL("radial", "Radial Wheel");

    companion object {
        fun fromId(id: String): MenuLayoutType {
            return entries.find { it.id == id } ?: GRID
        }
    }
}