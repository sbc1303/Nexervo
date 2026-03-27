# NEXERVO — Sistema de Gestión de Reservas para Restaurante

**TFC · Ciclo Formativo de Grado Superior en Desarrollo de Aplicaciones Multiplataforma**
Alumno: Santiago Bermejo Caballero · Curso: 2º DAM

---

## ¿Qué es NEXERVO?

NEXERVO es una aplicación de escritorio para gestionar reservas en un restaurante.
La desarrollé como TFC aplicando lo aprendido en clase (Java, JavaFX, MySQL, MVC...)
pero también basándome en mis 12 años de experiencia en hostelería como camarero,
maître y cocinero.

El nombre viene de "nexo" + "servicio" — la idea es que la app sea el nexo entre
el equipo de sala y la información que necesita para dar un buen servicio.

---

## ¿Por qué elegí este proyecto?

Después de 12 años en hostelería, he visto cómo muchos restaurantes siguen gestionando
las reservas en papel o en una hoja de Excel. Los sistemas profesionales como TheFork
o Resy son potentes pero caros y con más funciones de las que un restaurante pequeño
o mediano necesita.

Quise hacer algo que un restaurante de barrio pudiera usar: sencillo, con las funciones
que realmente se usan cada día, y con atención especial a los detalles de sala que marcan
la diferencia (alergias, ocasiones especiales, clientes habituales...).

---

## Tecnologías

| Tecnología | Uso |
|---|---|
| Java 21 + JavaFX 21 | Interfaz gráfica de escritorio |
| MySQL 8 | Base de datos principal |
| Maven | Gestión del proyecto y dependencias |
| Jakarta Mail 2 | Envío de email de confirmación (Gmail SMTP) |
| Apache PDFBox 3 | Generación del parte de sala en PDF |
| JUnit 5 + Mockito | Tests unitarios y de integración |
| H2 Database | Base de datos en memoria para los tests |
| SLF4J + Logback | Logging estructurado |
| JaCoCo | Cobertura de tests |

Patrón de diseño: **MVC** con capa de servicio (`ReservaServicio`, `ClienteServicio`)
entre los controladores y los DAOs.

---

## Funcionalidades

**Nueva reserva (vista de cliente):**
- Búsqueda por teléfono con autorelleno automático de datos
- Mapa visual de mesas con disponibilidad en tiempo real
- Registro de los 14 alérgenos obligatorios (Reglamento UE 1169/2011)
- Ocasión especial y peticiones de sala
- Preautorización de pago como garantía (10 € por comensal)
- Email de confirmación automático al completar la reserva

**Gestión de reservas (panel interno):**
- Vista por fecha con tabla y panel de detalle
- Alerta visual para clientes con alergias y ocasiones especiales
- Exportar la hoja del día a PDF para entregar a cocina

**Módulo de pagos:**
- Procesar devolución de preautorización (cliente se presentó)
- Cobrar penalización por no-show (cliente no se presentó)

**Ficha de clientes:**
- Datos personales, alergias e historial de visitas
- KPIs: visitas completadas, ausencias, gasto estimado

**Dashboard de estadísticas** (solo administrador):
- Reservas por día de la semana (últimos 90 días)
- Franjas horarias pico
- Top 5 clientes más frecuentes
- Ingresos estimados por mes

**Control de acceso:**
- Rol ADMIN: acceso completo + estadísticas + gestión de pagos
- Rol EMPLEADO: nueva reserva, gestión básica, ficha de clientes

---

## Estructura del proyecto

```
src/
├── main/
│   ├── java/com/nexervo/
│   │   ├── Launcher.java              (punto de entrada)
│   │   ├── controlador/               (controladores JavaFX — 9 clases)
│   │   ├── datos/                     (DAOs: acceso a MySQL)
│   │   ├── modelo/                    (entidades: Reserva, Cliente, Mesa...)
│   │   └── servicio/                  (lógica de negocio: ReservaServicio, EmailService...)
│   └── resources/
│       ├── vista/                     (archivos FXML)
│       ├── Estilo.css
│       └── logback.xml
└── test/
    └── java/nexervo/                  (10 clases de test, 61 métodos)
        ├── ReservaDAOTest.java        (integración con H2)
        ├── ClienteDAOTest.java
        ├── PagoDAOTest.java
        ├── MesaDAOTest.java
        ├── EmailServiceTest.java
        ├── ReservaDAOMockTest.java    (con Mockito)
        ├── ImportePagoTest.java
        ├── ClienteValidacionTest.java
        ├── ReservaModelTest.java
        └── ConexionH2Test.java        (utilidad H2 compartida)
```

---

## Instalación y puesta en marcha

### Requisitos
- Java 21 o superior
- MySQL 8
- Maven (o usar el runner de IntelliJ)

### Pasos

1. **Crear la base de datos**

   ```sql
   source Base_de_datos_NEXERVO_v3.sql
   ```

2. **Configurar la conexión**

   Copia el archivo de ejemplo y edítalo con tus credenciales:

   ```bash
   cp src/main/resources/config.properties.example src/main/resources/config.properties
   ```

   Edita `config.properties` con tus datos de MySQL y, si quieres activar el email,
   con una contraseña de aplicación de Gmail (Seguridad → Verificación en 2 pasos →
   Contraseñas de aplicación).

3. **Ejecutar desde IntelliJ**

   Importa como proyecto Maven y lanza `com.nexervo.Launcher`.

4. **Ejecutar desde Maven**

   ```bash
   mvn javafx:run
   ```

5. **Ejecutar los tests**

   ```bash
   mvn test
   ```

   Los tests usan H2 en memoria — no necesitan MySQL instalado.

### Credenciales de prueba

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin1234` | ADMIN |
| `empleado` | `empleado1234` | EMPLEADO |

---

## Decisiones de diseño

**NO_SHOW vs CANCELADA** son estados distintos. Cancelar implica aviso previo; el no-show
es que el cliente no aparece sin avisar. En sala la diferencia es importante: con la
cancelación tienes tiempo de reasignar la mesa, con el no-show no. Lo aprendí en mis años
de trabajo en hostelería.

**Capa de servicio** (`ReservaServicio`, `ClienteServicio`): los controladores no acceden
directamente a los DAOs para operaciones con lógica de negocio. La comprobación de
conflictos de mesa, la creación de la preautorización y la cancelación coordinada quedan
en un sitio centralizado y testeable.

**EstadisticasDAO separado de ReservaDAO**: el DAO de solo lectura para el dashboard quedó
separado del DAO que hace escrituras. Al principio lo tenía todo junto y era difícil de
mantener.

**Preautorización de 10 €/comensal**: sin TPV integrado, uso este valor como ticket medio
orientativo para un restaurante de precio medio en España. En un sistema real se conectaría
con el TPV para calcular el importe real.

---

## Posibles mejoras futuras

- Integración con TPV para calcular el gasto real por mesa
- API REST para acceder desde dispositivos móviles o PDAs de sala
- Recordatorios automáticos por SMS/email antes de la reserva
- Gestión de lista de espera para turnos completos
- Exportar el historial de clientes a PDF o Excel

---

*Proyecto desarrollado entre febrero y abril de 2026.*
