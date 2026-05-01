package com.campus.material.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class BusinessMetrics {

    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter refreshFailureCounter;
    private final Counter applySubmitCounter;
    private final Counter applyApproveCounter;
    private final Counter transferExecuteCounter;
    private final Counter inventoryStockInCounter;
    private final Counter inventoryStockOutCounter;
    private final Counter warningCreatedCounter;
    private final Counter warningScanCounter;
    private final Timer warningScanTimer;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.loginSuccessCounter = Counter.builder("campus_auth_login_total")
                .description("Total login attempts grouped by outcome")
                .tag("outcome", "success")
                .register(meterRegistry);
        this.loginFailureCounter = Counter.builder("campus_auth_login_total")
                .description("Total login attempts grouped by outcome")
                .tag("outcome", "failure")
                .register(meterRegistry);
        this.refreshFailureCounter = Counter.builder("campus_auth_refresh_total")
                .description("Total refresh token attempts grouped by outcome")
                .tag("outcome", "failure")
                .register(meterRegistry);
        this.applySubmitCounter = Counter.builder("campus_apply_submit_total")
                .description("Successful apply order submissions")
                .register(meterRegistry);
        this.applyApproveCounter = Counter.builder("campus_apply_approve_total")
                .description("Successful apply order approvals")
                .register(meterRegistry);
        this.transferExecuteCounter = Counter.builder("campus_transfer_execute_total")
                .description("Successful transfer executions")
                .register(meterRegistry);
        this.inventoryStockInCounter = Counter.builder("campus_inventory_stock_in_total")
                .description("Successful stock-in operations")
                .register(meterRegistry);
        this.inventoryStockOutCounter = Counter.builder("campus_inventory_stock_out_total")
                .description("Successful stock-out operations")
                .register(meterRegistry);
        this.warningCreatedCounter = Counter.builder("campus_warning_created_total")
                .description("Warnings generated during scanning")
                .register(meterRegistry);
        this.warningScanCounter = Counter.builder("campus_warning_scan_total")
                .description("Completed warning scans")
                .register(meterRegistry);
        this.warningScanTimer = Timer.builder("campus_warning_scan_seconds")
                .description("Warning scan execution time")
                .register(meterRegistry);
    }

    public void recordLoginSuccess() {
        loginSuccessCounter.increment();
    }

    public void recordLoginFailure() {
        loginFailureCounter.increment();
    }

    public void recordRefreshFailure() {
        refreshFailureCounter.increment();
    }

    public void recordApplySubmit() {
        applySubmitCounter.increment();
    }

    public void recordApplyApprove() {
        applyApproveCounter.increment();
    }

    public void recordTransferExecute() {
        transferExecuteCounter.increment();
    }

    public void recordInventoryStockIn() {
        inventoryStockInCounter.increment();
    }

    public void recordInventoryStockOut() {
        inventoryStockOutCounter.increment();
    }

    public void recordWarningScan(Duration duration, long createdWarnings) {
        warningScanCounter.increment();
        warningScanTimer.record(duration);
        if (createdWarnings > 0) {
            warningCreatedCounter.increment(createdWarnings);
        }
    }
}
