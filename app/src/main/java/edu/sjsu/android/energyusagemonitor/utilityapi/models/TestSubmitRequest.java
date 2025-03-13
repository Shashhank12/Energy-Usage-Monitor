package edu.sjsu.android.energyusagemonitor.utilityapi.models;

public class TestSubmitRequest {
    private String utility;
    private String scenario;

    public TestSubmitRequest(String utility, String scenario) {
        this.utility = utility;
        this.scenario = scenario;
    }
}
