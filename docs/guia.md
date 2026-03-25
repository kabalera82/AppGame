# RPG Manager en JavaFX
### Java 21 · JavaFX 21 · Maven · MVC · Herencia · Polimorfismo · Interfaces · Colecciones · Animaciones

---

## Índice

1. [Conceptos clave](#1-conceptos-clave)
2. [Estructura del proyecto](#2-estructura-del-proyecto)
3. [Interfaces — los contratos](#3-interfaces)
4. [Modelo base — Personaje](#4-modelo-base)
5. [Subclases — Guerrero y Mago](#5-subclases)
6. [Colecciones — Inventario](#6-colecciones)
7. [Controlador — MVC](#7-controlador)
8. [Vistas — MainView y PersonajeView](#8-vistas)
9. [Movimiento y animación — GameView](#9-movimiento)
10. [Sistema de combate por turnos](#10-combate)
11. [App.java — punto de entrada](#11-appjava)
12. [pom.xml](#12-pomxml)
13. [Cómo conectar piezas nuevas](#13-conectar-piezas)
14. [Errores comunes](#14-errores-comunes)

---

## 1. Conceptos clave

### MVC — Model View Controller

Divide el código en tres capas que **nunca se mezclan**:

| Capa | Qué hace | Ejemplo |
|---|---|---|
| **Modelo (M)** | Guarda y gestiona los datos | `Personaje.java`, `Inventario.java` |
| **Vista (V)** | Muestra los datos en pantalla | `MainView.java`, `PersonajeView.java` |
| **Controlador (C)** | Conecta modelo y vista, contiene la lógica | `PersonajeController.java` |

> **Regla de oro:** la Vista nunca toca el Modelo directamente. Siempre pasa por el Controlador.

### Herencia

Una clase hija hereda los atributos y métodos del padre.  
`Guerrero` y `Mago` heredan de `Personaje` → tienen `nombre`, `vida` y `nivel` sin repetir código.

### Polimorfismo

Un `Guerrero` y un `Mago` son ambos `Personaje`, pero cuando llamas a `atacar()` cada uno responde diferente. **Mismo método, comportamientos distintos.**

### Interfaces

Un contrato. Si una clase implementa `Atacable`, **DEBE** tener el método `atacar()`. Garantiza que puedes tratar objetos distintos de forma uniforme.

### Colecciones

`List<Personaje>` guarda todos los personajes en orden. Acepta `Guerrero`, `Mago` o cualquier subclase. Puedes recorrerlos, añadir, eliminar y buscar.

---

## 2. Estructura del proyecto

```
src/main/java/k82studio/
├── App.java                        ← lanza la app
├── interfaces/
│   ├── Atacable.java               ← contrato de ataque
│   └── Mostrable.java              ← contrato de visualización
├── models/
│   ├── Personaje.java              ← clase abstracta base
│   ├── Guerrero.java               ← extiende Personaje
│   ├── Mago.java                   ← extiende Personaje
│   └── Inventario.java             ← List<Personaje>
├── controllers/
│   ├── PersonajeController.java    ← conecta modelo y vista
│   └── CombateController.java      ← lógica de combate
└── views/
    ├── MainView.java               ← contenedor + menú
    ├── PersonajeView.java          ← ficha de personaje
    ├── GameView.java               ← canvas + movimiento
    └── CombateView.java            ← pantalla de combate
```

> Cada carpeta tiene una responsabilidad única. Nunca metas lógica en las vistas ni UI en los modelos.

---

## 3. Interfaces

Las interfaces definen **QUÉ** debe hacer una clase sin decir **CÓMO**.

### `interfaces/Atacable.java`

```java
package k82studio.interfaces;

public interface Atacable {
    int atacar();            // devuelve el daño
    String getTipoAtaque();  // "espada", "magia"...
}
```

### `interfaces/Mostrable.java`

```java
package k82studio.interfaces;

public interface Mostrable {
    String getResumen(); // cada clase decide cómo mostrarse
}
```

---

## 4. Modelo base

Clase `abstract` = no se puede instanciar con `new Personaje()`. Obliga a usar `Guerrero`, `Mago`, etc.

### `models/Personaje.java`

```java
package k82studio.models;

import k82studio.interfaces.Atacable;
import k82studio.interfaces.Mostrable;

public abstract class Personaje implements Atacable, Mostrable {

    private String nombre;
    private int vida;
    private int nivel;

    public Personaje(String nombre, int vida, int nivel) {
        this.nombre = nombre;
        this.vida   = vida;
        this.nivel  = nivel;
    }

    public String getNombre() { return nombre; }
    public int getVida()      { return vida;   }
    public int getNivel()     { return nivel;  }
    public void setVida(int v){ this.vida = v; }

    // abstract = cada subclase DEBE implementarlo
    @Override public abstract int atacar();
    @Override public abstract String getTipoAtaque();
    @Override public abstract String getResumen();
}
```

---

## 5. Subclases

### `models/Guerrero.java`

```java
package k82studio.models;

public class Guerrero extends Personaje {

    private int fuerza;

    public Guerrero(String nombre, int nivel, int fuerza) {
        super(nombre, 120, nivel); // más vida que el mago
        this.fuerza = fuerza;
    }

    @Override public int atacar()           { return fuerza * 2; }
    @Override public String getTipoAtaque() { return "Espada";   }

    @Override
    public String getResumen() {
        return "[GUERRERO] " + getNombre()
             + " | Vida: "   + getVida()
             + " | Fuerza: " + fuerza
             + " | Nivel: "  + getNivel();
    }
}
```

### `models/Mago.java`

```java
package k82studio.models;

public class Mago extends Personaje {

    private int magia;

    public Mago(String nombre, int nivel, int magia) {
        super(nombre, 70, nivel); // menos vida que el guerrero
        this.magia = magia;
    }

    @Override public int atacar()           { return magia * 3; }
    @Override public String getTipoAtaque() { return "Magia";   }

    @Override
    public String getResumen() {
        return "[MAGO] "  + getNombre()
             + " | Vida: " + getVida()
             + " | Magia: " + magia
             + " | Nivel: " + getNivel();
    }
}
```

> Para añadir un `Arquero` solo creas `Arquero.java` extendiendo `Personaje`. El resto del sistema lo reconoce automáticamente.

---

## 6. Colecciones

`List<Personaje>` acepta `Guerrero`, `Mago` o cualquier subclase — eso es **polimorfismo en colecciones**.

### `models/Inventario.java`

```java
package k82studio.models;

import java.util.ArrayList;
import java.util.List;

public class Inventario {

    private List<Personaje> personajes = new ArrayList<>();

    public void agregar(Personaje p)    { personajes.add(p);       }
    public void eliminar(Personaje p)   { personajes.remove(p);    }
    public List<Personaje> getTodos()   { return personajes;        }
    public Personaje getPorIndice(int i){ return personajes.get(i); }
    public int getTamanio()             { return personajes.size(); }
}
```

---

## 7. Controlador

El controlador es el intermediario. La vista le pide cosas, él consulta el modelo y le dice a la vista qué mostrar.

### `controllers/PersonajeController.java`

```java
package k82studio.controllers;

import k82studio.models.Inventario;
import k82studio.models.Personaje;
import k82studio.views.MainView;

public class PersonajeController {

    private Inventario inventario;
    private MainView vista;

    public PersonajeController(Inventario inventario, MainView vista) {
        this.inventario = inventario;
        this.vista      = vista;
    }

    public void seleccionarPersonaje(int indice) {
        Personaje p = inventario.getPorIndice(indice);
        vista.mostrarPersonaje(p);
    }

    public void mostrarTodos() {
        vista.mostrarLista(inventario.getTodos());
    }
}
```

> El flujo siempre es: **Vista → Controller → Modelo → Controller → Vista**

---

## 8. Vistas

### `views/PersonajeView.java`

```java
package k82studio.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import k82studio.models.Personaje;

public class PersonajeView extends VBox {

    public PersonajeView(Personaje personaje) {
        setSpacing(12);
        setPadding(new Insets(40));
        setAlignment(Pos.CENTER);

        Label nombre = new Label(personaje.getNombre());
        Label tipo   = new Label("Ataque: " + personaje.getTipoAtaque());
        Label vida   = new Label("Vida: "   + personaje.getVida());
        Label nivel  = new Label("Nivel: "  + personaje.getNivel());
        Label danio  = new Label("Daño: "   + personaje.atacar());

        nombre.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        tipo.setStyle  ("-fx-font-size: 16px;");
        vida.setStyle  ("-fx-font-size: 16px;");
        nivel.setStyle ("-fx-font-size: 16px;");
        danio.setStyle ("-fx-font-size: 16px; -fx-text-fill: #e74c3c;");

        getChildren().addAll(nombre, tipo, vida, nivel, danio);
    }
}
```

### `views/MainView.java`

```java
package k82studio.views;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import k82studio.controllers.PersonajeController;
import k82studio.models.Inventario;
import k82studio.models.Personaje;

import java.util.List;

public class MainView extends BorderPane {

    private StackPane centro;
    private PersonajeController controller;

    public MainView(Inventario inventario) {
        this.controller = new PersonajeController(inventario, this);
        setLeft(crearMenu(inventario));
        setCenter(crearCentro());
    }

    private VBox crearMenu(Inventario inventario) {
        VBox menu = new VBox(8);
        menu.setPadding(new Insets(15));
        menu.setPrefWidth(220);
        menu.setStyle("-fx-background-color: #2c3e50;");

        Label titulo = new Label("PERSONAJES");
        titulo.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        menu.getChildren().add(titulo);

        // un botón por cada personaje — automático
        for (int i = 0; i < inventario.getTamanio(); i++) {
            final int indice = i;
            Button btn = new Button(inventario.getPorIndice(i).getNombre());
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white;");
            btn.setOnAction(e -> controller.seleccionarPersonaje(indice));
            menu.getChildren().add(btn);
        }

        Button btnJugar = new Button("▶ JUGAR");
        btnJugar.setMaxWidth(Double.MAX_VALUE);
        btnJugar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnJugar.setOnAction(e -> abrirJuego(inventario));
        menu.getChildren().add(btnJugar);

        return menu;
    }

    private StackPane crearCentro() {
        centro = new StackPane();
        centro.setStyle("-fx-background-color: #ecf0f1;");
        Label placeholder = new Label("Selecciona un personaje");
        placeholder.setStyle("-fx-font-size: 18px; -fx-text-fill: #95a5a6;");
        centro.getChildren().add(placeholder);
        return centro;
    }

    // métodos públicos que llama el Controller
    public void mostrarPersonaje(Personaje p) {
        centro.getChildren().clear();
        centro.getChildren().add(new PersonajeView(p));
    }

    public void mostrarLista(List<Personaje> lista) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        for (Personaje p : lista) {
            box.getChildren().add(new Label(p.getResumen()));
        }
        centro.getChildren().clear();
        centro.getChildren().add(box);
    }

    private void abrirJuego(Inventario inventario) {
        GameView gameView = new GameView(inventario.getPorIndice(0));
        centro.getChildren().clear();
        centro.getChildren().add(gameView);
        gameView.requestFocus(); // importante para capturar teclas
    }
}
```

---

## 9. Movimiento

### Herramientas de animación en JavaFX

| Herramienta | Cuándo usarla |
|---|---|
| `AnimationTimer` | Bucle continuo a ~60fps. Para movimiento, juegos, física |
| `Timeline` | Animaciones puntuales con keyframes. Para transiciones, efectos |

### `views/GameView.java`

```java
package k82studio.views;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import k82studio.models.Personaje;

import java.util.HashSet;
import java.util.Set;

public class GameView extends VBox {

    private double x = 400, y = 300;
    private final double VELOCIDAD = 3;
    private int vidaActual;
    private final int vidaMax;

    // guarda TODAS las teclas pulsadas a la vez → permite diagonal
    private final Set<KeyCode> teclasActivas = new HashSet<>();

    private final Canvas canvas;
    private final ProgressBar barraVida;
    private final Label labelVida;
    private final Personaje personaje;

    public GameView(Personaje personaje) {
        this.personaje  = personaje;
        this.vidaMax    = personaje.getVida();
        this.vidaActual = vidaMax;

        setPadding(new Insets(10));
        setSpacing(8);
        setStyle("-fx-background-color: #1a1a2e;");

        labelVida = new Label("Vida: " + vidaActual + " / " + vidaMax);
        labelVida.setStyle("-fx-text-fill: white;");

        barraVida = new ProgressBar(1.0);
        barraVida.setPrefWidth(400);
        barraVida.setStyle("-fx-accent: #2ecc71;");

        canvas = new Canvas(900, 550);

        getChildren().addAll(labelVida, barraVida, canvas);

        setFocusTraversable(true);
        setOnKeyPressed(e  -> teclasActivas.add(e.getCode()));
        setOnKeyReleased(e -> teclasActivas.remove(e.getCode()));

        iniciarBucle();
    }

    private void iniciarBucle() {
        new AnimationTimer() {
            @Override public void handle(long now) {
                mover();
                dibujar();
            }
        }.start();
    }

    private void mover() {
        double w = canvas.getWidth(), h = canvas.getHeight();
        if (teclasActivas.contains(KeyCode.LEFT)  && x > 20)    x -= VELOCIDAD;
        if (teclasActivas.contains(KeyCode.RIGHT) && x < w - 20) x += VELOCIDAD;
        if (teclasActivas.contains(KeyCode.UP)    && y > 20)    y -= VELOCIDAD;
        if (teclasActivas.contains(KeyCode.DOWN)  && y < h - 20) y += VELOCIDAD;

        if (teclasActivas.contains(KeyCode.Q)) cambiarVida(-1);
        if (teclasActivas.contains(KeyCode.E)) cambiarVida(+1);
    }

    private void dibujar() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#16213e"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        String color = personaje.getTipoAtaque().equals("Espada") ? "#3498db" : "#9b59b6";
        gc.setFill(Color.web(color));
        gc.fillOval(x - 20, y - 20, 40, 40);

        gc.setFill(Color.WHITE);
        gc.fillText(personaje.getNombre(), x - 25, y - 28);
    }

    private void cambiarVida(int cantidad) {
        vidaActual = Math.max(0, Math.min(vidaMax, vidaActual + cantidad));
        double pct = (double) vidaActual / vidaMax;
        barraVida.setProgress(pct);
        labelVida.setText("Vida: " + vidaActual + " / " + vidaMax);

        if (pct > 0.5)       barraVida.setStyle("-fx-accent: #2ecc71;");
        else if (pct > 0.25) barraVida.setStyle("-fx-accent: #f39c12;");
        else                 barraVida.setStyle("-fx-accent: #e74c3c;");
    }
}
```

### Controles

| Tecla | Acción |
|---|---|
| `← ↑ ↓ →` | Mover personaje |
| `Q` | Quitar vida |
| `E` | Recuperar vida |

---

## 10. Combate

### `controllers/CombateController.java`

```java
package k82studio.controllers;

import k82studio.models.Personaje;
import k82studio.views.CombateView;

public class CombateController {

    private Personaje jugador;
    private Personaje enemigo;
    private CombateView vista;
    private boolean turnoJugador = true;

    public CombateController(Personaje jugador, Personaje enemigo, CombateView vista) {
        this.jugador = jugador;
        this.enemigo = enemigo;
        this.vista   = vista;
    }

    public void atacar() {
        if (turnoJugador) {
            int danio = jugador.atacar();
            enemigo.setVida(enemigo.getVida() - danio);
            vista.log(jugador.getNombre() + " ataca por " + danio);
        } else {
            int danio = enemigo.atacar();
            jugador.setVida(jugador.getVida() - danio);
            vista.log(enemigo.getNombre() + " contraataca por " + danio);
        }
        turnoJugador = !turnoJugador;
        vista.actualizar(jugador, enemigo);
        comprobarFin();
    }

    public void defender() {
        vista.log(jugador.getNombre() + " se defiende — daño reducido");
        turnoJugador = !turnoJugador;
        vista.actualizar(jugador, enemigo);
    }

    public void huir() {
        vista.log("Has huido del combate");
        vista.cerrar();
    }

    private void comprobarFin() {
        if (enemigo.getVida() <= 0) vista.mostrarVictoria();
        if (jugador.getVida() <= 0) vista.mostrarDerrota();
    }
}
```

### `views/CombateView.java` — estructura mínima

```java
package k82studio.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import k82studio.controllers.CombateController;
import k82studio.models.Personaje;

public class CombateView extends VBox {

    private Label infoJugador, infoEnemigo, logLabel;
    private CombateController controller;

    public CombateView(Personaje jugador, Personaje enemigo) {
        setSpacing(15);
        setPadding(new Insets(30));
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #1a1a2e;");

        this.controller = new CombateController(jugador, enemigo, this);

        infoJugador = new Label(jugador.getResumen());
        infoEnemigo = new Label(enemigo.getResumen());
        logLabel    = new Label("¡Combate iniciado!");

        infoJugador.setStyle("-fx-text-fill: #3498db; -fx-font-size: 16px;");
        infoEnemigo.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 16px;");
        logLabel.setStyle   ("-fx-text-fill: white;   -fx-font-size: 14px;");

        Button btnAtacar  = new Button("⚔ Atacar");
        Button btnDefender = new Button("🛡 Defender");
        Button btnHuir    = new Button("🏃 Huir");

        btnAtacar.setOnAction(e  -> controller.atacar());
        btnDefender.setOnAction(e -> controller.defender());
        btnHuir.setOnAction(e    -> controller.huir());

        HBox botones = new HBox(10, btnAtacar, btnDefender, btnHuir);
        botones.setAlignment(Pos.CENTER);

        getChildren().addAll(infoEnemigo, infoJugador, logLabel, botones);
    }

    // métodos que llama el Controller
    public void log(String mensaje)                        { logLabel.setText(mensaje);          }
    public void actualizar(Personaje j, Personaje e)       { infoJugador.setText(j.getResumen());
                                                             infoEnemigo.setText(e.getResumen()); }
    public void mostrarVictoria()                          { log("¡Victoria! Has ganado.");       }
    public void mostrarDerrota()                           { log("Has sido derrotado...");        }
    public void cerrar()                                   { log("Huiste del combate.");          }
}
```

---

## 11. App.java

Solo tres responsabilidades: crear los datos, crear la vista, mostrar la ventana.

```java
package k82studio;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import k82studio.models.*;
import k82studio.views.MainView;

public class App extends Application {

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        // 1. datos
        Inventario inventario = new Inventario();
        inventario.agregar(new Guerrero("Aragorn", 10, 45));
        inventario.agregar(new Mago("Gandalf", 15, 80));
        inventario.agregar(new Guerrero("Legolas", 8, 38));

        // 2. vista
        MainView vista = new MainView(inventario);
        Scene scene = new Scene(vista, 1026, 720);

        // 3. ventana
        stage.setTitle("RPG Manager");
        stage.setScene(scene);
        stage.show();
    }
}
```

---

## 12. pom.xml

```xml
<dependencies>
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
</dependencies>

<build><plugins>
    <plugin>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-maven-plugin</artifactId>
        <version>0.0.8</version>
        <configuration>
            <mainClass>k82studio.App</mainClass>
        </configuration>
    </plugin>
</plugins></build>
```

> **Para ejecutar:** `mvn javafx:run` en el terminal. No uses el botón verde de IntelliJ.

---

## 13. Conectar piezas nuevas

### Añadir un nuevo tipo de personaje (Arquero)

1. Crea `Arquero.java` extendiendo `Personaje`
2. Implementa `atacar()`, `getTipoAtaque()` y `getResumen()`
3. En `App.java`: `inventario.agregar(new Arquero(...))`
4. El menú lo detecta automáticamente — no tocas `MainView`

### Añadir una nueva pantalla

1. Crea la vista en `views/` extendiendo un layout (`VBox`, `BorderPane`...)
2. Añade un botón en `MainView` que limpie el centro y añada la nueva vista
3. Si necesita lógica, crea su propio Controller en `controllers/`

### Inyección de dependencias

El Controller recibe el `Inventario` por parámetro, no lo crea él mismo.  
Eso significa que puedes pasarle cualquier `Inventario` diferente sin tocar el Controller.  
**Eso es lo que hace el sistema intercambiable.**

---

## 14. Errores comunes

| Error | Causa | Solución |
|---|---|---|
| `ClassNotFoundException: k82studio.App` | `mainClass` en pom.xml incorrecto | Comprueba que el nombre coincide exactamente |
| `Error: faltan componentes JavaFX runtime` | Lanzando con el botón verde | Usa `mvn javafx:run` en el terminal |
| `NullPointerException` en `getResumen()` | Personaje sin inicializar | Asegúrate de llamar a `super()` en el constructor |
| `GameView` no detecta teclas | Falta `requestFocus()` | Llama a `gameView.requestFocus()` después de añadirlo al centro |
| `ProgressBar` no cambia de color | `setStyle()` no aplica en algunos temas | Añade `.bar { -fx-background-color: ... }` en CSS externo |

---

*k82studio · RPG Manager Guide*