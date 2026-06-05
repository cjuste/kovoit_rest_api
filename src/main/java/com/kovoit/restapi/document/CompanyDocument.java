package com.kovoit.restapi.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "companies")
public class CompanyDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private final String name;

    private final AddressDocument address;

    public CompanyDocument(String name, AddressDocument address) {
        this.name = name;
        this.address = address;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public AddressDocument getAddress() { return address; }
}