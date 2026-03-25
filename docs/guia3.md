# RPG Manager en JavaFX — Fase 3
### Mapa · Tiles · Terrenos · Objetos · Colisiones · Generación Aleatoria · Interacciones

> Continuación directa de la Fase 2.  
> Asume que tienes `GameView` con delta time corregido, `Enemigo` con IA, `DatabaseManager` y el sistema de combate funcionando.

---

## Índice Fase 3

27. [Conceptos nuevos — qué aprenderás](#27-conceptos-nuevos)
28. [Nueva estructura del proyecto](#28-nueva-estructura)
29. [El sistema de Tiles — cómo funciona un mapa en cuadrícula](#29-tiles)
30. [TipoTile — enum de terrenos y objetos](#30-tipotile)
31. [Tile.java — la celda del mapa](#31-tilejava)
32. [Mapa.java — la cuadrícula completa](#32-mapajava)
33. [Efectos de terreno — barro, arena, agua](#33-efectos-de-terreno)
34. [Objetos sólidos — árboles, rocas y colisión](#34-objetos-sólidos)
35. [Generación aleatoria de mapas](#35-generación-aleatoria)
36. [Renderizado del mapa en el Canvas](#36-renderizado)
37. [Cámara — seguir al jugador en mapas grandes](#37-cámara)
38. [El jugador modifica el terreno](#38-el-jugador-modifica-el-terreno)
39. [El jugador crea objetos](#39-el-jugador-crea-objetos)
40. [MapController — lógica de interacciones](#40-mapcontroller)
41. [GameView actualizado — todo conectado](#41-gameview-actualizado)
42. [Persistencia del mapa en BBDD](#42-persistencia-del-mapa)
43. [Errores comunes Fase 3](#43-errores-fase-3)
44. [Lo que implementas tú](#44-lo-que-implementas-tú)

---

## 27. Conceptos nuevos

### ¿Qué es un sistema de Tiles?

Un **tile** (baldosa) es la unidad mínima del mapa. En lugar de manejar posiciones pixel a pixel, el mundo se divide en una cuadrícula de celdas iguales. Cada celda tiene un tipo: hierba, agua, roca...

```
Mundo en píxeles (caótico):       Mundo en tiles (ordenado):
x=347, y=219 → ¿qué hay ahí?      fila=6, col=10 → HIERBA
x=351, y=220 → ¿qué hay ahí?      fila=6, col=11 → AGUA
```

La conversión es simple:
```
col  = (int)(pixelX / TILE_SIZE)
fila = (int)(pixelY / TILE_SIZE)

pixelX = col  * TILE_SIZE
pixelY = fila * TILE_SIZE
```

Ventajas de trabajar con tiles:
- La colisión se reduce a "¿el tile destino es sólido?"
- La generación aleatoria es llenar una matriz
- La persistencia es guardar una tabla en BBDD
- El renderizado es iterar la cuadrícula y dibujar rectángulos

### ¿Qué es un Enum?

Un `enum` es una lista fija de constantes con nombre. Perfecto para tipos de tile porque los tipos son conocidos y finitos.

```java
// Sin enum — frágil, el compilador no avisa si escribes mal
int tipo = 3; // ¿qué es 3? nadie lo sabe sin documentación

// Con enum — claro, seguro, autocompletado en el IDE
TipoTile tipo = TipoTile.AGUA; // inmediatamente legible
```

Además los enums pueden tener campos y métodos, lo que permite encapsular las propiedades de cada tile dentro del propio enum.

### ¿Qué es el patrón Strategy?

Los efectos de terreno son un caso clásico del patrón **Strategy**: defines una interfaz `EfectoTerreno` con un método `aplicar(jugador)`, y cada tipo de terreno tiene su propia implementación.

```
Interface EfectoTerreno
    → EfectoBarro.aplicar()   → reduce velocidad al 40%
    → EfectoArena.aplicar()   → reduce velocidad al 60%
    → EfectoAgua.aplicar()    → no puede pasar (o reduce al 20%)
    → EfectoNormal.aplicar()  → velocidad normal (100%)
```

El jugador no sabe qué terreno pisa. Solo llama a `efecto.aplicar(this)` y el terreno decide qué hacer. Mismo método, comportamiento diferente según el objeto → **polimorfismo en acción**.

### ¿Qué es la generación procedural?

Crear contenido mediante algoritmos en lugar de diseñarlo a mano. En juegos se usa para que cada partida sea diferente. Aquí usaremos dos técnicas:

- **Ruido aleatorio puro**: cada tile tiene % de probabilidad de ser X — rápido pero caótico
- **Noise de Perlin simplificado**: suaviza el ruido para que terrenos similares aparezcan agrupados — más natural

### ¿Qué es una Cámara en 2D?

Cuando el mapa es más grande que la pantalla, la cámara decide qué porción mostrar. La cámara tiene una posición `(camX, camY)` que representa el pixel del mundo en la esquina superior izquierda de la pantalla.

```
Mundo: 3200 x 3200 píxeles
Pantalla: 900 x 550 píxeles

Cámara en (500, 300):
    → muestra el mundo desde (500,300) hasta (1400,850)

Para dibujar un tile en mundo (800, 600):
    posicionEnPantalla = (800 - camX, 600 - camY)
                       = (300, 300) ← aquí lo pinto en el canvas
```

### ¿Qué es el Command Pattern?

Cuando el jugador crea o modifica el terreno, cada acción es un **Command**: un objeto que encapsula la operación y puede deshacerse. Útil para implementar `Ctrl+Z` en el futuro.

```
Command: PlantarArbol(fila=5, col=8)
    → execute(): mapa[5][8] = ARBOL
    → undo():    mapa[5][8] = HIERBA  ← lo que había antes
```

---

## 28. Nueva estructura del proyecto

```
src/main/java/k82studio/
├── App.java
│
├── interfaces/
│   ├── Atacable.java
│   ├── Mostrable.java
│   └── EfectoTerreno.java          ← NUEVO: Strategy de terreno
│
├── models/
│   ├── Personaje.java              ← + velocidadActual, posX, posY
│   ├── Guerrero.java
│   ├── Mago.java
│   ├── Inventario.java
│   ├── Enemigo.java
│   └── mapa/                       ← NUEVO: todo el sistema de mapa
│       ├── TipoTile.java           ← enum con propiedades de cada tipo
│       ├── Tile.java               ← una celda del mapa
│       └── Mapa.java               ← la cuadrícula completa
│
├── efectos/                        ← NUEVO: implementaciones de EfectoTerreno
│   ├── EfectoNormal.java
│   ├── EfectoBarro.java
│   ├── EfectoArena.java
│   └── EfectoAgua.java
│
├── generacion/                     ← NUEVO: algoritmos de generación
│   └── GeneradorMapa.java
│
├── db/
│   ├── DatabaseManager.java        ← + tabla mapa_tiles
│   ├── PersonajeDAO.java
│   └── MapaDAO.java                ← NUEVO: persistencia del mapa
│
├── controllers/
│   ├── PersonajeController.java
│   ├── CombateController.java
│   └── MapController.java          ← NUEVO: lógica de interacciones del mapa
│
└── views/
    ├── MainView.java
    ├── PersonajeView.java
    ├── GameView.java               ← actualizado: renderiza mapa + cámara
    ├── CombateView.java
    └── MediaView.java
```

---

## 29. Tiles

### Concepto: coordenadas mundo vs coordenadas pantalla

Este es el concepto más importante de la Fase 3. Hay **dos sistemas de coordenadas** que debes tener siempre claros:

```
MUNDO: coordenadas absolutas del mapa completo
    → tile [fila=10][col=15] siempre está en la misma posición del mundo
    → pixelMundoX = col  * TILE_SIZE = 15 * 32 = 480
    → pixelMundoY = fila * TILE_SIZE = 10 * 32 = 320

PANTALLA: coordenadas relativas a lo que ve la cámara
    → depende de dónde está la cámara
    → pixelPantallaX = pixelMundoX - camara.x
    → pixelPantallaY = pixelMundoY - camara.y
```

El jugador siempre se mueve en coordenadas MUNDO. La cámara sigue al jugador. El renderizado convierte coordenadas MUNDO a PANTALLA restando la posición de la cámara.

### Tamaño de tile

```java
// Constante global — define la resolución de tu cuadrícula
public static final int TILE_SIZE = 32; // 32x32 píxeles por tile

// Tamaño del mapa en tiles
public static final int MAPA_COLS  = 100; // 100 tiles de ancho
public static final int MAPA_FILAS = 100; // 100 tiles de alto

// Tamaño total del mundo en píxeles
// = 100 * 32 = 3200 x 3200 píxeles
```

---

## 30. TipoTile

El enum `TipoTile` encapsula **todo lo que define un tipo de tile**: color, si es sólido, si tiene efecto de velocidad, si el jugador puede modificarlo y su nombre legible.

Cada campo del enum es una propiedad del tile. Los métodos son comportamiento del tile.

```java
package k82studio.models.mapa;

import javafx.scene.paint.Color;
import k82studio.interfaces.EfectoTerreno;
import k82studio.efectos.*;

public enum TipoTile {

    //          nombre        color               sólido  velMod  modificable
    HIERBA     ("Hierba",     Color.web("#4CAF50"), false,  1.00,   true),
    BOSQUE     ("Bosque",     Color.web("#2E7D32"), false,  0.80,   true),
    BARRO      ("Barro",      Color.web("#795548"), false,  0.40,   true),
    ARENA      ("Arena",      Color.web("#FDD835"), false,  0.60,   true),
    AGUA       ("Agua",       Color.web("#1565C0"), true,   0.00,   false),
    ROCA_SUELO ("Roca suelo", Color.web("#90A4AE"), false,  0.90,   true),

    // objetos sólidos — ocupan un tile completo
    ARBOL      ("Árbol",      Color.web("#1B5E20"), true,   0.00,   false),
    ROCA       ("Roca",       Color.web("#546E7A"), true,   0.00,   false),
    MURO       ("Muro",       Color.web("#37474F"), true,   0.00,   false),

    // objetos que el jugador puede colocar
    HOGUERA    ("Hoguera",    Color.web("#FF6F00"), false,  0.70,   true),
    PUENTE     ("Puente",     Color.web("#A1887F"), false,  0.85,   true);

    // ── Propiedades de cada tipo ───────────────────────────────
    private final String  nombre;
    private final Color   color;
    private final boolean solido;       // true = no se puede atravesar
    private final double  modVelocidad; // 1.0 = normal, 0.5 = mitad
    private final boolean modificable;  // true = el jugador puede cambiarlo

    // Constructor del enum — se llama una vez por constante al arrancar
    TipoTile(String nombre, Color color, boolean solido,
             double modVelocidad, boolean modificable) {
        this.nombre       = nombre;
        this.color        = color;
        this.solido       = solido;
        this.modVelocidad = modVelocidad;
        this.modificable  = modificable;
    }

    // Getters
    public String  getNombre()       { return nombre;       }
    public Color   getColor()        { return color;        }
    public boolean isSolido()        { return solido;       }
    public double  getModVelocidad() { return modVelocidad; }
    public boolean isModificable()   { return modificable;  }

    // Devuelve el efecto de terreno correspondiente (patrón Strategy)
    // Cada tipo sabe qué efecto tiene — no hay switch externo
    public EfectoTerreno getEfecto() {
        return switch (this) {
            case BARRO  -> new EfectoBarro();
            case ARENA  -> new EfectoArena();
            case AGUA   -> new EfectoAgua();
            case BOSQUE -> new EfectoBosque();
            default     -> new EfectoNormal();
        };
    }

    // ¿Es un objeto que el jugador puede colocar en el mapa?
    public boolean esColocable() {
        return this == HOGUERA || this == PUENTE || this == MURO;
    }
}
```

---

## 31. Tile.java

Un `Tile` es una celda concreta del mapa. Tiene tipo, posición en la cuadrícula y estado (puede estar dañado, en llamas, etc.).

Separar `Tile` de `TipoTile` es importante: `TipoTile` es la plantilla (compartida por todos los tiles del mismo tipo), `Tile` es la instancia concreta con su propio estado.

```java
package k82studio.models.mapa;

public class Tile {

    private TipoTile tipo;
    private final int fila;
    private final int col;

    // estado mutable — puede cambiar durante el juego
    private boolean modificadoPorJugador = false;
    private TipoTile tipoOriginal;        // para poder restaurarlo

    public Tile(TipoTile tipo, int fila, int col) {
        this.tipo         = tipo;
        this.tipoOriginal = tipo;
        this.fila         = fila;
        this.col          = col;
    }

    // ── Cambio de tipo (el jugador modifica el terreno) ────────
    public boolean cambiarTipo(TipoTile nuevoTipo) {
        if (!tipo.isModificable()) {
            return false; // no se puede modificar
        }
        this.tipo                   = nuevoTipo;
        this.modificadoPorJugador   = true;
        return true;
    }

    public void restaurar() {
        this.tipo                 = tipoOriginal;
        this.modificadoPorJugador = false;
    }

    // Delega en TipoTile — el tile no necesita saber los detalles
    public boolean isSolido()          { return tipo.isSolido();        }
    public double  getModVelocidad()   { return tipo.getModVelocidad(); }

    // Getters
    public TipoTile getTipo()                 { return tipo;                   }
    public int      getFila()                 { return fila;                   }
    public int      getCol()                  { return col;                    }
    public boolean  isModificadoPorJugador()  { return modificadoPorJugador;   }
    public TipoTile getTipoOriginal()         { return tipoOriginal;           }
}
```

---

## 32. Mapa.java

El `Mapa` es la cuadrícula completa. Contiene la matriz de `Tile`s y expone métodos para consultar y modificar el mundo. **Es el modelo — no sabe nada de píxeles ni de JavaFX.**

```java
package k82studio.models.mapa;

public class Mapa {

    public static final int TILE_SIZE  = 32;
    public static final int COLS       = 100;
    public static final int FILAS      = 100;

    // la cuadrícula — acceso: tiles[fila][col]
    private final Tile[][] tiles;
    private final String nombre;

    public Mapa(String nombre) {
        this.nombre = nombre;
        this.tiles  = new Tile[FILAS][COLS];
        // inicializar con HIERBA por defecto
        for (int f = 0; f < FILAS; f++)
            for (int c = 0; c < COLS; c++)
                tiles[f][c] = new Tile(TipoTile.HIERBA, f, c);
    }

    // ── Acceso por tile ────────────────────────────────────────
    public Tile getTile(int fila, int col) {
        if (!dentroDelMapa(fila, col)) return null;
        return tiles[fila][col];
    }

    public void setTipo(int fila, int col, TipoTile tipo) {
        if (!dentroDelMapa(fila, col)) return;
        tiles[fila][col].cambiarTipo(tipo);
    }

    // ── Consultas por posición en píxeles (coordenadas mundo) ──
    // Convierte píxeles → tile. Útil para saber qué hay bajo el jugador
    public Tile getTileEnPixel(double pixelX, double pixelY) {
        int col  = (int)(pixelX / TILE_SIZE);
        int fila = (int)(pixelY / TILE_SIZE);
        return getTile(fila, col);
    }

    // ── ¿Se puede mover a esa posición? ───────────────────────
    // radio = radio del personaje en píxeles
    // Comprueba las 4 esquinas del bounding box circular
    public boolean puedeMoverA(double pixelX, double pixelY, double radio) {
        // comprueba los 4 puntos extremos del personaje
        return !esSolido(pixelX - radio, pixelY - radio) &&
               !esSolido(pixelX + radio, pixelY - radio) &&
               !esSolido(pixelX - radio, pixelY + radio) &&
               !esSolido(pixelX + radio, pixelY + radio);
    }

    private boolean esSolido(double px, double py) {
        Tile t = getTileEnPixel(px, py);
        return t == null || t.isSolido(); // null = fuera del mapa = sólido
    }

    // ── Bounds del mapa en píxeles ─────────────────────────────
    public double getAnchoPx()  { return COLS  * TILE_SIZE; }
    public double getAltoPx()   { return FILAS * TILE_SIZE; }

    // ── Helpers ────────────────────────────────────────────────
    public boolean dentroDelMapa(int fila, int col) {
        return fila >= 0 && fila < FILAS && col >= 0 && col < COLS;
    }

    public Tile[][] getTiles() { return tiles;  }
    public String   getNombre(){ return nombre; }
}
```

---

## 33. Efectos de terreno

### La interfaz `EfectoTerreno`

```java
package k82studio.interfaces;

import k82studio.models.Personaje;

// Patrón Strategy: define el contrato
// Cada terreno implementa este método a su manera
public interface EfectoTerreno {
    void aplicar(Personaje jugador);
    void quitar(Personaje jugador);  // se llama al salir del tile
    String getDescripcion();
}
```

### `efectos/EfectoNormal.java` — implementación base

```java
package k82studio.efectos;

import k82studio.interfaces.EfectoTerreno;
import k82studio.models.Personaje;

public class EfectoNormal implements EfectoTerreno {

    @Override
    public void aplicar(Personaje jugador) {
        jugador.setModificadorVelocidad(1.0); // velocidad completa
    }

    @Override
    public void quitar(Personaje jugador) {
        jugador.setModificadorVelocidad(1.0); // restaurar
    }

    @Override
    public String getDescripcion() { return "Terreno normal"; }
}
```

### `efectos/EfectoBarro.java`

```java
package k82studio.efectos;

import k82studio.interfaces.EfectoTerreno;
import k82studio.models.Personaje;

public class EfectoBarro implements EfectoTerreno {

    private static final double MOD_VELOCIDAD = 0.40;

    @Override
    public void aplicar(Personaje jugador) {
        jugador.setModificadorVelocidad(MOD_VELOCIDAD);
        System.out.println("¡Barro! Movimiento muy reducido.");
        // TU TURNO: añade un efecto visual — cambiar color del personaje
        // TU TURNO: reproduce sonido de barro
    }

    @Override
    public void quitar(Personaje jugador) {
        jugador.setModificadorVelocidad(1.0);
    }

    @Override
    public String getDescripcion() { return "Barro — velocidad 40%"; }
}
```

### `efectos/EfectoArena.java` y `efectos/EfectoAgua.java`

```java
// EfectoArena — TU TURNO: implementa como EfectoBarro pero con 0.60
// EfectoAgua  — TU TURNO: el agua bloquea el paso (el tile ya es sólido)
//               o implementa como penalización de vida si decides que
//               el jugador puede nadar pero pierde vida cada segundo
```

### Añadir `modificadorVelocidad` a `Personaje.java`

```java
// En Personaje.java — añade estos campos y métodos:

private double modificadorVelocidad = 1.0; // 1.0 = velocidad normal

public double getModificadorVelocidad()         { return modificadorVelocidad; }
public void   setModificadorVelocidad(double m) { this.modificadorVelocidad = m; }

// La velocidad efectiva = velocidad base × modificador del terreno
public double getVelocidadEfectiva(double velocidadBase) {
    return velocidadBase * modificadorVelocidad;
}
```

### Cómo aplicar efectos en `GameView`

```java
// En el bucle de movimiento, después de mover al jugador:
private void aplicarEfectoTerreno() {
    Tile tileActual = mapa.getTileEnPixel(x, y);
    if (tileActual == null) return;

    // solo aplica si cambió de tile
    if (tileActual != ultimoTile) {
        if (ultimoTile != null) {
            ultimoTile.getTipo().getEfecto().quitar(personaje); // quitar efecto anterior
        }
        tileActual.getTipo().getEfecto().aplicar(personaje);    // aplicar nuevo efecto
        ultimoTile = tileActual;

        // mostrar descripción del terreno en la HUD
        labelTerreno.setText(tileActual.getTipo().getNombre());
    }
}

private Tile ultimoTile = null;
```

---

## 34. Objetos sólidos

### Concepto: colisión con tiles

Con el sistema de tiles, la colisión contra árboles y rocas es gratis. `Mapa.puedeMoverA()` ya comprueba si el tile destino es sólido. Simplemente no muevas al jugador si la posición objetivo es sólida.

```java
// En GameView.mover() — versión actualizada con colisión contra mapa
private void mover(double delta) {
    double vel  = personaje.getVelocidadEfectiva(VELOCIDAD_BASE) * delta;
    double nuevoX = x;
    double nuevoY = y;

    if (teclasActivas.contains(KeyCode.LEFT))  nuevoX -= vel;
    if (teclasActivas.contains(KeyCode.RIGHT)) nuevoX += vel;
    if (teclasActivas.contains(KeyCode.UP))    nuevoY -= vel;
    if (teclasActivas.contains(KeyCode.DOWN))  nuevoY += vel;

    // ── Colisión con tiles del mapa ──────────────────────────
    // Separamos X e Y para poder "deslizarse" contra paredes
    if (mapa.puedeMoverA(nuevoX, y, RADIO_JUGADOR)) {
        x = nuevoX; // X libre → mover
    }
    if (mapa.puedeMoverA(x, nuevoY, RADIO_JUGADOR)) {
        y = nuevoY; // Y libre → mover
    }

    // ── Límites del mundo ─────────────────────────────────────
    x = Math.max(RADIO_JUGADOR, Math.min(mapa.getAnchoPx() - RADIO_JUGADOR, x));
    y = Math.max(RADIO_JUGADOR, Math.min(mapa.getAltoPx()  - RADIO_JUGADOR, y));
}
```

> **Por qué separamos X e Y:** Si hay una pared al noreste y vas hacia arriba-derecha, sin separación te bloqueas completamente. Separando, el eje libre (arriba) sigue funcionando y puedes "resbalar" a lo largo de la pared. Esto se llama **sliding collision**.

### Colisión con objetos en la misma posición que el jugador

Los tiles sólidos (ARBOL, ROCA) ya bloquean el movimiento automáticamente porque `puedeMoverA` los detecta. Pero si quieres efectos adicionales al acercarte (interactuar, recoger objetos), usa la detección por proximidad:

```java
// ¿Hay un tile interactuable al frente del jugador?
private Tile getTileAlFrente() {
    double dirX = 0, dirY = 0;
    if (teclasActivas.contains(KeyCode.LEFT))  dirX = -1;
    if (teclasActivas.contains(KeyCode.RIGHT)) dirX =  1;
    if (teclasActivas.contains(KeyCode.UP))    dirY = -1;
    if (teclasActivas.contains(KeyCode.DOWN))  dirY =  1;

    // tile a 1.5 radios de distancia en la dirección de movimiento
    double distInteraccion = RADIO_JUGADOR * 1.5;
    return mapa.getTileEnPixel(x + dirX * distInteraccion,
                               y + dirY * distInteraccion);
}
```

---

## 35. Generación aleatoria

### Concepto: dos técnicas

**Técnica 1 — Ruido aleatorio puro:** cada tile se decide con `Math.random()`. Rápido, pero el resultado es sal-y-pimienta: tiles individuales mezclados sin coherencia geográfica.

**Técnica 2 — Agrupación por semillas (Flood Fill aleatorio):** colocas semillas de cada tipo y las "expandes" hacia los vecinos con cierta probabilidad. Produce biomas coherentes con bordes naturales.

### `generacion/GeneradorMapa.java`

```java
package k82studio.generacion;

import k82studio.models.mapa.Mapa;
import k82studio.models.mapa.TipoTile;

import java.util.Random;

public class GeneradorMapa {

    private final Random random;

    public GeneradorMapa(long semilla) {
        this.random = new Random(semilla);
        // La semilla fija hace que el mismo número genere siempre el mismo mapa.
        // Útil para compartir mapas con otros jugadores: "juega el mapa 42"
    }

    public GeneradorMapa() {
        this.random = new Random(); // semilla aleatoria
    }

    // ── GENERACIÓN PRINCIPAL ──────────────────────────────────
    public Mapa generar(String nombre) {
        Mapa mapa = new Mapa(nombre);

        pasadaBase(mapa);         // 1. rellenar con hierba
        generarAgua(mapa);        // 2. ríos y lagos
        generarTerrenos(mapa);    // 3. barro, arena, bosque
        generarObjetos(mapa);     // 4. árboles y rocas
        generarBordes(mapa);      // 5. bordes del mapa = muros infranqueables

        return mapa;
    }

    // ── PASO 1: base de hierba ─────────────────────────────────
    private void pasadaBase(Mapa mapa) {
        for (int f = 0; f < Mapa.FILAS; f++)
            for (int c = 0; c < Mapa.COLS; c++)
                mapa.setTipo(f, c, TipoTile.HIERBA);
    }

    // ── PASO 2: agua — ríos mediante caminata aleatoria ───────
    // Una "caminata aleatoria" parte de un punto y da pasos en
    // dirección aleatoria. Produce formas orgánicas tipo río.
    private void generarAgua(Mapa mapa) {
        int numRios = 3;
        for (int r = 0; r < numRios; r++) {
            // punto de inicio aleatorio en el borde
            int f = random.nextInt(Mapa.FILAS);
            int c = 0; // empieza en el borde izquierdo

            int longitud  = 50 + random.nextInt(50);
            int anchura   = 2  + random.nextInt(3);

            for (int paso = 0; paso < longitud; paso++) {
                // pintar un tramo de anchura variable
                for (int w = -anchura; w <= anchura; w++) {
                    mapa.setTipo(f + w, c, TipoTile.AGUA);
                }
                // avanzar en dirección variable (tendencia hacia la derecha)
                c += 1;
                f += random.nextInt(3) - 1; // -1, 0 o +1
                f  = Math.max(0, Math.min(Mapa.FILAS - 1, f));
                if (c >= Mapa.COLS) break;
            }
        }
    }

    // ── PASO 3: terrenos por probabilidad zonal ────────────────
    // Divide el mapa en zonas y asigna tipos con distinta probabilidad
    // según la posición — norte más frío (barro), sur más cálido (arena)
    private void generarTerrenos(Mapa mapa) {
        for (int f = 0; f < Mapa.FILAS; f++) {
            for (int c = 0; c < Mapa.COLS; c++) {
                if (mapa.getTile(f, c).getTipo() == TipoTile.AGUA) continue;

                double pct = random.nextDouble(); // [0.0, 1.0)

                // zona norte (filas 0..39) → más barro
                if (f < Mapa.FILAS * 0.4) {
                    if (pct < 0.15) mapa.setTipo(f, c, TipoTile.BARRO);
                    if (pct < 0.08) mapa.setTipo(f, c, TipoTile.BOSQUE);
                }
                // zona sur (filas 60..99) → más arena
                else if (f > Mapa.FILAS * 0.6) {
                    if (pct < 0.20) mapa.setTipo(f, c, TipoTile.ARENA);
                    if (pct < 0.05) mapa.setTipo(f, c, TipoTile.ROCA_SUELO);
                }
                // zona central → bosque mixto
                else {
                    if (pct < 0.10) mapa.setTipo(f, c, TipoTile.BOSQUE);
                    if (pct < 0.03) mapa.setTipo(f, c, TipoTile.BARRO);
                }
            }
        }
    }

    // ── PASO 4: objetos sólidos ───────────────────────────────
    private void generarObjetos(Mapa mapa) {
        for (int f = 0; f < Mapa.FILAS; f++) {
            for (int c = 0; c < Mapa.COLS; c++) {
                TipoTile actual = mapa.getTile(f, c).getTipo();
                if (actual == TipoTile.AGUA) continue;

                double pct = random.nextDouble();

                // en zonas de bosque, más árboles
                if (actual == TipoTile.BOSQUE && pct < 0.30) {
                    mapa.setTipo(f, c, TipoTile.ARBOL);
                }
                // en roca suelo, más rocas
                else if (actual == TipoTile.ROCA_SUELO && pct < 0.20) {
                    mapa.setTipo(f, c, TipoTile.ROCA);
                }
                // en cualquier sitio, árbol o roca ocasional
                else if (pct < 0.04) {
                    mapa.setTipo(f, c, TipoTile.ARBOL);
                } else if (pct < 0.02) {
                    mapa.setTipo(f, c, TipoTile.ROCA);
                }
            }
        }
    }

    // ── PASO 5: muros en los bordes ───────────────────────────
    private void generarBordes(Mapa mapa) {
        for (int f = 0; f < Mapa.FILAS; f++) {
            mapa.setTipo(f, 0,            TipoTile.MURO);
            mapa.setTipo(f, Mapa.COLS -1, TipoTile.MURO);
        }
        for (int c = 0; c < Mapa.COLS; c++) {
            mapa.setTipo(0,             c, TipoTile.MURO);
            mapa.setTipo(Mapa.FILAS -1, c, TipoTile.MURO);
        }
    }

    // ── SPAWN DEL JUGADOR ─────────────────────────────────────
    // Busca una posición libre para el jugador (no sólida, no agua)
    public double[] encontrarSpawnJugador(Mapa mapa) {
        for (int intentos = 0; intentos < 1000; intentos++) {
            int f = 5 + random.nextInt(Mapa.FILAS - 10);
            int c = 5 + random.nextInt(Mapa.COLS  - 10);
            if (!mapa.getTile(f, c).isSolido()) {
                return new double[]{
                    c * Mapa.TILE_SIZE + Mapa.TILE_SIZE / 2.0,
                    f * Mapa.TILE_SIZE + Mapa.TILE_SIZE / 2.0
                };
            }
        }
        // fallback: centro del mapa
        return new double[]{
            Mapa.COLS  / 2.0 * Mapa.TILE_SIZE,
            Mapa.FILAS / 2.0 * Mapa.TILE_SIZE
        };
    }
}
```

---

## 36. Renderizado

### Concepto: solo dibujar lo que se ve

Un mapa de 100×100 tiles tiene 10.000 celdas. Dibujar todas cada frame a 60fps es innecesario y lento. Solo dibujamos los tiles **dentro del viewport** de la cámara.

```
Viewport = lo que cabe en la pantalla
    tilesVisiblesX = anchoPantalla / TILE_SIZE + 2  (+ 2 para el borde)
    tilesVisiblesY = altoPantalla  / TILE_SIZE + 2

Tile inicial = cámara / TILE_SIZE
Tile final   = tile inicial + tilesVisibles
```

### Renderizador en `GameView`

```java
// Dibuja solo los tiles visibles en el viewport de la cámara
private void dibujarMapa(GraphicsContext gc) {
    int tileInicioC = (int)(camaraX / Mapa.TILE_SIZE);
    int tileInicioF = (int)(camaraY / Mapa.TILE_SIZE);
    int tileFinalC  = tileInicioC + (int)(canvas.getWidth()  / Mapa.TILE_SIZE) + 2;
    int tileFinalF  = tileInicioF + (int)(canvas.getHeight() / Mapa.TILE_SIZE) + 2;

    // clamp — no salirse de los límites del mapa
    tileInicioC = Math.max(0, tileInicioC);
    tileInicioF = Math.max(0, tileInicioF);
    tileFinalC  = Math.min(Mapa.COLS  - 1, tileFinalC);
    tileFinalF  = Math.min(Mapa.FILAS - 1, tileFinalF);

    for (int f = tileInicioF; f <= tileFinalF; f++) {
        for (int c = tileInicioC; c <= tileFinalC; c++) {
            Tile tile = mapa.getTile(f, c);

            // coordenadas en pantalla = coordenadas mundo - cámara
            double screenX = c * Mapa.TILE_SIZE - camaraX;
            double screenY = f * Mapa.TILE_SIZE - camaraY;

            // color base del tile
            gc.setFill(tile.getTipo().getColor());
            gc.fillRect(screenX, screenY, Mapa.TILE_SIZE, Mapa.TILE_SIZE);

            // borde sutil para ver la cuadrícula
            gc.setStroke(Color.rgb(0, 0, 0, 0.1));
            gc.strokeRect(screenX, screenY, Mapa.TILE_SIZE, Mapa.TILE_SIZE);

            // indicador visual de tile modificado por el jugador
            if (tile.isModificadoPorJugador()) {
                gc.setStroke(Color.YELLOW);
                gc.setLineWidth(1.5);
                gc.strokeRect(screenX + 1, screenY + 1,
                              Mapa.TILE_SIZE - 2, Mapa.TILE_SIZE - 2);
                gc.setLineWidth(1.0);
            }
        }
    }
}
```

---

## 37. Cámara

La cámara sigue al jugador manteniéndolo **centrado en la pantalla**, pero sin salirse de los límites del mundo.

```java
// Variables de cámara en GameView
private double camaraX = 0;
private double camaraY = 0;

// Se llama cada frame después de mover al jugador
private void actualizarCamara() {
    double mitadW = canvas.getWidth()  / 2.0;
    double mitadH = canvas.getHeight() / 2.0;

    // la cámara intenta centrar al jugador
    camaraX = x - mitadW;
    camaraY = y - mitadH;

    // clamp: no mostrar fuera del mundo
    double maxCamX = mapa.getAnchoPx() - canvas.getWidth();
    double maxCamY = mapa.getAltoPx()  - canvas.getHeight();

    camaraX = Math.max(0, Math.min(maxCamX, camaraX));
    camaraY = Math.max(0, Math.min(maxCamY, camaraY));
}

// Conversión útil: mundo → pantalla (para dibujar cualquier entidad)
private double mundoAPantallaX(double mundoX) { return mundoX - camaraX; }
private double mundoAPantallaY(double mundoY) { return mundoY - camaraY; }
```

---

## 38. El jugador modifica el terreno

### Concepto: acción contextual con tecla

El jugador pulsa `F` para interactuar con el tile al frente. La acción depende del tile actual:
- `BOSQUE` → talar → se convierte en `HIERBA` (y cae un "ítem")
- `HIERBA` → plantar → se convierte en `BOSQUE`
- `ROCA_SUELO` → minar → se convierte en `HIERBA` (y cae una "piedra")

```java
// En GameView — procesar tecla de interacción
private void procesarInteraccion() {
    if (!teclasActivas.contains(KeyCode.F)) return;
    teclasActivas.remove(KeyCode.F); // consumir tecla (no repetir)

    Tile tileFrontal = getTileAlFrente();
    if (tileFrontal == null) return;

    mapController.interactuar(tileFrontal, personaje);
}
```

### `controllers/MapController.java`

```java
package k82studio.controllers;

import k82studio.models.Personaje;
import k82studio.models.mapa.Mapa;
import k82studio.models.mapa.Tile;
import k82studio.models.mapa.TipoTile;
import k82studio.views.GameView;

public class MapController {

    private final Mapa     mapa;
    private final GameView vista;

    public MapController(Mapa mapa, GameView vista) {
        this.mapa  = mapa;
        this.vista = vista;
    }

    // ── INTERACCIÓN con el tile al frente ─────────────────────
    public void interactuar(Tile tile, Personaje jugador) {
        switch (tile.getTipo()) {

            case BOSQUE -> talarArbol(tile, jugador);
            case ARBOL  -> talarArbol(tile, jugador);
            case HIERBA -> plantarArbol(tile);
            case ARENA  -> construirHoguera(tile);
            case BARRO  -> limpiarBarro(tile);

            default -> vista.mostrarMensaje(
                "No puedes interactuar con " + tile.getTipo().getNombre()
            );
        }
    }

    // ── ACCIONES ──────────────────────────────────────────────

    private void talarArbol(Tile tile, Personaje jugador) {
        if (tile.cambiarTipo(TipoTile.HIERBA)) {
            vista.mostrarMensaje("Has talado un árbol");
            // TU TURNO: añadir madera al inventario del jugador
        }
    }

    private void plantarArbol(Tile tile) {
        if (tile.cambiarTipo(TipoTile.ARBOL)) {
            vista.mostrarMensaje("Has plantado un árbol");
        }
    }

    private void construirHoguera(Tile tile) {
        if (tile.cambiarTipo(TipoTile.HOGUERA)) {
            vista.mostrarMensaje("Has construido una hoguera");
            // TU TURNO: la hoguera podría restaurar vida al acercarse
        }
    }

    private void limpiarBarro(Tile tile) {
        if (tile.cambiarTipo(TipoTile.HIERBA)) {
            vista.mostrarMensaje("Has limpiado el barro");
        }
    }

    // ── COLOCAR OBJETO ─────────────────────────────────────────
    // El jugador selecciona un tipo y lo coloca en la posición indicada
    public void colocarObjeto(int fila, int col, TipoTile tipo, Personaje jugador) {
        if (!tipo.esColocable()) {
            vista.mostrarMensaje("No puedes colocar " + tipo.getNombre());
            return;
        }

        Tile tile = mapa.getTile(fila, col);
        if (tile == null || tile.isSolido()) {
            vista.mostrarMensaje("No puedes colocar aquí");
            return;
        }

        tile.cambiarTipo(tipo);
        vista.mostrarMensaje("Has colocado: " + tipo.getNombre());
    }

    // ── RESTAURAR tile a su estado original ───────────────────
    public void restaurarTile(int fila, int col) {
        Tile tile = mapa.getTile(fila, col);
        if (tile != null) tile.restaurar();
    }
}
```

---

## 39. El jugador crea objetos

### Menú de construcción

El jugador pulsa `B` para abrir un menú de construcción. Selecciona qué colocar y luego hace click en el mapa para posicionar el objeto.

```java
// En GameView — tecla B abre modo construcción
private boolean modoConstruccion = false;
private TipoTile tipoSeleccionado = TipoTile.HOGUERA;

// En procesarAcciones():
if (teclasActivas.contains(KeyCode.B)) {
    teclasActivas.remove(KeyCode.B);
    modoConstruccion = !modoConstruccion;
    vista.mostrarMensaje(modoConstruccion ? "Modo construcción ON" : "Modo construcción OFF");
}

// Click del ratón en modo construcción — colocar objeto
canvas.setOnMouseClicked(event -> {
    if (!modoConstruccion) return;

    // convertir clic en pantalla → coordenadas mundo → tile
    double mundoX = event.getX() + camaraX;
    double mundoY = event.getY() + camaraY;
    int col  = (int)(mundoX / Mapa.TILE_SIZE);
    int fila = (int)(mundoY / Mapa.TILE_SIZE);

    mapController.colocarObjeto(fila, col, tipoSeleccionado, personaje);
});

// En el renderizado: resaltar el tile bajo el cursor en modo construcción
canvas.setOnMouseMoved(event -> {
    if (!modoConstruccion) return;
    double mundoX = event.getX() + camaraX;
    double mundoY = event.getY() + camaraY;
    tileCursor = mapa.getTileEnPixel(mundoX, mundoY);
});
```

### Resaltar tile bajo el cursor

```java
// En dibujarMapa(), al final del loop de tiles:
if (modoConstruccion && tile == tileCursor) {
    gc.setFill(Color.rgb(255, 255, 0, 0.4)); // amarillo semitransparente
    gc.fillRect(screenX, screenY, Mapa.TILE_SIZE, Mapa.TILE_SIZE);
}
```

---

## 40. MapController

Ya implementado en la sección 38. Aquí el resumen de sus responsabilidades:

```
MapController
    ├── interactuar(tile, jugador)       ← decide qué pasa según el tipo de tile
    ├── colocarObjeto(f, c, tipo, j)     ← coloca un objeto en el mapa
    ├── restaurarTile(f, c)              ← restaura al estado original
    └── [TU TURNO] minarRecurso(tile)    ← extrae recursos (madera, piedra)
```

**Regla de diseño:** el `MapController` es el único que llama a `tile.cambiarTipo()`. Ni `GameView` ni `Mapa` cambian tiles directamente — siempre pasa por el controller.

---

## 41. GameView actualizado

Esta es la versión completa de `GameView` con todo integrado. La estructura del bucle principal:

```java
@Override
public void handle(long ahora) {
    if (ultimoFrame == 0) { ultimoFrame = ahora; return; }
    double delta = (ahora - ultimoFrame) / 1_000_000_000.0;
    ultimoFrame = ahora;

    // 1. Input → movimiento
    mover(delta);

    // 2. Efectos del terreno actual
    aplicarEfectoTerreno();

    // 3. Enemigos siguen al jugador
    actualizarEnemigos(delta);

    // 4. Cámara sigue al jugador
    actualizarCamara();

    // 5. Colisiones jugador-enemigo
    comprobarColisiones();

    // 6. Interacciones con el mapa
    procesarInteraccion();

    // 7. Dibujar en este orden (los de arriba quedan debajo)
    dibujarMapa(gc);           // tiles del suelo
    dibujarEnemigos(gc);       // enemigos
    dibujarJugador(gc);        // jugador (siempre encima)
    dibujarHUD(gc);            // interfaz (vida, terreno, modo)
}
```

### HUD (Heads-Up Display)

```java
// Se dibuja SIEMPRE en las mismas coordenadas de pantalla
// No necesita compensación de cámara — está fijo en la UI
private void dibujarHUD(GraphicsContext gc) {
    // fondo semitransparente
    gc.setFill(Color.rgb(0, 0, 0, 0.5));
    gc.fillRoundRect(10, 10, 250, 80, 10, 10);

    gc.setFill(Color.WHITE);
    gc.setFont(javafx.scene.text.Font.font(13));
    gc.fillText("Vida: " + personaje.getVida(), 20, 30);
    gc.fillText("Terreno: " + (ultimoTile != null ? ultimoTile.getTipo().getNombre() : "—"), 20, 50);
    gc.fillText("Pos: (" + (int)(x/Mapa.TILE_SIZE) + ", " + (int)(y/Mapa.TILE_SIZE) + ")", 20, 70);

    if (modoConstruccion) {
        gc.setFill(Color.YELLOW);
        gc.fillText("MODO CONSTRUCCIÓN — Click para colocar", 20, 100);
    }
}
```

---

## 42. Persistencia del mapa en BBDD

### La tabla

```sql
CREATE TABLE IF NOT EXISTS mapa_tiles (
    mapa_nombre  TEXT    NOT NULL,
    fila         INTEGER NOT NULL,
    col          INTEGER NOT NULL,
    tipo         TEXT    NOT NULL,
    modificado   INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (mapa_nombre, fila, col)
)
```

### `db/MapaDAO.java`

Solo guardamos los tiles **modificados por el jugador** — no tiene sentido guardar 10.000 tiles si el jugador solo cambió 20.

```java
package k82studio.db;

import k82studio.models.mapa.Mapa;
import k82studio.models.mapa.Tile;
import k82studio.models.mapa.TipoTile;

import java.sql.*;

public class MapaDAO {

    private final Connection conn;

    public MapaDAO() {
        this.conn = DatabaseManager.getInstance().getConexion();
        crearTabla();
    }

    private void crearTabla() {
        String sql = """
            CREATE TABLE IF NOT EXISTS mapa_tiles (
                mapa_nombre TEXT    NOT NULL,
                fila        INTEGER NOT NULL,
                col         INTEGER NOT NULL,
                tipo        TEXT    NOT NULL,
                modificado  INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (mapa_nombre, fila, col)
            )
            """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creando tabla mapa_tiles: " + e.getMessage());
        }
    }

    // ── GUARDAR solo los tiles modificados ─────────────────────
    // INSERT OR REPLACE: si ya existe la fila (mismo pk), la reemplaza
    public void guardarModificaciones(Mapa mapa) {
        String sql = "INSERT OR REPLACE INTO mapa_tiles (mapa_nombre, fila, col, tipo, modificado) VALUES (?,?,?,?,?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false); // transacción: todos o ninguno

            for (Tile[] fila : mapa.getTiles()) {
                for (Tile tile : fila) {
                    if (!tile.isModificadoPorJugador()) continue;

                    stmt.setString (1, mapa.getNombre());
                    stmt.setInt    (2, tile.getFila());
                    stmt.setInt    (3, tile.getCol());
                    stmt.setString (4, tile.getTipo().name()); // enum → String
                    stmt.setInt    (5, 1);
                    stmt.addBatch();
                }
            }

            stmt.executeBatch(); // ejecutar todo de golpe
            conn.commit();
            conn.setAutoCommit(true);
            System.out.println("Mapa guardado");

        } catch (SQLException e) {
            System.err.println("Error guardando mapa: " + e.getMessage());
            try { conn.rollback(); } catch (SQLException ex) { /* ignorar */ }
        }
    }

    // ── CARGAR modificaciones sobre un mapa ya generado ────────
    public void cargarModificaciones(Mapa mapa) {
        String sql = "SELECT fila, col, tipo FROM mapa_tiles WHERE mapa_nombre = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, mapa.getNombre());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int      fila = rs.getInt("fila");
                int      col  = rs.getInt("col");
                TipoTile tipo = TipoTile.valueOf(rs.getString("tipo")); // String → enum

                Tile tile = mapa.getTile(fila, col);
                if (tile != null) tile.cambiarTipo(tipo);
            }
            System.out.println("Mapa cargado con modificaciones");

        } catch (SQLException e) {
            System.err.println("Error cargando mapa: " + e.getMessage());
        }
    }
}
```

> **¿Por qué `INSERT OR REPLACE`?** SQLite tiene este comando que en un solo paso inserta la fila si no existe o la reemplaza si ya existe con la misma PRIMARY KEY. Evita tener que hacer `SELECT` primero para saber si usar `INSERT` o `UPDATE`.

---

## 43. Errores comunes Fase 3

| Error | Causa | Solución |
|---|---|---|
| El jugador atraviesa los árboles | `puedeMoverA` no se llama | Asegúrate de usarlo antes de actualizar `x` e `y` |
| El jugador se "pega" a las paredes | No se separan X e Y en la colisión | Comprueba X e Y por separado (sliding collision) |
| El mapa no se ve | `camaraX/Y` no se inicializan | Llama a `actualizarCamara()` antes del primer frame |
| El personaje desaparece fuera del viewport | No se restan las coordenadas de cámara al dibujar | Usa `mundoAPantallaX(x)` para todas las entidades |
| `TipoTile.valueOf()` lanza excepción | El String de la BBDD no coincide con el enum | Usa `name()` para guardar y `valueOf()` para leer — no los cambies a mano |
| Generación muy lenta | Demasiadas operaciones en el hilo JavaFX | Genera el mapa en un `Task` (hilo secundario) y actualiza la UI al terminar |
| El modo construcción no detecta el click | El canvas no tiene foco | Llama a `canvas.setFocusTraversable(true)` |
| `NullPointerException` en `getTile()` | Acceso fuera de los límites | Siempre usa `dentroDelMapa()` antes de acceder |

---

## 44. Lo que implementas tú

| # | Qué implementar | Clase | Pista |
|---|---|---|---|
| 1 | `EfectoArena` | `efectos/EfectoArena.java` | Copia `EfectoBarro` con `MOD = 0.60` |
| 2 | `EfectoAgua` | `efectos/EfectoAgua.java` | Agua bloquea o drena vida cada segundo |
| 3 | `EfectoBosque` | `efectos/EfectoBosque.java` | Bosque reduce velocidad al 80%, esconde al jugador de enemigos |
| 4 | `actualizarVida()` en `PersonajeDAO` | `db/PersonajeDAO.java` | `UPDATE personajes SET vida = ? WHERE nombre = ?` |
| 5 | `minarRecurso()` en `MapController` | `controllers/MapController.java` | Extrae items, añade al inventario del jugador |
| 6 | Teclas `1`, `2`, `3` para seleccionar objeto a construir | `views/GameView.java` | Asigna `tipoSeleccionado` según la tecla |
| 7 | La hoguera restaura vida al estar encima | `controllers/MapController.java` | En `aplicarEfectoTerreno()`, si tile es HOGUERA, `jugador.setVida(+1)` |
| 8 | Guardar el mapa al cerrar la app | `App.java` | `stage.setOnCloseRequest(e -> mapaDAO.guardarModificaciones(mapa))` |
| 9 | Generación con semilla desde menú | `views/MainView.java` | `TextField` para introducir semilla, pásala a `new GeneradorMapa(semilla)` |

---

## Resumen: el flujo completo Fase 3

```
App.java
  → DatabaseManager.getInstance()        // conectar BBDD
  → new GeneradorMapa(semilla).generar()  // generar el mundo
  → mapaDAO.cargarModificaciones(mapa)    // aplicar cambios guardados
  → new GameView(personaje, mapa)         // mostrar el juego

GameView (cada frame ~16ms)
  → mover(delta)                         // input + colisión tiles
  → aplicarEfectoTerreno()               // Strategy según tile actual
  → actualizarEnemigos(delta)            // IA seguimiento
  → actualizarCamara()                   // cámara sigue al jugador
  → comprobarColisiones()                // jugador vs enemigos
  → procesarInteraccion()                // tecla F → MapController
  → dibujarMapa(gc)                      // solo tiles visibles
  → dibujarEnemigos(gc)                  // entidades
  → dibujarJugador(gc)                   // jugador
  → dibujarHUD(gc)                       // interfaz fija

MapController (cuando el jugador interactúa)
  → tile.cambiarTipo(nuevoTipo)          // modifica el mundo
  → mapaDAO.guardarModificaciones()      // persiste en BBDD
```

---

*k82studio · RPG Manager Guide — Fase 3*