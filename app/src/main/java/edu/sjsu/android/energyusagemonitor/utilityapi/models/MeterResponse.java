package edu.sjsu.android.energyusagemonitor.utilityapi.models;

public class MeterResponse {
    private int bill_count;
    private String status;

    public int getBillCount() {
        return bill_count;
    }

    public String getStatus() {
        return status;
    }
}
