package com.invoice.api.dto;

public class DtoInvoiceList {
	
	private String id;
	
	private Integer user_id;
		
	private String created_at;
	
	private Double subtotal;
	
	private Double taxes;
	
	private Double total;

	private Double discount;

	private String shipping_address;

	private String payment_method;

	private String coupon_code;
	
	public DtoInvoiceList() {
		
	}

	public DtoInvoiceList(String id, Integer user_id, String created_at, Double subtotal, Double taxes, Double total,
			Double discount, String shipping_address, String payment_method, String coupon_code) {
		super();
		this.id = id;
		this.user_id = user_id;
		this.created_at = created_at;
		this.subtotal = subtotal;
		this.taxes = taxes;
		this.total = total;
		this.discount = discount;
		this.shipping_address = shipping_address;
		this.payment_method = payment_method;
		this.coupon_code = coupon_code;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Integer getUser_id() {
		return user_id;
	}

	public void setUser_id(Integer user_id) {
		this.user_id = user_id;
	}

	public String getCreated_at() {
		return created_at;
	}

	public void setCreated_at(String created_at) {
		this.created_at = created_at;
	}

	public Double getSubtotal() {
		return subtotal;
	}

	public void setSubtotal(Double subtotal) {
		this.subtotal = subtotal;
	}

	public Double getTaxes() {
		return taxes;
	}

	public void setTaxes(Double taxes) {
		this.taxes = taxes;
	}

	public Double getTotal() {
		return total;
	}

	public void setTotal(Double total) {
		this.total = total;
	}

	public Double getDiscount() {
		return discount;
	}

	public void setDiscount(Double discount) {
		this.discount = discount;
	}

	public String getShipping_address() {
		return shipping_address;
	}

	public void setShipping_address(String shipping_address) {
		this.shipping_address = shipping_address;
	}

	public String getPayment_method() {
		return payment_method;
	}

	public void setPayment_method(String payment_method) {
		this.payment_method = payment_method;
	}

	public String getCoupon_code() {
		return coupon_code;
	}

	public void setCoupon_code(String coupon_code) {
		this.coupon_code = coupon_code;
	}
}
