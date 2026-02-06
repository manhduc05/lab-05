package com.example.lab5_starter;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;
    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        citiesRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.w(TAG, "Listen failed.", error);
                    return;
                }
                cityArrayList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    String name = doc.getId();
                    String province = doc.getString("province");
                    cityArrayList.add(new City(name, province));
                }
                cityArrayAdapter.notifyDataSetChanged();
            }
        });

        addCityButton.setOnClickListener(view -> {
            CityDialogFragment dialog = new CityDialogFragment();
            dialog.show(getSupportFragmentManager(), "Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, position, id) -> {
            City selectedCity = cityArrayAdapter.getItem(position);
            CityDialogFragment dialog = CityDialogFragment.newInstance(selectedCity);
            dialog.show(getSupportFragmentManager(), "City Details");
        });

        cityListView.setOnItemLongClickListener((adapterView, view, position, id) -> {
            City selectedCity = cityArrayAdapter.getItem(position);

            // Confirm before deleting
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete city")
                    .setMessage("Delete " + selectedCity.getName() + " from the list?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (d, which) -> deleteCity(selectedCity, position))
                    .show();

            return true; // consume the long-click
        });

    }

    @Override
    public void updateCity(City city, String name, String province) {
        city.setName(name);
        city.setProvince(province);
        cityArrayAdapter.notifyDataSetChanged();
        Map<String, Object> data = new HashMap<>();
        data.put("province", province);
        citiesRef.document(name)
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "City updated: " + name));
    }

    private void deleteCity(City city, int position) {
        // Optimistically remove from UI (Firestore listener will later reconcile the list)
        cityArrayList.remove(position);
        cityArrayAdapter.notifyDataSetChanged();

        citiesRef.document(city.getName())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "City deleted: " + city.getName()))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to delete city: " + city.getName(), e);
                    // Put it back if Firestore delete fails
                    cityArrayList.add(position, city);
                    cityArrayAdapter.notifyDataSetChanged();
                });
    }

    @Override
    public void addCity(City city) {
        cityArrayList.add(city);
        cityArrayAdapter.notifyDataSetChanged();

        Map<String, Object> data = new HashMap<>();
        data.put("province", city.getProvince());

        citiesRef.document(city.getName())
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "City added: " + city.getName()));
    }

    public void addDummyData(){
        City m1 = new City("Edmonton", "AB");
        City m2 = new City("Vancouver", "BC");
        cityArrayList.add(m1);
        cityArrayList.add(m2);
        cityArrayAdapter.notifyDataSetChanged();
    }
}
