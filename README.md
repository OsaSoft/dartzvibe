# DartzVibe - Kotlin Multiplatform App

A Kotlin Multiplatform project targeting Android and iOS, built with Compose Multiplatform.

## 🏗️ Project Architecture

This project follows a clean architecture pattern similar to Spring Boot applications:

```
composeApp/src/commonMain/kotlin/cloud/osasoft/dartzvibe/
├── config/           # App configuration (like @ConfigurationProperties)
├── data/
│   ├── model/        # Data classes (like DTOs/Entities)
│   └── repository/   # Data access layer (like @Repository)
├── di/               # Dependency injection (like @Configuration)
├── network/          # HTTP client setup (like RestTemplate/WebClient config)
└── ui/
    └── screen/       # UI screens with ScreenModels (like @Controller + ViewModels)
```

## 📚 Libraries & Spring Boot Equivalents

| Library | Purpose | Spring Boot Equivalent |
|---------|---------|----------------------|
| **kotlin-inject** | Compile-time DI | Spring DI (`@Component`, `@Autowired`) |
| **Ktor Client** | HTTP networking | RestTemplate / WebClient |
| **Kotlinx Serialization** | JSON serialization | Jackson |
| **Kotest** | Testing framework | JUnit + AssertJ |
| **Kermit** | Logging | SLF4J + Logback |
| **Multiplatform Settings** | Key-value storage | `application.properties` |
| **Voyager** | Navigation + ScreenModel | Spring MVC Controllers |

## 🚀 Quick Start

### Dependency Injection (kotlin-inject)

Similar to Spring's `@Component` and `@Autowired`, but compile-time:

```kotlin
// Define a component (like @Configuration class)
@Component
abstract class AppComponent {
    // Like @Bean
    @Provides
    fun provideHttpClient(): HttpClient = HttpClientFactory.create()
    
    // Like component scanning + @Autowired
    abstract val greetingRepository: GreetingRepository
}

// Use @Inject like @Component + constructor injection
@Inject
class GreetingRepositoryImpl : GreetingRepository {
    // ...
}
```

### HTTP Client (Ktor)

Similar to Spring's WebClient:

```kotlin
val client = HttpClient {
    install(ContentNegotiation) { json() }  // Like Jackson config
    install(Logging) { level = LogLevel.INFO }  // Like interceptors
}

// Making requests
val user = client.get("https://api.example.com/users/1").body<User>()
```

### Testing with Kotest

BDD-style assertions similar to AssertJ:

```kotlin
@Test
fun `user should have valid email`() = runTest {
    val user = repository.getUser(1)
    
    user.email shouldContain "@"
    user.name shouldNotBe null
    user.age shouldBeGreaterThan 0
}
```

### Data Classes with Serialization

Like Jackson's `@JsonProperty`:

```kotlin
@Serializable
data class User(
    val id: Long,
    @SerialName("created_at")  // Like @JsonProperty
    val createdAt: String
)
```

## 📁 Project Structure

* [/composeApp](./composeApp/src) - Shared Compose Multiplatform code
    - [commonMain](./composeApp/src/commonMain/kotlin) - Code shared across all platforms
    - [androidMain](./composeApp/src/androidMain/kotlin) - Android-specific code
    - [iosMain](./composeApp/src/iosMain/kotlin) - iOS-specific code
    - [commonTest](./composeApp/src/commonTest/kotlin) - Shared tests

* [/iosApp](./iosApp/iosApp) - iOS application entry point

## 🛠️ Build Commands

### Android

```shell
# Debug build
.\gradlew.bat :composeApp:assembleDebug

# Run tests
.\gradlew.bat :composeApp:testDebugUnitTest

# All common tests
.\gradlew.bat :composeApp:allTests
```

### iOS (requires macOS)

```shell
# Build iOS framework
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

Or open `/iosApp` in Xcode.

## 🔧 Key Concepts for Spring Developers

### expect/actual Pattern

KMP's way of platform-specific code (like conditional beans):

```kotlin
// commonMain - declare expectation
expect fun getPlatform(): Platform

// androidMain - Android implementation
actual fun getPlatform(): Platform = AndroidPlatform()

// iosMain - iOS implementation  
actual fun getPlatform(): Platform = IOSPlatform()
```

### Coroutines = Reactive Streams

If you know Spring WebFlux:
- `suspend fun` ≈ `Mono<T>`
- `Flow<T>` ≈ `Flux<T>`
- `runBlocking` ≈ `.block()`

### ScreenModel = Controller + ViewModel

```kotlin
@Inject
class HomeScreenModel(
    private val repository: GreetingRepository  // Constructor injection
) : ScreenModel {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun loadData() {
        screenModelScope.launch {  // Like @Async
            val data = repository.getData()
            _uiState.value = UiState.Success(data)
        }
    }
}
```

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
