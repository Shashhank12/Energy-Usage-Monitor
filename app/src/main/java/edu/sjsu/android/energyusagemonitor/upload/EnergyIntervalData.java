package edu.sjsu.android.energyusagemonitor.upload;

import java.time.LocalDateTime;

public class EnergyIntervalData {
    private LocalDateTime startTimestamp;
    private double usageKWh;
    private double cost;

    public EnergyIntervalData(LocalDateTime startTimestamp, double usageKWh, double cost) {
        this.startTimestamp = startTimestamp;
        this.usageKWh = usageKWh;
        this.cost = cost;
    }

    public EnergyIntervalData() {}

    public LocalDateTime getStartTimestamp() { return startTimestamp; }


    public double getUsageKWh() { return usageKWh; }
    public double getCost() { return cost; }

    public void setStartTimestamp(LocalDateTime startTimestamp) { this.startTimestamp = startTimestamp; }
    public void setUsageKWh(double usageKWh) { this.usageKWh = usageKWh; }
    public void setCost(double cost) { this.cost = cost; }
}