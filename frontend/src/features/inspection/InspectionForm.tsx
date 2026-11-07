import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Button } from "../../components/ui/button";
import { useInspectionApi } from "../../hooks/useInspectionApi";
import { ApiError } from "../../services/inspection-api";
import type {
  Condition,
  Inspection,
  InspectionFieldErrors,
  InspectionFormItems,
  InspectionInput,
  InspectionItemForm,
  Vehicle,
} from "../../types/inspection";
import {
  checkForm,
  conditions,
  emptyItems,
  errorFields,
  noKeyNote,
  points,
  sections,
  statusLabels,
  toFormItems,
  withoutKey,
} from "./form-model";
import "../../App.css";

function InspectionForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const {
    vehicles: loadVehicles,
    detail: loadDetail,
    create,
    update,
    submit,
  } = useInspectionApi();
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [inspection, setInspection] = useState<Inspection | null>(null);
  const [vehicleId, setVehicleId] = useState("");
  const [name, setName] = useState("");
  const [hasKey, setHasKey] = useState(true);
  const [items, setItems] = useState<InspectionFormItems>(emptyItems);
  const [errors, setErrors] = useState<InspectionFieldErrors>({});
  const [loadError, setLoadError] = useState("");
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState<{
    text: string;
    kind: "success" | "error";
  } | null>(null);
  const [saving, setSaving] = useState(false);
  const editable =
    !inspection ||
    inspection.status === "DRAFT" ||
    inspection.status === "RECHECK";

  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true);
      setLoadError("");
      setInspection(null);
      setVehicleId("");
      setName("");
      setHasKey(true);
      setItems(emptyItems());
      setErrors({});
      try {
        const vehicleList = await loadVehicles();
        if (!active) return;
        setVehicles(vehicleList);
        if (id) {
          if (!/^\d+$/.test(id) || Number(id) < 1)
            throw new ApiError("ID inspeksi tidak valid.");
          const record = await loadDetail(Number(id));
          if (!active) return;
          setInspection(record);
          setVehicleId(String(record.vehicle_id));
          setName(record.inspector_name);
          setHasKey(record.has_key);
          const loadedItems = toFormItems(record.items);
          setItems(record.has_key ? loadedItems : withoutKey(loadedItems));
        }
      } catch (error) {
        if (active)
          setLoadError(
            error instanceof Error ? error.message : "Gagal memuat inspeksi.",
          );
      } finally {
        if (active) setLoading(false);
      }
    };
    void load();
    return () => {
      active = false;
    };
  }, [id, loadVehicles, loadDetail]);

  useEffect(() => {
    if (!toast) return;
    const timeout = window.setTimeout(() => setToast(null), 5000);
    return () => window.clearTimeout(timeout);
  }, [toast]);

  const changeItem = (code: string, patch: Partial<InspectionItemForm>) => {
    setItems((current) => ({
      ...current,
      [code]: { ...current[code], ...patch },
    }));
    setErrors((current) =>
      Object.fromEntries(
        Object.entries(current).filter(
          ([field]) => !field.startsWith(`items.${code}`),
        ),
      ),
    );
  };

  const changeKey = (checked: boolean) => {
    setHasKey(checked);
    if (!checked) {
      setItems(withoutKey);
      setErrors((current) =>
        Object.fromEntries(
          Object.entries(current).filter(
            ([field]) => !field.startsWith("items.ENG_"),
          ),
        ),
      );
    } else {
      setItems((current) => {
        const next = { ...current };
        for (const point of points.filter(
          (entry) => entry.section === "ENGINE",
        )) {
          const item = current[point.point_code];
          if (item.note === noKeyNote)
            next[point.point_code] = { condition: "", note: "", photos: "" };
        }
        return next;
      });
    }
  };

  const save = async (submitAfter: boolean) => {
    if (saving || !editable) return;
    const validation = checkForm(name, vehicleId, items, submitAfter);
    if (Object.keys(validation).length) {
      setErrors(validation);
      setToast({
        text: "Periksa kolom yang ditandai sebelum melanjutkan.",
        kind: "error",
      });
      return;
    }
    const input: InspectionInput = {
      vehicle_id: Number(vehicleId),
      inspector_name: name.trim(),
      has_key: hasKey,
      items: points
        .filter((point) => items[point.point_code].condition)
        .map((point) => {
          const item = items[point.point_code];
          return {
            point_code: point.point_code,
            condition: item.condition as Condition,
            note: item.note.trim() || null,
            photo_refs: item.photos
              .split(",")
              .map((photo) => photo.trim())
              .filter(Boolean),
          };
        }),
    };
    setSaving(true);
    setErrors({});
    let createdId: number | null = null;
    try {
      const record = inspection
        ? await update(inspection.id, inspection.version, input)
        : await create(input);
      setInspection(record);
      if (!inspection) createdId = record.id;
      if (submitAfter) {
        const submitted = await submit(record.id, name.trim());
        setInspection(submitted);
        setToast({ text: "Inspeksi berhasil dikirim.", kind: "success" });
      } else {
        setToast({ text: "Draft berhasil disimpan.", kind: "success" });
      }
    } catch (error) {
      if (error instanceof ApiError) {
        setErrors(errorFields(error.details, input.items));
        setToast({
          text:
            error.status === 409
              ? "Data berubah di server. Muat ulang halaman sebelum menyimpan lagi."
              : error.message,
          kind: "error",
        });
      } else {
        setToast({
          text: "Gagal menyimpan inspeksi. Coba lagi.",
          kind: "error",
        });
      }
    } finally {
      setSaving(false);
      if (createdId !== null)
        navigate(`/inspections/${createdId}`, { replace: true });
    }
  };

  const filled = points.filter(
    ({ point_code }) => items[point_code].condition,
  ).length;
  const vehicle = vehicles.find((entry) => entry.id === Number(vehicleId));

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main">
        Langsung ke formulir
      </a>
      <header className="site-header">
        <div className="header-inner">
          <Link className="wordmark" to="/inspections/new">
            Inspeksi Kendaraan
          </Link>
          <nav aria-label="Navigasi utama">
            <Link className="header-link" to="/inspections/new">
              Inspeksi baru
            </Link>
          </nav>
        </div>
      </header>
      <main id="main" className="page">
        <div className="page-heading">
          <div>
            <p className="overline">Form inspeksi kendaraan</p>
            <h1>{id ? "Detail inspeksi" : "Inspeksi baru"}</h1>
            <p className="intro">
              Catat kondisi setiap bagian kendaraan. Simpan sebagai draft, lalu
              kirim setelah seluruh titik diperiksa.
            </p>
          </div>
          {inspection && (
            <div className="record-identity">
              <span className="record-number">Inspeksi #{inspection.id}</span>
              <span
                className={`status status-${inspection.status.toLowerCase()}`}
              >
                {statusLabels[inspection.status]}
              </span>
              <span>Versi {inspection.version}</span>
            </div>
          )}
        </div>
        {loading ? (
          <div className="state-panel" role="status">
            Memuat data kendaraan dan inspeksi...
          </div>
        ) : loadError ? (
          <div className="state-panel state-error" role="alert">
            <h2>Data tidak dapat dimuat</h2>
            <p>{loadError}</p>
            <button
              type="button"
              className="secondary-button"
              onClick={() => window.location.reload()}
            >
              Coba lagi
            </button>
          </div>
        ) : vehicles.length === 0 ? (
          <div className="state-panel">
            <h2>Belum ada kendaraan</h2>
            <p>
              Form belum dapat diisi. Pastikan data kendaraan tersedia di
              server.
            </p>
            <button
              type="button"
              className="secondary-button"
              onClick={() => window.location.reload()}
            >
              Muat ulang
            </button>
          </div>
        ) : (
          <form
            onSubmit={(event: FormEvent) => {
              event.preventDefault();
              void save(false);
            }}
            noValidate
          >
            <div className="form-layout">
              <div className="form-main">
                {!editable && (
                  <div className="notice" role="status">
                    Inspeksi {statusLabels[inspection!.status].toLowerCase()}{" "}
                    tidak dapat diubah.
                  </div>
                )}
                <section
                  className="form-section"
                  aria-labelledby="vehicle-heading"
                >
                  <div className="section-heading">
                    <h2 id="vehicle-heading">Data kendaraan</h2>
                    <p>Pastikan kendaraan dan pemeriksa sudah sesuai.</p>
                  </div>
                  <div className="identity-fields">
                    <div className="field">
                      <label htmlFor="vehicle">
                        Kendaraan <span aria-hidden="true">*</span>
                      </label>
                      <select
                        id="vehicle"
                        value={vehicleId}
                        disabled={!!inspection || saving}
                        aria-invalid={!!errors.vehicle_id}
                        aria-describedby={
                          errors.vehicle_id ? "vehicle-error" : undefined
                        }
                        onChange={(event) => {
                          setVehicleId(event.target.value);
                          setErrors((current) =>
                            Object.fromEntries(
                              Object.entries(current).filter(
                                ([field]) => field !== "vehicle_id",
                              ),
                            ),
                          );
                        }}
                      >
                        <option value="">Pilih kendaraan</option>
                        {vehicles.map((entry) => (
                          <option key={entry.id} value={entry.id}>
                            {entry.plate_number} - {entry.brand} {entry.model} (
                            {entry.year})
                          </option>
                        ))}
                      </select>
                      {errors.vehicle_id && (
                        <p className="field-error" id="vehicle-error">
                          {errors.vehicle_id}
                        </p>
                      )}
                      {vehicle && (
                        <p className="field-hint">
                          {vehicle.type} / {vehicle.brand} {vehicle.model}
                        </p>
                      )}
                    </div>
                    <div className="field">
                      <label htmlFor="inspector">
                        Nama pemeriksa <span aria-hidden="true">*</span>
                      </label>
                      <input
                        id="inspector"
                        value={name}
                        disabled={!editable || saving}
                        aria-invalid={!!errors.inspector_name}
                        aria-describedby={
                          errors.inspector_name ? "inspector-error" : undefined
                        }
                        onChange={(event) => {
                          setName(event.target.value);
                          setErrors((current) =>
                            Object.fromEntries(
                              Object.entries(current).filter(
                                ([field]) => field !== "inspector_name",
                              ),
                            ),
                          );
                        }}
                        autoComplete="name"
                      />
                      {errors.inspector_name && (
                        <p className="field-error" id="inspector-error">
                          {errors.inspector_name}
                        </p>
                      )}
                    </div>
                  </div>
                  <label className="key-control" htmlFor="has-key">
                    <input
                      type="checkbox"
                      id="has-key"
                      checked={hasKey}
                      disabled={!editable || saving}
                      onChange={(event) => changeKey(event.target.checked)}
                    />
                    <span>
                      <strong>Kunci kendaraan tersedia</strong>
                      <small>
                        Tanpa kunci, titik mesin otomatis ditandai tidak dapat
                        diperiksa.
                      </small>
                    </span>
                  </label>
                </section>
                {sections.map((section) => (
                  <section
                    className="form-section"
                    key={section.code}
                    aria-labelledby={`${section.code}-heading`}
                  >
                    <div className="section-heading">
                      <h2 id={`${section.code}-heading`}>{section.name}</h2>
                      <p>{section.description}</p>
                    </div>
                    <div className="point-list">
                      {points
                        .filter((point) => point.section === section.code)
                        .map((point) => {
                          const item = items[point.point_code];
                          const prefix = `items.${point.point_code}`;
                          const relevantErrors = Object.entries(errors).filter(
                            ([field]) =>
                              field === prefix ||
                              field.startsWith(`${prefix}.`),
                          );
                          const locked =
                            !editable ||
                            saving ||
                            (!hasKey && section.code === "ENGINE");
                          return (
                            <div className="point" key={point.point_code}>
                              <div className="point-top">
                                <h3>{point.label}</h3>
                                <span className="point-code">
                                  {point.point_code}
                                </span>
                              </div>
                              <fieldset
                                disabled={locked}
                                aria-describedby={
                                  relevantErrors.length
                                    ? `${point.point_code}-error`
                                    : undefined
                                }
                              >
                                <legend>Kondisi {point.label}</legend>
                                <div className="condition-options">
                                  {conditions.map((option) => (
                                    <label
                                      className={`condition-option ${item.condition === option.value ? "selected" : ""}`}
                                      key={option.value}
                                    >
                                      <input
                                        type="radio"
                                        name={`condition-${point.point_code}`}
                                        value={option.value}
                                        checked={
                                          item.condition === option.value
                                        }
                                        onChange={() =>
                                          changeItem(point.point_code, {
                                            condition: option.value,
                                          })
                                        }
                                      />
                                      <span>{option.label}</span>
                                    </label>
                                  ))}
                                </div>
                              </fieldset>
                              <div className="point-fields">
                                <div className="field">
                                  <label htmlFor={`note-${point.point_code}`}>
                                    Catatan{" "}
                                    {item.condition === "NOT_INSPECTABLE" && (
                                      <span aria-hidden="true">*</span>
                                    )}
                                  </label>
                                  <textarea
                                    id={`note-${point.point_code}`}
                                    rows={2}
                                    value={item.note}
                                    disabled={!editable || saving}
                                    aria-invalid={!!errors[`${prefix}.note`]}
                                    aria-describedby={
                                      errors[`${prefix}.note`]
                                        ? `${point.point_code}-error`
                                        : undefined
                                    }
                                    placeholder="Keterangan kondisi, jika diperlukan"
                                    onChange={(event) =>
                                      changeItem(point.point_code, {
                                        note: event.target.value,
                                      })
                                    }
                                  />
                                </div>
                                <div className="field">
                                  <label htmlFor={`photo-${point.point_code}`}>
                                    Nama file foto{" "}
                                    {item.condition === "MAJOR_DAMAGE" && (
                                      <span aria-hidden="true">*</span>
                                    )}
                                  </label>
                                  <input
                                    id={`photo-${point.point_code}`}
                                    type="text"
                                    value={item.photos}
                                    disabled={!editable || saving}
                                    aria-invalid={
                                      !!errors[`${prefix}.photo_refs`]
                                    }
                                    aria-describedby={
                                      errors[`${prefix}.photo_refs`]
                                        ? `${point.point_code}-error`
                                        : undefined
                                    }
                                    placeholder="foto-depan.jpg, foto-samping.jpg"
                                    onChange={(event) =>
                                      changeItem(point.point_code, {
                                        photos: event.target.value,
                                      })
                                    }
                                  />
                                  <p className="field-hint">
                                    Pisahkan beberapa nama file dengan koma.
                                    Tidak mengunggah file.
                                  </p>
                                </div>
                              </div>
                              {relevantErrors.length > 0 && (
                                <div
                                  id={`${point.point_code}-error`}
                                  className="field-error"
                                  role="alert"
                                >
                                  {relevantErrors.map(([field, reason]) => (
                                    <p key={field}>{reason}</p>
                                  ))}
                                </div>
                              )}
                            </div>
                          );
                        })}
                    </div>
                  </section>
                ))}
              </div>
              <aside className="form-sidebar" aria-label="Ringkasan inspeksi">
                <div className="summary">
                  <h2>Ringkasan</h2>
                  <p className="summary-progress">
                    <strong>{filled}</strong> dari {points.length} titik terisi
                  </p>
                  <div
                    className="progress-track"
                    role="progressbar"
                    aria-valuenow={filled}
                    aria-valuemin={0}
                    aria-valuemax={points.length}
                    aria-label="Titik inspeksi terisi"
                  >
                    <span
                      style={{ width: `${(filled / points.length) * 100}%` }}
                    />
                  </div>
                  <p className="summary-copy">
                    Semua titik wajib memiliki kondisi sebelum inspeksi dapat
                    dikirim.
                  </p>
                  {Object.keys(errors).length > 0 && (
                    <p className="summary-error" role="alert">
                      Ada {Object.values(errors).filter(Boolean).length} kolom
                      yang perlu diperiksa.
                    </p>
                  )}
                  {editable && (
                    <div className="actions">
                       <Button
                         type="submit"
                         disabled={saving}
                       >
                         {saving ? "Menyimpan..." : "Simpan draft"}
                       </Button>
                       <Button
                         variant="secondary"
                         type="button"
                         disabled={saving}
                         onClick={() => void save(true)}
                       >
                         {saving ? "Memproses..." : "Simpan dan kirim"}
                       </Button>
                    </div>
                  )}
                  {inspection && (
                    <p className="version-note">
                      Status: {statusLabels[inspection.status]}
                      <br />
                      Versi: {inspection.version}
                    </p>
                  )}
                </div>
              </aside>
            </div>
          </form>
        )}
      </main>
      <footer className="site-footer">Checklist inspeksi kendaraan</footer>
      {toast && (
        <div
          className={`toast toast-${toast.kind}`}
          role={toast.kind === "error" ? "alert" : "status"}
        >
          {toast.text}
          <button
            type="button"
            aria-label="Tutup pemberitahuan"
            onClick={() => setToast(null)}
          >
            Tutup
          </button>
        </div>
      )}
    </div>
  );
}

export default InspectionForm;
