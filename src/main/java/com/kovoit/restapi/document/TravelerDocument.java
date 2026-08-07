package com.kovoit.restapi.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "travelers")
public class TravelerDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private final String firstName;

    @Field(type = FieldType.Text)
    private final String lastName;

    @Field(type = FieldType.Text)
    private final String email;

    private final AddressDocument address;

    @Field(type = FieldType.Boolean)
    private final boolean isDriver;

    @Field(type = FieldType.Keyword)
    private final String companyId;

    public TravelerDocument(String firstName, String lastName, String email, AddressDocument address,
                             boolean isDriver, String companyId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.address = address;
        this.isDriver = isDriver;
        this.companyId = companyId;
    }

    public String getFirstName() { return firstName; }

    public String getLastName() { return lastName; }

    public String getEmail() { return email; }

    public AddressDocument getAddress() { return address; }

    public boolean isDriver() { return isDriver; }

    public String getCompanyId() { return companyId; }
}
