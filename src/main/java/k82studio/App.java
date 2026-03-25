package k82studio;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import k82studio.models.*;
import k82studio.views.MainView;

public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

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