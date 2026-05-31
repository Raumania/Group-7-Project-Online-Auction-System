package auction_system.client.controller;

import auction_system.client.service.AdminUserService;
import auction_system.client.store.AdminUserStore;
import auction_system.common.dto.UserDTO;
import auction_system.common.enums.UserRole;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Optional;

public class AdminUserManageController implements Initializable {

    @FXML private TableView<UserDTO> userTable;
    @FXML private TableColumn<UserDTO, Integer> colId;
    @FXML private TableColumn<UserDTO, String> colFullname;
    @FXML private TableColumn<UserDTO, String> colUsername;
    @FXML private TableColumn<UserDTO, String> colStatus;
    @FXML private TableColumn<UserDTO, String> colRoles;
    @FXML private TableColumn<UserDTO, BigDecimal> colAvailableBalance;
    @FXML private TableColumn<UserDTO, BigDecimal> colFrozenBalance;
    @FXML private TextField txtFullname;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cbRoles;
    @FXML private TextField txtAvailableBalance;
    @FXML private TextField txtFrozenBalance;
    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnBan;

    private final ObservableList<UserDTO> userList = AdminUserStore.getInstance().getUsers();
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupTableSelectionListener();
        setupBalanceInput();
        cbRoles.setItems(FXCollections.observableArrayList("ADMIN", "BIDDER/SELLER"));

        btnEdit.setDisable(true);
        btnBan.setDisable(true);
        btnAdd.setDisable(false);

        txtFrozenBalance.setText("0");
        txtFrozenBalance.setDisable(true);

