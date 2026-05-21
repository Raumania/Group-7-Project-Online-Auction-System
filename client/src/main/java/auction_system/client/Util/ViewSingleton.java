package auction_system.client.Util;

import javafx.scene.layout.StackPane;

public class ViewSingleton {
    private static ViewSingleton singleton;
    private StackPane viewport;

    private ViewSingleton() { }
    public static ViewSingleton getInstance() {
        if( singleton == null ) {
            singleton = new ViewSingleton();
        }
        return singleton;
    }

    public void setViewport(StackPane viewport) {
        this.viewport = viewport;
    }

    public StackPane getViewport() {
        return viewport;
    }
}
