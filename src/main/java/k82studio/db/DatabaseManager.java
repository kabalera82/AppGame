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