        // Load real database data on load
        handleRefreshTable(null);
    }

    private void setupBalanceInput() {
        Pattern validDoubleText = Pattern.compile("^\\d*\\.?\\d{0,2}$");
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (validDoubleText.matcher(newText).matches()) {
                return change;
            }
            return null;
        };
        txtAvailableBalance.setTextFormatter(new TextFormatter<>(filter));
        
        UnaryOperator<TextFormatter.Change> filter2 = change -> {
            String newText = change.getControlNewText();
            if (validDoubleText.matcher(newText).matches()) {
                return change;
            }
            return null;
        };
        txtFrozenBalance.setTextFormatter(new TextFormatter<>(filter2));
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFullname.setCellValueFactory(new PropertyValueFactory<>("fullname"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colStatus.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().getStatus() != null ? cellData.getValue().getStatus().name() : "ACTIVE"
        ));
        colStatus.setCellFactory(column -> new TableCell<UserDTO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    UserDTO user = getTableRow().getItem();
                    if (user.getStatus() != null) {
                        setText(user.getStatus().name());
                        if (user.getStatus() == auction_system.common.enums.UserStatus.BANNED) {
                            setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                        }
                    } else {
                        setText("ACTIVE");
                        setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    }
                }
            }
        });
        colAvailableBalance.setCellValueFactory(new PropertyValueFactory<>("availableBalance"));
        colAvailableBalance.setCellFactory(column -> new TableCell<UserDTO, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormatter.format(item));
                }
            }
        });

        colFrozenBalance.setCellValueFactory(new PropertyValueFactory<>("frozenBalance"));
        colFrozenBalance.setCellFactory(column -> new TableCell<UserDTO, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormatter.format(item));
                }
            }
        });

        colRoles.setCellFactory(column -> new TableCell<UserDTO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    UserDTO user = getTableRow().getItem();
                    if (user.getRoles() != null) {
                        String rolesStr = user.getRoles().stream()
                                .map(UserRole::name)
                                .collect(Collectors.joining(", "));
                        setText(rolesStr);
                    } else {
                        setText("");
                    }
                }
            }
        });

        userTable.setItems(userList);
    }

    private void setupTableSelectionListener() {
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                txtFullname.setText(newSelection.getFullname());
                txtUsername.setText(newSelection.getUsername());
                txtPassword.clear();
                txtAvailableBalance.setText(newSelection.getAvailableBalance() != null ? newSelection.getAvailableBalance().toPlainString() : "");
                txtFrozenBalance.setText(newSelection.getFrozenBalance() != null ? newSelection.getFrozenBalance().toPlainString() : "");
                
                if (newSelection.getRoles() != null) {
                    if (newSelection.getRoles().contains(UserRole.ADMIN)) {
                        cbRoles.setValue("ADMIN");
                    } else {
                        cbRoles.setValue("BIDDER/SELLER");
                    }
                } else {
                    cbRoles.getSelectionModel().clearSelection();
                }

                txtUsername.setEditable(false);
                txtFrozenBalance.setDisable(true);
                cbRoles.setDisable(true);

                btnAdd.setDisable(true);
                btnEdit.setDisable(false);
                btnBan.setDisable(false);
            } else {
                txtUsername.setEditable(true);
                txtFrozenBalance.setDisable(true);
                cbRoles.setDisable(false);
            }
        });
    }

    @FXML
    void handleAdd(ActionEvent event) {
        String fullname = txtFullname.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String availableBalanceStr = txtAvailableBalance.getText().trim();
        String frozenBalanceStr = txtFrozenBalance.getText().trim();
        String roleSelection = cbRoles.getValue();

        if (fullname.isEmpty() || username.isEmpty() || password.isEmpty() || availableBalanceStr.isEmpty() || frozenBalanceStr.isEmpty() || roleSelection == null) {
            showError("Please fill in all information!");
            return;
        }

        BigDecimal availableBalance;
        BigDecimal frozenBalance;
        try {
            availableBalance = new BigDecimal(availableBalanceStr);
            frozenBalance = new BigDecimal(frozenBalanceStr);
        } catch (NumberFormatException e) {
            showError("Available Balance and Frozen Balance must be valid numbers!");
            return;
        }

        Set<UserRole> roles = new HashSet<>();
        if ("ADMIN".equals(roleSelection)) {
            roles.add(UserRole.ADMIN);
        } else if ("BIDDER/SELLER".equals(roleSelection)) {
            roles.add(UserRole.BIDDER);
            roles.add(UserRole.SELLER);
        }

        UserDTO newUser = new UserDTO(0, fullname, username, roles, availableBalance, frozenBalance);
        newUser.setPassword(password);

        new Thread(() -> {
            boolean success = AdminUserService.getInstance().createUser(newUser);
            javafx.application.Platform.runLater(() -> {
                if (success) {
                    AdminUserStore.getInstance().addUser(newUser);
                    showInfo("New user added successfully!");
                    handleResetForm(null);
                    handleRefreshTable(null);
                } else {
                    showError("An error occurred while adding a new user from the system!");
                }
            });
        }).start();
    }

    @FXML
    void handleEdit(ActionEvent event) {
        UserDTO selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showError("Please select the user to edit!");
            return;
        }

        String fullname = txtFullname.getText().trim();
        String username = txtUsername.getText().trim();
        String availableBalanceStr = txtAvailableBalance.getText().trim();
        String frozenBalanceStr = txtFrozenBalance.getText().trim();
        String roleSelection = cbRoles.getValue();

        if (fullname.isEmpty() || username.isEmpty() || availableBalanceStr.isEmpty() || frozenBalanceStr.isEmpty() || roleSelection == null) {
            showError("Please fill in all information!");
            return;
        }

        BigDecimal availableBalance;
        BigDecimal frozenBalance;
        try {
            availableBalance = new BigDecimal(availableBalanceStr);
            frozenBalance = new BigDecimal(frozenBalanceStr);
        } catch (NumberFormatException e) {
            showError("Available Balance and Frozen Balance must be valid numbers!");
            return;
        }

        Set<UserRole> roles = new HashSet<>();
        if ("ADMIN".equals(roleSelection)) {
            roles.add(UserRole.ADMIN);
        } else if ("BIDDER/SELLER".equals(roleSelection)) {
            roles.add(UserRole.BIDDER);
            roles.add(UserRole.SELLER);
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Update");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to update this user's information?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            selectedUser.setFullname(fullname);
            selectedUser.setUsername(username);
            selectedUser.setAvailableBalance(availableBalance);
            selectedUser.setFrozenBalance(frozenBalance);
            selectedUser.setRoles(roles);
            
            String password = txtPassword.getText().trim();
            if (!password.isEmpty()) {
                selectedUser.setPassword(password);
            } else {
                selectedUser.setPassword(null);
            }

            // Disable button to prevent double-submit
            btnEdit.setDisable(true);

            // Perform network I/O on background thread
            UserDTO userToEdit = selectedUser;
            new Thread(() -> {
                boolean success = AdminUserService.getInstance().updateUser(userToEdit);

                javafx.application.Platform.runLater(() -> {
                    btnEdit.setDisable(false);
                    if (success) {
                        AdminUserStore.getInstance().updateUser(userToEdit);
                        userTable.refresh();
                        showInfo("User information updated successfully!");
                        handleResetForm(null);
                    } else {
                        showError("Failed to update information from system!");
                    }
                });
            }, "admin-edit-user-worker").start();
        }
    }

    @FXML
    void handleBan(ActionEvent event) {
        UserDTO selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showError("Please select the user to ban!");
            return;
        }

        if (selectedUser.getStatus() == auction_system.common.enums.UserStatus.BANNED) {
            showError("This user's account has already been banned!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Account Ban");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to ban the account '" + selectedUser.getUsername() + "'?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Disable button to prevent double-click
            btnBan.setDisable(true);

            // Perform network I/O on background thread
            UserDTO userToBan = selectedUser;
            new Thread(() -> {
                boolean success = AdminUserService.getInstance().banUser(userToBan.getId());

                javafx.application.Platform.runLater(() -> {
                    btnBan.setDisable(false);
                    if (success) {
                        userToBan.setStatus(auction_system.common.enums.UserStatus.BANNED);
                        AdminUserStore.getInstance().updateUser(userToBan);
                        userTable.refresh();
                        showInfo("User account banned successfully!");
                        handleResetForm(null);
                    } else {
                        showError("Failed to ban user account!");
                    }
                });
            }, "admin-ban-user-worker").start();
        }
    }

    @FXML
    void handleRefreshTable(ActionEvent event) {
        // Perform data loading on background thread
        new Thread(() -> {
            try {
                List<UserDTO> dbUsers = AdminUserService.getInstance().getAllUsers();
                javafx.application.Platform.runLater(() -> {
                    AdminUserStore.getInstance().setUsers(dbUsers);
                    userTable.refresh();
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> showError("Failed to load user list from server!"));
            }
        }, "admin-refresh-users-worker").start();
    }

    @FXML
    void handleResetForm(ActionEvent event) {
        txtFullname.clear();
        txtUsername.clear();
        txtPassword.clear();
        cbRoles.getSelectionModel().clearSelection();
        txtAvailableBalance.clear();
        txtFrozenBalance.setText("0");
        
        txtUsername.setEditable(true);
        txtFrozenBalance.setDisable(true);
        cbRoles.setDisable(false);
        
        userTable.getSelectionModel().clearSelection();
        btnAdd.setDisable(false);
        btnEdit.setDisable(true);
        btnBan.setDisable(true);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notification");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}