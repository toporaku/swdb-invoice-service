# Servicio de Facturación (Invoice Service) - SWDB 2026

Este repositorio contiene el microservicio de **Servicio de Facturación (Invoice Service)** como un componente independiente del ecosistema SWDB 2026.

---

## Descripción

Microservicio orquestador central para la transacción de compra. Se encarga del flujo de checkout: valida el carrito del usuario, aplica cupones de descuento válidos, descuenta el stock de manera definitiva, vacía el carrito y emite la factura fiscal correspondiente.

---

## Tech Stack

*   **Lenguaje de Programación:** Java 17
*   **Framework Principal:** Spring Boot 3
*   **Gestor de Dependencias:** Maven
*   **Base de Datos:** MySQL 8+
*   **Componente en el Ecosistema:** Descubre y se configura dinámicamente mediante Eureka Registry e interactúa con el resto del ecosistema mediante llamadas balanceadas.

---

## Guía de Ejecución

Si desea arrancar este microservicio por separado para depuración o pruebas locales, siga estas instrucciones:

### Prerrequisitos
1. Asegúrese de que el **Config Server** (`puerto 8888`) y el **Registry Service** (`puerto 8761`) estén activos.
2. Asegúrese de que la base de datos MySQL esté activa y cuente con los permisos de usuario correspondientes configurados en `setup.sql` global.

### Comando de Arranque
Navegue a la carpeta raíz de este servicio y ejecute:
```bash
mvn spring-boot:run
```
*El servicio se desplegará localmente escuchando en el puerto `8084`.*

---

## Documentación de Endpoints (Swagger / OpenAPI)

Este microservicio cuenta con documentación de API interactiva autogenerada con OpenAPI. 

Una vez que el servicio esté corriendo, puede explorar y probar los endpoints interactivos ingresando a la siguiente dirección en su navegador:
 **[Swagger UI - Servicio de Facturación (Invoice Service)](http://localhost:8084/swagger-ui/index.html)**

*Nota: La ruta de metadatos OpenAPI cruda en formato JSON está disponible en: `http://localhost:8084/v3/api-docs`.*

