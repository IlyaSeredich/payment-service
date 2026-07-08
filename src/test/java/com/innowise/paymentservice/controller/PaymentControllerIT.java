package com.innowise.paymentservice.controller;

import com.innowise.paymentservice.client.RandomNumFeignClient;
import com.innowise.paymentservice.dto.*;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.enumtype.PaymentStatus;
import com.innowise.paymentservice.repository.PaymentRepository;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class PaymentControllerIT {

    @Container
    protected static final KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:26.4.5")
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            .withStartupTimeout(Duration.ofMinutes(5));

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer =
            new MongoDBContainer("mongo:4.4");

    @Container
    @ServiceConnection
    static KafkaContainer kafkaContainer =
            new KafkaContainer(
                    DockerImageName.parse("apache/kafka-native:3.8.0")
            );

    @DynamicPropertySource
    private static void sourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> keycloakContainer.getAuthServerUrl() +
                        "/realms/test-realm/protocol/openid-connect/certs");
        registry.add("spring.mongodb.uri",
                () -> mongoDBContainer.getReplicaSetUrl());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private RandomNumFeignClient randomNumFeignClient;

    private static final String REALM = "test-realm";
    private static final String USER_CLIENT = "user-test-client";
    private static final String ADMIN_CLIENT = "admin-test-client";
    private static final String SECRET = "test-secret";
    private static final String USER_ROLE = "user";
    private static final String ADMIN_ROLE = "admin";

    @BeforeAll
    static void setupKeycloak() {
        String authUrl = keycloakContainer.getAuthServerUrl();

        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(authUrl)
                .realm("master")
                .clientId("admin-cli")
                .username("admin")
                .password("admin")
                .build();

        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM);
        realm.setEnabled(true);
        keycloak.realms().create(realm);

        ClientRepresentation userClient = new ClientRepresentation();
        userClient.setClientId(USER_CLIENT);
        userClient.setStandardFlowEnabled(true);
        userClient.setPublicClient(false);
        userClient.setSecret(SECRET);
        userClient.setServiceAccountsEnabled(true);

        ClientRepresentation adminClient = new ClientRepresentation();
        adminClient.setClientId(ADMIN_CLIENT);
        adminClient.setStandardFlowEnabled(true);
        adminClient.setPublicClient(false);
        adminClient.setSecret(SECRET);
        adminClient.setServiceAccountsEnabled(true);

        Response userResponse = keycloak.realm(REALM).clients().create(userClient);
        Response adminResponse = keycloak.realm(REALM).clients().create(adminClient);

        String userId = CreatedResponseUtil.getCreatedId(userResponse);
        String adminId = CreatedResponseUtil.getCreatedId(adminResponse);

        UserRepresentation userServiceAccountUser = keycloak.realm(REALM)
                .clients()
                .get(userId)
                .getServiceAccountUser();

        UserRepresentation adminServiceAccountUser = keycloak.realm(REALM)
                .clients()
                .get(adminId)
                .getServiceAccountUser();

        RoleRepresentation userRole = new RoleRepresentation();
        userRole.setName(USER_ROLE);
        keycloak.realm(REALM).roles().create(userRole);

        RoleRepresentation adminRole = new RoleRepresentation();
        adminRole.setName(ADMIN_ROLE);
        keycloak.realm(REALM).roles().create(adminRole);

        RoleRepresentation userRoleRepresentation = keycloak.realm(REALM).roles()
                .get(USER_ROLE)
                .toRepresentation();

        RoleRepresentation adminRoleRepresentation = keycloak.realm(REALM).roles()
                .get(ADMIN_ROLE)
                .toRepresentation();

        keycloak.realm(REALM).users()
                .get(userServiceAccountUser.getId())
                .roles()
                .realmLevel()
                .add(List.of(userRoleRepresentation));

        keycloak.realm(REALM).users()
                .get(adminServiceAccountUser.getId())
                .roles()
                .realmLevel()
                .add(List.of(adminRoleRepresentation));
    }

    @BeforeEach
    void setup() {
        when(randomNumFeignClient.getNum())
                .thenReturn(List.of(new RandomNumResponseDto(2L)));
    }

    @AfterEach
    void cleanDb() {
        paymentRepository.deleteAll();
    }

    @Test
    void shouldCreateSuccessfulPaymentWhenItHasAdminRole() throws Exception {
        PaymentCreateDto request = new PaymentCreateDto(
                1L,
                UUID.randomUUID(),
                BigDecimal.valueOf(100)
        );

        mockMvc.perform(
                        post("/api/payments")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + getAdminAccessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated());


        List<Payment> payments = paymentRepository.findAll();

        assertThat(payments).hasSize(1);
    }

    @Test
    void shouldNotCreatePaymentWhenItHasNotAdminRole() throws Exception {
        PaymentCreateDto request = new PaymentCreateDto(
                1L,
                UUID.randomUUID(),
                BigDecimal.valueOf(100)
        );

        mockMvc.perform(
                        post("/api/payments")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + getUserAccessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldGetAllUsersPayments() throws Exception {
        createPayment();

        MvcResult result = mockMvc.perform(
                        get("/api/payments/my")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + getUserAccessToken()
                                ))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        PagePaymentResponseDto pagePaymentResponseDto =
                objectMapper.readValue(content, PagePaymentResponseDto.class);

        assertThat(pagePaymentResponseDto.totalElements()).isEqualTo(1L);
    }

    @Test
    void shouldRejectGettingPaymentsByUserIdWhenItHasNotAdminRole() throws Exception {
        mockMvc.perform(
                        get("/api/payments/users")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + getUserAccessToken()
                                ))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    @Test
    void shouldGetPaymentsByUserIdWhenItHasAdminRole() throws Exception {
        PaymentResponseDto payment = createPayment();

        UUID uuid = payment.userId();

        MvcResult result = mockMvc.perform(
                        get("/api/payments/users")
                                .param("userId", uuid.toString())
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + getAdminAccessToken()
                                ))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        PagePaymentResponseDto pagePaymentResponseDto =
                objectMapper.readValue(content, PagePaymentResponseDto.class);

        assertThat(pagePaymentResponseDto.totalElements()).isEqualTo(1L);
    }

    @Test
    void shouldRejectGettingPaymentsByOrderIdWhenItHasNotAdminRole() throws Exception {
        mockMvc.perform(
                        get("/api/payments/orders")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + getUserAccessToken()
                                ))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    @Test
    void shouldGetPaymentsByOrderIdWhenItHasAdminRole() throws Exception {
        createPayment();

        MvcResult result = mockMvc.perform(
                        get("/api/payments/orders")
                                .param("orderId", "1")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + getAdminAccessToken()
                                ))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        PagePaymentResponseDto pagePaymentResponseDto =
                objectMapper.readValue(content, PagePaymentResponseDto.class);

        assertThat(pagePaymentResponseDto.totalElements()).isEqualTo(1L);
    }

    @Test
    void shouldRejectGettingPaymentsByStatusWhenItHasNotAdminRole() throws Exception {
        mockMvc.perform(
                        get("/api/payments/statuses")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + getUserAccessToken()
                                ))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    @Test
    void shouldGetPaymentsByStatusWhenItHasAdminRole() throws Exception {
        createPayment();

        MvcResult result = mockMvc.perform(
                        get("/api/payments/statuses")
                                .param("status", "SUCCESS")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + getAdminAccessToken()
                                ))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        PagePaymentResponseDto pagePaymentResponseDto =
                objectMapper.readValue(content, PagePaymentResponseDto.class);

        assertThat(pagePaymentResponseDto.totalElements()).isEqualTo(1L);
    }

    @Test
    void shouldGetUsersSum() throws Exception {
        UUID uuid = UUID.fromString(extractSubject(getUserAccessToken()));
        LocalDateTime time1 = LocalDateTime.of(2000, 1, 1, 1, 1);
        LocalDateTime time2 = LocalDateTime.of(2001, 1, 1, 1, 1);

        Payment payment1 = new Payment();
        payment1.setOrderId(1L);
        payment1.setUserId(uuid);
        payment1.setTimestamp(time1);
        payment1.setStatus(PaymentStatus.SUCCESS);
        payment1.setPaymentAmount(BigDecimal.valueOf(100L));

        Payment payment2 = new Payment();
        payment2.setOrderId(2L);
        payment2.setUserId(uuid);
        payment2.setTimestamp(time2);
        payment2.setStatus(PaymentStatus.SUCCESS);
        payment2.setPaymentAmount(BigDecimal.valueOf(100L));

        paymentRepository.saveAll(List.of(payment1, payment2));

        PaymentRangeDateDto paymentRangeDateDto = new PaymentRangeDateDto(time1, time2);


        MvcResult result = mockMvc.perform(
                        get("/api/payments/my/total")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + getUserAccessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(paymentRangeDateDto))
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        SumResponseDto sumResponseDto = objectMapper.readValue(content, SumResponseDto.class);

        assertThat(sumResponseDto.totalSum()).isEqualTo(BigDecimal.valueOf(200L));
    }

    @Test
    void shouldGetAllUsersSum() throws Exception {
        UUID uuid = UUID.fromString(extractSubject(getUserAccessToken()));
        LocalDateTime time1 = LocalDateTime.of(2000, 1, 1, 1, 1);
        LocalDateTime time2 = LocalDateTime.of(2001, 1, 1, 1, 1);

        Payment payment1 = new Payment();
        payment1.setOrderId(1L);
        payment1.setUserId(uuid);
        payment1.setTimestamp(time1);
        payment1.setStatus(PaymentStatus.SUCCESS);
        payment1.setPaymentAmount(BigDecimal.valueOf(100L));

        Payment payment2 = new Payment();
        payment2.setOrderId(2L);
        payment2.setUserId(uuid);
        payment2.setTimestamp(time2);
        payment2.setStatus(PaymentStatus.SUCCESS);
        payment2.setPaymentAmount(BigDecimal.valueOf(100L));

        paymentRepository.saveAll(List.of(payment1, payment2));

        PaymentRangeDateDto paymentRangeDateDto = new PaymentRangeDateDto(time1, time2);


        MvcResult result = mockMvc.perform(
                        get("/api/payments/total")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + getAdminAccessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(paymentRangeDateDto))
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        SumResponseDto sumResponseDto = objectMapper.readValue(content, SumResponseDto.class);

        assertThat(sumResponseDto.totalSum()).isEqualTo(BigDecimal.valueOf(200L));
    }

    private PaymentResponseDto createPayment() throws Exception {
        PaymentCreateDto request = new PaymentCreateDto(
                1L,
                UUID.fromString(extractSubject(getUserAccessToken())),
                BigDecimal.valueOf(100)
        );

        MvcResult result = mockMvc.perform(
                        post("/api/payments")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + getAdminAccessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andReturn();

        String content = result.getResponse().getContentAsString();


        PaymentResponseDto paymentResponseDto = objectMapper.readValue(content, PaymentResponseDto.class);

        return paymentResponseDto;
    }

    private String getUserAccessToken() {
        String tokenUrl = keycloakContainer.getAuthServerUrl()
                + "/realms/test-realm/protocol/openid-connect/token";

        Map<String, String> params = new HashMap<>();
        params.put("realm", REALM);
        params.put("client_id", USER_CLIENT);
        params.put("client_secret", SECRET);
        params.put("grant_type", OAuth2Constants.CLIENT_CREDENTIALS);


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = new RestTemplate().postForEntity(tokenUrl, request, Map.class);

        return (String) response.getBody().get("access_token");
    }

    private String extractSubject(String token) {
            String payload = token.split("\\.")[1];

            byte[] decoded = Base64.getUrlDecoder()
                    .decode(payload);

            try {
                Map<String, Object> claims = objectMapper.readValue(decoded, Map.class);

                return (String) claims.get("sub");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    private String getAdminAccessToken() {
        String tokenUrl = keycloakContainer.getAuthServerUrl()
                + "/realms/test-realm/protocol/openid-connect/token";

        Map<String, String> params = new HashMap<>();
        params.put("realm", "test-realm");
        params.put("client_id", "admin-test-client");
        params.put("client_secret", SECRET);
        params.put("grant_type", OAuth2Constants.CLIENT_CREDENTIALS);


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = new RestTemplate().postForEntity(tokenUrl, request, Map.class);

        return (String) response.getBody().get("access_token");
    }

}