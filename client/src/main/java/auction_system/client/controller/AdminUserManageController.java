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
                txtFrozenBalance.setEditable(false);
                cbRoles.setDisable(true);

                btnAdd.setDisable(true);
                btnEdit.setDisable(false);
                btnBan.setDisable(false);
            } else {
                txtUsername.setEditable(true);
                txtFrozenBalance.setEditable(true);
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
            showError("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        BigDecimal availableBalance;
        BigDecimal frozenBalance;
        try {
            availableBalance = new BigDecimal(availableBalanceStr);
            frozenBalance = new BigDecimal(frozenBalanceStr);
        } catch (NumberFormatException e) {
            showError("Available Balance và Frozen Balance phải là một số hợp lệ!");
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
                    showInfo("Thêm người dùng mới thành công!");
                    handleResetForm(null);
                    handleRefreshTable(null);
                } else {
                    showError("Có lỗi xảy ra khi thêm người dùng mới từ hệ thống!");
                }
            });
        }).start();
    }

    @FXML
    void handleEdit(ActionEvent event) {
        UserDTO selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showError("Vui lòng chọn người dùng cần chỉnh sửa!");
            return;
        }

        String fullname = txtFullname.getText().trim();
        String username = txtUsername.getText().trim();
        String availableBalanceStr = txtAvailableBalance.getText().trim();
        String frozenBalanceStr = txtFrozenBalance.getText().trim();
        String roleSelection = cbRoles.getValue();

        if (fullname.isEmpty() || username.isEmpty() || availableBalanceStr.isEmpty() || frozenBalanceStr.isEmpty() || roleSelection == null) {
            showError("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        BigDecimal availableBalance;
        BigDecimal frozenBalance;
        try {
            availableBalance = new BigDecimal(availableBalanceStr);
            frozenBalance = new BigDecimal(frozenBalanceStr);
        } catch (NumberFormatException e) {
            showError("Available Balance và Frozen Balance phải là một số hợp lệ!");
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
        confirm.setTitle("Xác nhận cập nhật");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc muốn cập nhật thông tin người dùng này?");
        
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

            // Disable nút để tránh double-submit
            btnEdit.setDisable(true);

            // Thực hiện network I/O trên background thread
            UserDTO userToEdit = selectedUser;
            new Thread(() -> {
                boolean success = AdminUserService.getInstance().updateUser(userToEdit);

                javafx.application.Platform.runLater(() -> {
                    btnEdit.setDisable(false);
                    if (success) {
                        AdminUserStore.getInstance().updateUser(userToEdit);
                        userTable.refresh();
                        showInfo("Cập nhật thông tin người dùng thành công!");
                        handleResetForm(null);
                    } else {
                        showError("Cập nhật thông tin thất bại từ hệ thống!");
                    }
                });
            }, "admin-edit-user-worker").start();
        }
    }

    @FXML
    void handleBan(ActionEvent event) {
        UserDTO selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showError("Vui lòng chọn người dùng cần khóa!");
            return;
        }

        if (selectedUser.getStatus() == auction_system.common.enums.UserStatus.BANNED) {
            showError("Người dùng này đã bị khóa tài khoản từ trước!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận khóa tài khoản");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc chắn muốn khóa tài khoản '" + selectedUser.getUsername() + "' không?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Disable nút để tránh double-click
            btnBan.setDisable(true);

            // Thực hiện network I/O trên background thread
            UserDTO userToBan = selectedUser;
            new Thread(() -> {
                boolean success = AdminUserService.getInstance().banUser(userToBan.getId());

                javafx.application.Platform.runLater(() -> {
                    btnBan.setDisable(false);
                    if (success) {
                        userToBan.setStatus(auction_system.common.enums.UserStatus.BANNED);
                        AdminUserStore.getInstance().updateUser(userToBan);
                        userTable.refresh();
                        showInfo("Khóa tài khoản người dùng thành công!");
                        handleResetForm(null);
                    } else {
                        showError("Khóa tài khoản người dùng thất bại!");
                    }
                });
            }, "admin-ban-user-worker").start();
        }
    }

    @FXML
    void handleRefreshTable(ActionEvent event) {
        // Thực hiện tải dữ liệu trên background thread
        new Thread(() -> {
            try {
                List<UserDTO> dbUsers = AdminUserService.getInstance().getAllUsers();
                javafx.application.Platform.runLater(() -> {
                    AdminUserStore.getInstance().setUsers(dbUsers);
                    userTable.refresh();
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> showError("Không thể tải danh sách người dùng từ máy chủ!"));
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
        txtFrozenBalance.clear();
        
        txtUsername.setEditable(true);
        txtFrozenBalance.setEditable(true);
        cbRoles.setDisable(false);
        
        userTable.getSelectionModel().clearSelection();
        btnAdd.setDisable(false);
        btnEdit.setDisable(true);
        btnBan.setDisable(true);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}