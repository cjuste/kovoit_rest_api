package com.kovoit.restapi.bean;

public record Traveler(PersonalInfo personalInfo, Car car) {

    public Traveler(PersonalInfo personalInfo) {
        this(personalInfo, null);
    }
}
