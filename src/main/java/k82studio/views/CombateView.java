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
