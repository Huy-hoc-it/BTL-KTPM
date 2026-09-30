package com.example.cinema.modules.booking.business;

import java.util.List;

public class Booking {
    private String userID;
    private String showTimeID;
    private List<String> seatsID;

    public Booking(String userID, String showTimeID, List<String> seatsID) {
        this.userID = userID;
        this.showTimeID = showTimeID;
        this.seatsID = seatsID;
    }

    public void setSeatsID(List<String> seatsID) {
        this.seatsID = seatsID;
    }
}