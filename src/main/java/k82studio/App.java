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

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // 1. inicializar base de datos (Singleton — crea rpg.db y tablas si no existen)
        DatabaseManager.getInstance();
        PersonajeDAO dao = new PersonajeDAO();

        // 2. intentar cargar personajes desde la BBDD
        List<Personaje> cargados = dao.obtenerTodos();

        Inventario inventario = new Inventario();

        if (cargados.isEmpty()) {
            // primera vez: crear personajes y guardarlos
            Guerrero aragorn  = new Guerrero("Aragorn", 10, 45);
            Mago     gandalf  = new Mago("Gandalf", 15, 80);
            Guerrero legolas  = new Guerrero("Legolas", 8, 38);

            inventario.agregar(aragorn);
            inventario.agregar(gandalf);
            inventario.agregar(legolas);

            dao.guardar(aragorn);
            dao.guardar(gandalf);
            dao.guardar(legolas);

            System.out.println("Personajes creados y guardados en BBDD.");
        } else {
            // sesiones posteriores: cargar desde la BBDD
            for (Personaje p : cargados) {
                inventario.agregar(p);
            }
            System.out.println("Personajes cargados desde BBDD: " + cargados.size());
        }

        // 3. vista
        MainView vista = new MainView(inventario);
        Scene scene = new Scene(vista, 1026, 720);

        // 4. ventana
        stage.setTitle("RPG Manager");
        stage.setScene(scene);
        stage.show();
    }
}
