package edu.sjsu.android.energyusagemonitor.utilityapi.models;

import java.util.List;

public class HistoricalCollectionRequest {
    private List<String> meters;
    private Integer collection_duration;

    public HistoricalCollectionRequest(List<String> meters, Integer collectionDuration) {
        this.meters = meters;
        this.collection_duration = collectionDuration;
    }
}