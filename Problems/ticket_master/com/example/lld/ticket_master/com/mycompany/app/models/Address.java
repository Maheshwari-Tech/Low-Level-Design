package com.mycompany.app.models;

public class Address {
    private final String country;
    private final String state;
    private final String city;
    private final  String zipcode;
    private final String line;


    public Address(String country, String state, String city, String zipcode, String line) {
        this.country = country;
        this.state = state;
        this.city = city;
        this.zipcode = zipcode;
        this.line = line;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
