package cloud.osasoft.dartzvibe.util

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

@Suppress("CAST_NEVER_SUCCEEDS")
actual fun formatDate(timestamp: Long): String {
    val date = NSDate(timeIntervalSinceReferenceDate = (timestamp / 1000.0) - NSTimeIntervalSince1970)
    val formatter = NSDateFormatter().apply {
        dateFormat = "MMM d, yyyy"
    }
    return formatter.stringFromDate(date)
}

@Suppress("CAST_NEVER_SUCCEEDS")
actual fun formatDateTime(timestamp: Long): String {
    val date = NSDate(timeIntervalSinceReferenceDate = (timestamp / 1000.0) - NSTimeIntervalSince1970)
    val formatter = NSDateFormatter().apply {
        dateFormat = "MMM d, yyyy 'at' h:mm a"
    }
    return formatter.stringFromDate(date)
}
