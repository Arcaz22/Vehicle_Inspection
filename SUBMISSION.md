# SUBMISSION

| Item | Isi |
|---|---|
| Nama kandidat | Chandra Arcychan Azfar |
| URL repository | https://github.com/Arcaz22/Vehicle_Inspection.git |
| Tag final | `v1.0-submission` |
| Tanggal selesai | 08 November 2026 |
| Total jam kerja (perkiraan jujur) | - 2 hari |

## 1. Cara verifikasi

| Langkah | Perintah / URL |
|---|---|
| Backend + database | `docker compose up --build` (http://localhost:8080) |
| OpenAPI | `http://localhost:8080/q/openapi` |
| Health | `http://localhost:8080/q/health/ready` (PostgreSQL dan MongoDB: UP) |
| Tes backend | `cd backend && ./mvnw test` (4 tes lulus) |
| Seed data 50.000 inspeksi | `uv run --no-project --python 3.12 scripts/seed_via_api.py --count 50000`   |
| Frontend | `cd frontend && npm run build && npm run lint` (lulus) |

## 2. Status fitur

Status berdasarkan source dan hasil verifikasi yang disebutkan pada catatan.

| Item | Status | Catatan |
|---|---|---|
| E1 GET /api/v1/vehicles (20 kendaraan dari migrasi) | Selesai | Mengembalikan HTTP 200; migrasi menyediakan 20 kendaraan. |
| E2 POST /api/v1/inspections | Selesai | Digunakan oleh tes backend dan seed 50.000 data. |
| E3 PUT /api/v1/inspections/{id} | Selesai | Tes regresi penggantian items lulus setelah perbaikan collision unique key. |
| E4 GET /api/v1/inspections/{id} | Selesai | ID 6801 berhasil dimuat melalui API. |
| E5 GET /api/v1/inspections | Selesai | Query daftar menghasilkan HTTP 200 dan dipakai saat pengukuran performa. |
| E6 POST /api/v1/inspections/{id}/transition | Sebagian | Transisi ilegal DRAFT ke APPROVED diuji menghasilkan 409; transisi submit valid belum diuji otomatis. |
| E7 POST /api/v1/inspections/sync | Sebagian | Seed 50.000 berhasil; tes mencakup CREATED, DUPLICATE, dan CONFLICT. S5 belum diuji otomatis. |
| E8 GET /api/v1/inspections/{id}/audit | Selesai | Endpoint mengembalikan HTTP 200; koleksi audit MongoDB berisi 50.000 dokumen setelah seed. |
| R1 point_code, condition, dan vehicle_id valid | Sebagian | Validasi ada di service; belum ada tes khusus seluruh kasus R1. |
| R2 tanpa kunci, titik ENGINE wajib NOT_INSPECTABLE | Selesai | Tes mengirim kondisi ENGINE yang salah saat `has_key=false`; respons 400. |
| R3 NOT_INSPECTABLE wajib note, MAJOR_DAMAGE wajib foto | Sebagian | Validasi ada di service; belum ada tes khusus R3. |
| R4 transisi status sah (selain itu 409) | Selesai | Tes transisi DRAFT ke APPROVED mengonfirmasi respons 409. |
| R5 SUBMITTED hanya bila 14 titik terisi | Sebagian | Validasi ada di service; belum ada tes khusus R5. |
| R6 hanya DRAFT/RECHECK yang bisa diubah, version basi ditolak (409) | Sebagian | Implementasi tersedia; belum ada tes khusus stale version/status final. |
| R7 audit log tercatat di MongoDB | Selesai | MongoDB menunjukkan 50.000 dokumen audit setelah seed. |
| S1 sync: client_id baru, CREATED | Selesai | Seed menghasilkan 50.000 CREATED tanpa item gagal. |
| S2 sync: kirim ulang batch sama, DUPLICATE | Selesai | Dicakup tes backend; hasil DUPLICATE. |
| S3 sync: base_version sesuai, UPDATED | Sebagian | Implementasi tersedia; belum ada tes khusus UPDATED. |
| S4 sync: data basi atau APPROVED, CONFLICT | Selesai | Dicakup tes backend; base_version basi menghasilkan CONFLICT. |
| S5 sync: satu item invalid, item lain tetap diproses | Sebagian | Implementasi tersedia; belum ada tes khusus isolasi item batch. |
| Health check memeriksa PostgreSQL dan MongoDB | Selesai | `/q/health/ready` melaporkan kedua pemeriksaan UP. |
| Migrasi database dan index | Selesai | Flyway V1 membuat skema/data kendaraan; V2 menambahkan index daftar inspeksi. |
| Tes otomatis (minimal 4) | Selesai | 4 tes lulus: PUT replacement, R2, R4, S2, dan S4. |
| Frontend: halaman form inspeksi | Selesai | Form tersedia; build dan lint berhasil. Penyimpanan form masih online-only. |
| Bonus (maksimal 1, sebutkan yang dikerjakan) | Tidak | Tidak dikerjakan. |

## 3. Performa (sebelum dan sesudah)

Data 50.000 inspeksi. Latensi endpoint adalah median beberapa request lokal berurutan (3 baseline, 5 sesudah). `EXPLAIN` membandingkan query PostgreSQL sebelum dan sesudah index.

Seed selesai dalam 1188.3 detik; laju akhir yang dilaporkan script 42 inspeksi/detik, 50.000 CREATED dan 0 gagal.

| Query / endpoint | Sebelum (ms) | Sesudah (ms) | Perubahan | Ringkasan EXPLAIN |
|---|---|---|---|---|
| `GET /inspections?status=SUBMITTED&limit=20&page=1` | 41.905 | 19.298 | -53.9% | Seq Scan + top-N Sort, 17.315 ms -> Index Scan, 0.103 ms |
| `GET /inspections?status=SUBMITTED&limit=20&page=2000` | 64.175 | 25.494 | -60.3% | Seq Scan + external merge sort, 47.231 ms -> Index Scan, 5.693 ms |

## 4. Refleksi

**Q1.** Risiko keamanan terbesar adalah API belum memiliki autentikasi atau otorisasi, sehingga siapa pun yang dapat menjangkau server bisa membaca atau mengubah inspeksi. PostgreSQL menyimpan status inspeksi, sedangkan MongoDB menyimpan audit; karena keduanya tidak berbagi transaksi, kegagalan saat menulis lintas database dapat membuat status dan audit tidak sinkron. Referensi foto hanya berupa nama file, bukan file yang diunggah atau disimpan aplikasi.

**Q2.** Saya akan menambahkan autentikasi dan otorisasi sebelum API dipakai di lingkungan publik, lalu menguji kegagalan saat penulisan audit MongoDB. Setelah itu saya akan menetapkan kebijakan backup dan retensi untuk PostgreSQL serta MongoDB, dan menambah antrean lokal agar form dapat menyimpan data saat perangkat offline.

## 5. Asumsi dan keputusan di luar kontrak

- Frontend belum memiliki antrean lokal offline; sinkronisasi offline tersedia di endpoint E7, tetapi form saat ini mengirim langsung ke server.

## 6. Belum dikerjakan atau diketahui bermasalah (beserta alasan)

- Belum ada tes otomatis khusus R1, R3, R5, R6, S3, dan S5.
- Form frontend belum mendukung simpan lokal ketika offline.
- Halaman daftar inspeksi frontend (bonus B2) tidak dikerjakan.
- `npm audit` frontend (7 November 2026): 10 moderate, 0 high, 0 critical. Temuan berada pada rantai dev dependency ESLint/TypeScript ESLint melalui `minimatch`; saran fix npm mengubah ESLint ke major 9, jadi belum diterapkan sebelum kompatibilitas diperiksa.

## 7. Deklarasi AI

- Tools yang dipakai: Zed Editor Agent, harness web claude(free tier).
- Bagian yang dibantu AI: review/refactor struktur frontend, diagnosis dan perbaikan PUT, penyusunan tes backend, index performa, README, dan draft submission.
- Bagian yang ditulis sendiri: inisialisasi project baik backend/frontend, alur awal seperti struktur folder yang digunakan(layered arsitektur), Base awal gaya penulisannya(nanti di review ai lalu di refactor).
- Satu contoh saat hasil AI salah atau berlebihan, dan bagaimana Anda menyadarinya: Saran awal tes memakai `Response.path("data.id", Number.class)` tidak sesuai API RestAssured; compiler testCompile melaporkan signature tidak cocok, lalu ekstraksi diperbaiki menjadi `Object` dan dikonversi melalui `Number.longValue()`.
