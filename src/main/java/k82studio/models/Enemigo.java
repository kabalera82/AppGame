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
    public void actualizar(double delta, double jugadorX, double jugadorY,
                           double maxX, double maxY) {
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

        // límites del canvas — el enemigo no puede salir
        double radio = 18.0;
        x = Math.max(radio, Math.min(maxX - radio, x));
        y = Math.max(radio, Math.min(maxY - radio, y));
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
