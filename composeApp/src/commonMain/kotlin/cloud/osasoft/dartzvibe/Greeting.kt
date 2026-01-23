package cloud.osasoft.dartzvibe

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = "Hello, ${platform.name}!"
}
