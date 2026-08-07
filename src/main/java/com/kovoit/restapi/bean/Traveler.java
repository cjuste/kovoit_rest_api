package com.kovoit.restapi.bean;

public record Traveler(PersonalInfo personalInfo, boolean isDriver, Company company) {

    public Traveler(PersonalInfo personalInfo) {
        this(personalInfo, false, null);
    }

    public Traveler(PersonalInfo personalInfo, boolean isDriver) {
        this(personalInfo, isDriver, null);
    }
}
