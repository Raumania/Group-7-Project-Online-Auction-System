package auction_system.client.controller;

import auction_system.client.service.AdminUserService;
import auction_system.client.store.AdminUserStore;
import auction_system.common.dto.UserDTO;
import auction_system.common.enums.UserRole;
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
    @FXML private TableColumn<UserDTO, String> colPassword;
    @FXML private TableColumn<UserDTO, String> colRoles;
    @FXML private TableColumn<UserDTO, BigDecimal> colBalance;
    @FXML private TextField txtFullname;
    @FXML private TextField txtUsername;
    @FXML private TextField txtRoles;
    @FXML private TextField txtBalance;
    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    private final ObservableList<UserDTO> userList = AdminUserStore.getInstance().getUsers();
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupTableSelectionListener();
        setupBalanceInput();

        btnEdit.setDisable(true);
        btnDelete.setDisable(true);
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
        txtBalance.setTextFormatter(new TextFormatter<>(filter));
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFullname.setCellValueFactory(new PropertyValueFactory<>("fullname"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colPassword.setCellValueFactory(new PropertyValueFactory<>("password"));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));

        colBalance.setCellFactory(column -> new TableCell<UserDTO, BigDecimal>() {
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
                txtBalance.setText(newSelection.getBalance() != null ? newSelection.getBalance().toPlainString() : "");
                
                if (newSelection.getRoles() != null) {
                    String rolesStr = newSelection.getRoles().stream()
                             .map(UserRole::name)
                             .collect(Collectors.joining(", "));
                    txtRoles.setText(rolesStr);
                } else {
                    txtRoles.clear();
                }

                btnAdd.setDisable(true);
                btnEdit.setDisable(false);
                btnDelete.setDisable(false);
            }
        });
    }

    @FXML
    void handleAdd(ActionEvent event) {
        String fullname = txtFullname.getText().trim();
        String username = txtUsername.getText().trim();
        String balanceStr = txtBalance.getText().trim();
        String rolesStr = txtRoles.getText().trim();

        if (fullname.isEmpty() || username.isEmpty() || balanceStr.isEmpty() || rolesStr.isEmpty()) {
            showError("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        BigDecimal balance;
        try {
            balance = new BigDecimal(balanceStr);
        } catch (NumberFormatException e) {
            showError("Balance phải là một số hợp lệ!");
            return;
        }

        Set<UserRole> roles = parseRoles(rolesStr);
        if (roles.isEmpty()) {
            showError("Roles không hợp lệ. Vui lòng nhập: ADMIN, SELLER, hoặc BIDDER (cách nhau bởi dấu phẩy).");
            return;
        }

        UserDTO newUser = new UserDTO(0, fullname, username, roles, balance);
        newUser.setPassword("123456"); // Default initial password

        if (AdminUserService.getInstance().createUser(newUser)) {
            AdminUserStore.getInstance().addUser(newUser);
            showInfo("Thêm người dùng mới thành công!");
            handleResetForm(null);
            handleRefreshTable(null);
        } else {
            showError("Có lỗi xảy ra khi thêm người dùng mới từ hệ thống!");
        }
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
        String balanceStr = txtBalance.getText().trim();
        String rolesStr = txtRoles.getText().trim();

        if (fullname.isEmpty() || username.isEmpty() || balanceStr.isEmpty() || rolesStr.isEmpty()) {
            showError("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        BigDecimal balance;
        try {
            balance = new BigDecimal(balanceStr);
        } catch (NumberFormatException e) {
            showError("Balance phải là một số hợp lệ!");
            return;
        }

        Set<UserRole> roles = parseRoles(rolesStr);
        if (roles.isEmpty()) {
            showError("Roles không hợp lệ. Vui lòng nhập: ADMIN, SELLER, hoặc BIDDER (cách nhau bởi dấu phẩy).");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận cập nhật");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc muốn cập nhật thông tin người dùng này?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            selectedUser.setFullname(fullname);
            selectedUser.setUsername(username);
            selectedUser.setBalance(balance);
            selectedUser.setRoles(roles);
            
            if (AdminUserService.getInstance().updateUser(selectedUser)) {
                AdminUserStore.getInstance().updateUser(selectedUser);
                userTable.refresh();
                showInfo("Cập nhật thông tin người dùng thành công!");
                handleResetForm(null);
            } else {
                showError("Cập nhật thông tin thất bại từ hệ thống!");
            }
        }
    }

    @FXML
    void handleDelete(ActionEvent event) {
        UserDTO selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showError("Vui lòng chọn người dùng cần xóa!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc chắn muốn xóa người dùng '" + selectedUser.getUsername() + "' không?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (AdminUserService.getInstance().deleteUser(selectedUser.getId())) {
                AdminUserStore.getInstance().removeUser(selectedUser.getId());
                showInfo("Xóa người dùng thành công!");
                handleResetForm(null);
            } else {
                showError("Xóa người dùng thất bại từ hệ thống!");
            }
        }
    }

    @FXML
    void handleRefreshTable(ActionEvent event) {
        try {
            List<UserDTO> dbUsers = AdminUserService.getInstance().getAllUsers();
            AdminUserStore.getInstance().setUsers(dbUsers);
            userTable.refresh();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Không thể tải danh sách người dùng từ máy chủ!");
        }
    }

    @FXML
    void handleResetForm(ActionEvent event) {
        txtFullname.clear();
        txtUsername.clear();
        txtRoles.clear();
        txtBalance.clear();
        
        userTable.getSelectionModel().clearSelection();
        btnAdd.setDisable(false);
        btnEdit.setDisable(true);
        btnDelete.setDisable(true);
    }

    private Set<UserRole> parseRoles(String rolesStr) {
        Set<UserRole> roles = new HashSet<>();
        String[] parts = rolesStr.split(",");
        for (String part : parts) {
            try {
                roles.add(UserRole.valueOf(part.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid
            }
        }
        return roles;
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