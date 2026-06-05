package com.invoice;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoice.api.dto.CartItemDto;
import com.invoice.api.dto.DtoCheckout;
import com.invoice.api.dto.ProductDto;
import com.invoice.api.entity.Invoice;
import com.invoice.api.repository.RepoInvoice;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class InvoiceServiceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepoInvoice repo;

    @Autowired
    private com.invoice.api.repository.RepoCoupon repoCoupon;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RestTemplate restTemplate;

    @Value("${jwt.secret}")
    private String testSecret;

    private String customerTokenUser1;
    private String customerTokenUser2;
    private String adminToken;

    private final String activeGtin = "1234567890123";
    private final String activeGtin2 = "1111111111111";
    private final String notFoundGtin = "0000000000000";

    @BeforeEach
    void setup() {
        repo.deleteAll();
        repoCoupon.deleteAll();
        repoCoupon.save(new com.invoice.api.entity.Coupon("SAVE10", 10.0));

        // Generate tokens
        customerTokenUser1 = generateToken("customer1", 1, List.of("User"));
        customerTokenUser2 = generateToken("customer2", 2, List.of("User"));
        adminToken = generateToken("admin_user", 99, List.of("Administrator"));

        // Setup mock for product-service
        ProductDto activeProduct = new ProductDto();
        activeProduct.setGtin(activeGtin);
        activeProduct.setProduct("Smart TV");
        activeProduct.setPrice(500.0);
        activeProduct.setStock(10);
        activeProduct.setStatus(1);

        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://PRODUCT/product/gtin/" + activeGtin),
                Mockito.eq(HttpMethod.GET),
                Mockito.any(HttpEntity.class),
                Mockito.eq(ProductDto.class)
        )).thenReturn(new ResponseEntity<>(activeProduct, HttpStatus.OK));

        ProductDto activeProduct2 = new ProductDto();
        activeProduct2.setGtin(activeGtin2);
        activeProduct2.setProduct("HDMI Cable");
        activeProduct2.setPrice(10.0);
        activeProduct2.setStock(5);
        activeProduct2.setStatus(1);

        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://PRODUCT/product/gtin/" + activeGtin2),
                Mockito.eq(HttpMethod.GET),
                Mockito.any(HttpEntity.class),
                Mockito.eq(ProductDto.class)
        )).thenReturn(new ResponseEntity<>(activeProduct2, HttpStatus.OK));

        // Setup mock for product-service not found
        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://PRODUCT/product/gtin/" + notFoundGtin),
                Mockito.eq(HttpMethod.GET),
                Mockito.any(HttpEntity.class),
                Mockito.eq(ProductDto.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "El producto no existe"));
    }

    private String generateToken(String username, Integer id, List<String> roles) {
        Key key = Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(username)
                .claim("id", id)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // @spec INV-CHK-001, INV-CHK-004, INV-CHK-005, INV-CHK-006, INV-CHK-007
    @Test
    void testCheckoutSuccessWithoutBonus() throws Exception {
        // Setup mock cart: 2 Smart TVs for user 1
        CartItemDto[] cartItems = { new CartItemDto(1, 1, activeGtin, 2) };
        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://CART-SERVICE/cart-item/user/1"),
                Mockito.eq(HttpMethod.GET),
                Mockito.any(HttpEntity.class),
                Mockito.eq(CartItemDto[].class)
        )).thenReturn(new ResponseEntity<>(cartItems, HttpStatus.OK));

        // Mock product stock decrement
        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://PRODUCT/product/gtin/" + activeGtin + "/stock"),
                Mockito.eq(HttpMethod.PATCH),
                Mockito.any(HttpEntity.class),
                Mockito.eq(String.class)
        )).thenReturn(new ResponseEntity<>("Stock decremented", HttpStatus.OK));

        // Mock cart clear
        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://CART-SERVICE/cart-item/user/1"),
                Mockito.eq(HttpMethod.DELETE),
                Mockito.any(HttpEntity.class),
                Mockito.eq(String.class)
        )).thenReturn(new ResponseEntity<>("Cart cleared", HttpStatus.OK));

        mockMvc.perform(post("/invoice")
                .header("Authorization", "Bearer " + customerTokenUser1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("La factura ha sido registrada")));

        // Verify persisted invoice
        List<Invoice> invoices = repo.findAllByUserId(1);
        assertEquals(1, invoices.size());
        Invoice invoice = invoices.get(0);
        assertEquals(1000.0, invoice.getTotal());
        assertEquals(160.0, invoice.getTaxes());
        assertEquals(840.0, invoice.getSubtotal());
        assertEquals(1, invoice.getItems().size());
        assertEquals(activeGtin, invoice.getItems().get(0).getGtin());
        assertEquals(2, invoice.getItems().get(0).getQuantity());
    }

    // @spec INV-CHK-002
    @Test
    void testCheckoutEmptyCart() throws Exception {
        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://CART-SERVICE/cart-item/user/1"),
                Mockito.eq(HttpMethod.GET),
                Mockito.any(HttpEntity.class),
                Mockito.eq(CartItemDto[].class)
        )).thenReturn(new ResponseEntity<>(new CartItemDto[0], HttpStatus.OK));

        mockMvc.perform(post("/invoice")
                .header("Authorization", "Bearer " + customerTokenUser1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // @spec INV-CHK-003
    @Test
    void testCheckoutInsufficientStock() throws Exception {
        // Setup mock cart: 11 Smart TVs (stock is 10)
        CartItemDto[] cartItems = { new CartItemDto(1, 1, activeGtin, 11) };
        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://CART-SERVICE/cart-item/user/1"),
                Mockito.eq(HttpMethod.GET),
                Mockito.any(HttpEntity.class),
                Mockito.eq(CartItemDto[].class)
        )).thenReturn(new ResponseEntity<>(cartItems, HttpStatus.OK));

        mockMvc.perform(post("/invoice")
                .header("Authorization", "Bearer " + customerTokenUser1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());

        // Verify no invoice saved
        assertTrue(repo.findAllByUserId(1).isEmpty());
    }

    // @spec INV-CHK-008, INV-CHK-009
    @Test
    void testCheckoutWithBonusSuccess() throws Exception {
        // Setup mock cart: 2 Smart TVs for user 1
        CartItemDto[] cartItems = { new CartItemDto(1, 1, activeGtin, 2) };
        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://CART-SERVICE/cart-item/user/1"),
                Mockito.eq(HttpMethod.GET),
                Mockito.any(HttpEntity.class),
                Mockito.eq(CartItemDto[].class)
        )).thenReturn(new ResponseEntity<>(cartItems, HttpStatus.OK));

        // Mock product stock decrement
        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://PRODUCT/product/gtin/" + activeGtin + "/stock"),
                Mockito.eq(HttpMethod.PATCH),
                Mockito.any(HttpEntity.class),
                Mockito.eq(String.class)
        )).thenReturn(new ResponseEntity<>("Stock decremented", HttpStatus.OK));

        // Mock cart clear
        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://CART-SERVICE/cart-item/user/1"),
                Mockito.eq(HttpMethod.DELETE),
                Mockito.any(HttpEntity.class),
                Mockito.eq(String.class)
        )).thenReturn(new ResponseEntity<>("Cart cleared", HttpStatus.OK));

        DtoCheckout checkout = new DtoCheckout("123 Main St", "Credit Card", "SAVE10");

        mockMvc.perform(post("/invoice")
                .header("Authorization", "Bearer " + customerTokenUser1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(checkout)))
                .andExpect(status().isOk());

        List<Invoice> invoices = repo.findAllByUserId(1);
        assertEquals(1, invoices.size());
        Invoice invoice = invoices.get(0);
        
        assertEquals(900.0, invoice.getTotal());
        assertEquals(144.0, invoice.getTaxes());
        assertEquals(756.0, invoice.getSubtotal());
        
        assertEquals("123 Main St", invoice.getShipping_address());
        assertEquals("Credit Card", invoice.getPayment_method());
    }

    // @spec INV-SEC-001, INV-SEC-002
    @Test
    void testFindAllSecurity() throws Exception {
        // Setup two invoices for user 1 and user 2
        Invoice inv1 = new Invoice();
        inv1.setUser_id(1);
        inv1.setSubtotal(100.0);
        inv1.setTaxes(16.0);
        inv1.setTotal(116.0);
        inv1.setCreated_at("2026-06-05");
        inv1 = repo.save(inv1);

        Invoice inv2 = new Invoice();
        inv2.setUser_id(2);
        inv2.setSubtotal(200.0);
        inv2.setTaxes(32.0);
        inv2.setTotal(232.0);
        inv2.setCreated_at("2026-06-05");
        inv2 = repo.save(inv2);

        String id1 = String.valueOf(inv1.getInvoice_id());

        // Customer 1 gets only their own
        mockMvc.perform(get("/invoice")
                .header("Authorization", "Bearer " + customerTokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(id1)));

        // Admin gets all
        mockMvc.perform(get("/invoice")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // @spec INV-SEC-003
    @Test
    void testFindByIdSecurity() throws Exception {
        Invoice inv1 = new Invoice();
        inv1.setUser_id(1);
        inv1.setSubtotal(100.0);
        inv1.setTaxes(16.0);
        inv1.setTotal(116.0);
        inv1.setCreated_at("2026-06-05");
        inv1 = repo.save(inv1);

        Integer id = inv1.getInvoice_id();

        // Customer 1 gets their own
        mockMvc.perform(get("/invoice/" + id)
                .header("Authorization", "Bearer " + customerTokenUser1))
                .andExpect(status().isOk());

        // Customer 2 forbidden to get Customer 1's
        mockMvc.perform(get("/invoice/" + id)
                .header("Authorization", "Bearer " + customerTokenUser2))
                .andExpect(status().isForbidden());

        // Admin can get Customer 1's
        mockMvc.perform(get("/invoice/" + id)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
