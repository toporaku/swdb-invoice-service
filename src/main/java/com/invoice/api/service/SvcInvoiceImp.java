package com.invoice.api.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.invoice.api.dto.ApiResponse;
import com.invoice.api.dto.CartItemDto;
import com.invoice.api.dto.DtoCheckout;
import com.invoice.api.dto.DtoInvoiceList;
import com.invoice.api.dto.ProductDto;
import com.invoice.api.entity.Invoice;
import com.invoice.api.entity.InvoiceItem;
import com.invoice.api.repository.RepoInvoice;
import com.invoice.commons.mapper.MapperInvoice;
import com.invoice.commons.util.JwtDecoder;
import com.invoice.exception.ApiException;
import com.invoice.exception.DBAccessException;

@Service
public class SvcInvoiceImp implements SvcInvoice {
	
	@Autowired
    private RepoInvoice repo;
	
	@Autowired
	private JwtDecoder jwtDecoder;
	
	@Autowired
	private MapperInvoice mapper;

	@Autowired
	private RestTemplate restTemplate;

	private HttpHeaders getHeadersWithToken() {
		HttpHeaders headers = new HttpHeaders();
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attributes != null) {
			String authHeader = attributes.getRequest().getHeader("Authorization");
			if (authHeader != null) {
				headers.set("Authorization", authHeader);
			}
		}
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	// @spec INV-SEC-001, INV-SEC-002
	@Override
	public List<DtoInvoiceList> findAll() {
		try {
			if(jwtDecoder.isAdmin()) {
				return mapper.toDtoList(repo.findAll());
			}else {
				Integer user_id = jwtDecoder.getUserId();
				return mapper.toDtoList(repo.findAllByUserId(user_id));
			}
		}catch (DataAccessException e) {
	        throw new DBAccessException();
	    }
	}

	// @spec INV-SEC-003
	@Override
	public Invoice findById(Integer id) {
		try {
			Invoice invoice = repo.findById(id).get();
			if(!jwtDecoder.isAdmin()) {
				Integer user_id = jwtDecoder.getUserId();
				if(!invoice.getUser_id().equals(user_id)) {
					throw new ApiException(HttpStatus.FORBIDDEN, "El token no es válido para consultar esta factura");
				}
			}
			return invoice;
		}catch (DataAccessException e) {
	        throw new DBAccessException();
	    }catch (NoSuchElementException e) {
			throw new ApiException(HttpStatus.NOT_FOUND, "El id de la factura no existe");
	    }
	}

	// @spec INV-CHK-001, INV-CHK-002, INV-CHK-003, INV-CHK-004, INV-CHK-005, INV-CHK-006, INV-CHK-007, INV-CHK-008, INV-CHK-009
	@Override
	public ApiResponse create(DtoCheckout checkout) {
		try {
			Integer userId = jwtDecoder.getUserId();
			
			HttpHeaders headers = getHeadersWithToken();
			HttpEntity<?> entity = new HttpEntity<>(headers);
			
			// 1. Fetch cart items
			String cartUrl = "http://CART-SERVICE/cart-item/user/" + userId;
			ResponseEntity<CartItemDto[]> cartResponse;
			try {
				cartResponse = restTemplate.exchange(cartUrl, HttpMethod.GET, entity, CartItemDto[].class);
			} catch (Exception e) {
				throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al consultar el carrito del usuario");
			}
			
			CartItemDto[] cartItems = cartResponse.getBody();
			if (cartItems == null || cartItems.length == 0) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "El carrito está vacío");
			}
			
			// 2. Validation Phase: Validate stock for all items
			List<ProductDto> productList = new ArrayList<>();
			double subtotal = 0.0;
			
