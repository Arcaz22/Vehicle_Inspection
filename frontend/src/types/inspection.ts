export type Condition =
  | "GOOD"
  | "MINOR_DAMAGE"
  | "MAJOR_DAMAGE"
  | "NOT_INSPECTABLE";

export type InspectionStatus = "DRAFT" | "SUBMITTED" | "APPROVED" | "RECHECK";

export interface Vehicle {
  id: number;
  plate_number: string;
  type: string;
  brand: string;
  model: string;
  year: number;
}

export interface InspectionItem {
  point_code: string;
  condition: Condition;
  note: string | null;
  photo_refs: string[];
}

export interface Inspection {
  id: number;
  vehicle_id: number;
  client_id: string | null;
  inspector_name: string;
  has_key: boolean;
  status: InspectionStatus;
  version: number;
  created_at: string;
  updated_at: string;
  items: InspectionItem[];
}

export interface InspectionInput {
  vehicle_id: number;
  inspector_name: string;
  has_key: boolean;
  items: InspectionItem[];
}

export interface ApiErrorDetail {
  field: string;
  reason: string;
}

export interface InspectionPoint {
  point_code: string;
  section: string;
  label: string;
}

export interface InspectionItemForm {
  condition: Condition | "";
  note: string;
  photos: string;
}

export type InspectionFormItems = Record<string, InspectionItemForm>;
export type InspectionFieldErrors = Record<string, string>;
