# STIMA Battle Bot (Battlecode 2025)

## Deskripsi Singkat
Proyek ini berisi beberapa bot Battlecode berbasis Java dengan pendekatan algoritma greedy. Setiap bot mengambil keputusan lokal terbaik pada setiap ronde berdasarkan informasi yang tersedia saat itu (paint, jarak, cooldown, dan target terdekat).

## Strategi Greedy Tiap Bot

### 1. mainbot
- **Soldier**: Prioritas utama mengecat area bernilai tertinggi (enemy/empty paint) dengan skor lokal, lalu membangun tower di ruin secara oportunistik jika posisi mendukung. Upgrade tower dijadikan fallback, bukan prioritas utama.
- **Mopper**: Membersihkan enemy paint dengan prioritas area dekat tower sekutu (defensif), melakukan mop swing ke arah paling menguntungkan, serta transfer/withdraw paint sesuai kebutuhan tim.
- **Splasher**: Mengecat target area secara greedy (nilai lokal tertinggi), membantu penyelesaian pola ruin saat relevan, dan menjaga persebaran unit agar tidak menumpuk di sekitar tower.
- **Tower**: Spawn unit berdasarkan rule sederhana (komposisi Mopper/Splasher/Soldier), menyerang musuh yang berada dalam jangkauan, serta mengirim pesan periodik ke unit sekutu.

### 2. alternative_bots_1
- **Karakter umum**: Greedy agresif berbasis target terdekat.
- **Soldier**: Mengejar tower musuh yang terlihat terlebih dahulu, jika tidak ada maka mengejar unit/paint musuh terdekat.
- **Mopper/Splasher**: Menyerang atau membersihkan target yang paling cepat dijangkau, dengan fallback random move saat tidak ada target jelas.
- **Tower**: Memilih tipe unit berdasarkan kondisi paint menara saat ini.

### 3. alternative_bots_2
- **Karakter umum**: Greedy sederhana dengan rule minimum (baseline).
- **Soldier**: Fokus menyelesaikan pattern ruin jika memungkinkan dan upgrade tower saat resource tinggi.
- **Mopper**: Transfer paint ke ally terdekat bila bisa, lalu mop swing.
- **Splasher**: Menyerang tile enemy paint yang langsung bisa dieksekusi.
- **Tower**: Spawn unit berdasarkan threshold chips/paint.

## Requirement dan Instalasi

### Requirement
- Java Development Kit (JDK) 17 atau versi yang kompatibel dengan Battlecode 2025 Java scaffold.
- Gradle Wrapper (sudah tersedia pada proyek: `gradlew` dan `gradlew.bat`).
- Battlecode 2025 Client (folder `client/` pada proyek ini).
- OS Windows/Linux/macOS.

### Instalasi Singkat
1. Clone repository ini.
2. Pastikan Java sudah terpasang dan dikenali sistem:
   - `java -version`
3. Pastikan berada di root folder proyek (`STIMA-battle`).

## Langkah Compile/Build dan Menjalankan

### Windows (PowerShell/CMD)
1. Build proyek:
   - `./gradlew.bat build`
2. Menjalankan simulasi sesuai konfigurasi `gradle.properties`:
   - `./gradlew.bat run`
3. Menjalankan test:
   - `./gradlew.bat test`
4. Membuat file submit:
   - `./gradlew.bat zipForSubmit`
5. Masuk ke folder client:
   - `cd client`
6. Jalankan Battlecode Client (exe):
   - `./Stima Battle Client.exe`
7. Kembali ke root proyek (opsional):
   - `cd ..`

### Linux/macOS
1. Build proyek:
   - `./gradlew build`
2. Menjalankan simulasi:
   - `./gradlew run`
3. Menjalankan test:
   - `./gradlew test`
4. Membuat file submit:
   - `./gradlew zipForSubmit`

## Author
| No | Nama                                 | NIM      | 
|----|--------------------------------------|----------|
| 1  | Muhammad Azzam Robbani               | 18223025 | 
| 2  | Surya Suharna                        | 18223075 | 
| 3  | Ni Made Sekar Jelita Parameswari     | 18223101 | 
