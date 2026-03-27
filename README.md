# NEXERVO — Sistema de Gestión de Reservas para Restaurante

**TFC · Ciclo Formativo de Grado Superior en Desarrollo de Aplicaciones Multiplataforma**
Alumno: Santiago Bermejo Caballero
Curso: 2º DAM

---

## ¿Qué es NEXERVO?

NEXERVO es una aplicación de escritorio para gestionar reservas en un restaurante.
La desarrollé como TFC aplicando lo que hemos visto en clase (Java, JavaFX, MySQL, MVC...)
pero también apoyándome en mis años de experiencia trabajando en hostelería como camarero,
maître y cocinero.

El nombre viene de "nexo" + "servicio" — la idea es que la app sea el nexo entre
el equipo de sala y la información que necesita para dar un buen servicio.

---

## ¿Por qué elegí este proyecto?

Después de 12 años en hostelería, he visto de primera mano cómo muchos restaurantes
siguen gestionando las reservas en papel o en una hoja de Excel básica. Los sistemas
profesionales como TheFork o Resy son muy potentes pero caros y con más funciones
de las que un restaurante pequeño o mediano necesita.

Quise hacer algo que un restaurante de barrio pudiera usar: sencillo, con las funciones
que realmente se usan cada día, y con atención especial a los detalles de sala que marcan
la diferencia (alergias, ocasiones especiales, conocer a los clientes habituales...).

---

## Tecnologías usadas

- **Java 21** con **JavaFX 21** (interfaz gráfica de escritorio)
- **MySQL 8** (base de datos)
- **Maven** (gestión del proyecto y dependencias)
- **JUnit 5** (tests unitarios del modelo)
- Patrón **MVC** con separación entre modelo, DAO y controladores FXML

---

## Funcionalidades principales

**Nueva reserva:**
- Búsqueda de cliente por teléfono con autorelleno automático de datos
- Mapa visual de mesas con disponibilidad en tiempo real
- Registro de alergias e intolerancias (14 alérgenos obligatorios según Regl. UE 1169/2011)
- Ocasión especial (cumpleaños, aniversario, etc.) y peticiones de sala
- Preautorización de pago como garantía de reserva

**Gestión de reservas:**
- Vista por fecha con tabla y panel de detalle
- Estados: CONFIRMADA → FINALIZADA / CANCELADA / NO_SHOW
- Alerta visual para clientes con alergias y reservas con ocasión especial
- **Hoja del día**: genera automáticamente el parte de sala para entregar a cocina

**Clientes:**
- Ficha completa con datos, alergias y observaciones
- Historial de visitas con KPIs: visitas completadas, no-shows, gasto estimado

**Dashboard de estadísticas** (solo administrador):
- Reservas por día de la semana (últimos 90 días)
- Franjas horarias pico
- Top 5 clientes más frecuentes
- Ingresos estimados por mes

**Control de acceso:**
- Rol ADMIN: acceso completo + estadísticas + marcar no-show + finalizar reservas
- Rol EMPLEADO: nueva reserva, gestión básica, ficha de clientes

---

## Estructura del proyecto

```
src/
├── main/
│   ├── java/
│   │   ├── com/nexervo/
│   │   │   ├── Launcher.java          (punto de entrada)
│   │   │   └── controlador/           (controladores JavaFX)
│   │   ├── datos/                     (DAOs: acceso a BD)
│   │   └── modelo/                    (entidades: Reserva, Cliente, Mesa...)
│   └── resources/
│       └── vista/                     (archivos FXML + CSS)
└── test/
    └── java/nexervo/
        └── ReservaModelTest.java      (tests unitarios)
```

---

## Instalación y puesta en marcha

### Requisitos
- Java 21 o superior
- MySQL 8
- Maven (opcional, se puede usar el wrapper del IDE)

### Pasos

1. **Crear la base de datos**
   Ejecuta el script `Base_de_datos_NEXERVO_v3.sql` en tu servidor MySQL:
   ```sql
   source /ruta/al/archivo/Base_de_datos_NEXERVO_v3.sql
   ```

2. **Configurar la conexión**
   Edita el archivo `src/main/java/datos/ConexionConMySQL.java` con tus credenciales:
   ```java
   private static final String URL      = "jdbc:mysql://localhost:3306/nexervo_db";
   private static final String USUARIO  = "root";
   private static final String PASSWORD = "tupassword";
   ```

3. **Ejecutar desde IntelliJ / Eclipse**
   Importa el proyecto como proyecto Maven y lanza la clase `Launcher`.

4. **Ejecutar desde Maven**
   ```bash
   mvn javafx:run
   ```

### Credenciales de prueba
| Usuario    | Contraseña     | Rol       |
|------------|----------------|-----------|
| `admin`    | `admin1234`    | ADMIN     |
| `empleado` | `empleado1234` | EMPLEADO  |

---

## Tests

Los tests del modelo se ejecutan sin necesidad de conexión a BD:

```bash
mvn test
```

Hay 7 tests unitarios sobre la clase `Reserva` (constructor, lógica de ocasión especial,
estados editables...). Los tests de DAO los dejé pendientes porque requieren BD de prueba
y en el tiempo del TFC no llegué a configurar H2 ni un esquema de test.

---

## Decisiones de diseño

Algunas cosas que hice de una manera concreta y que quiero dejar explicadas:

- **NO_SHOW vs CANCELADA**: son estados diferentes. Cancelar implica aviso previo;
  el no-show es que el cliente simplemente no aparece. En un servicio completo la
  diferencia es importante porque en uno tienes tiempo de reasignar la mesa y en el
  otro no. Lo distinguí porque lo he vivido muchas veces.

- **Estimación de gasto (10 €/comensal)**: no tengo tabla de precios en la BD,
  así que uso 10 € como ticket medio orientativo. En un sistema real se conectaría
  con el TPV para tener los números reales. Lo dejo señalado con un TODO en el código.

- **EstadisticasDAO separado de ReservaDAO**: mi tutor me recomendó separar el DAO
  de solo lectura del que hace escrituras. Al principio lo tenía todo junto y era
  difícil de mantener.

- **lookupAll para colorear gráficos**: JavaFX 21 no permite cambiar el color de las
  barras directamente desde FXML. Tuve que buscar cómo hacerlo por código y la solución
  es usar `chart.lookupAll(".bar")` después del render. No es lo más limpio pero funciona.

---

## Posibles mejoras futuras

- Exportar la hoja del día a PDF (o enviarla por email/WhatsApp directamente)
- Integración con TPV para calcular el gasto real por mesa
- API REST para acceder desde dispositivos móviles o PDAs de sala
- Recordatorios automáticos por SMS/email antes de la reserva
- Gestión de lista de espera para turnos completos

---

*Proyecto desarrollado entre febrero y abril de 2026.*
