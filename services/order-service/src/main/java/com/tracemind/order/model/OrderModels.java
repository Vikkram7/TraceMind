package com.tracemind.order.model;

import java.util.List;

public class OrderModels {

    public static class OrderItem {
        private String itemId;
        private int quantity;
        private double price;

        public OrderItem() {}
        public OrderItem(String itemId, int quantity, double price) {
            this.itemId = itemId;
            this.quantity = quantity;
            this.price = price;
        }

        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
    }

    public static class OrderRequest {
        private String customerId;
        private List<OrderItem> items;
        private String paymentMethod = "CREDIT_CARD";

        public OrderRequest() {}

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public List<OrderItem> getItems() { return items; }
        public void setItems(List<OrderItem> items) { this.items = items; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }

    public static class OrderResponse {
        private String orderId;
        private String status;
        private String traceId;
        private double totalAmount;
        private Object paymentResult;
        private Object inventoryResult;
        private String message;

        public OrderResponse() {}

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getTraceId() { return traceId; }
        public void setTraceId(String traceId) { this.traceId = traceId; }

        public double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

        public Object getPaymentResult() { return paymentResult; }
        public void setPaymentResult(Object paymentResult) { this.paymentResult = paymentResult; }

        public Object getInventoryResult() { return inventoryResult; }
        public void setInventoryResult(Object inventoryResult) { this.inventoryResult = inventoryResult; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
