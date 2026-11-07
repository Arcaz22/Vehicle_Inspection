# Form Inspeksi Kendaraan

Frontend React + TypeScript untuk formulir inspeksi kendaraan. Rute `/inspections/new` membuat draft, sedangkan `/inspections/:id` memuat dan mengubah draft atau inspeksi berstatus RECHECK. Status SUBMITTED dan APPROVED ditampilkan sebagai baca-saja.

## Menjalankan

1. Jalankan backend dari root repository: `docker compose up --build -d`.
2. Masuk ke `frontend`, jalankan `npm install` lalu `npm run dev`.
3. Buka alamat yang ditampilkan Vite, lalu kunjungi `/inspections/new`.

Vite meneruskan request `/api` ke `http://localhost:8080`. Jika backend memakai alamat lain, buat `frontend/.env.local` dengan `VITE_API_URL=http://localhost:PORT` (lihat `.env.example`) lalu ulangi `npm run dev`. Saat frontend di-deploy, siapkan reverse proxy `/api` ke backend pada origin yang sama; proxy Vite hanya berlaku saat pengembangan.

## Perilaku formulir

- Kendaraan diambil dari E1; titik inspeksi dibaca langsung dari `../checklist.json`.
- Simpan draft mengirim E2 untuk data baru atau E3 dengan `version` terkini untuk data yang sudah tersimpan. Titik yang belum diisi tidak dikirim saat menyimpan draft.
- Simpan dan kirim memvalidasi 14 titik, menyimpan data terlebih dahulu, lalu memanggil E6 dengan `to: SUBMITTED` dan `actor` dari nama pemeriksa. Jika transisi gagal, draft yang telah disimpan tetap dapat dibuka di URL inspeksinya.
- Tanpa kunci, titik ENGINE otomatis menjadi NOT_INSPECTABLE dengan alasan yang bisa diubah. Rusak berat wajib punya minimal satu nama file foto; tidak ada upload file.
- Konflik versi (HTTP 409) ditampilkan tanpa menimpa perubahan yang ada di server. Muat ulang halaman untuk mengambil versi terbaru sebelum mencoba lagi.
- UI menyediakan keadaan memuat, kosong, error, dan pemberitahuan hasil aksi. Tema terang/gelap mengikuti pengaturan perangkat.

Perintah verifikasi: `npm run build`, `npm run lint`, dan `npm run test`. Tes browser menggunakan Chrome atau Edge lokal melalui `playwright-core` dan API mock, sehingga tidak mengubah data backend. Jika browser tidak terdeteksi otomatis, set `CHROME_PATH` ke lokasi executable Chrome/Chromium.
