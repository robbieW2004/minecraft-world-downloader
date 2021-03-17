package gui.realms;

import com.google.gson.Gson;
import gui.GuiSettings;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import proxy.auth.ClientAuthenticator;

import java.io.IOException;
import java.util.Arrays;

import static util.ExceptionHandling.attempt;

public class RealmsTabController {
    public ListView<RealmEntry> serverList;

    public RealmsTabController() { }

    private boolean requested;

    private ClientAuthenticator auth;

    private GuiSettings settings;

    @FXML
    void initialize() {
        serverList.setFocusTraversable( false );
        serverList.setSelectionModel(new NoSelectionModel<>());
        serverList.setCellFactory(e -> new ListCell<RealmEntry>() {
            Parent node;

            @Override
            protected void updateItem(RealmEntry realmEntry, boolean empty) {
                super.updateItem(realmEntry, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                try {
                    FXMLLoader loader = new FXMLLoader(RealmsTabController.class.getResource("/ui/RealmItem.fxml"));
                    loader.setController(new RealmItemController(realmEntry, settings));

                    node = loader.load();
                    node = (Parent) node.lookup("#item");
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return;
                }
                setGraphic(node);
            }
        });
    }

    public void opened(GuiSettings guiSettings) {
        if (requested) {
            return;
        }
        requested = true;
        this.settings = guiSettings;

        serverList.setItems(FXCollections.observableArrayList(new RealmEntry("Loading...")));

        auth = new ClientAuthenticator();
        auth.requestRealms(str -> {
            RealmServers serversTemp = null;
            try {
                serversTemp = new Gson().fromJson(str, RealmServers.class);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            final RealmServers servers = serversTemp;
            Platform.runLater(() -> attempt(() -> {
                serverList.getItems().clear();

                if (servers != null && servers.servers.length > 0) {
                    Arrays.stream(servers.servers).forEach(realm -> {
                        realm.setAuth(auth);
                        realm.reset();
                    });
                    serverList.getItems().addAll(servers.servers);
                } else {
                    serverList.getItems().add(new RealmEntry("No realms found for user " + auth.getDetails().getUsername()));
                    this.requested = false;
                }
                serverList.refresh();
            }));
        });
    }

    private static class RealmServers { RealmEntry[] servers; }
}



