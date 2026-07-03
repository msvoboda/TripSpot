# TripSpot Starter

Startovací Android aplikace pro koncept TripSpot.

## Stack
- Kotlin
- Jetpack Compose
- Material 3
- Room SQLite
- Repository pattern
- Fake API interface

## Co obsahuje
- seznam Tripů
- detail Tripu jako jednoduchý roadbook/timeline
- lokální SQLite přes Room
- `TripSpotApi` interface + `FakeTripSpotApi`

## Spuštění
1. Otevři složku `TripSpotStarter` v Android Studiu.
2. Nech doběhnout Gradle Sync.
3. Spusť aplikaci na emulátoru nebo telefonu.
4. Klikni na `Vytvořit ukázkový trip`.

## Další kroky
- přidat reálné formuláře pro Trip a Spot
- GPS načítání
- fotky přes CameraX / Photo Picker
- reálné API místo `FakeTripSpotApi`
- mapa a GPX import
