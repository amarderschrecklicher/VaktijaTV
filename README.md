# Vaktija TV — Mesdžid Prikaz Vakata

Minimalistička Android aplikacija za TV ekrane u džamijama. Dizajnirana u modernom Apple stilu.

---

## FUNKCIONALNOSTI

- **Slideshow ekran** (default): Hadisi i ajeti na bosanskom, sat, datum, lokacija
- **Vakat ekran**: Prikazuje svih 6 vakta 30 minuta prije i za trajanja namaza
- **Džuma ekran**: Svaki petak — poseban ekran sa džuma ajetom (El-Džumu'a 9-10), centriran 40 min
- **Ezan upozorenje**: 2 minute prije svakog vakta — opcija za otkazivanje automatskog ezana
- **Automatski ezan**: Ako se ne otkaže, pušta ezan automatski sa zvučnika
- **Sabah ezan offset**: Ručno podešavanje koliko minuta prije/poslije sabah vakta ide ezan

---

## KAKO BUILDOVATI APK

### Potrebno
- **Android Studio** (besplatan) — https://developer.android.com/studio
- Java JDK 11+ (dolazi s Android Studiom)

### Koraci
1. Otvori Android Studio
2. `File → Open` → Odaberi `VaktijaTVApp/` folder
3. Pričekaj da Gradle sync završi (preuzima dependencije ~5 min)
4. `Build → Build Bundle(s) / APK(s) → Build APK(s)`
5. APK se nalazi u: `app/build/outputs/apk/debug/app-debug.apk`

### Za release APK (potpisan)
1. `Build → Generate Signed Bundle / APK`
2. Odaberi APK, kreiraj keystore
3. Builduj release APK

---

## DODAVANJE EZANA (ZVUK)

**VAŽNO**: Placeholder audio fajlovi u `app/src/main/res/raw/` su tihi/prazni.
Moraš ih zamijeniti pravim ezan audio fajlovima:

- `azan.mp3` — Ezan za podne, ikindija, akšam, jacija (standardni)
- `azan_fajr.mp3` — Ezan za sabah (sa "Es-salatu hajrum mine-n-nevm")

Format: MP3, mono ili stereo, bilo koji bitrate. Preporučeno: 128kbps.

---

## PODEŠAVANJA (na TV-u)

- Pritisni **Menu** dugme na TV daljinskom (ili tipku S na tastaturi)
- Otvara se ekran za podešavanja:
  - Odabir lokacije (svi gradovi BiH i regiona)
  - Sabah ezan offset (minuta prije šuruka ili poslije sabah vakta)

---

## INSTALACIJA NA TV

1. Kopiraj APK na USB
2. Na TV-u: Podešavanja → Sigurnost → Dozvoli instalaciju iz nepoznatih izvora
3. Instaliraj putem file managera
4. Aplikacija se automatski pokreće pri startu TV-a

---

## TEHNIČKE INFORMACIJE

- API: https://api.vaktija.ba/vaktija/v1/{lokacija_id}
- Sarajevo = ID 78 (default)
- Podaci se osvježavaju svakog dana pri pokretanju
- Minimalna verzija Androida: 5.0 (API 21)
- Optimizovano za landscape TV ekrane (1080p / 4K)

---

## STRUKTURA PROJEKTA

```
VaktijaTVApp/
├── app/src/main/
│   ├── java/ba/vaktija/tv/
│   │   ├── MainActivity.java       # Glavni ekran, logika, ezan
│   │   ├── SettingsActivity.java   # Podešavanja
│   │   └── BootReceiver.java       # Auto-start pri paljenju TV-a
│   ├── res/
│   │   ├── layout/                 # XML layouti
│   │   ├── drawable/               # Pozadine, dugmad
│   │   ├── values/                 # Boje, stringovi, teme
│   │   ├── raw/                    # Ezan audio fajlovi ← ZAMIJENI OVE
│   │   └── mipmap-*/              # Ikonica aplikacije
│   └── AndroidManifest.xml
├── build.gradle
└── README.md
```
