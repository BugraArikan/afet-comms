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


<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 24" src="https://github.com/user-attachments/assets/87eb2f37-830c-434f-924f-fb08baefcce2" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 24 (4)" src="https://github.com/user-attachments/assets/73a26c24-31a9-4c98-8ecd-42f5a678c528" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 24 (3)" src="https://github.com/user-attachments/assets/43c79fed-e585-403e-a8c0-9c28f144651f" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 24 (2)" src="https://github.com/user-attachments/assets/f53ec3d3-b39c-4e25-a61c-65e4588a99a5" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 24 (1)" src="https://github.com/user-attachments/assets/972625ec-d6d2-4d04-b299-2d18e69a88c6" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 23" src="https://github.com/user-attachments/assets/85f365fa-66ea-404d-accd-9e9236158880" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 23 (2)" src="https://github.com/user-attachments/assets/9944cbb0-d8aa-49ac-880b-f168bced7a63" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 23 (1)" src="https://github.com/user-attachments/assets/a1326bb7-9ff0-4039-b97c-e399ecbede19" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 22" src="https://github.com/user-attachments/assets/fe780d63-8eee-40d4-bf8f-6f464fdee0f4" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 22 (2)" src="https://github.com/user-attachments/assets/52f4404d-e81e-4348-96ad-eca3b7d29d31" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 22 (1)" src="https://github.com/user-attachments/assets/c7f04e25-c77b-4933-8b3b-913daee515f4" />
