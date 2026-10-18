package com.tracemind.payment.fault;

import org.springframework.stereotype.Component;

@Component
public class FaultConfiguration {
    private volatile int latencyMs = 0;
    private volatile double errorRate = 0.0;
    private volatile boolean serviceUnavailable = false;
    private volatile boolean paymentGatewayTimeout = false;
    private volatile String customErrorMessage = null;

    public int getLatencyMs() { return latencyMs; }
    public void setLatencyMs(int latencyMs) { this.latencyMs = latencyMs; }

    public double getErrorRate() { return errorRate; }
    public void setErrorRate(double errorRate) { this.errorRate = errorRate; }

    public boolean isServiceUnavailable() { return serviceUnavailable; }
    public void setServiceUnavailable(boolean serviceUnavailable) { this.serviceUnavailable = serviceUnavailable; }

    public boolean isPaymentGatewayTimeout() { return paymentGatewayTimeout; }
    public void setPaymentGatewayTimeout(boolean paymentGatewayTimeout) { this.paymentGatewayTimeout = paymentGatewayTimeout; }

    public String getCustomErrorMessage() { return customErrorMessage; }
    public void setCustomErrorMessage(String customErrorMessage) { this.customErrorMessage = customErrorMessage; }

    public void reset() {
        this.latencyMs = 0;
        this.errorRate = 0.0;
        this.serviceUnavailable = false;
        this.paymentGatewayTimeout = false;
        this.customErrorMessage = null;
    }
}
