package edu.sjsu.android.energyusagemonitor.firestore;

import com.google.firebase.firestore.IgnoreExtraProperties;
@IgnoreExtraProperties
public class users
{
    private String email;
    private String firstName;
    private String lastName;
    private String budget;

    public users() {}

    public users(String email, String firstName, String lastName, String budget) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.budget = budget;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }
}
