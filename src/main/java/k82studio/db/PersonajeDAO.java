package k82studio.db;

import k82studio.models.Arquero;
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
        String tipo;
        if (p instanceof Guerrero) tipo = "GUERRERO";
        else if (p instanceof Mago) tipo = "MAGO";
        else tipo = "ARQUERO";

        String sql = "INSERT INTO personajes (nombre, tipo, nivel, vida, ataque) VALUES (?, ?, ?, ?, ?)";

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
                Personaje p = switch (tipo) {
                    case "GUERRERO" -> new Guerrero(nombre, nivel, ataque);
                    case "MAGO"     -> new Mago(nombre, nivel, ataque);
                    case "ARQUERO"  -> new Arquero(nombre, nivel, ataque);
                    default         -> new Guerrero(nombre, nivel, ataque);
                };

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
                return switch (tipo) {
                    case "GUERRERO" -> new Guerrero(nombre, nivel, ataque);
                    case "MAGO"     -> new Mago(nombre, nivel, ataque);
                    case "ARQUERO"  -> new Arquero(nombre, nivel, ataque);
                    default         -> new Guerrero(nombre, nivel, ataque);
                };
            }
        } catch (SQLException e) {
            System.err.println("Error buscando personaje: " + e.getMessage());
        }
        return null;
    }

    // ── UPDATE ─────────────────────────────────────────────────
    // SQL: UPDATE personajes SET vida = ? WHERE nombre = ?
    public void actualizarVida(String nombre, int nuevaVida) {
        String sql = "UPDATE personajes SET vida = ? WHERE nombre = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, nuevaVida);
            stmt.setString(2, nombre);
            stmt.executeUpdate();
            System.out.println("Vida actualizada para: " + nombre + " → " + nuevaVida);
        } catch (SQLException e) {
            System.err.println("Error actualizando vida: " + e.getMessage());
        }
    }

    // ── DELETE ─────────────────────────────────────────────────
    // SQL: DELETE FROM personajes WHERE nombre = ?
    public void eliminar(String nombre) {
        String sql = "DELETE FROM personajes WHERE nombre = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nombre);
            stmt.executeUpdate();
            System.out.println("Personaje eliminado: " + nombre);
        } catch (SQLException e) {
            System.err.println("Error eliminando personaje: " + e.getMessage());
        }
    }
}
