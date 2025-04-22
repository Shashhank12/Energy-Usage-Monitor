package edu.sjsu.android.energyusagemonitor.upload;

import java.time.LocalDateTime;

public class TimePeriodUsage {
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private double totalKwh;
    private double totalCost;

    public TimePeriodUsage(LocalDateTime periodStart, LocalDateTime periodEnd, double totalKwh, double totalCost) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalKwh = totalKwh;
        this.totalCost = totalCost;
    }

    public TimePeriodUsage() {}

    public LocalDateTime getPeriodStart() { return periodStart; }
    public LocalDateTime getPeriodEnd() { return periodEnd; }
    public double getTotalKwh() { return totalKwh; }
    public double getTotalCost() { return totalCost; }

    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }
    public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }
    public void setTotalKwh(double totalKwh) { this.totalKwh = totalKwh; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
}