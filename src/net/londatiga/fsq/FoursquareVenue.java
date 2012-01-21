package net.londatiga.fsq;

import java.util.ArrayList;
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
    public String icon = "";
    public String icon_32 = "";
    public String icon_44 = "";
    public String icon_64 = "";
    public String icon_88 = "";
    public String icon_256 = "";
    public double lat;
    public double lng;
	public int herenow;
    public int tip_count;
    public int my_tip_count = 0;
    public int other_tip_count = 0;
    
    public List<Tip> other_tips = new ArrayList<Tip>();
    public List<Tip> my_tips = new ArrayList<Tip>();
    
	public class Tip {
		String type;
		int count;
		String id;
		long createdAt;
		String text;
		String user_id;
		String firstName;
		String lastName;
		String photo;
		String home_city;
	}
    
	public Tip createTip() {
		return new Tip();
	}
}
