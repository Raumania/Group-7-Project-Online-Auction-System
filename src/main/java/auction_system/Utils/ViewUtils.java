package auction_system.Utils;

import javafx.beans.binding.Bindings;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;

public class ViewUtils {
    public static void makeResizable(Scene scene, Region content, double designWidth, double designHeight) {
        Scale scale = new Scale(1, 1, 0, 0);
        scale.xProperty().bind(Bindings.divide(scene.widthProperty(), designWidth));
        scale.yProperty().bind(Bindings.divide(scene.heightProperty(), designHeight));
        content.getTransforms().add(scale);

        content.setPrefWidth(designWidth);
        content.setPrefHeight(designHeight);
        content.setMinWidth(Region.USE_PREF_SIZE);
        content.setMinHeight(Region.USE_PREF_SIZE);
        content.setMaxWidth(Region.USE_PREF_SIZE);
        content.setMaxHeight(Region.USE_PREF_SIZE);
    }

    public static double getSystemScale() {
        return Screen.getPrimary().getOutputScaleX();
    }
}
