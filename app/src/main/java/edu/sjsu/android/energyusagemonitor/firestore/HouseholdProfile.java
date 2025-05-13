package edu.sjsu.android.energyusagemonitor.firestore;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

@IgnoreExtraProperties
public class HouseholdProfile {
    private int bedrooms;
    private int occupants;
    private String zipCode;
    @ServerTimestamp
    private Date lastUpdated;

    // Required empty constructor for Firestore
    public HouseholdProfile() {}

    public HouseholdProfile(int bedrooms, int occupants, String zipCode) {
        this.bedrooms = bedrooms;
        this.occupants = occupants;
        this.zipCode = zipCode;
    }

    public int getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(int bedrooms) {
        this.bedrooms = bedrooms;
    }

    public int getOccupants() {
        return occupants;
    }

    public void setOccupants(int occupants) {
        this.occupants = occupants;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}