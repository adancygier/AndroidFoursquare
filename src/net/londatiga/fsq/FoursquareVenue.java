package net.londatiga.fsq;

import java.util.List;

import android.location.Location;

public class FoursquareVenue {
    
	public String id;
	public String name;
	public String address;
	public String type;
	public Location location;
	public int direction;
	public int distance;
    public String postalCode;
    public String city;
    public String state;
    public String icon;
    public double lat;
    public double lng;
	public int herenow;
    public int tip_count;
    
    public List<Tip> other_tips;
    public List<Tip> my_tips;
    
	class Tip {
		String type;
		int count;
		String id;
		long createdAt;
		String text;
		String user_id;
		String firstName;
		String photo;
		String home_city;
	}
}
