import { useCallback, useState } from "react";
import { inspectionApi } from "../services/inspection-api";
import type { InspectionInput } from "../types/inspection";

export function useInspectionApi() {
  const [busy, setBusy] = useState(false);

  const run = useCallback(async <T>(action: () => Promise<T>): Promise<T> => {
    setBusy(true);
    try {
      return await action();
    } finally {
      setBusy(false);
    }
  }, []);

  const vehicles = useCallback(() => run(inspectionApi.vehicles), [run]);
  const detail = useCallback(
    (id: number) => run(() => inspectionApi.detail(id)),
    [run],
  );
  const create = useCallback(
    (input: InspectionInput) => run(() => inspectionApi.create(input)),
    [run],
  );
  const update = useCallback(
    (id: number, version: number, input: InspectionInput) =>
      run(() => inspectionApi.update(id, version, input)),
    [run],
  );
  const submit = useCallback(
    (id: number, actor: string) => run(() => inspectionApi.submit(id, actor)),
    [run],
  );

  return { busy, vehicles, detail, create, update, submit };
}
