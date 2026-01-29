package cloud.osasoft.dartzvibe.util

/**
 * Get current time in milliseconds since epoch.
 */
expect fun currentTimeMillis(): Long

/**
 * Format a timestamp to a human-readable date string.
 * Format: "MMM d, yyyy" (e.g., "Jan 15, 2024")
 */
expect fun formatDate(timestamp: Long): String

/**
 * Format a timestamp to a human-readable date and time string.
 * Format: "MMM d, yyyy 'at' h:mm a" (e.g., "Jan 15, 2024 at 3:45 PM")
 */
expect fun formatDateTime(timestamp: Long): String
