# Dağıtık Hata-Tolere Kayıt Sistemi (Distributed Fault-Tolerant Disk Register)

Bu proje, **Dağıtık Sistemler** dersi kapsamında geliştirilmiş; verileri birden fazla sunucuda yedekleyen, hata toleranslı ve yüksek performanslı bir mesaj kayıt sistemidir.

## 🚀 Proje Özellikleri

1.  **Lider-Üye Mimarisi:** İstemciler sadece Lider ile konuşur; Lider, yükü ve replikasyonu yönetir.
2.  **Hata Toleransı (Fault Tolerance):** Bir sunucu çökse bile (Crash Fault), veriler diğer sunucudan okunarak sistemin sürekliliği sağlanır.
3.  **Protokol Çeşitliliği:**
    * **İstemci <-> Lider:** Java Soket Programlama (Text-based TCP).
    * **Lider <-> Üye:** gRPC (Google Protocol Buffers).
4.  **Zero-Copy Disk I/O:** Veriler RAM'de tutulmaz. Java NIO (`FileChannel`) kullanılarak yüksek performansla diske (`data/` klasörlerine) yazılır.
5.  **Dinamik Konfigürasyon:** `tolerance.conf` dosyası ile replikasyon sayısı (N) ayarlanabilir.

---

## 🛠️ Gereksinimler

* Java JDK 11 veya üzeri
* Apache Maven

---

## ⚙️ Kurulum ve Derleme

Projeyi derlemek ve bağımlılıkları indirmek için proje dizininde şu komutu çalıştırın:

```bash
mvn clean compile


▶️ Çalıştırma Adımları
Sistemi tam fonksiyonlu (Hata Toleranslı) olarak çalıştırmak için 4 farklı terminal açınız ve aşağıdaki komutları sırasıyla giriniz:

1. Terminal: Birinci Üye Sunucu (Port 5555)
Bash

mvn exec:java "-Dexec.mainClass=hdd.member.MemberServer" "-Dexec.args=5555"
2. Terminal: İkinci Üye Sunucu (Port 5556)
Bash

mvn exec:java "-Dexec.mainClass=hdd.member.MemberServer" "-Dexec.args=5556"
3. Terminal: Lider Sunucu (Port 6666)
Lider sunucu, tolerance.conf dosyasındaki ayara göre (Örn: 2) her iki üyeyi de otomatik tanır.

Bash

mvn exec:java "-Dexec.mainClass=hdd.member.LeaderServer"
4. Terminal: İstemci (Client)
Bash

mvn exec:java "-Dexec.mainClass=hdd.client.SimpleTextClient"
🧪 Test Senaryoları
İstemci terminalinde aşağıdaki komutları kullanarak sistemi test edebilirsiniz:

1. Veri Kaydetme (Replikasyon Testi)
Veriyi sisteme kaydeder. Lider bu veriyi hem 5555 hem de 5556 portlu üyelere yazar.

Plaintext

SET 101 Merhaba_Dagitik_Sistem
Kontrol: Proje klasöründe data_5555 ve data_5556 klasörlerinde 101.txt dosyası oluşur.

2. Veri Okuma
Kaydedilen veriyi geri okur.

Plaintext

GET 101
3. Hata Toleransı Testi (Fault Tolerance - 100 Puanlık Senaryo)
Sistemin çökmelere karşı dayanıklılığını test etmek için:

Veriyi kaydedin:

Plaintext

SET 999 TestMesaji
Terminal 1'e gidip Ctrl+C ile 5555 portlu sunucuyu kapatın (Sunucu Çökmesi Simülasyonu).

İstemciden veriyi tekrar isteyin:

Plaintext

GET 999
Sonuç: Sistem, 5555 kapalı olsa bile veriyi 5556'dan getirerek çalışmaya devam eder ve < Liderden Cevap: TestMesaji çıktısını verir.

📂 Proje Yapısı
src/main/proto: gRPC servis tanımları (FamilyService.proto).

src/main/java/hdd/member/LeaderServer.java: Koordinatör sunucu (Socket + gRPC Client).

src/main/java/hdd/member/MemberServer.java: Veriyi diske yazan depolama birimi (gRPC Server + NIO).

src/main/java/hdd/client/SimpleTextClient.java: Son kullanıcı arayüzü (Socket Client).

tolerance.conf: Replikasyon sayısı ayarı.
