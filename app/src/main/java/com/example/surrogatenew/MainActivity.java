package com.example.surrogatenew;

import android.content.Intent; // Make sure this import is present
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

// Don't need these imports if you remove setContentView(R.layout.activity_main);
// import androidx.activity.EdgeToEdge;
// import androidx.core.view.ViewCompat;
// import androidx.core.graphics.Insets;
// import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- START OF CHANGES FOR TESTING ---

        // OPTIONAL: If MainActivity is purely for redirecting during testing
        // and doesn't need to display its own layout (activity_main.xml),
        // you can comment out or remove the setContentView line.
        // If you keep it, activity_main.xml will briefly show before redirect.
        // setContentView(R.layout.activity_main);

        // Create an Intent to launch VRequestsDirectionsMap
        Intent intent = new Intent(this, VolunteerRequests.class); // Correct way to use Context // Use 'this' for the Context
        startActivity(intent);

        // Call finish() so that when you press the back button from VRequestsDirectionsMap,
        // you don't return to an empty MainActivity screen.
        finish();

        // --- END OF CHANGES FOR TESTING ---

        // All the EdgeToEdge setup and other existing onCreate code
        // that was for the main activity's layout can be removed or commented out
        // if you are always redirecting and not showing activity_main.xml.
        // For example:
        /*
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main); // Make sure this is commented out or removed if you want to skip it

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        */
    }
}