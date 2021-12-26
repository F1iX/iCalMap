package me.keppler.icalmap;

import android.location.Address;

public class Event {
    String organizer;
    String title;
    String street;
    String description;
    Address address;

    public Event(String organizer, String title, String street, String description){
        this.organizer = organizer;
        this.title = title;
        this.street = street;
        this.description = description;
    }
}
