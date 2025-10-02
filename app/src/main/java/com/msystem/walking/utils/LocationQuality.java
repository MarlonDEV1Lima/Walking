package com.msystem.walking.utils;

/**
 * Enumeration representing different levels of GPS location quality
 */
public enum LocationQuality {
    EXCELLENT,    // ±5m accuracy
    GOOD,         // ±10m accuracy
    FAIR,         // ±20m accuracy
    POOR,         // ±50m accuracy
    VERY_POOR,    // >50m accuracy
    OUTDATED,     // Location is too old
    NO_LOCATION   // No location available
}
