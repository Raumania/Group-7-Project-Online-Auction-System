package auction_system.client.controller;

import auction_system.client.service.AuctionListService;
import auction_system.client.store.AuctionStore;
import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.AuctionStatus;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ListViewportController implements Initializable {
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private FlowPane itemContainer;
    @FXML private ComboBox<String> sortByComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private ScrollPane listScrollPane;

    private final ObservableList<AuctionDTO> allAuctions = AuctionStore.getInstance().getAuctions();
    // Lưu reference listener để có thể removeListener() khi controller bị destroy (defense in depth)
    private ListChangeListener<AuctionDTO> auctionStoreListener;

    @FXML
    void handleFilterChange(ActionEvent event) {
        renderAuctions();
    }

    public void initialize(URL location, ResourceBundle resources) {
        //Categories
        ObservableList<String> categoryOptions = javafx.collections.FXCollections.observableArrayList(
                "All Category", "Electronics", "Art", "Vehicle"
        );
        categoryComboBox.setItems(categoryOptions);
        categoryComboBox.getSelectionModel().selectFirst();
        //Status
        ObservableList<String> statusOptions = javafx.collections.FXCollections.observableArrayList(
                "All Status", "OPEN", "RUNNING", "FINISHED", "PAID", "CANCELLED"
        );
        statusComboBox.setItems(statusOptions);
        statusComboBox.getSelectionModel().selectFirst();
        //Sortby
        ObservableList<String> sortByOptions = javafx.collections.FXCollections.observableArrayList(
                "Newest", "Oldest"
        );
        sortByComboBox.setItems(sortByOptions);
        sortByComboBox.getSelectionModel().selectFirst();

        // Đăng ký listener để tự động render lại khi dữ liệu trong store thay đổi
        auctionStoreListener = c -> renderAuctions();
        allAuctions.addListener(auctionStoreListener);

        // ✅ Auto-cleanup + Auto-reconnect khi controller vào/ra khỏi scene.
        // - Khi rời scene (newScene == null): xóa listener để tránh memory leak.
        // - Khi vào lại scene (newScene != null): đăng ký lại listener VÀ render lại
        //   để hiển thị những auction mới được thêm trong lúc đang ở tab khác.
        categoryComboBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                // Rời scene → xóa listener
                if (auctionStoreListener != null) {
                    allAuctions.removeListener(auctionStoreListener);
                    auctionStoreListener = null;
                }
            } else {
                // Vào lại scene → đăng ký lại listener + render lại để cập nhật data mới
                if (auctionStoreListener == null) {
                    auctionStoreListener = c -> renderAuctions();
                    allAuctions.addListener(auctionStoreListener);
                }
                renderAuctions();
            }
        });

        // Listen to width changes to adjust card widths dynamically (responsive grid)
        listScrollPane.widthProperty().addListener((obs, oldVal, newVal) -> adjustCardWidths());

        // Render dữ liệu có sẵn trong store ngay lập tức
        renderAuctions();
    }

    private void renderAuctions() {
        itemContainer.getChildren().clear();

        // 1. Get current filter & sort values
        String selectedCategory = categoryComboBox.getValue();
        String selectedStatus = statusComboBox.getValue();
        String selectedSort = sortByComboBox.getValue();

        // 2. Filter & sort
        List<AuctionDTO> filteredList = new ArrayList<>(allAuctions);

        // Filter by Category
        if (selectedCategory != null && !selectedCategory.equals("All Category")) {
            filteredList.removeIf(auction -> {
                if (auction.getType() == null) return true;
                return !auction.getType().name().equalsIgnoreCase(selectedCategory);
            });
        }

        // Filter by Status
        if (selectedStatus != null && !selectedStatus.equals("All Status")) {
            filteredList.removeIf(auction -> {
                if (auction.getStatus() == null) return true;
                return !auction.getStatus().name().equalsIgnoreCase(selectedStatus);
            });
        } else {
            filteredList.removeIf(auction -> auction.getStatus() == AuctionStatus.CANCELLED);
        }

        // Filter by Search Keyword
        String keyword = "";
        if (MainAuctionController.getInstance() != null) {
            keyword = MainAuctionController.getInstance().getSearchKeyword();
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String finalKeyword = keyword.trim().toLowerCase();
            filteredList.removeIf(auction -> {
                if (auction.getName() == null) return true;
                return !auction.getName().toLowerCase().contains(finalKeyword);
            });
        }

        // Sort
        if (selectedSort != null) {
            if (selectedSort.equals("Newest")) {
                filteredList.sort((a, b) -> {
                    if (a.getStartTime() != null && b.getStartTime() != null) {
                        return b.getStartTime().compareTo(a.getStartTime());
                    }
                    return Integer.compare(b.getId(), a.getId());
                });
            } else if (selectedSort.equals("Oldest")) {
                filteredList.sort((a, b) -> {
                    if (a.getStartTime() != null && b.getStartTime() != null) {
                        return a.getStartTime().compareTo(b.getStartTime());
                    }
                    return Integer.compare(a.getId(), b.getId());
                });
            }
        }
        // 3. Render
        for (AuctionDTO auction : filteredList) {
            try {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("/fxml/itemCard.fxml"));
                VBox pane = loader.load();
                ItemCardController itemCardController = loader.getController();
                itemCardController.setData(auction);
                itemContainer.getChildren().add(pane);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        adjustCardWidths();
    }

    public void searchBar(String text) {
        renderAuctions();
    }

    private void adjustCardWidths() {
        if (listScrollPane == null) return;
        double width = listScrollPane.getWidth();
        if (width <= 0) return;

        // Subtract 18px for the vertical scrollbar to prevent horizontal overflow and layout feedback loops
        double scrollbarReserve = 18.0;
        double leftPadding = 20.0;
        double rightPadding = 20.0;
        double hgap = 20.0;
        double minCardWidth = 210.0;

        double availableWidth = width - scrollbarReserve - leftPadding - rightPadding;
        if (availableWidth <= 0) return;

        // Calculate how many columns can fit
        int columns = (int) Math.floor((availableWidth + hgap) / (minCardWidth + hgap));
        if (columns < 1) columns = 1;

        double cardWidth = (availableWidth - (columns - 1) * hgap) / columns;

        for (Node node : itemContainer.getChildren()) {
            if (node instanceof VBox) {
                VBox card = (VBox) node;
                card.setPrefWidth(cardWidth);
                card.setMinWidth(cardWidth);
                card.setMaxWidth(cardWidth);
            }
        }
    }
}