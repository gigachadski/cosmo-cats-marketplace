# Cosmo Cats Intergalactic Marketplace 🐱🚀

Бекенд платформи **Cosmo Cats Intergalactic Marketplace** — лабораторна робота 1
«Створення контракту API для інтергалактичного ринку».

## Стек

- Java 21, Spring Boot 4.1
- Maven (wrapper `mvnw`)

## Запуск

```bash
./mvnw spring-boot:run
```

## Сторонній сервіс курсів (WireMock)

RestClient звертається до сервісу Galactic Exchange за адресою `cosmo.clients.exchange-rate.base-url` (за замовчуванням `http://localhost:8089`).
Локально його імітує WireMock зі stub-маппінгами з теки [`wiremock/`](wiremock/mappings/exchange-rates.json):

```bash
docker compose up -d
```

або без Docker: `java -jar wiremock-standalone-3.13.1.jar --port 8089 --root-dir wiremock`.

Після цього `GET /api/v1/products/1/price?currency=EUR` повертає ціну, перераховану за курсом.

## API

Контракт: [`src/main/resources/api-specs/product-api-spec.yml`](src/main/resources/api-specs/product-api-spec.yml) (OpenAPI 3.0.3).

| Метод    | Шлях                                     | Успіх                  |
|----------|------------------------------------------|------------------------|
| `GET`    | `/api/v1/products?page=0&size=10`        | `200` paged envelope   |
| `GET`    | `/api/v1/products/{id}`                  | `200`                  |
| `POST`   | `/api/v1/products`                       | `201` + `Location`     |
| `PUT`    | `/api/v1/products/{id}`                  | `200`                  |
| `DELETE` | `/api/v1/products/{id}`                  | `204`                  |
| `GET`    | `/api/v1/products/{id}/price?currency=EUR` | `200` (через RestClient) |

Помилки повертаються у форматі RFC 9457 (`application/problem+json`):

```json
{
  "type": "https://cosmo-cats.market/errors/validation",
  "title": "Validation failed",
  "status": 400,
  "detail": "Field 'price' must be greater than 0.",
  "instance": "/api/v1/products",
  "errors": [
    { "field": "price", "message": "must be greater than 0" }
  ]
}
```

## Структура

```
domain/       Product, Category, Order, OrderItem, OrderStatus, Cart, CartItem (DDD, незмінні, BigDecimal)
dto/          ProductRequestDto (Bean Validation), ProductResponseDto, CategoryDto, PageResponse
mapper/       ProductMapper (ручний toDto/toDomain), ProductMapStructMapper (MapStruct, використовується сервісом)
validation/   @CosmicWordCheck + CosmicWordCheckValidator
repository/   інтерфейси + in-memory mock-реалізації
service/      ProductService (CRUD, пагінація, конвертація ціни)
client/       RestClient для Galactic Exchange (локально імітується WireMock, див. вище)
web/          ProductController, GlobalExceptionHandler (RFC 9457)
```

## Гілки

| Гілка        | Призначення                                                   |
|--------------|---------------------------------------------------------------|
| `main`       | Базовий каркас застосунку                                     |
| `lab1-draft` | Первинна версія з типовими code smells (PR1 → `main`, draft)  |
| `lab1-fixes` | Виправлення за self-review (PR2 → `lab1-draft`)               |
