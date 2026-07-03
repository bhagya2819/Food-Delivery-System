pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "food-delivery"

// Common modules
include("common:common-lib")
include("common:common-config")

// Infrastructure
include("infrastructure:eureka-server")
include("infrastructure:config-server")
include("infrastructure:gateway-service")

// Services
include("services:user-service")
include("services:restaurant-service")
include("services:cart-service")
include("services:order-service")
include("services:delivery-service")
include("services:payment-service")
include("services:notification-service")
include("services:search-service")
