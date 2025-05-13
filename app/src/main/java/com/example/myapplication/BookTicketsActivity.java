// BookTicketsActivity.java
package com.example.myapplication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class BookTicketsActivity extends AppCompatActivity {

    private EditText fromEditText, toEditText, dateEditText;
    private Button searchButton;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        fromEditText = findViewById(R.id.from_field);
        toEditText = findViewById(R.id.to_field);
        dateEditText = findViewById(R.id.date_field);
        searchButton = findViewById(R.id.search_button);

        calendar = Calendar.getInstance();

        dateEditText.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(BookTicketsActivity.this, dateSetListener,
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
            datePicker.show();
        });

        searchButton.setOnClickListener(v -> searchTickets());
    }

    private final DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        dateEditText.setText(sdf.format(calendar.getTime()));
    };

    private void searchTickets() {
        String from = fromEditText.getText().toString().trim().toLowerCase();
        String to = toEditText.getText().toString().trim().toLowerCase();
        String date = dateEditText.getText().toString().trim();

        if (from.isEmpty() || to.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://bdtickets.com/bus/search/" + from + "-to-" + to + "?journeyDate=" + date;
        android.util.Log.d("SearchDebug", "Searching URL: " + url);

        new Thread(() -> {
            try {
                Document doc = Jsoup.connect(url).get();
                Elements trips = doc.select("app-trip");
                android.util.Log.d("SearchDebug", "Trips found: " + trips.size());

                ArrayList<TicketModel> ticketList = new ArrayList<>();
                for (Element trip : trips) {
                    String operator = trip.select(".trip-name span").first().text();
                    String type = trip.select(".trip-name span").get(1).text();
                    String route = trip.select(".trip-name span").get(2).text();
                    String time = trip.select(".departure-time").text();
                    String price = trip.select(".fare-to-pay").text();
                    String seats = trip.select(".trip-action span").text();
                    String fullLink = url;

                    android.util.Log.d("SearchDebug", "Parsed ticket: " +
                            operator + " | " + type + " | " + route + " | " + time + " | " + price + " | " + seats);

                    ticketList.add(new TicketModel(operator, type, route, time, price, seats, fullLink));
                }

                runOnUiThread(() -> {
                  
                    Intent intent = new Intent(BookTicketsActivity.this, TicketWebViewActivity.class);
                    intent.putExtra("from", from);
                    intent.putExtra("to", to);
                    intent.putExtra("date", date);
                    startActivity(intent);

                    intent.putParcelableArrayListExtra("tickets", ticketList);
                    startActivity(intent);
                });
            } catch (Exception e) {
                android.util.Log.e("SearchDebug", "Error during fetch: ", e);
                runOnUiThread(() -> Toast.makeText(BookTicketsActivity.this, "Failed to fetch tickets.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

}