			for (CartItemDto item : cartItems) {
				String productUrl = "http://PRODUCT/product/gtin/" + item.getGtin();
				ResponseEntity<ProductDto> prodResponse;
				try {
					prodResponse = restTemplate.exchange(productUrl, HttpMethod.GET, entity, ProductDto.class);
				} catch (Exception e) {
					throw new ApiException(HttpStatus.NOT_FOUND, "El producto con GTIN " + item.getGtin() + " no existe");
				}
				ProductDto prod = prodResponse.getBody();
				if (prod == null || prod.getStatus() != 1) {
					throw new ApiException(HttpStatus.BAD_REQUEST, "El producto no está activo");
				}
				if (item.getQuantity() > prod.getStock()) {
					throw new ApiException(HttpStatus.CONFLICT, "Stock insuficiente para el producto: " + prod.getProduct());
				}
				productList.add(prod);
				subtotal += prod.getPrice() * item.getQuantity();
			}
			
			// 3. Commit Phase
			// Calculate discount
			double discount = 0.0;
			if (checkout != null && checkout.getCouponCode() != null && !checkout.getCouponCode().trim().isEmpty()) {
				String coupon = checkout.getCouponCode().trim();
				if ("SAVE10".equalsIgnoreCase(coupon)) {
					discount = subtotal * 0.10;
				} else {
					throw new ApiException(HttpStatus.BAD_REQUEST, "Código de descuento no es válido");
				}
			}
			
			double totalAmount = subtotal - discount;
			double taxesAmount = totalAmount * 0.16;
			double subtotalAmount = totalAmount - taxesAmount;
			
			Invoice invoice = new Invoice();
			invoice.setUser_id(userId);
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			invoice.setCreated_at(sdf.format(new Date()));
			
			invoice.setTotal(totalAmount);
			invoice.setTaxes(taxesAmount);
			invoice.setSubtotal(subtotalAmount);
			invoice.setDiscount(discount);
			
			if (checkout != null) {
				invoice.setShipping_address(checkout.getShippingAddress());
				invoice.setPayment_method(checkout.getPaymentMethod());
				invoice.setCoupon_code(checkout.getCouponCode());
			}
			
			List<InvoiceItem> itemsList = new ArrayList<>();
			for (int i = 0; i < cartItems.length; i++) {
				CartItemDto item = cartItems[i];
				ProductDto prod = productList.get(i);
				
				InvoiceItem invoiceItem = new InvoiceItem();
				invoiceItem.setGtin(item.getGtin());
				invoiceItem.setQuantity(item.getQuantity());
				invoiceItem.setUnit_price(prod.getPrice());
				
				double itemTotal = prod.getPrice() * item.getQuantity();
				double itemTaxes = itemTotal * 0.16;
				double itemSubtotal = itemTotal - itemTaxes;
				
				invoiceItem.setTotal(itemTotal);
				invoiceItem.setTaxes(itemTaxes);
				invoiceItem.setSubtotal(itemSubtotal);
				
				itemsList.add(invoiceItem);
			}
			invoice.setItems(itemsList);
			
			// Save Invoice (persists InvoiceItems via Cascade)
			repo.save(invoice);
			
			// 4. Decrement stock for all items in product-service
			for (CartItemDto item : cartItems) {
				String stockUrl = "http://PRODUCT/product/gtin/" + item.getGtin() + "/stock";
				HttpEntity<Integer> stockEntity = new HttpEntity<>(item.getQuantity(), headers);
				try {
					restTemplate.exchange(stockUrl, HttpMethod.PATCH, stockEntity, String.class);
				} catch (Exception e) {
					System.out.println("Failed to decrement stock for GTIN: " + item.getGtin());
				}
			}
			
			// 5. Clear user's cart in cart-service
			try {
				restTemplate.exchange(cartUrl, HttpMethod.DELETE, entity, String.class);
			} catch (Exception e) {
				System.out.println("Failed to clear cart for user: " + userId);
			}
			
			return new ApiResponse("La factura ha sido registrada");
			
		} catch (ApiException e) {
			throw e;
		} catch (DataAccessException e) {
			throw new DBAccessException();
		} catch (Exception e) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error inesperado al crear la factura: " + e.getMessage());
		}
	}
}
