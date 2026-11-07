import checklist from "../../../../checklist.json";
import type {
  ApiErrorDetail,
  Condition,
  Inspection,
  InspectionFieldErrors,
  InspectionFormItems,
  InspectionItem,
  InspectionPoint,
} from "../../types/inspection";

export const points = checklist.vehicle_types.CAR as InspectionPoint[];

export const sections = [
  {
    code: "EXTERIOR",
    name: "Eksterior",
    description: "Periksa kondisi bodi dan bagian luar.",
  },
  {
    code: "INTERIOR",
    name: "Interior",
    description: "Periksa kabin dan ruang penyimpanan.",
  },
  {
    code: "ENGINE",
    name: "Mesin",
    description: "Periksa kondisi mesin dan ruang mesin.",
  },
];

export const conditions: { value: Condition; label: string }[] = [
  { value: "GOOD", label: "Baik" },
  { value: "MINOR_DAMAGE", label: "Rusak ringan" },
  { value: "MAJOR_DAMAGE", label: "Rusak berat" },
  { value: "NOT_INSPECTABLE", label: "Tidak dapat diperiksa" },
];

export const statusLabels: Record<Inspection["status"], string> = {
  DRAFT: "Draft",
  SUBMITTED: "Dikirim",
  APPROVED: "Disetujui",
  RECHECK: "Periksa ulang",
};

export const noKeyNote = "Tidak dapat diperiksa tanpa kunci";

export function emptyItems(): InspectionFormItems {
  return Object.fromEntries(
    points.map(({ point_code }) => [
      point_code,
      { condition: "", note: "", photos: "" },
    ]),
  );
}

export function withoutKey(items: InspectionFormItems): InspectionFormItems {
  const next = { ...items };
  for (const point of points.filter((entry) => entry.section === "ENGINE")) {
    next[point.point_code] = {
      ...next[point.point_code],
      condition: "NOT_INSPECTABLE",
      note: next[point.point_code].note.trim() || noKeyNote,
      photos: "",
    };
  }
  return next;
}

export function toFormItems(items: InspectionItem[]): InspectionFormItems {
  const form = emptyItems();
  for (const item of items) {
    if (form[item.point_code])
      form[item.point_code] = {
        condition: item.condition,
        note: item.note || "",
        photos: (item.photo_refs || []).join(", "),
      };
  }
  return form;
}

export function errorFields(
  details: ApiErrorDetail[],
  submittedItems: InspectionItem[],
): InspectionFieldErrors {
  return Object.fromEntries(
    details.map(({ field, reason }) => {
      const match = /^items\[(\d+)\](.*)$/.exec(field);
      if (match) {
        const item = submittedItems[Number(match[1])];
        if (item) return [`items.${item.point_code}${match[2]}`, reason];
      }
      return [field, reason];
    }),
  );
}

export function checkForm(
  name: string,
  vehicleId: string,
  items: InspectionFormItems,
  complete: boolean,
): InspectionFieldErrors {
  const errors: InspectionFieldErrors = {};
  if (!vehicleId) errors.vehicle_id = "Pilih kendaraan.";
  if (!name.trim()) errors.inspector_name = "Nama pemeriksa wajib diisi.";
  for (const point of points) {
    const item = items[point.point_code];
    if (!item.condition) {
      if (complete)
        errors[`items.${point.point_code}`] = "Pilih kondisi untuk titik ini.";
      continue;
    }
    if (item.condition === "NOT_INSPECTABLE" && !item.note.trim()) {
      errors[`items.${point.point_code}.note`] =
        "Tulis alasan titik tidak dapat diperiksa.";
    }
    if (
      item.condition === "MAJOR_DAMAGE" &&
      !item.photos.split(",").some((photo) => photo.trim())
    ) {
      errors[`items.${point.point_code}.photo_refs`] =
        "Tambahkan minimal satu nama file foto.";
    }
  }
  return errors;
}
