import type {
  ApiErrorDetail,
  Inspection,
  InspectionInput,
  Vehicle,
} from "../types/inspection";

export class ApiError extends Error {
  readonly details: ApiErrorDetail[];
  readonly status: number;

  constructor(
    message: string,
    details: ApiErrorDetail[] = [],
    status = 0,
  ) {
    super(message);
    this.name = "ApiError";
    this.details = details;
    this.status = status;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`/api/v1${path}`, {
      ...init,
      headers: { "Content-Type": "application/json", ...init?.headers },
    });
  } catch {
    throw new ApiError(
      "Tidak dapat menghubungi server. Periksa koneksi dan coba lagi.",
    );
  }

  let body: {
    status_code?: number;
    message?: string;
    data?: T;
    errors?: ApiErrorDetail[];
  };
  try {
    body = await response.json();
  } catch {
    throw new ApiError(
      "Respons server tidak dapat dibaca.",
      [],
      response.status,
    );
  }
  if (
    !response.ok ||
    body.status_code !== response.status ||
    body.data === undefined
  ) {
    throw new ApiError(
      body.message || "Permintaan gagal.",
      body.errors || [],
      response.status,
    );
  }
  return body.data;
}

export const inspectionApi = {
  vehicles: () => request<Vehicle[]>("/vehicles"),
  detail: (id: number) => request<Inspection>(`/inspections/${id}`),
  create: (input: InspectionInput) =>
    request<Inspection>("/inspections", {
      method: "POST",
      body: JSON.stringify(input),
    }),
  update: (id: number, version: number, input: InspectionInput) =>
    request<Inspection>(`/inspections/${id}`, {
      method: "PUT",
      body: JSON.stringify({
        version,
        inspector_name: input.inspector_name,
        has_key: input.has_key,
        items: input.items,
      }),
    }),
  submit: (id: number, actor: string) =>
    request<Inspection>(`/inspections/${id}/transition`, {
      method: "POST",
      body: JSON.stringify({ to: "SUBMITTED", actor }),
    }),
};
