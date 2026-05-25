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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ListViewportController implements Initializable {
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private FlowPane itemContainer;
    @FXML
    private ComboBox<String> sortByComboBox;
    @FXML
    private ComboBox<String> statusComboBox;

    private final ObservableList<AuctionDTO> allAuctions = AuctionStore.getInstance().getAuctions();

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

        // Thêm listener để tự động render lại khi dữ liệu trong store thay đổi
        allAuctions.addListener((ListChangeListener<AuctionDTO>) c -> renderAuctions());

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
    }

    public void searchBar(String text) {
        itemContainer.getChildren().clear();
        String keyword = (text == null) ? "" : text.trim().toLowerCase();
        List<AuctionDTO> findauctions=new ArrayList<>(allAuctions);
        findauctions.removeIf(auction -> auction.getStatus() == AuctionStatus.CANCELLED);
        for (AuctionDTO auction : findauctions) {
            String auctionName = auction.getName();
            if (auctionName == null) continue;
            if (keyword.isEmpty() || auctionName.toLowerCase().contains(keyword)) {
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
        }
    }
}