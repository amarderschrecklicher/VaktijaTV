package ba.vaktija.tv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    // Prayer names in Bosnian
    private static final String[] PRAYER_NAMES = {"Zora (Sabah)", "Izlazak sunca", "Podne", "Ikindija", "Akšam", "Jacija"};
    private static final String[] PRAYER_NAMES_SHORT = {"Sabah", "Izlazak sunca", "Podne", "Ikindija", "Akšam", "Jacija"};

    // UI screens
    private FrameLayout screenMain;
    private FrameLayout screenPrayerTimes;
    private FrameLayout screenJummah;
    private FrameLayout screenAzan;

    // Main slideshow
    private TextView tvSlideText;
    private TextView tvSlideSubtext;
    private TextView tvClock;
    private TextView tvDate;
    private TextView tvLocation;

    // Prayer times screen
    private TextView tvPrayerCity;
    private TextView tvPrayerDate;
    private TextView tvPrayerSabah, tvPrayerSuruk, tvPrayerPodne, tvPrayerIkindija, tvPrayerAksam, tvPrayerJacija;
    private TextView tvNextPrayerLabel, tvNextPrayerCountdown;
    private TextView tvClockPrayer;

    // Jummah screen
    private TextView tvJummahTime;
    private TextView tvJummahAyet;
    private TextView tvJummahCountdown;
    private TextView tvClockJummah;

    // Azan notification screen
    private TextView tvAzanPrayerName;
    private TextView tvAzanCountdown;
    private View btnCancelAzan;
    private TextView tvBtnCancelText;

    private String[] prayerTimes = null;
    private int locationId = 78; // Sarajevo default
    private String locationName = "Sarajevo";

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Timer clockTimer;
    private Timer slideshowTimer;
    private Timer prayerCheckTimer;

    private int currentSlide = -1; // Start at -1 so first advance shows slide 0
    private int currentScreen = -1; // Start at -1 so first showScreen always applies

    private boolean azanCancelled = false;
    private boolean azanPlaying = false;
    private MediaPlayer mediaPlayer;
    private String currentAzanPrayer = "";

    private SharedPreferences prefs;

    // === TEST MODE ===
    private boolean testMode = true;
    private static final String[] SCREEN_NAMES = {"Slideshow", "Vaktija", "Džuma", "Ezan"};

    // Hadiths and Ayats in Bosnian
    private static final String[][] SLIDES = {
        {"Hadis", "Ko bude postio ramazan sa imanom i nadajući se nagradi od Allaha, biće mu oprošteni prethodni grijesi.", "— Buharija i Muslim"},
        {"Ajet", "Zaista, s mukom je i lahkoća. Zaista, s mukom je i lahkoća.", "— Inširah, 5-6"},
        {"Hadis", "Uzvišeni Allah kaže: 'Ja sam u misli moga roba o Meni, i Ja sam s njim kada Me On spominje.'", "— Buharija"},
        {"Ajet", "I zikrite Allaha često, i jutrom i večerom Ga veličajte.", "— El-Ahzab, 41-42"},
        {"Hadis", "Najdraža djela Allahu su redovna, makar bila mala.", "— Buharija i Muslim"},
        {"Ajet", "Allah je Svjetlost nebesa i Zemlje. Primjer Njegove svjetlosti je udubina u zidu...", "— En-Nur, 35"},
        {"Hadis", "Osmijeh prema bratu tvome je sadaka.", "— Tirmizi"},
        {"Ajet", "O vi koji vjerujete, tražite pomoć u strpljenju i namazu, zaista Allah je sa strpljivima.", "— El-Bekare, 153"},
        {"Hadis", "Ko je od vas u stanju da štiti svog brata od vatre, neka to učini, makar i jednom riječju.", "— Muslim"},
        {"Ajet", "I ne gubite nadu u milost Allahovu, zaista u milost Allahovu ne gube nade jedino nevjernici.", "— Ez-Zumer, 53"},
        {"Hadis", "Džennetu su bliži onome koji oprosti, a od kojih je tražen oprost.", "— Tirmizi"},
        {"Ajet", "Allahu se vraćaju sve stvari.", "— Šura, 53"},
        {"Hadis", "Najdraži čovjek Allahu je onaj koji je najkorisniji ljudima.", "— Taberani"},
        {"Ajet", "Reci robovima Mojim: Neka govore ono što je najljepše.", "— El-Isra, 53"},
        {"Hadis", "Musliman je onaj od čijeg jezika i ruke su sigurni drugi muslimani.", "— Buharija"},
    };

    private static final String JUMMAH_AYET =
        "O vjernici, kada se u petak na namaz pozovete, pohitajte na Allahov zikr i ostavite trgovinu. To vam je bolje ako znate. A kada namaz bude obavljen, razidite se po zemlji i tražite Allahovu blagodat i spominjite Allaha često, da biste postigli što želite.\n\n— El-Džumu'a, 9-10";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen, keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("vaktija_prefs", MODE_PRIVATE);
        locationId = prefs.getInt("location_id", 78);
        locationName = prefs.getString("location_name", "Sarajevo");

        initViews();
        showScreen(0); // Ensure initial screen state is applied
        startClock();
        fetchPrayerTimes();
        startSlideshow();
        startPrayerChecker();
    }

    private void initViews() {
        screenMain = findViewById(R.id.screen_main);
        screenPrayerTimes = findViewById(R.id.screen_prayer_times);
        screenJummah = findViewById(R.id.screen_jummah);
        screenAzan = findViewById(R.id.screen_azan);

        // Main screen
        tvSlideText = findViewById(R.id.tv_slide_text);
        tvSlideSubtext = findViewById(R.id.tv_slide_subtext);
        tvClock = findViewById(R.id.tv_clock);
        tvDate = findViewById(R.id.tv_date);
        tvLocation = findViewById(R.id.tv_location);

        // Prayer times screen
        tvPrayerCity = findViewById(R.id.tv_prayer_city);
        tvPrayerDate = findViewById(R.id.tv_prayer_date);
        tvPrayerSabah = findViewById(R.id.tv_prayer_sabah);
        tvPrayerSuruk = findViewById(R.id.tv_prayer_suruk);
        tvPrayerPodne = findViewById(R.id.tv_prayer_podne);
        tvPrayerIkindija = findViewById(R.id.tv_prayer_ikindija);
        tvPrayerAksam = findViewById(R.id.tv_prayer_aksam);
        tvPrayerJacija = findViewById(R.id.tv_prayer_jacija);
        tvNextPrayerLabel = findViewById(R.id.tv_next_prayer_label);
        tvNextPrayerCountdown = findViewById(R.id.tv_next_prayer_countdown);
        tvClockPrayer = findViewById(R.id.tv_clock_prayer);

        // Jummah screen
        tvJummahTime = findViewById(R.id.tv_jummah_time);
        tvJummahAyet = findViewById(R.id.tv_jummah_ayet);
        tvJummahCountdown = findViewById(R.id.tv_jummah_countdown);
        tvClockJummah = findViewById(R.id.tv_clock_jummah);

        // Azan screen
        tvAzanPrayerName = findViewById(R.id.tv_azan_prayer_name);
        tvAzanCountdown = findViewById(R.id.tv_azan_countdown);
        btnCancelAzan = findViewById(R.id.btn_cancel_azan);
        tvBtnCancelText = findViewById(R.id.tv_btn_cancel_text);

        btnCancelAzan.setOnClickListener(v -> cancelAzan());

        // Settings button on main screen
        View btnSettings = findViewById(R.id.btn_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> openSettings());
        }

        tvLocation.setText(locationName);
    }

    private void startClock() {
        clockTimer = new Timer();
        clockTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mainHandler.post(() -> updateClock());
            }
        }, 0, 1000);
    }

    private void updateClock() {
        Calendar cal = Calendar.getInstance();
        String time = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
        String date = formatDateBosnian(cal);

        if (tvClock != null) tvClock.setText(time);
        if (tvClockPrayer != null) tvClockPrayer.setText(time);
        if (tvClockJummah != null) tvClockJummah.setText(time);
        if (tvDate != null) tvDate.setText(date);
        if (tvPrayerDate != null) tvPrayerDate.setText(date);
        if (tvPrayerCity != null) tvPrayerCity.setText(locationName.toUpperCase());

        updateCountdowns(cal);
    }

    private String formatDateBosnian(Calendar cal) {
        String[] daniBS = {"nedjelja", "ponedjeljak", "utorak", "srijeda", "četvrtak", "petak", "subota"};
        String[] mjeseciBS = {"januar", "februar", "mart", "april", "maj", "juni", "juli", "august", "septembar", "oktobar", "novembar", "decembar"};
        int dow = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int mon = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        return daniBS[dow] + ", " + day + ". " + mjeseciBS[mon] + " " + year + ".";
    }

    private void updateCountdowns(Calendar cal) {
        if (prayerTimes == null) return;

        int nowSecs = cal.get(Calendar.HOUR_OF_DAY) * 3600
                + cal.get(Calendar.MINUTE) * 60
                + cal.get(Calendar.SECOND);
        int nextIdx = -1;
        int secsDiff = Integer.MAX_VALUE;

        for (int i = 0; i < prayerTimes.length; i++) {
            int pSecs = parsePrayerMinutes(prayerTimes[i]) * 60; // prayer time in seconds
            int diff = pSecs - nowSecs;
            if (diff > 0 && diff < secsDiff) {
                secsDiff = diff;
                nextIdx = i;
            }
        }

        if (nextIdx == -1) {
            // After Jacija, next is Sabah next day
            nextIdx = 0;
            int sabahSecs = parsePrayerMinutes(prayerTimes[0]) * 60;
            secsDiff = (24 * 3600 - nowSecs) + sabahSecs;
        }

        // Update prayer times screen countdown
        if (tvNextPrayerLabel != null) {
            tvNextPrayerLabel.setText("Sljedeći: " + PRAYER_NAMES_SHORT[nextIdx]);
        }
        if (tvNextPrayerCountdown != null) {
            int h = secsDiff / 3600;
            int m = (secsDiff % 3600) / 60;
            int s = secsDiff % 60;
            String countStr = String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s);
            tvNextPrayerCountdown.setText(countStr);
        }

        // Jummah countdown
        if (tvJummahCountdown != null && prayerTimes.length > 2) {
            int podneSecs = parsePrayerMinutes(prayerTimes[2]) * 60;
            int diff = podneSecs - nowSecs;
            if (diff > 0) {
                int h = diff / 3600;
                int m = (diff % 3600) / 60;
                int s = diff % 60;
                tvJummahCountdown.setText(String.format(Locale.getDefault(),
                        "%d:%02d:%02d do džume", h, m, s));
            } else {
                tvJummahCountdown.setText("Džuma je počela");
            }
        }
    }

    private int parsePrayerMinutes(String time) {
        try {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    private void fetchPrayerTimes() {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.vaktija.ba/vaktija/v1/" + locationId;
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> Toast.makeText(MainActivity.this,
                        "Greška pri učitavanju vakta: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    JSONArray vakat = json.getJSONArray("vakat");
                    prayerTimes = new String[vakat.length()];
                    for (int i = 0; i < vakat.length(); i++) {
                        prayerTimes[i] = vakat.getString(i);
                    }
                    mainHandler.post(() -> {
                        updatePrayerUI();
                        scheduleAzanChecks();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updatePrayerUI() {
        if (prayerTimes == null || prayerTimes.length < 6) return;
        if (tvPrayerSabah != null) tvPrayerSabah.setText(prayerTimes[0]);
        if (tvPrayerSuruk != null) tvPrayerSuruk.setText(prayerTimes[1]);
        if (tvPrayerPodne != null) tvPrayerPodne.setText(prayerTimes[2]);
        if (tvPrayerIkindija != null) tvPrayerIkindija.setText(prayerTimes[3]);
        if (tvPrayerAksam != null) tvPrayerAksam.setText(prayerTimes[4]);
        if (tvPrayerJacija != null) tvPrayerJacija.setText(prayerTimes[5]);
        if (tvJummahTime != null) tvJummahTime.setText("Džuma — " + prayerTimes[2]);
        if (tvJummahAyet != null) tvJummahAyet.setText(JUMMAH_AYET);
    }

    private void scheduleAzanChecks() {
        // Already running via prayerCheckTimer
    }

    private void startSlideshow() {
        slideshowTimer = new Timer();
        slideshowTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mainHandler.post(() -> advanceSlide());
            }
        }, 0, 8000);
    }

    private void advanceSlide() {
        currentSlide = (currentSlide + 1) % SLIDES.length;
        if (tvSlideText != null) {
            // Fade out then update text
            tvSlideText.animate().alpha(0f).setDuration(500).withEndAction(() -> {
                tvSlideText.setText(SLIDES[currentSlide][1]);
                if (tvSlideSubtext != null) {
                    tvSlideSubtext.setText(SLIDES[currentSlide][0] + " " + SLIDES[currentSlide][2]);
                }
                tvSlideText.animate().alpha(1f).setDuration(500).start();
            }).start();
        }
    }

    private void startPrayerChecker() {
        prayerCheckTimer = new Timer();
        prayerCheckTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mainHandler.post(() -> {
                    if (!testMode) {
                        checkScreenMode();
                    }
                });
            }
        }, 5000, 30000); // Check every 30 seconds
    }

    private void checkScreenMode() {
        if (prayerTimes == null) return;

        Calendar cal = Calendar.getInstance();
        int nowMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        boolean isPrayer30Min = false;
        int prayerIn30 = -1;

        for (int i = 0; i < prayerTimes.length; i++) {
            int pMins = parsePrayerMinutes(prayerTimes[i]);
            int diff = pMins - nowMins;
            if (diff >= -5 && diff <= 30) {
                isPrayer30Min = true;
                prayerIn30 = i;
                break;
            }
        }

        // Jummah: Friday, within 40min of Podne or post-azan
        boolean isJummah = false;
        if (dayOfWeek == Calendar.FRIDAY && prayerTimes.length > 2) {
            int podneMins = parsePrayerMinutes(prayerTimes[2]);
            int diff = podneMins - nowMins;
            if (diff >= -40 && diff <= 30) {
                isJummah = true;
            }
        }

        // Azan check: 2 min before any prayer (except Šuruk)
        for (int i = 0; i < prayerTimes.length; i++) {
            if (i == 1) continue; // Skip Šuruk
            int pMins = parsePrayerMinutes(prayerTimes[i]);
            int diff = pMins - nowMins;
            if (diff == 2 && !azanCancelled && currentAzanPrayer.isEmpty()) {
                final int idx = i;
                showAzanWarning(idx);
                return;
            }
        }

        // Reset azan cancelled flag after prayer
        if (!currentAzanPrayer.isEmpty()) {
            return; // Let azan flow complete
        }

        // Reset cancellation after relevant prayer passes
        for (int i = 0; i < prayerTimes.length; i++) {
            int pMins = parsePrayerMinutes(prayerTimes[i]);
            int diff = nowMins - pMins;
            if (diff == 5) {
                azanCancelled = false;
                break;
            }
        }

        if (isJummah) {
            showScreen(2);
        } else if (isPrayer30Min) {
            showScreen(1);
        } else {
            showScreen(0);
        }
    }

    private void showAzanWarning(int prayerIdx) {
        currentAzanPrayer = PRAYER_NAMES_SHORT[prayerIdx];
        if (tvAzanPrayerName != null) {
            tvAzanPrayerName.setText(PRAYER_NAMES_SHORT[prayerIdx]);
        }
        showScreen(3);
        azanCancelled = false;

        // Countdown 2 min then play azan if not cancelled
        final int[] secsLeft = {120};
        Timer azanTimer = new Timer();
        azanTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                secsLeft[0]--;
                mainHandler.post(() -> {
                    if (tvAzanCountdown != null) {
                        tvAzanCountdown.setText(secsLeft[0] + "s");
                    }
                });
                if (secsLeft[0] <= 0) {
                    azanTimer.cancel();
                    mainHandler.post(() -> {
                        if (!azanCancelled) {
                            playAzan(prayerIdx);
                        } else {
                            currentAzanPrayer = "";
                            checkScreenMode();
                        }
                    });
                }
            }
        }, 1000, 1000);
    }

    private void playAzan(int prayerIdx) {
        azanPlaying = true;
        showScreen(1); // Show prayer times while azan plays

        // Play the azan audio resource
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            int resId = (prayerIdx == 0) ? R.raw.azan_fajr : R.raw.azan;
            mediaPlayer = MediaPlayer.create(this, resId);
            if (mediaPlayer != null) {
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(mp -> {
                    azanPlaying = false;
                    currentAzanPrayer = "";
                    mp.release();
                    mediaPlayer = null;
                });
            }
        } catch (Exception e) {
            azanPlaying = false;
            currentAzanPrayer = "";
        }
    }

    private void cancelAzan() {
        azanCancelled = true;
        currentAzanPrayer = "";
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        azanPlaying = false;
        Toast.makeText(this, "Ezan će biti dat uživo", Toast.LENGTH_SHORT).show();
        checkScreenMode();
    }

    private void showScreen(int screen) {
        if (currentScreen == screen) return;
        currentScreen = screen;

        screenMain.setVisibility(screen == 0 ? View.VISIBLE : View.GONE);
        screenPrayerTimes.setVisibility(screen == 1 ? View.VISIBLE : View.GONE);
        screenJummah.setVisibility(screen == 2 ? View.VISIBLE : View.GONE);
        screenAzan.setVisibility(screen == 3 ? View.VISIBLE : View.GONE);
    }

    /** Force-show a screen even if it's already the current one (for test mode) */
    private void forceShowScreen(int screen) {
        currentScreen = -1; // Reset so showScreen applies
        showScreen(screen);
    }

    // === TEST MODE ===

    private void toggleTestMode() {
        testMode = !testMode;
        if (testMode) {
            // Load dummy prayer times if none available yet
            if (prayerTimes == null) {
                prayerTimes = new String[]{"04:32", "06:10", "12:53", "16:45", "19:48", "21:18"};
                updatePrayerUI();
            }
            // Also populate azan screen data for preview
            if (tvAzanPrayerName != null) tvAzanPrayerName.setText("Akšam");
            if (tvAzanCountdown != null) tvAzanCountdown.setText("120s");

            Toast.makeText(this,
                    "TEST MODE ON\n← → mijenjaj ekrane\n0-3 direktan ekran\nPonovo D za isključiti",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "TEST MODE OFF — automatski režim", Toast.LENGTH_SHORT).show();
            currentScreen = -1; // Force refresh on next auto-check
        }
    }

    private void testShowScreen(int screen) {
        if (screen < 0 || screen > 3) return;
        forceShowScreen(screen);
        Toast.makeText(this, "Ekran " + screen + ": " + SCREEN_NAMES[screen], Toast.LENGTH_SHORT).show();
    }

    private void testNextScreen() {
        int next = (currentScreen + 1) % 4;
        testShowScreen(next);
    }

    private void testPrevScreen() {
        int prev = (currentScreen - 1 + 4) % 4;
        testShowScreen(prev);
    }

    // === KEY HANDLING ===

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // D key = toggle test/debug mode
        if (keyCode == KeyEvent.KEYCODE_D) {
            toggleTestMode();
            return true;
        }

        // Test mode controls
        if (testMode) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    testNextScreen();
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    testPrevScreen();
                    return true;
                case KeyEvent.KEYCODE_0:
                    testShowScreen(0);
                    return true;
                case KeyEvent.KEYCODE_1:
                    testShowScreen(1);
                    return true;
                case KeyEvent.KEYCODE_2:
                    testShowScreen(2);
                    return true;
                case KeyEvent.KEYCODE_3:
                    testShowScreen(3);
                    return true;
            }
        }

        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_S) {
            openSettings();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh location
        locationId = prefs.getInt("location_id", 78);
        locationName = prefs.getString("location_name", "Sarajevo");
        if (tvLocation != null) tvLocation.setText(locationName);
        fetchPrayerTimes();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clockTimer != null) clockTimer.cancel();
        if (slideshowTimer != null) slideshowTimer.cancel();
        if (prayerCheckTimer != null) prayerCheckTimer.cancel();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
