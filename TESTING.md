# AfetComms — Test Rehberi

## Tek cihaz (Debug / Simülasyon)

Debug derlemesinde `USE_FAKE_TRANSPORT=true` — gerçek BLE/Wi-Fi kullanılmaz.

1. Uygulamayı çalıştır, kurulumu tamamla (ad + aile kodu).
2. **Güvendeyim** veya **SOS** gönder.
3. Mesaj durumu önce `OUTBOX`, ardından `SENT` olmalı.
4. ~2 saniye sonra **Mesajlar** ekranında `SIM_Aile_Uyesi` gönderenli `RECEIVED` mesaj görünmeli.
5. **Mesajlar** ekranında özet satırı, TTL süresi ve (varsa) **Başarısızları yeniden gönder** butonu görünür.
6. Ana ekranda konum izni verilirse GPS ~15 sn'de bir güncellenir.
7. **Ayarlar** → simülasyon aç/kapa, aile kodu, SOS uyarıları.
8. **SOS** basınca kırmızı tam ekran uyarı + bildirim; ~2 sn sonra (açıksa) `SIM_Aile_Uyesi` → `RECEIVED`.
9. **Güvendeyim** varsayılan olarak sahte yanıt üretmez; Ayarlar → "Güvendeyim için de sahte yanıt" ile açılabilir.

## İki cihaz (Release veya `USE_FAKE_TRANSPORT=false`)

1. Her iki cihazda aynı **aile kodu**.
2. Bluetooth açık, tüm izinler verildi.
3. Cihaz A: SOS → Cihaz B: Mesajlar → `RECEIVED`.

## Birim testler

```bash
./gradlew test
```

`MessagePayload` JSON serileştirme testleri `app/src/test` altında.
