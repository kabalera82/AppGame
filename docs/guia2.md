# RPG Manager en JavaFX — Fase 2
### BBDD · Enemigos en Mapa · Combate Simulado · Media (Imágenes, Audio, Vídeo)

> Esta guía es la continuación directa de la Fase 1.  
> Asume que ya tienes `Personaje`, `Inventario`, `GameView`, `CombateController` y el resto funcionando.

---

## Índice Fase 2

15. [Conceptos nuevos — qué aprenderás](#15-conceptos-nuevos)
16. [Nueva estructura del proyecto](#16-nueva-estructura)
17. [Base de datos SQL — SQLite con JDBC](#17-base-de-datos-sql)
18. [DAO — el patrón que conecta BBDD y Modelos](#18-dao)
19. [Corrección del movimiento en GameView](#19-corrección-del-movimiento)
20. [Enemigos en el mapa — IA básica](#20-enemigos-en-el-mapa)
21. [Detección de colisión jugador-enemigo](#21-detección-de-colisión)
22. [Sistema de combate simulado — animaciones de ataque](#22-combate-simulado)
23. [Media — imágenes, audio y vídeo en JavaFX](#23-media)
24. [pom.xml actualizado — todas las dependencias](#24-pomxml-fase-2)
25. [Cómo conectar todo en App.java](#25-appjava-fase-2)
26. [Errores comunes Fase 2](#26-errores-fase-2)

---

## 15. Conceptos nuevos

### ¿Qué es JDBC?

**JDBC (Java Database Connectivity)** es la API estándar de Java para conectarse a bases de datos.  
Tú escribes SQL normal (`SELECT`, `INSERT`, `UPDATE`, `DELETE`) y JDBC lo manda a la base de datos y te devuelve los resultados.

```
Tu código Java
    ↓  JDBC
Base de datos (SQLite, MySQL, PostgreSQL...)
```

### ¿Por qué SQLite?

SQLite guarda toda la base de datos en **un solo archivo** `.db` dentro del proyecto.  
No necesitas instalar ningún servidor. Perfecto para aprender y para juegos de escritorio.

| Base de datos | Necesita servidor | Archivo local | Ideal para |
|---|---|---|---|
| **SQLite** | No | Sí (.db) | Apps de escritorio, aprender, juegos |
| MySQL | Sí | No | Webs, APIs, producción |
| PostgreSQL | Sí | No | Sistemas grandes, producción |

### ¿Qué es el patrón DAO?

**DAO (Data Access Object)** es un patrón de diseño que separa la lógica de negocio del acceso a datos.

```
Controller → DAO → Base de datos
Controller NO sabe si los datos vienen de SQLite, MySQL o un archivo de texto.
El DAO abstrae esa decisión.
```

Beneficio clave: si mañana cambias de SQLite a MySQL, solo cambias el DAO. El Controller no se toca.

### ¿Qué es la IA de enemigos?

En este contexto, "IA" es simplemente una función que decide el movimiento del enemigo cada frame.  
Usaremos **IA de seguimiento**: el enemigo calcula la dirección hacia el jugador y se mueve hacia él.  
No es inteligencia artificial real — es matemática básica de vectores.

### ¿Qué es la detección de colisión?

Comprobar si dos objetos se tocan en la pantalla. La técnica más simple es **AABB (Axis-Aligned Bounding Box)**: cada objeto tiene un rectángulo invisible. Si los rectángulos se solapan, hay colisión.

```
Jugador rect: (x=100, y=100, w=40, h=40)
Enemigo rect: (x=125, y=115, w=40, h=40)
→ Se solapan → ¡colisión! → iniciar combate
```

### ¿Qué es Timeline en JavaFX?

`Timeline` ejecuta código en momentos concretos con delay. Perfecto para animaciones de combate:

```
Timeline:
  → a los 0ms   : mostrar "¡ATAQUE!"
  → a los 500ms : animar golpe
  → a los 1000ms: actualizar vida
  → a los 1500ms: turno del enemigo
```

---

## 16. Nueva estructura del proyecto

```
src/main/java/k82studio/
├── App.java
├── interfaces/
│   ├── Atacable.java
│   └── Mostrable.java
│
├── models/
│   ├── Personaje.java              ← sin cambios
│   ├── Guerrero.java               ← sin cambios
│   ├── Mago.java                   ← sin cambios
│   ├── Inventario.java             ← ahora carga desde BBDD
│   └── Enemigo.java                ← NUEVO: enemigo con IA
│
├── db/                             ← NUEVO: todo lo de base de datos
│   ├── DatabaseManager.java        ← conexión y creación de tablas
│   └── PersonajeDAO.java           ← operaciones CRUD para Personaje
│
├── controllers/
│   ├── PersonajeController.java    ← sin cambios
│   └── CombateController.java      ← actualizado: animaciones
│
└── views/
    ├── MainView.java               ← sin cambios
    ├── PersonajeView.java          ← sin cambios
    ├── GameView.java               ← corregido + enemigos en mapa
    ├── CombateView.java            ← actualizado: animaciones visuales
    └── MediaView.java              ← NUEVO: imágenes, audio, vídeo

src/main/resources/
├── images/                         ← sprites e imágenes
│   └── guerrero.png
├── audio/                          ← efectos de sonido y música
│   └── ataque.mp3
└── video/                          ← vídeos de intro o cinemáticas
    └── intro.mp4
```

---

## 17. Base de datos SQL — SQLite con JDBC

### Concepto: ¿cómo funciona una conexión JDBC?

Toda conexión JDBC sigue el mismo patrón de 4 pasos:

```java
// 1. Obtener conexión
Connection conn = DriverManager.getConnection("jdbc:sqlite:rpg.db");

// 2. Preparar la consulta (PreparedStatement evita SQL injection)
PreparedStatement stmt = conn.prepareStatement("SELECT * FROM personajes WHERE id = ?");
stmt.setInt(1, 5); // sustituye el ? por el valor 5

// 3. Ejecutar y leer resultados
ResultSet rs = stmt.executeQuery();
while (rs.next()) {
    String nombre = rs.getString("nombre");
    int vida = rs.getInt("vida");
}

// 4. Cerrar recursos (siempre en finally o try-with-resources)
rs.close(); stmt.close(); conn.close();
```

### `db/DatabaseManager.java`

Clase singleton — solo existe una conexión a la BBDD en toda la app.  
**Singleton** = patrón que garantiza que solo se crea una instancia de una clase.

```java
package k82studio.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    // ── SINGLETON ──────────────────────────────────────────────
    // instancia única guardada aquí
    private static DatabaseManager instancia;

    // conexión única a la BBDD
    private Connection conexion;

    // constructor privado → nadie puede hacer new DatabaseManager()
    private DatabaseManager() {
        conectar();
        crearTablas();
    }

    // punto de acceso global → siempre devuelve la misma instancia
    public static DatabaseManager getInstance() {
        if (instancia == null) {
            instancia = new DatabaseManager();
        }
        return instancia;
    }

    // ── CONEXIÓN ───────────────────────────────────────────────
    private void conectar() {
        try {
            // el archivo rpg.db se crea automáticamente si no existe
            conexion = DriverManager.getConnection("jdbc:sqlite:rpg.db");
            System.out.println("BBDD conectada: rpg.db");
        } catch (SQLException e) {
            System.err.println("Error conectando BBDD: " + e.getMessage());
        }
    }

    public Connection getConexion() {
        return conexion;
    }

    // ── CREACIÓN DE TABLAS ─────────────────────────────────────
    // IF NOT EXISTS → solo crea la tabla si no existe todavía
    private void crearTablas() {
        String sqlPersonajes = """
            CREATE TABLE IF NOT EXISTS personajes (
                id      INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre  TEXT    NOT NULL,
                tipo    TEXT    NOT NULL,
                nivel   INTEGER NOT NULL,
                vida    INTEGER NOT NULL,
                ataque  INTEGER NOT NULL
            )
            """;

        String sqlEnemigos = """
            CREATE TABLE IF NOT EXISTS enemigos (
                id      INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre  TEXT    NOT NULL,
                vida    INTEGER NOT NULL,
                ataque  INTEGER NOT NULL,
                velocidad INTEGER NOT NULL
            )
            """;

        try (Statement stmt = conexion.createStatement()) {
            stmt.execute(sqlPersonajes);
            stmt.execute(sqlEnemigos);
            System.out.println("Tablas creadas/verificadas");
        } catch (SQLException e) {
            System.err.println("Error creando tablas: " + e.getMessage());
        }
    }
}
```

---

## 18. DAO

### Concepto: CRUD

CRUD son las 4 operaciones básicas de cualquier BBDD:

| Operación | SQL | Qué hace |
|---|---|---|
| **C**reate | `INSERT` | Crea un registro nuevo |
| **R**ead | `SELECT` | Lee registros |
| **U**pdate | `UPDATE` | Modifica un registro |
| **D**elete | `DELETE` | Borra un registro |

### `db/PersonajeDAO.java`

Te pongo el ejemplo completo de `guardar` para que implementes el resto tú.

```java
package k82studio.db;

import k82studio.models.Guerrero;
import k82studio.models.Mago;
import k82studio.models.Personaje;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PersonajeDAO {

    private final Connection conn;

    public PersonajeDAO() {
        // obtiene la conexión del Singleton — no crea una nueva
        this.conn = DatabaseManager.getInstance().getConexion();
    }

    // ── CREATE ─────────────────────────────────────────────────
    // PreparedStatement: los ? se sustituyen por los valores reales
    // Esto previene SQL Injection (nunca concatenes strings con SQL)
    public void guardar(Personaje p) {
        String tipo = (p instanceof Guerrero) ? "GUERRERO" : "MAGO";
        String sql  = "INSERT INTO personajes (nombre, tipo, nivel, vida, ataque) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, p.getNombre());
            stmt.setString(2, tipo);
            stmt.setInt(3, p.getNivel());
            stmt.setInt(4, p.getVida());
            stmt.setInt(5, p.atacar());
            stmt.executeUpdate();
            System.out.println("Personaje guardado: " + p.getNombre());
        } catch (SQLException e) {
            System.err.println("Error guardando personaje: " + e.getMessage());
        }
    }

    // ── READ — todos ───────────────────────────────────────────
    // ResultSet es como un cursor que recorre fila a fila los resultados
    public List<Personaje> obtenerTodos() {
        List<Personaje> lista = new ArrayList<>();
        String sql = "SELECT * FROM personajes";

        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String tipo   = rs.getString("tipo");
                String nombre = rs.getString("nombre");
                int nivel     = rs.getInt("nivel");
                int ataque    = rs.getInt("ataque");

                // reconstruimos el objeto correcto según el tipo guardado
                Personaje p = tipo.equals("GUERRERO")
                    ? new Guerrero(nombre, nivel, ataque)
                    : new Mago(nombre, nivel, ataque);

                lista.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error leyendo personajes: " + e.getMessage());
        }
        return lista;
    }

    // ── READ — por nombre ──────────────────────────────────────
    public Personaje obtenerPorNombre(String nombre) {
        String sql = "SELECT * FROM personajes WHERE nombre = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nombre);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String tipo  = rs.getString("tipo");
                int nivel    = rs.getInt("nivel");
                int ataque   = rs.getInt("ataque");
                return tipo.equals("GUERRERO")
                    ? new Guerrero(nombre, nivel, ataque)
                    : new Mago(nombre, nivel, ataque);
            }
        } catch (SQLException e) {
            System.err.println("Error buscando personaje: " + e.getMessage());
        }
        return null;
    }

    // ── UPDATE ─────────────────────────────────────────────────
    // TU TURNO: implementa actualizarVida(String nombre, int nuevaVida)
    // SQL: UPDATE personajes SET vida = ? WHERE nombre = ?

    // ── DELETE ─────────────────────────────────────────────────
    // TU TURNO: implementa eliminar(String nombre)
    // SQL: DELETE FROM personajes WHERE nombre = ?
}
```

### Cómo usar el DAO en App.java

```java
PersonajeDAO dao = new PersonajeDAO();

// guardar personajes nuevos (solo la primera vez)
dao.guardar(new Guerrero("Aragorn", 10, 45));
dao.guardar(new Mago("Gandalf", 15, 80));

// cargar desde BBDD en lugar de crearlos hardcodeados
List<Personaje> personajes = dao.obtenerTodos();
Inventario inventario = new Inventario();
personajes.forEach(inventario::agregar);
```

---

## 19. Corrección del movimiento

### El problema del movimiento original

El código original tiene un bug: el movimiento **se acumula infinitamente** mientras el `AnimationTimer` corre aunque no haya teclas pulsadas. Además, no hay delta time — si el ordenador va más rápido, el personaje se mueve más rápido.

**Corrección 1: Delta time** — el movimiento es independiente de los FPS.

```java
// ── VARIABLES CORREGIDAS ──────────────────────────────────────
private long ultimoFrame = 0;
private final double VELOCIDAD_PX_SEG = 200.0; // píxeles por segundo, no por frame

// ── BUCLE CORREGIDO ───────────────────────────────────────────
@Override
public void handle(long ahora) {
    if (ultimoFrame == 0) {
        ultimoFrame = ahora;
        return; // primer frame: solo inicializar
    }

    // delta = tiempo transcurrido desde el último frame en segundos
    double delta = (ahora - ultimoFrame) / 1_000_000_000.0;
    ultimoFrame = ahora;

    mover(delta);
    actualizarEnemigos(delta);
    dibujar();
}

// ── MOVIMIENTO CORREGIDO ──────────────────────────────────────
private void mover(double delta) {
    double dist = VELOCIDAD_PX_SEG * delta; // píxeles a mover este frame
    double w = canvas.getWidth();
    double h = canvas.getHeight();
    double r = RADIO_JUGADOR;

    // movimiento con límites correctos (el radio evita que se salga)
    if (teclasActivas.contains(KeyCode.LEFT)  && x - r > 0)    x -= dist;
    if (teclasActivas.contains(KeyCode.RIGHT) && x + r < w)    x += dist;
    if (teclasActivas.contains(KeyCode.UP)    && y - r > 0)    y -= dist;
    if (teclasActivas.contains(KeyCode.DOWN)  && y + r < h)    y += dist;
}
```

**Corrección 2: Radio del personaje como constante**

```java
private static final double RADIO_JUGADOR = 20.0;
// Antes: los 20 estaban hardcodeados en 3 sitios distintos → inconsistencia
// Ahora: cambias RADIO_JUGADOR y todo se actualiza
```

**Corrección 3: Separar teclas de movimiento de teclas de acción**

```java
private void mover(double delta) {
    // solo procesa movimiento — sin mezclar lógica de vida aquí
    // la vida se cambia desde la lógica de combate, no del movimiento
}

// las teclas de acción se procesan en un método separado
private void procesarAcciones() {
    // Q y E eran de prueba — en la versión real esto lo maneja CombateController
}
```

---

## 20. Enemigos en el mapa

### Concepto: IA de seguimiento (Steering behavior)

El enemigo calcula cada frame el vector desde su posición hasta el jugador, normaliza ese vector (lo convierte en dirección pura sin magnitud) y multiplica por su velocidad.

```
Vector dirección = (jugador.x - enemigo.x, jugador.y - enemigo.y)
Longitud = sqrt(dx² + dy²)
Vector normalizado = (dx / longitud, dy / longitud)
Movimiento = vector normalizado × velocidad × delta
```

### `models/Enemigo.java`

```java
package k82studio.models;

public class Enemigo {

    private double x, y;             // posición en el canvas
    private int vida;
    private final int vidaMax;
    private final int ataque;
    private final double velocidad;  // píxeles por segundo
    private final String nombre;
    private boolean vivo = true;

    public Enemigo(String nombre, int vida, int ataque, double velocidad, double x, double y) {
        this.nombre    = nombre;
        this.vida      = vida;
        this.vidaMax   = vida;
        this.ataque    = ataque;
        this.velocidad = velocidad;
        this.x         = x;
        this.y         = y;
    }

    // ── IA de seguimiento ──────────────────────────────────────
    // delta = tiempo desde el último frame en segundos
    // jugadorX, jugadorY = posición actual del jugador
    public void actualizar(double delta, double jugadorX, double jugadorY) {
        if (!vivo) return;

        double dx = jugadorX - x;
        double dy = jugadorY - y;
        double distancia = Math.sqrt(dx * dx + dy * dy);

        // evita división por cero cuando el enemigo está encima del jugador
        if (distancia > 1) {
            // normalizar y mover
            x += (dx / distancia) * velocidad * delta;
            y += (dy / distancia) * velocidad * delta;
        }
    }

    public void recibirDanio(int danio) {
        vida = Math.max(0, vida - danio);
        if (vida == 0) vivo = false;
    }

    // Getters
    public double getX()       { return x;         }
    public double getY()       { return y;         }
    public int getVida()       { return vida;       }
    public int getVidaMax()    { return vidaMax;    }
    public int getAtaque()     { return ataque;     }
    public String getNombre()  { return nombre;     }
    public boolean isVivo()    { return vivo;       }
}
```

### Integración en `GameView.java` — actualizado

```java
// ── NUEVAS VARIABLES en GameView ──────────────────────────────
private List<Enemigo> enemigos = new ArrayList<>();
private static final double RADIO_JUGADOR  = 20.0;
private static final double RADIO_ENEMIGO  = 18.0;
private static final double DIST_COMBATE   = 50.0; // colisión = combate

// ── INICIALIZAR ENEMIGOS ───────────────────────────────────────
private void inicializarEnemigos() {
    enemigos.add(new Enemigo("Orco",   60, 15, 80,  700, 100));
    enemigos.add(new Enemigo("Troll", 100, 25, 50,  200, 500));
    enemigos.add(new Enemigo("Goblin", 40, 10, 120, 100, 300));
}

// ── ACTUALIZAR ENEMIGOS cada frame ────────────────────────────
private void actualizarEnemigos(double delta) {
    for (Enemigo e : enemigos) {
        if (e.isVivo()) {
            e.actualizar(delta, x, y);
        }
    }
}

// ── DIBUJAR ENEMIGOS ──────────────────────────────────────────
private void dibujarEnemigos(GraphicsContext gc) {
    for (Enemigo e : enemigos) {
        if (!e.isVivo()) continue;

        // cuerpo del enemigo (rojo)
        gc.setFill(Color.web("#e74c3c"));
        gc.fillOval(e.getX() - RADIO_ENEMIGO, e.getY() - RADIO_ENEMIGO,
                    RADIO_ENEMIGO * 2, RADIO_ENEMIGO * 2);

        // nombre encima
        gc.setFill(Color.WHITE);
        gc.fillText(e.getNombre(), e.getX() - 20, e.getY() - 25);

        // mini barra de vida del enemigo
        double pct = (double) e.getVida() / e.getVidaMax();
        gc.setFill(Color.web("#2ecc71"));
        gc.fillRect(e.getX() - 20, e.getY() - 22, 40 * pct, 4);
        gc.setStroke(Color.WHITE);
        gc.strokeRect(e.getX() - 20, e.getY() - 22, 40, 4);
    }
}
```

---

## 21. Detección de colisión

### Concepto: distancia euclidiana

Para dos círculos, hay colisión cuando la distancia entre sus centros es menor que la suma de sus radios.

```
distancia = sqrt( (x2-x1)² + (y2-y1)² )
colisión  = distancia < (radio1 + radio2)
```

### En `GameView.java`

```java
// ── COMPROBAR COLISIONES ──────────────────────────────────────
// Se llama cada frame desde el bucle principal
private void comprobarColisiones() {
    for (Enemigo e : enemigos) {
        if (!e.isVivo()) continue;

        double dx = x - e.getX();
        double dy = y - e.getY();
        double distancia = Math.sqrt(dx * dx + dy * dy);

        if (distancia < DIST_COMBATE) {
            iniciarCombate(e);
            break; // solo un combate a la vez
        }
    }
}

// ── INICIAR COMBATE ───────────────────────────────────────────
// Para el bucle del mapa y abre la pantalla de combate
private void iniciarCombate(Enemigo enemigo) {
    timer.stop(); // detiene el AnimationTimer del mapa

    // el listener es un callback — GameView avisa a MainView que hay combate
    if (onCombateListener != null) {
        onCombateListener.accept(enemigo);
    }
}

// Callback que MainView registra para saber cuándo hay combate
private java.util.function.Consumer<Enemigo> onCombateListener;

public void setOnCombate(java.util.function.Consumer<Enemigo> listener) {
    this.onCombateListener = listener;
}
```

### Cómo conectarlo en `MainView.java`

```java
private void abrirJuego(Inventario inventario) {
    Personaje jugador = inventario.getPorIndice(0);
    GameView gameView = new GameView(jugador);

    // registramos qué pasa cuando el jugador toca a un enemigo
    gameView.setOnCombate(enemigo -> {
        CombateView combate = new CombateView(jugador, enemigo);

        // cuando el combate acaba, volvemos al mapa
        combate.setOnCombateTerminado(() -> abrirJuego(inventario));

        centro.getChildren().clear();
        centro.getChildren().add(combate);
    });

    centro.getChildren().clear();
    centro.getChildren().add(gameView);
    gameView.requestFocus();
}
```

---

## 22. Combate simulado

### Concepto: Timeline para animaciones por turnos

`Timeline` en JavaFX ejecuta `KeyFrame`s en momentos específicos. Cada `KeyFrame` es un instante en el tiempo con código a ejecutar.

```java
Timeline timeline = new Timeline(
    new KeyFrame(Duration.millis(0),    e -> { /* turno 0ms */ }),
    new KeyFrame(Duration.millis(600),  e -> { /* turno 600ms */ }),
    new KeyFrame(Duration.millis(1200), e -> { /* turno 1200ms */ })
);
timeline.play();
```

### `controllers/CombateController.java` — actualizado con Timeline

```java
package k82studio.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import k82studio.models.Enemigo;
import k82studio.models.Personaje;
import k82studio.views.CombateView;

public class CombateController {

    private final Personaje jugador;
    private final Enemigo   enemigo;
    private final CombateView vista;
    private boolean turnoJugador = true;
    private boolean enAnimacion  = false; // evita clicks durante animación

    public CombateController(Personaje jugador, Enemigo enemigo, CombateView vista) {
        this.jugador = jugador;
        this.enemigo = enemigo;
        this.vista   = vista;
    }

    // ── ATAQUE con animación ───────────────────────────────────
    public void atacar() {
        if (enAnimacion) return; // bloquear botones durante animación
        enAnimacion = true;
        vista.deshabilitarBotones(true);

        Timeline timeline = new Timeline(

            // 0ms: mostrar animación de ataque del jugador
            new KeyFrame(Duration.millis(0), e -> {
                vista.mostrarEfectoAtaque("jugador");
                vista.log(jugador.getNombre() + " prepara su ataque...");
            }),

            // 400ms: aplicar daño al enemigo
            new KeyFrame(Duration.millis(400), e -> {
                int danio = jugador.atacar();
                enemigo.recibirDanio(danio);
                vista.log(jugador.getNombre() + " golpea por " + danio + " de daño!");
                vista.actualizar(jugador, enemigo);
                vista.mostrarEfectoAtaque("enemigo");
            }),

            // 800ms: comprobar si el enemigo murió
            new KeyFrame(Duration.millis(800), e -> {
                if (!enemigo.isVivo()) {
                    vista.mostrarVictoria();
                    enAnimacion = false;
                    return;
                }
                // si sigue vivo, el enemigo contraataca
                turnoEnemigo();
            })
        );
        timeline.play();
    }

    // ── TURNO DEL ENEMIGO ──────────────────────────────────────
    private void turnoEnemigo() {
        Timeline timeline = new Timeline(

            new KeyFrame(Duration.millis(0), e ->
                vista.log(enemigo.getNombre() + " prepara su ataque...")
            ),

            new KeyFrame(Duration.millis(500), e -> {
                int danio = enemigo.getAtaque();
                jugador.setVida(jugador.getVida() - danio);
                vista.log(enemigo.getNombre() + " golpea por " + danio + "!");
                vista.actualizar(jugador, enemigo);
                vista.mostrarEfectoAtaque("jugador");
            }),

            new KeyFrame(Duration.millis(900), e -> {
                if (jugador.getVida() <= 0) {
                    vista.mostrarDerrota();
                } else {
                    vista.deshabilitarBotones(false); // devolver control al jugador
                }
                enAnimacion = false;
            })
        );
        timeline.play();
    }

    public void defender() {
        if (enAnimacion) return;
        // TU TURNO: implementa defensa — reduce el daño del siguiente ataque enemigo
        // Pista: guarda un boolean 'defendiendo' y aplica reducción en turnoEnemigo()
    }

    public void huir() {
        if (enAnimacion) return;
        vista.log("Has huido del combate.");
        vista.cerrar();
    }
}
```

### `views/CombateView.java` — actualizado con efectos visuales

```java
package k82studio.views;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import k82studio.controllers.CombateController;
import k82studio.models.Enemigo;
import k82studio.models.Personaje;

public class CombateView extends VBox {

    private Label infoJugador, infoEnemigo, logLabel;
    private ProgressBar vidaJugador, vidaEnemigo;
    private Button btnAtacar, btnDefender, btnHuir;
    private StackPane zonaJugador, zonaEnemigo;
    private CombateController controller;
    private Runnable onCombateTerminado;

    public CombateView(Personaje jugador, Enemigo enemigo) {
        setSpacing(20);
        setPadding(new Insets(30));
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #0d0d1a;");

        this.controller = new CombateController(jugador, enemigo, this);

        // ── ZONA ENEMIGO ──────────────────────────────────────
        zonaEnemigo = crearZonaPersonaje("#e74c3c", "ENEMIGO");
        infoEnemigo = new Label(enemigo.getNombre() + " | Vida: " + enemigo.getVida());
        infoEnemigo.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 16px;");
        vidaEnemigo = crearBarraVida();

        // ── ZONA JUGADOR ──────────────────────────────────────
        zonaJugador = crearZonaPersonaje("#3498db", "JUGADOR");
        infoJugador = new Label(jugador.getResumen());
        infoJugador.setStyle("-fx-text-fill: #3498db; -fx-font-size: 16px;");
        vidaJugador = crearBarraVida();
        vidaJugador.setStyle("-fx-accent: #3498db;");

        // ── LOG ────────────────────────────────────────────────
        logLabel = new Label("¡Combate iniciado!");
        logLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 14px; -fx-font-style: italic;");

        // ── BOTONES ────────────────────────────────────────────
        btnAtacar   = new Button("⚔  Atacar");
        btnDefender = new Button("🛡  Defender");
        btnHuir     = new Button("🏃  Huir");
        HBox botones = new HBox(15, btnAtacar, btnDefender, btnHuir);
        botones.setAlignment(Pos.CENTER);

        btnAtacar.setOnAction(e   -> controller.atacar());
        btnDefender.setOnAction(e -> controller.defender());
        btnHuir.setOnAction(e     -> controller.huir());

        getChildren().addAll(
            zonaEnemigo, infoEnemigo, vidaEnemigo,
            logLabel,
            vidaJugador, infoJugador, zonaJugador,
            botones
        );
    }

    // ── MÉTODOS QUE LLAMA EL CONTROLLER ───────────────────────

    public void log(String mensaje) {
        logLabel.setText(mensaje);
    }

    public void actualizar(Personaje j, Enemigo e) {
        infoJugador.setText(j.getResumen());
        infoEnemigo.setText(e.getNombre() + " | Vida: " + e.getVida());
        vidaJugador.setProgress((double) j.getVida() / j.getVida()); // TU TURNO: ajusta con vidaMax
        vidaEnemigo.setProgress((double) e.getVida() / e.getVidaMax());
    }

    // Efecto flash cuando recibe daño
    public void mostrarEfectoAtaque(String quien) {
        StackPane zona = quien.equals("jugador") ? zonaJugador : zonaEnemigo;

        FadeTransition flash = new FadeTransition(Duration.millis(100), zona);
        flash.setFromValue(1.0);
        flash.setToValue(0.2);
        flash.setCycleCount(4);      // parpadea 4 veces
        flash.setAutoReverse(true);
        flash.play();
    }

    public void deshabilitarBotones(boolean deshabilitar) {
        btnAtacar.setDisable(deshabilitar);
        btnDefender.setDisable(deshabilitar);
        btnHuir.setDisable(deshabilitar);
    }

    public void mostrarVictoria() {
        log("¡¡VICTORIA!! Has derrotado a " + "¡el enemigo!");
        deshabilitarBotones(true);
        if (onCombateTerminado != null) {
            // espera 2 segundos y vuelve al mapa
            new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.seconds(2),
                    e -> onCombateTerminado.run())
            ).play();
        }
    }

    public void mostrarDerrota() {
        log("Has sido derrotado... Game Over");
        deshabilitarBotones(true);
    }

    public void cerrar() {
        if (onCombateTerminado != null) onCombateTerminado.run();
    }

    public void setOnCombateTerminado(Runnable callback) {
        this.onCombateTerminado = callback;
    }

    // ── HELPERS ────────────────────────────────────────────────
    private StackPane crearZonaPersonaje(String color, String etiqueta) {
        StackPane zona = new StackPane();
        zona.setPrefSize(120, 120);

        Rectangle fondo = new Rectangle(120, 120);
        fondo.setFill(Color.web(color + "33")); // color con 20% opacidad
        fondo.setArcWidth(10);
        fondo.setArcHeight(10);

        Label label = new Label(etiqueta);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px; -fx-font-weight: bold;");

        zona.getChildren().addAll(fondo, label);
        return zona;
    }

    private ProgressBar crearBarraVida() {
        ProgressBar barra = new ProgressBar(1.0);
        barra.setPrefWidth(300);
        barra.setStyle("-fx-accent: #2ecc71;");
        return barra;
    }
}
```

---

## 23. Media

### Concepto: recursos en JavaFX

Los archivos de imagen, audio y vídeo se guardan en `src/main/resources/`.  
Para acceder a ellos en código:

```java
// getClass().getResource() busca el archivo en el classpath (resources/)
URL url = getClass().getResource("/images/guerrero.png");
String path = url.toExternalForm(); // convierte a String que acepta JavaFX
```

**Nunca uses rutas absolutas** como `C:\proyecto\imagen.png` — no funcionará en otro ordenador.

### Imágenes

```java
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

// cargar imagen desde resources/images/
Image imagen = new Image(getClass().getResourceAsStream("/images/guerrero.png"));
ImageView vista = new ImageView(imagen);

// ajustar tamaño manteniendo proporción
vista.setFitWidth(200);
vista.setPreserveRatio(true);

// añadir a cualquier layout
vbox.getChildren().add(vista);
```

### Audio — efectos de sonido

```java
import javafx.scene.media.AudioClip;

// AudioClip: para sonidos cortos (ataques, explosiones, pasos)
// Se carga en memoria completo → reproducción instantánea sin lag
AudioClip sonidoAtaque = new AudioClip(
    getClass().getResource("/audio/ataque.mp3").toExternalForm()
);

// reproducir cuando el jugador ataca
sonidoAtaque.play();

// ajustar volumen (0.0 a 1.0)
sonidoAtaque.setVolume(0.7);
```

### Audio — música de fondo

```java
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

// MediaPlayer: para música larga (no carga todo en memoria)
Media musica = new Media(
    getClass().getResource("/audio/musica_combate.mp3").toExternalForm()
);
MediaPlayer player = new MediaPlayer(musica);

player.setCycleCount(MediaPlayer.INDEFINITE); // loop infinito
player.setVolume(0.4);
player.play();

// pausar / reanudar
player.pause();
player.play();
```

### Vídeo — cinemáticas o intro

```java
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

// Media + MediaPlayer igual que el audio
Media video = new Media(
    getClass().getResource("/video/intro.mp4").toExternalForm()
);
MediaPlayer player = new MediaPlayer(video);

// MediaView es el nodo visual que muestra el vídeo
MediaView vistaVideo = new MediaView(player);
vistaVideo.setFitWidth(800);
vistaVideo.setPreserveRatio(true);

// cuando el vídeo termina, ir al menú principal
player.setOnEndOfMedia(() -> {
    player.stop();
    // aquí llamarías a mainView.mostrarMenu() o similar
});

player.play();
```

### `views/MediaView.java` — ejemplo completo integrado

```java
package k82studio.views;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class MediaView extends StackPane {

    private MediaPlayer musicaFondo;
    private AudioClip   sonidoAtaque;

    public MediaView() {
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #0d0d1a;");

        cargarImagen();
        cargarAudio();
    }

    private void cargarImagen() {
        try {
            Image img = new Image(
                getClass().getResourceAsStream("/images/guerrero.png")
            );
            ImageView vista = new ImageView(img);
            vista.setFitWidth(400);
            vista.setPreserveRatio(true);
            getChildren().add(vista);
        } catch (Exception e) {
            System.err.println("Imagen no encontrada: " + e.getMessage());
            // nunca dejes que un recurso que falta crashee la app
        }
    }

    private void cargarAudio() {
        try {
            sonidoAtaque = new AudioClip(
                getClass().getResource("/audio/ataque.mp3").toExternalForm()
            );
        } catch (Exception e) {
            System.err.println("Audio no encontrado: " + e.getMessage());
        }
    }

    public void reproducirAtaque() {
        if (sonidoAtaque != null) sonidoAtaque.play();
    }

    public void iniciarMusica(String ruta) {
        try {
            Media media  = new Media(getClass().getResource(ruta).toExternalForm());
            musicaFondo  = new MediaPlayer(media);
            musicaFondo.setCycleCount(MediaPlayer.INDEFINITE);
            musicaFondo.setVolume(0.3);
            musicaFondo.play();
        } catch (Exception e) {
            System.err.println("Música no encontrada: " + e.getMessage());
        }
    }

    public void detenerMusica() {
        if (musicaFondo != null) musicaFondo.stop();
    }
}
```

---

## 24. pom.xml Fase 2

Añade estas dependencias a las que ya tenías:

```xml
<dependencies>
    <!-- JavaFX — ya las tenías -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21.0.2</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>21.0.2</version>
    </dependency>

    <!-- JavaFX Media — NUEVO: para audio y vídeo -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-media</artifactId>
        <version>21.0.2</version>
    </dependency>

    <!-- SQLite JDBC — NUEVO: driver de base de datos -->
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.45.1.0</version>
    </dependency>
</dependencies>
```

---

## 25. App.java Fase 2

```java
package k82studio;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import k82studio.db.DatabaseManager;
import k82studio.db.PersonajeDAO;
import k82studio.models.*;
import k82studio.views.MainView;

import java.util.List;

public class App extends Application {

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {

        // 1. Inicializar BBDD (crea tablas si no existen)
        DatabaseManager.getInstance();

        // 2. Cargar o crear personajes
        PersonajeDAO dao = new PersonajeDAO();
        List<Personaje> cargados = dao.obtenerTodos();

        Inventario inventario = new Inventario();

        if (cargados.isEmpty()) {
            // primera vez: crear y guardar en BBDD
            Guerrero aragorn = new Guerrero("Aragorn", 10, 45);
            Mago     gandalf = new Mago("Gandalf", 15, 80);
            dao.guardar(aragorn);
            dao.guardar(gandalf);
            inventario.agregar(aragorn);
            inventario.agregar(gandalf);
        } else {
            // siguientes veces: cargar desde BBDD
            cargados.forEach(inventario::agregar);
        }

        // 3. Mostrar la app
        MainView vista = new MainView(inventario);
        Scene scene = new Scene(vista, 1280, 720);

        stage.setTitle("RPG Manager");
        stage.setScene(scene);
        stage.show();
    }
}
```

---

## 26. Errores comunes Fase 2

| Error | Causa | Solución |
|---|---|---|
| `No suitable driver found for jdbc:sqlite` | Falta la dependencia sqlite-jdbc en pom.xml | Añade la dependencia y ejecuta `mvn clean install` |
| `NullPointerException` al cargar imagen | El archivo no está en `resources/images/` | Verifica la ruta y que el archivo exista en el classpath |
| `MediaException: Could not create player` | Falta `javafx-media` en pom.xml | Añade la dependencia de media |
| El enemigo sale del canvas | `Enemigo.actualizar()` no tiene límites | Añade clamp en x e y dentro de `actualizar()` |
| El combate se inicia en bucle | `comprobarColisiones()` dispara múltiples veces | Añade un flag `combateActivo = true` para bloquearlo |
| Los botones siguen activos durante animación | Falta llamar `deshabilitarBotones(true)` | El controller debe deshabilitar antes de la Timeline |
| La música no para al cambiar de pantalla | `MediaPlayer` no se detiene | Guarda referencia y llama `detenerMusica()` al cambiar |
| `SQLException: table already exists` | La tabla se crea sin `IF NOT EXISTS` | Siempre usa `CREATE TABLE IF NOT EXISTS` |

---

## Lo que implementas tú (Fase 2)

Aquí tienes los ejercicios de la fase:

| # | Qué implementar | Pista |
|---|---|---|
| 1 | `actualizarVida()` en `PersonajeDAO` | `UPDATE personajes SET vida = ? WHERE nombre = ?` |
| 2 | `eliminar()` en `PersonajeDAO` | `DELETE FROM personajes WHERE nombre = ?` |
| 3 | `EnemigosDAO` para guardar/cargar enemigos | Mismo patrón que `PersonajeDAO` |
| 4 | Defensa en `CombateController` | Flag `defendiendo`, reduce daño en `turnoEnemigo()` |
| 5 | Límites del canvas para `Enemigo` | Clamp en `actualizar()` igual que el jugador |
| 6 | Sonido al atacar en `CombateView` | `mediaView.reproducirAtaque()` desde `mostrarEfectoAtaque()` |
| 7 | Vídeo de intro en `App.java` | `MediaView` antes de cargar `MainView` |

---

*k82studio · RPG Manager Guide — Fase 2*