# Backend Layanan Inspeksi Kendaraan

Backend menggunakan Quarkus 3 dan Java 21. PostgreSQL menyimpan kendaraan, inspeksi, dan item inspeksi. MongoDB menyimpan catatan audit.

## Menjalankan backend

Dari direktori root repository, jalankan PostgreSQL dan MongoDB:

```sh
docker compose up -d postgres mongo
```

Lalu dari direktori `backend/`, jalankan aplikasi dalam mode pengembangan:

```sh
./mvnw quarkus:dev
```

Migrasi Flyway berjalan saat aplikasi dimulai. Secara bawaan, backend memakai database lokal di port yang ditentukan konfigurasi aplikasi. Jika kredensial berbeda dari nilai bawaan, atur variabel lingkungan yang sesuai dengan `.env`.

## Dokumentasi dan pemeriksaan

- Swagger UI: `http://localhost:8080/q/swagger-ui/`
- Spesifikasi OpenAPI: `http://localhost:8080/q/openapi`
- Pemeriksaan kesiapan: `http://localhost:8080/q/health/ready`

Pemeriksaan kesiapan melaporkan koneksi PostgreSQL dan MongoDB.

## Tes dan build

Pastikan PostgreSQL dan MongoDB aktif, lalu jalankan dari direktori `backend/`:

```sh
./mvnw test
```

Untuk membuat paket aplikasi:

```sh
./mvnw package
```
