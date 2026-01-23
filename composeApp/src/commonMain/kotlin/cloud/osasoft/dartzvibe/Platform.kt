package cloud.osasoft.dartzvibe

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
