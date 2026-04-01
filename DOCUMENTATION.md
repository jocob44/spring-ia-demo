# Documentación Técnica del Controlador de Prueba
## Introducción
El Controlador de Prueba es un componente diseñado para proporcionar una funcionalidad básica de prueba en la aplicación. Su propósito general es permitir la verificación de la lógica de negocio y la funcionalidad de la aplicación mediante un conjunto de endpoints simples.

## Endpoints
A continuación, se presenta la lista de endpoints disponibles en el Controlador de Prueba:

* **GET /test**: Obtiene un mensaje de prueba según el valor de `isValid`.

## Detalle de Endpoints
### GET /test
* **Parámetros de entrada**: Ninguno
* **Tipo de retorno**: Cadena de texto (String)
* **Descripción**: Devuelve un mensaje de prueba que depende del valor de `isValid`. Si `isValid` es `true`, devuelve "Este es el valor correcto.", de lo contrario, devuelve "Este es el valor incorrecto.".

## Ejemplo de Uso
Para probar el endpoint `/test`, puede utilizar la herramienta `curl` de la siguiente manera:
bash
curl -X GET http://localhost:8080/test

Esto debería devolver el mensaje de prueba correspondiente al valor de `isValid` en ese momento.

## Notas
* El valor de `isValid` se establece inicialmente en `true`, pero puede ser modificado en el futuro para cambiar el comportamiento del endpoint `/test`.
* El Controlador de Prueba es solo un ejemplo y no debe ser utilizado en producción sin una revisión y pruebas exhaustivas.