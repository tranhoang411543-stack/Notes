package com.fini.todo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fini.todo.repository.CategoryRepository;
import com.fini.todo.repository.DeviceRepository;
import com.fini.todo.repository.PasswordResetTokenRepository;
import com.fini.todo.repository.TaskRepository;
import com.fini.todo.repository.UserRepository;
import com.fini.todo.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class TodoApiIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoBean
    private MailService mailService;

    @BeforeEach
    void cleanDatabase() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        passwordResetTokenRepository.deleteAll();
        taskRepository.deleteAll();
        categoryRepository.deleteAll();
        deviceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void authAndPasswordResetApisWork() throws Exception {
        AuthSession session = register("authUser", "Auth@Example.com", "secret123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map(
                                "emailOrUsername", "AUTH@example.com",
                                "password", "secret123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value("auth@example.com"));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map("email", "AUTH@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If this email exists, an OTP has been sent"));

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendOtpEmail(eq("auth@example.com"), otpCaptor.capture());
        String otp = otpCaptor.getValue();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map(
                                "email", "auth@example.com",
                                "otp", otp,
                                "newPassword", "secret123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password must be different from current password"));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map(
                                "email", "auth@example.com",
                                "otp", otp,
                                "newPassword", "newSecret123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password has been reset successfully"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map(
                                "emailOrUsername", session.username(),
                                "password", "newSecret123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void otpResetPasswordBruteForceProtectionWorks() throws Exception {
        AuthSession session = register("otpUser", "otp@example.com", "secret123");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map("email", "otp@example.com"))))
                .andExpect(status().isOk());

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendOtpEmail(eq("otp@example.com"), otpCaptor.capture());
        String correctOtp = otpCaptor.getValue();

        // 4 failed attempts throw "Invalid OTP"
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(map(
                                    "email", "otp@example.com",
                                    "otp", "999999", // wrong OTP
                                    "newPassword", "newSecret123"
                            ))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid OTP"));
        }

        // 5th failed attempt throws "OTP has been invalidated due to too many failed attempts"
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map(
                                "email", "otp@example.com",
                                "otp", "999999", // wrong OTP
                                "newPassword", "newSecret123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("OTP has been invalidated due to too many failed attempts"));

        // 6th attempt with the CORRECT OTP should fail with "Invalid OTP" because the token is already marked as used
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map(
                                "email", "otp@example.com",
                                "otp", correctOtp,
                                "newPassword", "newSecret123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid OTP"));
    }

    @Test
    void categoryAndTaskApisSupportCrudFilterSearchReminderRepeatAndLocation() throws Exception {
        AuthSession session = register("taskUser", "task@example.com", "secret123");
        String token = session.token();

        String workCategoryId = createCategory(token, " Work ", "#4285F4");
        String homeCategoryId = createCategory(token, "Home", "#0F9D58");

        mockMvc.perform(get("/api/categories/{id}", workCategoryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Work"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(put("/api/categories/{id}", homeCategoryId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map(
                                "name", "work",
                                "color", "#111111"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Category name already exists"));

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atTime(9, 0);
        LocalDateTime todayDue = today.atTime(23, 59, 59);
        LocalDateTime farFutureDue = today.plusWeeks(2).atTime(10, 0);

        String taskId = createTask(token, map(
                "categoryId", workCategoryId,
                "title", " Team meeting ",
                "note", "Discuss Android and car sync",
                "dueAt", todayDue.toString(),
                "reminderEnabled", true,
                "repeatType", "weekly",
                "repeatDays", "mon, wed",
                "hasLocation", true,
                "latitude", 10.762622,
                "longitude", 106.660172,
                "locationName", "Office",
                "address", "Ho Chi Minh City"
        ));

        createTask(token, map(
                "categoryId", homeCategoryId,
                "title", "Buy groceries",
                "dueAt", farFutureDue.toString(),
                "reminderEnabled", false,
                "repeatType", "NONE",
                "hasLocation", false
        ));

        mockMvc.perform(get("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("dateFilter", "TODAY")
                        .param("categoryId", workCategoryId)
                        .param("keyword", "meeting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(taskId))
                .andExpect(jsonPath("$[0].repeatDays").value("MON,WED"))
                .andExpect(jsonPath("$[0].hasLocation").value(true));

        mockMvc.perform(get("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("dateFilter", "THIS_WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Team meeting"));

        mockMvc.perform(patch("/api/tasks/{id}/complete", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("completed", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map(
                                "categoryId", workCategoryId,
                                "title", "Updated meeting",
                                "dueAt", todayDue.toString(),
                                "reminderEnabled", false,
                                "repeatType", "NONE",
                                "repeatDays", "MONDAY",
                                "hasLocation", false,
                                "latitude", 10.0,
                                "longitude", 106.0
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repeatDays").value(nullValue()))
                .andExpect(jsonPath("$.latitude").value(nullValue()))
                .andExpect(jsonPath("$.version").value(3));
        mockMvc.perform(patch("/api/tasks/{id}/trash", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Buy groceries"));

        mockMvc.perform(get("/api/tasks/trash")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(taskId))
                .andExpect(jsonPath("$[0].trashedAt").isNotEmpty());

        mockMvc.perform(patch("/api/tasks/{id}/restore", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trashedAt").value(nullValue()));
    }

    @Test
    void syncApiStoresDeviceAndMovesChangesBetweenPhoneAndCar() throws Exception {
        AuthSession session = register("syncUser", "sync@example.com", "secret123");
        String token = session.token();

        String categoryId = UUID.randomUUID().toString();
        String taskId = UUID.randomUUID().toString();
        LocalDateTime dueAt = LocalDate.now().plusDays(1).atTime(8, 30);

        MvcResult firstSync = mockMvc.perform(post("/api/sync")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map(
                                "deviceType", "PHONE",
                                "deviceName", "Pixel Emulator",
                                "fcmToken", "phone-token",
                                "changedCategories", List.of(map(
                                        "id", categoryId,
                                        "name", "Errands",
                                        "color", "#DB4437",
                                        "deleted", false
                                )),
                                "changedTasks", List.of(map(
                                        "id", taskId,
                                        "categoryId", categoryId,
                                        "title", "Pick up package",
                                        "dueAt", dueAt.toString(),
                                        "reminderEnabled", true,
                                        "repeatType", "daily",
                                        "hasLocation", false,
                                        "deleted", false
                                ))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").isNotEmpty())
                .andExpect(jsonPath("$.categories", hasSize(1)))
                .andExpect(jsonPath("$.tasks", hasSize(1)))
                .andExpect(jsonPath("$.tasks[0].repeatType").value("DAILY"))
                .andReturn();

        JsonNode firstSyncJson = responseJson(firstSync);
        String phoneDeviceId = firstSyncJson.get("deviceId").asText();
        String serverTime = firstSyncJson.get("serverTime").asText();

        mockMvc.perform(post("/api/sync")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map(
                                "deviceId", phoneDeviceId,
                                "deviceType", "PHONE",
                                "lastSyncAt", serverTime,
                                "changedCategories", List.of(),
                                "changedTasks", List.of()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(0)))
                .andExpect(jsonPath("$.tasks", hasSize(0)));

        mockMvc.perform(post("/api/sync")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map(
                                "deviceType", "CAR",
                                "deviceName", "Car Emulator",
                                "fcmToken", "car-token",
                                "changedCategories", List.of(),
                                "changedTasks", List.of()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").isNotEmpty())
                .andExpect(jsonPath("$.categories", hasSize(1)))
                .andExpect(jsonPath("$.categories[0].id").value(categoryId))
                .andExpect(jsonPath("$.tasks", hasSize(1)))
                .andExpect(jsonPath("$.tasks[0].id").value(taskId));
    }


    private AuthSession register(String username, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map(
                                "username", username,
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode body = responseJson(result);
        return new AuthSession(
                body.get("accessToken").asText(),
                body.get("userId").asText(),
                body.get("username").asText()
        );
    }

    private String createCategory(String token, String name, String color) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(map(
                                "name", name,
                                "color", color
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        return responseJson(result).get("id").asText();
    }

    private String createTask(String token, Map<String, Object> payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return responseJson(result).get("id").asText();
    }

    private JsonNode responseJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Map<String, Object> map(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();

        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }

        return map;
    }

    private record AuthSession(String token, String userId, String username) {
    }
}
