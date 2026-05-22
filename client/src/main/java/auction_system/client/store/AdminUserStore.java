package auction_system.client.store;

import auction_system.common.dto.UserDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

public class AdminUserStore {
    private static AdminUserStore instance;
    private final ObservableList<UserDTO> users = FXCollections.observableArrayList();

    private AdminUserStore() {
    }

    public static synchronized AdminUserStore getInstance() {
        if (instance == null) {
            instance = new AdminUserStore();
        }
        return instance;
    }

    public ObservableList<UserDTO> getUsers() {
        return users;
    }

    public void setUsers(List<UserDTO> newUsers) {
        users.setAll(newUsers);
    }

    public void addUser(UserDTO user) {
        users.add(user);
    }

    public void updateUser(UserDTO user) {
        int index = -1;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId() == user.getId()) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            users.set(index, user);
        }
    }

    public void removeUser(int userId) {
        users.removeIf(user -> user.getId() == userId);
    }
}
