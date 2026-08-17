variable "REGISTRY" {
  default = "ghcr.io/example/fullfix"
}

variable "TAG" {
  default = "latest"
}

variable "VERSION" {
  default = "1.0.0-SNAPSHOT"
}

function "img" {
  params = [name]
  result = ["${REGISTRY}/${name}:${TAG}", "${REGISTRY}/${name}:latest"]
}

target "auth-service" {
  context    = "."
  dockerfile = "infrastructure/docker/Dockerfile.runtime"
  args = {
    MODULE  = "auth-service"
    VERSION = VERSION
  }
  tags = img("auth-service")
}

target "catalog-service" {
  context    = "."
  dockerfile = "infrastructure/docker/Dockerfile.runtime"
  args = {
    MODULE  = "catalog-service"
    VERSION = VERSION
  }
  tags = img("catalog-service")
}

target "order-service" {
  context    = "."
  dockerfile = "infrastructure/docker/Dockerfile.runtime"
  args = {
    MODULE  = "order-service"
    VERSION = VERSION
  }
  tags = img("order-service")
}

target "inventory-service" {
  context    = "."
  dockerfile = "infrastructure/docker/Dockerfile.runtime"
  args = {
    MODULE  = "inventory-service"
    VERSION = VERSION
  }
  tags = img("inventory-service")
}

target "warehouse-service" {
  context    = "."
  dockerfile = "infrastructure/docker/Dockerfile.runtime"
  args = {
    MODULE  = "warehouse-service"
    VERSION = VERSION
  }
  tags = img("warehouse-service")
}

target "shipment-service" {
  context    = "."
  dockerfile = "infrastructure/docker/Dockerfile.runtime"
  args = {
    MODULE  = "shipment-service"
    VERSION = VERSION
  }
  tags = img("shipment-service")
}

target "payment-service" {
  context    = "."
  dockerfile = "infrastructure/docker/Dockerfile.runtime"
  args = {
    MODULE  = "payment-service"
    VERSION = VERSION
  }
  tags = img("payment-service")
}

target "notification-service" {
  context    = "."
  dockerfile = "infrastructure/docker/Dockerfile.runtime"
  args = {
    MODULE  = "notification-service"
    VERSION = VERSION
  }
  tags = img("notification-service")
}

target "analytics-service" {
  context    = "."
  dockerfile = "infrastructure/docker/Dockerfile.runtime"
  args = {
    MODULE  = "analytics-service"
    VERSION = VERSION
  }
  tags = img("analytics-service")
}

target "api-gateway" {
  context    = "."
  dockerfile = "infrastructure/docker/Dockerfile.runtime"
  args = {
    MODULE  = "api-gateway"
    VERSION = VERSION
  }
  tags = img("api-gateway")
}

target "operations-dashboard" {
  context    = "./frontend/operations-dashboard"
  dockerfile = "Dockerfile"
  tags       = img("operations-dashboard")
}

group "default" {
  targets = [
    "auth-service",
    "catalog-service",
    "order-service",
    "inventory-service",
    "warehouse-service",
    "shipment-service",
    "payment-service",
    "notification-service",
    "analytics-service",
    "api-gateway",
    "operations-dashboard",
  ]
}
