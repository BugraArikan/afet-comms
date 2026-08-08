# AfetComms

İnternet bağlantısı olmadan aile ve kurtarma ekipleri arasında acil durum iletişimi için Android uygulaması.

## Özellikler

- **Güvendeyim / SOS** mesajları (konum ekli)
- **BLE** ve **Wi-Fi Direct** taşıma katmanı
- **Simülasyon modu** — tek telefonda test
- **SOS tam ekran uyarı** + bildirim + titreşim
- **Mesaj geçmişi**, TTL, outbox / retry
- **Aile üyesi** listesi ve **Ayarlar** ekranı

## Gereksinimler

- Android 8.0+ (API 26)
- Bluetooth LE (gerçek mod için)
- İzinler: Bluetooth, konum (BLE), bildirim, titreşim

## Kurulum

1. Android Studio ile projeyi açın
2. **Sync Project with Gradle Files**
3. Fiziksel cihazda **Run** (emülatörde BLE sınırlıdır)

```bash
./gradlew assembleDebug
```

## Tek cihaz testi (Simülasyon)

Debug derlemesinde varsayılan: **Simülasyon modu açık**.

1. İlk açılışta ad + aile kodu girin
2. **SOS** veya **Güvendeyim** gönderin
3. **Mesajlar** → `SENT` ve ~2 sn sonra `SIM_Aile_Uyesi` → `RECEIVED`
4. **Ayarlar** → simülasyon, SOS uyarıları, profil

Detay: [TESTING.md](TESTING.md)

## İki cihaz testi (Gerçek BLE)

1. Her iki telefonda **Ayarlar** → **Simülasyon modu** kapatın
2. **Aynı aile kodu** ve farklı kullanıcı ID
3. Bluetooth açık, tüm izinler verildi
4. Cihaz A: SOS → Cihaz B: Mesajlar + SOS uyarısı

Release derlemesi varsayılan olarak simülasyon kapalıdır.

## Mimari

```
ui/          → MainActivity, Messages, Settings, SosAlert
ui/main/     → MainViewModel
transport/   → BLE, Wi-Fi Direct, Fake (sim)
data/        → Room (messages, members)
service/     → BleRelayService (foreground)
```

## Sonraki adımlar

- [ ] İki telefonla saha testi
- [ ] Paket adı `com.example` → üretim paketi
- [ ] Play Store imzalama ve gizlilik metni
