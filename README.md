# ms-rutaexpress-reportes

Microservicio de Dominio, Consumidor de Eventos Kafka y Motor Analitico en Spring Boot 3 para el calculo en tiempo real de KPIs, metricas operacionales y rendimiento logistico de RutaExpress.

---

## 1. Descripcion General

`ms-rutaexpress-reportes` procesa el flujo de eventos emitidos hacia el topico troncal `shipments.events` de Apache Kafka bajo el grupo de consumidores `reportes-group`. A traves de un motor analitico local optimizado (H2 en memoria con persistencia JPA), calcula metricas agregadas en tiempo real sin sobrecargar la base de datos transaccional Oracle de envios, habilitando tableros directivos, monitoreo operacional y paneles gerenciales.

---

## 2. Caracteristicas Principales

- **Consumo Asincrono y Continuo:** Escucha eventos de creacion y cambio de estado (`CREADO`, `ACEPTADO`, `EN_BODEGA`, `EN_RUTA`, `ENTREGADO`, `CANCELADO`) sin impacto en el rendimiento transaccional.
- **Calculo Dinamico de Lead Time:** Medicion automatica de la duracion exacta entre la emision inicial y la entrega efectiva al cliente destinatario.
- **Tasa de Cumplimiento Logistico:** Balance porcentual de entregas exitosas frente a cancelaciones operacionales.
- **Segmentacion por Franjas Horarias:** Analisis de volumen de envios por hora para identificacion de cuellos de botella y picos de demanda.
- **Rendimiento de Couriers y Operadores:** Monitoreo del volumen gestionado y tasa de exito individual por operador.
- **Ranking de Demanda de Servicios:** Medicion de preferencia de servicios logisticos (Express, Estandar, etc.) en diferentes ventanas temporales.

---

## 3. Arquitectura y Tecnologias

- **Lenguaje:** Java 17
- **Framework:** Spring Boot 3.3.5 (Spring Web, Spring Data JPA, Spring Kafka, Spring Boot Actuator)
- **Mensajeria:** Apache Kafka 3.7.x (Cluster Confluent en puerto 9092)
- **Almacen Analitico:** Base de datos H2 en memoria (Modo Oracle)
- **Puerto de Servicio:** `8086`

---

## 4. Endpoints de la API REST

Todos los endpoints estan redactados obligatoriamente en espanol bajo el patron REST:

### A. Resumen Ejecutivo de KPIs
`GET /api/reportes/kpis?rango=ultimas24h`
- **Filtro opcional `rango`:** `ultimas24h` (default), `ultimos7dias`, `ultimomes`, `historico`.
- **Respuesta (200 OK):**
```json
{
  "leadTimePromedioHoras": 3.45,
  "tasaCumplimiento": 97.2,
  "totalEnvios": 150,
  "totalActivos": 45,
  "totalEntregados": 102,
  "totalCancelados": 3,
  "distribucionPorEstado": {
    "CREADO": 5,
    "ACEPTADO": 10,
    "EN_BODEGA": 15,
    "EN_RUTA": 15,
    "ENTREGADO": 102,
    "CANCELADO": 3
  },
  "volumenPorHora": [
    { "hora": "09:00", "cantidad": 12 },
    { "hora": "10:00", "cantidad": 25 },
    { "hora": "11:00", "cantidad": 18 }
  ]
}
```

### B. Ranking de Servicios Mas Solicitados
`GET /api/reportes/top-servicios?rango=ultimos7dias`
- **Respuesta (200 OK):**
```json
[
  {
    "servicioNombre": "Envio Express 24h",
    "cantidadEnvios": 85,
    "porcentajeTotal": 56.7
  },
  {
    "servicioNombre": "Envio Estandar 48h",
    "cantidadEnvios": 45,
    "porcentajeTotal": 30.0
  },
  {
    "servicioNombre": "Envio Mismo Dia",
    "cantidadEnvios": 20,
    "porcentajeTotal": 13.3
  }
]
```

### C. Rendimiento por Courier u Operador
`GET /api/reportes/rendimiento-couriers`
- **Respuesta (200 OK):**
```json
[
  {
    "usuarioEmail": "courier1@rutaexpress.cl",
    "usuarioRol": "COURIER",
    "totalGestionados": 40,
    "entregados": 38,
    "cancelados": 2,
    "tasaExito": 95.0
  }
]
```

### D. Salud del Microservicio
`GET /api/publico/salud`
- **Respuesta (200 OK):**
```json
{
  "estado": "DISPONIBLE",
  "servicio": "ms-rutaexpress-reportes",
  "puerto": 8086,
  "timestamp": "2026-10-18T17:30:00"
}
```

---

## 5. Pruebas Automatizadas

```bash
mvn clean test
```
Incluye pruebas unitarias del calculo de Lead Time y metricas, pruebas de capa web con MockMvc y prueba E2E contra el broker Kafka real (`localhost:9092`).
