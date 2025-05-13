package com.example.myapplication;

import android.os.Parcel;
import android.os.Parcelable;

public class TicketModel implements Parcelable {
    private String operator, type, route, time, price, seats, url;

    public TicketModel(String operator, String type, String route, String time, String price, String seats, String url) {
        this.operator = operator;
        this.type = type;
        this.route = route;
        this.time = time;
        this.price = price;
        this.seats = seats;
        this.url = url;
    }

    protected TicketModel(Parcel in) {
        operator = in.readString();
        type = in.readString();
        route = in.readString();
        time = in.readString();
        price = in.readString();
        seats = in.readString();
        url = in.readString();
    }

    public static final Creator<TicketModel> CREATOR = new Creator<TicketModel>() {
        @Override
        public TicketModel createFromParcel(Parcel in) {
            return new TicketModel(in);
        }

        @Override
        public TicketModel[] newArray(int size) {
            return new TicketModel[size];
        }
    };

    public String getOperator() { return operator; }
    public String getType() { return type; }
    public String getRoute() { return route; }
    public String getTime() { return time; }
    public String getPrice() { return price; }
    public String getSeats() { return seats; }
    public String getUrl() { return url; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(operator);
        dest.writeString(type);
        dest.writeString(route);
        dest.writeString(time);
        dest.writeString(price);
        dest.writeString(seats);
        dest.writeString(url);
    }
}
