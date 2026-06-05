package com.invoice.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DtoCheckout {

    @JsonProperty("shipping_address")
    private String shippingAddress;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("coupon_code")
    private String couponCode;

    public DtoCheckout() {
    }

    public DtoCheckout(String shippingAddress, String paymentMethod, String couponCode) {
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.couponCode = couponCode;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}
