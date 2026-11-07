# Layanan Inspeksi Kendaraan

Aplikasi web untuk mencatat dan meninjau inspeksi kendaraan. Backend menyediakan REST API Quarkus dengan PostgreSQL untuk data kendaraan dan inspeksi, serta MongoDB untuk catatan audit yang hanya ditambahkan dan tidak diubah. Frontend React + TypeScript menyediakan formulir inspeksi.

## Arsitektur

- `backend/`: Quarkus 3 dan Java 21, berisi endpoint REST, layanan aturan bisnis, repository Panache, migrasi Flyway, serta layanan audit MongoDB.
- `frontend/`: Vite, React, dan TypeScript. Rute `/inspections/new` dan `/inspections/:id` menggunakan formulir inspeksi yang sama. Akses API berada di `services/`, pengelolaan status pemanggilan API di `hooks/`, dan tipe API/form di `types/`.
- `scripts/seed_via_api.py`: skrip pengisi data yang mengirim inspeksi dalam batch melalui endpoint sinkronisasi.
- `checklist.json`: daftar titik inspeksi untuk frontend, salinannya juga dimuat sebagai resource backend.
- `docker-compose.yml`: konfigurasi layanan backend, PostgreSQL, dan MongoDB.

## Menjalankan aplikasi

Salin `.env.example` menjadi `.env`, sesuaikan kredensial bila perlu, lalu jalankan layanan:

```sh
docker compose up --build
```

API tersedia di `http://localhost:8080`. Buka Swagger UI di `http://localhost:8080/q/swagger-ui/` untuk mencoba endpoint. Spesifikasi OpenAPI tersedia di `http://localhost:8080/q/openapi`, sedangkan pemeriksaan kesiapan di `/q/health/ready` melaporkan status PostgreSQL dan MongoDB.

Jalankan frontend di terminal lain:

```sh
cd frontend
npm install
npm run dev
```

Vite meneruskan permintaan `/api` ke backend. `VITE_API_URL` di `frontend/.env.example` digunakan untuk mengatur alamat tujuan proxy tersebut.

## Tes dan pengisian data

Pastikan PostgreSQL dan MongoDB berjalan:

```sh
docker compose up -d postgres mongo
```

Jalankan tes backend dari direktori `backend/`:

```sh
./mvnw test
```

Tes menggunakan database lokal yang dikonfigurasi melalui `DB_JDBC_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `MONGO_CONNECTION_STRING`, dan `MONGO_DATABASE`. Jika shell tidak memuat `.env` secara otomatis, atur variabel tersebut dengan nilai yang sama seperti di `.env` sebelum menjalankan tes.

Isi 50.000 inspeksi melalui endpoint sinkronisasi E7 dengan `uv`:

```sh
uv run --no-project --python 3.12 scripts/seed_via_api.py --count 50000
```

Atau gunakan Python 3.8+:

```sh
python3 scripts/seed_via_api.py --count 50000
```

Jalankan pemeriksaan frontend dari direktori `frontend/`:

```sh
npm run build
npm run lint
```

## Keputusan teknis dan konsekuensinya

1. **MongoDB untuk catatan audit:** event audit berbentuk dokumen yang hanya ditambahkan, sehingga disimpan terpisah dari data inspeksi relasional. Konsekuensinya, perubahan status di PostgreSQL dan penulisan event audit di MongoDB tidak berada dalam satu transaksi database.
2. **Idempotensi dan konflik sinkronisasi:** `client_id` mengidentifikasi inspeksi dari perangkat. Data kiriman yang identik menghasilkan `DUPLICATE`; `base_version` yang usang atau inspeksi server yang tidak lagi dapat diubah menghasilkan `CONFLICT`. Cara ini mencegah perangkat offline menimpa data server yang lebih baru, tetapi aplikasi klien perlu menangani konflik tersebut.
3. **Performa daftar inspeksi:** indeks gabungan `(status, updated_at DESC, id DESC)` membantu pencarian berdasarkan status dan pengurutan halaman yang konsisten. Indeks memakai ruang database dan menambah beban saat penulisan; hasil pengukuran endpoint dan ringkasan `EXPLAIN (ANALYZE)` dicatat di `SUBMISSION.md`.

## Asumsi dan deklarasi penggunaan AI

- Checklist berisi 14 titik inspeksi CAR yang disediakan bersama technical test.
- Form frontend saat ini menyimpan langsung ke server dan belum memiliki antrean lokal untuk kondisi offline. Sinkronisasi offline tersedia melalui E7 untuk integrasi klien.
- Bantuan AI: Zed Editor Agent dengan asisten AI digunakan untuk meninjau repositori, merapikan lapisan frontend, mendiagnosis masalah penggantian item saat PUT, serta membantu menyusun tes dan dokumentasi.
