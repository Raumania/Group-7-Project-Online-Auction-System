package auction_system.server.service;

import auction_system.common.enums.UserRole;
import auction_system.server.exception.AuthorizationException;
import auction_system.server.exception.ControllerException.*;
import auction_system.server.model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private UserService userService;
    private List<Integer> createdUserIds;

    // Chỉ giữ fullname và password cố định vì chúng không gây duplicate
    private static final String VALID_FULLNAME = "Nguyen Van A";
    private static final String VALID_PASSWORD = "secret123";

    // Mỗi lần gọi sinh ra username khác nhau → không bao giờ duplicate
    private String uniqueUsername() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Helper tạo user nhanh với username động
    private User createValidUser() {
        return trackUser(userService.registerUser(VALID_FULLNAME, uniqueUsername(), VALID_PASSWORD));
    }

    private void assertBigDecimalValueEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private User trackUser(User user) {
        if (user != null && user.getId() > 0) {
            createdUserIds.add(user.getId());
        }
        return user;
    }

    @BeforeEach
    void setUp() {
        userService = UserService.getInstance();
        createdUserIds = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        for (int userId : createdUserIds) {
            try {
                userService.removeUser(userId);
            } catch (RuntimeException ignored) {
            }
        }
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 1: registerUser
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("registerUser()")
    class RegisterUserTests {

        @Test
        @DisplayName("EP: Đăng ký hợp lệ → trả về User không null")
        void register_validInput_shouldReturnUser() {
            // ✅ username riêng biệt, không đụng test khác
            User user = trackUser(userService.registerUser(
                    VALID_FULLNAME, uniqueUsername(), VALID_PASSWORD
            ));

            assertNotNull(user);
        }

        @Test
        @DisplayName("EP: Đăng ký hợp lệ → mặc định có role BIDDER và SELLER")
        void register_validInput_shouldHaveDefaultRoles() {
            // ✅ username riêng biệt
            User user = trackUser(userService.registerUser(
                    VALID_FULLNAME, uniqueUsername(), VALID_PASSWORD
            ));

            assertTrue(user.hasRole(UserRole.BIDDER));
            assertTrue(user.hasRole(UserRole.SELLER));
        }

        @Test
        @DisplayName("EP: Username đã tồn tại → throw DuplicateResourceException")
        void register_duplicateUsername_shouldThrow() {
            // ✅ tạo 1 username cố định CHỈ trong scope test này
            String sharedUsername = uniqueUsername();

            trackUser(userService.registerUser(VALID_FULLNAME, sharedUsername, VALID_PASSWORD));

            // Lần 2 cùng username → phải throw
            assertThrows(DuplicateResourceException.class, () ->
                    userService.registerUser("Khac Ten", sharedUsername, "otherpass")
            );
        }

        @Test
        @DisplayName("EP: Roles rỗng → throw InvalidInputException")
        void register_emptyRoles_shouldThrow() {
            assertThrows(InvalidInputException.class, () ->
                    userService.registerUser(
                            VALID_FULLNAME, uniqueUsername(), VALID_PASSWORD,
                            Set.of()
                    )
            );
        }

        @Test
        @DisplayName("EP: Roles null → throw InvalidInputException")
        void register_nullRoles_shouldThrow() {
            assertThrows(InvalidInputException.class, () ->
                    userService.registerUser(
                            VALID_FULLNAME, uniqueUsername(), VALID_PASSWORD,
                            null
                    )
            );
        }

        @Test
        @DisplayName("BVA: Đăng ký đúng 1 role (biên dưới hợp lệ) → thành công")
        void register_exactlyOneRole_shouldSucceed() {
            User user = trackUser(userService.registerUser(
                    VALID_FULLNAME, uniqueUsername(), VALID_PASSWORD,
                    Set.of(UserRole.BIDDER)
            ));

            assertNotNull(user);
            assertTrue(user.hasRole(UserRole.BIDDER));
        }

        @ParameterizedTest
        @DisplayName("Combinatorial: mỗi role đơn lẻ đều đăng ký được")
        @CsvSource({"BIDDER", "SELLER", "ADMIN"})
        void register_singleRoleCombinations_shouldSucceed(String roleName) {
            UserRole role = UserRole.valueOf(roleName);

            // ✅ mỗi lần parameterized chạy đều có username riêng
            User user = trackUser(userService.registerUser(
                    VALID_FULLNAME, uniqueUsername(), VALID_PASSWORD,
                    Set.of(role)
            ));

            assertTrue(user.hasRole(role));
        }
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 2: login
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("login()")
    class LoginTests {

        // ✅ username và userId chỉ sống trong Nested class này
        private String testUsername;

        @BeforeEach
        void createLoginTestUser() {
            // Mỗi test trong nhóm này tạo user riêng
            testUsername = uniqueUsername();
            trackUser(userService.registerUser(VALID_FULLNAME, testUsername, VALID_PASSWORD));
        }

        @Test
        @DisplayName("EP: Đúng username + đúng password → trả về User")
        void login_correctCredentials_shouldReturnUser() {
            User user = userService.login(testUsername, VALID_PASSWORD);

            assertNotNull(user);
            assertEquals(testUsername, user.getUsername());
        }

        @Test
        @DisplayName("EP: Username không tồn tại → throw AuthenticationException")
        void login_wrongUsername_shouldThrow() {
            // ✅ dùng username chắc chắn không tồn tại
            assertThrows(AuthenticationException.class, () ->
                    userService.login("username_khong_ton_tai_xyz", VALID_PASSWORD)
            );
        }

        @Test
        @DisplayName("EP: Đúng username nhưng sai password → throw AuthenticationException")
        void login_wrongPassword_shouldThrow() {
            assertThrows(AuthenticationException.class, () ->
                    userService.login(testUsername, "satPassword")
            );
        }

        @Test
        @DisplayName("BVA: Password đúng nhưng thêm 1 ký tự cuối → sai")
        void login_passwordWithExtraChar_shouldFail() {
            assertThrows(AuthenticationException.class, () ->
                    userService.login(testUsername, VALID_PASSWORD + "x")
            );
        }

        @Test
        @DisplayName("BVA: Password đúng nhưng thiếu 1 ký tự cuối → sai")
        void login_passwordMissingLastChar_shouldFail() {
            String truncated = VALID_PASSWORD.substring(0, VALID_PASSWORD.length() - 1);

            assertThrows(AuthenticationException.class, () ->
                    userService.login(testUsername, truncated)
            );
        }

        @ParameterizedTest
        @DisplayName("Combinatorial: mọi tổ hợp sai đều throw")
        @CsvSource({
                "sai_user,  secret123",      // sai username, đúng password
                "sai_user,  satpassword"     // cả hai sai
                // ✅ bỏ case "đúng user + sai pass" vì testUsername là động
                //    → đã cover bởi login_wrongPassword_shouldThrow
        })
        void login_wrongCombinations_shouldThrow(String username, String password) {
            assertThrows(AuthenticationException.class, () ->
                    userService.login(username.trim(), password.trim())
            );
        }
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 3: deposit / withdraw
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("deposit() và withdraw()")
    class BalanceTests {

        // ✅ mỗi test trong nhóm có user riêng qua @BeforeEach
        private int testUserId;

        @BeforeEach
        void createBalanceTestUser() {
            User user = createValidUser(); // helper dùng uniqueUsername()
            testUserId = user.getId();
            System.out.println(testUserId);
        }

        @Test
        @DisplayName("EP: Nạp số tiền hợp lệ → số dư tăng đúng")
        void deposit_validAmount_shouldIncreaseBalance() {
            userService.deposit(testUserId, new BigDecimal("100000"));

            User user = userService.getUserById(testUserId);
            assertBigDecimalValueEquals("100000", user.getBalance());
        }

        @Test
        @DisplayName("BVA: Nạp đúng 1 đồng (biên dưới hợp lệ) → thành công")
        void deposit_minimumAmount_shouldSucceed() {
            assertDoesNotThrow(() ->
                    userService.deposit(testUserId, new BigDecimal("1"))
            );
        }

        @Test
        @DisplayName("BVA: Nạp 0 đồng → throw exception")
        void deposit_zeroAmount_shouldThrow() {
            assertThrows(Exception.class, () ->
                    userService.deposit(testUserId, BigDecimal.ZERO)
            );
        }

        @Test
        @DisplayName("BVA: Nạp số âm → throw exception")
        void deposit_negativeAmount_shouldThrow() {
            assertThrows(Exception.class, () ->
                    userService.deposit(testUserId, new BigDecimal("-1"))
            );
        }

        @Test
        @DisplayName("EP: Rút ít hơn số dư → thành công")
        void withdraw_validAmount_shouldSucceed() {
            userService.deposit(testUserId, new BigDecimal("500000"));

            assertDoesNotThrow(() ->
                    userService.withdraw(testUserId, new BigDecimal("200000"))
            );
        }

        @Test
        @DisplayName("BVA: Rút đúng bằng số dư → thành công")
        void withdraw_exactBalance_shouldSucceed() {
            userService.deposit(testUserId, new BigDecimal("100000"));

            assertDoesNotThrow(() ->
                    userService.withdraw(testUserId, new BigDecimal("100000"))
            );
        }

        @Test
        @DisplayName("BVA: Rút vượt số dư 1 đồng → throw exception")
        void withdraw_oneAboveBalance_shouldThrow() {
            userService.deposit(testUserId, new BigDecimal("100000"));

            assertThrows(Exception.class, () ->
                    userService.withdraw(testUserId, new BigDecimal("100001"))
            );
        }

        @ParameterizedTest
        @DisplayName("Combinatorial: deposit → withdraw → kiểm tra số dư còn lại")
        @CsvSource({
                "100000, 50000, 50000",
                "100000, 100000, 0",
                "50000,  10000, 40000"
        })
        void depositThenWithdraw_shouldLeaveCorrectBalance(
                String depositAmt, String withdrawAmt, String expectedBalance) {

            // ✅ mỗi parameterized case dùng chung testUserId
            //    nhưng @BeforeEach đã tạo user mới trước mỗi case → an toàn
            userService.deposit(testUserId, new BigDecimal(depositAmt));
            userService.withdraw(testUserId, new BigDecimal(withdrawAmt));

            User user = userService.getUserById(testUserId);
            assertBigDecimalValueEquals(expectedBalance, user.getBalance());
        }
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 4: getSellerById / getAdminById
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("getSellerById() và getAdminById()")
    class RoleCheckTests {

        @Test
        @DisplayName("EP: User có role SELLER → getSellerById thành công")
        void getSellerById_userIsSeller_shouldReturn() {
            User seller = trackUser(userService.createSeller(
                    VALID_FULLNAME, uniqueUsername(), VALID_PASSWORD
            ));

            assertNotNull(userService.getSellerById(seller.getId()));
        }

        @Test
        @DisplayName("EP: User chỉ có BIDDER → getSellerById throw AuthorizationException")
        void getSellerById_userIsNotSeller_shouldThrow() {
            User bidder = trackUser(userService.createBidder(
                    VALID_FULLNAME, uniqueUsername(), VALID_PASSWORD
            ));

            assertThrows(AuthorizationException.class, () ->
                    userService.getSellerById(bidder.getId())
            );
        }

        @Test
        @DisplayName("EP: User có role ADMIN → getAdminById thành công")
        void getAdminById_userIsAdmin_shouldReturn() {
            User admin = trackUser(userService.createAdmin(
                    VALID_FULLNAME, uniqueUsername(), VALID_PASSWORD
            ));

            assertNotNull(userService.getAdminById(admin.getId()));
        }

        @Test
        @DisplayName("EP: User không có ADMIN → getAdminById throw AuthorizationException")
        void getAdminById_userIsNotAdmin_shouldThrow() {
            User user = trackUser(userService.registerUser(
                    VALID_FULLNAME, uniqueUsername(), VALID_PASSWORD
            ));

            assertThrows(AuthorizationException.class, () ->
                    userService.getAdminById(user.getId())
            );
        }

        @Test
        @DisplayName("EP: ID không tồn tại → throw UserNotFoundException")
        void getSellerById_invalidId_shouldThrow() {
            assertThrows(UserNotFoundException.class, () ->
                    userService.getSellerById(99999)
            );
        }
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 5: removeUser
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("removeUser()")
    class RemoveUserTests {

        @Test
        @DisplayName("EP: Xóa user tồn tại → không throw")
        void removeUser_existingUser_shouldSucceed() {
            User user = createValidUser();

            assertDoesNotThrow(() -> userService.removeUser(user.getId()));
        }

        @Test
        @DisplayName("EP: Xóa user không tồn tại → throw UserNotFoundException")
        void removeUser_nonExistingUser_shouldThrow() {
            assertThrows(UserNotFoundException.class, () ->
                    userService.removeUser(99999)
            );
        }

        @Test
        @DisplayName("EP: Xóa xong → tìm lại theo ID → throw UserNotFoundException")
        void removeUser_thenGetById_shouldThrow() {
            User user = createValidUser();
            int id = user.getId();

            userService.removeUser(id);

            assertThrows(UserNotFoundException.class, () ->
                    userService.getUserById(id)
            );
        }
    }
}
