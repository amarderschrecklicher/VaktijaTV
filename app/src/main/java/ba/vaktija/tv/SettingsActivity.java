package ba.vaktija.tv;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String[] LOCATIONS = {
        "Banovići", "Banja Luka", "Bihać", "Bijeljina", "Bileća",
        "Bosanski Brod", "Bosanska Dubica", "Bosanska Gradiška", "Bosansko Grahovo", "Bosanska Krupa",
        "Bosanski Novi", "Bosanski Petrovac", "Bosanski Šamac", "Bratunac", "Brčko",
        "Breza", "Bugojno", "Busovača", "Bužim", "Cazin",
        "Čajniče", "Čapljina", "Čelić", "Čelinac", "Čitluk",
        "Derventa", "Doboj", "Donji Vakuf", "Drvar", "Foča",
        "Fojnica", "Gacko", "Glamoč", "Goražde", "Gornji Vakuf",
        "Gračanica", "Gradačac", "Grude", "Hadžići", "Han-Pijesak",
        "Hlivno", "Ilijaš", "Jablanica", "Jajce", "Kakanj",
        "Kalesija", "Kalinovik", "Kiseljak", "Kladanj", "Ključ",
        "Konjic", "Kotor-Varoš", "Kreševo", "Kupres", "Laktaši",
        "Lopare", "Lukavac", "Ljubinje", "Ljubuški", "Maglaj",
        "Modriča", "Mostar", "Mrkonjić-Grad", "Neum", "Nevesinje",
        "Novi Travnik", "Odžak", "Olovo", "Orašje", "Pale",
        "Posušje", "Prijedor", "Prnjavor", "Prozor", "Rogatica",
        "Rudo", "Sanski Most", "Sarajevo", "Skender-Vakuf", "Sokolac",
        "Srbac", "Srebrenica", "Srebrenik", "Stolac", "Šekovići",
        "Šipovo", "Široki Brijeg", "Teslić", "Tešanj", "Tomislav-Grad",
        "Travnik", "Trebinje", "Trnovo", "Tuzla", "Ugljevik",
        "Vareš", "Velika Kladuša", "Visoko", "Višegrad", "Vitez",
        "Vlasenica", "Zavidovići", "Zenica", "Zvornik", "Žepa",
        "Žepče", "Živinice", "Bijelo Polje", "Gusinje", "Nova Varoš",
        "Novi Pazar", "Plav", "Pljevlja", "Priboj", "Prijepolje",
        "Rožaje", "Sjenica", "Tutin"
    };

    private SharedPreferences prefs;
    private Spinner spinnerLocation;
    private NumberPicker pickerFajrOffset;
    private TextView tvFajrOffsetMode;
    private Button btnSave;

    private int selectedLocationIndex = 77; // Sarajevo = index 77 (1-based: 78)
    private int fajrOffsetMinutes = 0;
    private boolean fajrAfterSunrise = false; // false = before sunrise (default), true = after fajr start

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("vaktija_prefs", MODE_PRIVATE);

        spinnerLocation = findViewById(R.id.spinner_location);
        pickerFajrOffset = findViewById(R.id.picker_fajr_offset);
        tvFajrOffsetMode = findViewById(R.id.tv_fajr_offset_mode);
        btnSave = findViewById(R.id.btn_save_settings);

        Button btnBack = findViewById(R.id.btn_back_settings);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Setup location spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, LOCATIONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(adapter);

        // Load saved values
        int savedLocId = prefs.getInt("location_id", 78);
        selectedLocationIndex = savedLocId - 1; // Convert 1-based to 0-based
        if (selectedLocationIndex >= 0 && selectedLocationIndex < LOCATIONS.length) {
            spinnerLocation.setSelection(selectedLocationIndex);
        }

        fajrOffsetMinutes = prefs.getInt("fajr_offset_minutes", 0);
        fajrAfterSunrise = prefs.getBoolean("fajr_after_fajr_start", false);

        // Fajr offset picker
        pickerFajrOffset.setMinValue(0);
        pickerFajrOffset.setMaxValue(60);
        pickerFajrOffset.setValue(fajrOffsetMinutes);
        pickerFajrOffset.setWrapSelectorWheel(false);

        updateFajrModeText();

        // Toggle fajr mode
        if (tvFajrOffsetMode != null) {
            tvFajrOffsetMode.setOnClickListener(v -> {
                fajrAfterSunrise = !fajrAfterSunrise;
                updateFajrModeText();
            });
        }

        spinnerLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLocationIndex = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void updateFajrModeText() {
        if (tvFajrOffsetMode != null) {
            if (fajrAfterSunrise) {
                tvFajrOffsetMode.setText("Modo: Nakon početka sabah vakta\n(Tapni za promjenu)");
            } else {
                tvFajrOffsetMode.setText("Modo: Prije izlaska sunca (šuruka)\n(Tapni za promjenu)");
            }
        }
    }

    private void saveSettings() {
        fajrOffsetMinutes = pickerFajrOffset.getValue();
        int locationId = selectedLocationIndex + 1; // Convert back to 1-based
        String locationName = LOCATIONS[selectedLocationIndex];

        prefs.edit()
                .putInt("location_id", locationId)
                .putString("location_name", locationName)
                .putInt("fajr_offset_minutes", fajrOffsetMinutes)
                .putBoolean("fajr_after_fajr_start", fajrAfterSunrise)
                .apply();

        Toast.makeText(this, "Podešavanja sačuvana za " + locationName, Toast.LENGTH_SHORT).show();
        finish();
    }
}
