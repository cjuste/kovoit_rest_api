package com.kovoit.restapi.bean;

public record Traveler(PersonalInfo personalInfo, boolean isDriver) {

    public Traveler(PersonalInfo personalInfo) {
        this(personalInfo, false);
    }
}
