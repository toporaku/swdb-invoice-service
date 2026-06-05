package com.invoice.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CartItemDto {

    private Integer id;

    @JsonProperty("cart_id")
    private Integer cartId;

    private String gtin;

    private Integer quantity;

    public CartItemDto() {
    }

    public CartItemDto(Integer id, Integer cartId, String gtin, Integer quantity) {
        this.id = id;
        this.cartId = cartId;
        this.gtin = gtin;
        this.quantity = quantity;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCartId() {
        return cartId;
    }

    public void setCartId(Integer cartId) {
        this.cartId = cartId;
    }

    public String getGtin() {
        return gtin;
    }

    public void setGtin(String gtin) {
        this.gtin = gtin;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
