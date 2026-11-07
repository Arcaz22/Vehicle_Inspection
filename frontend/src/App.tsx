import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
} from "react-router-dom";
import InspectionForm from "./features/inspection/InspectionForm";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/inspections/new" replace />} />
        <Route path="/inspections/new" element={<InspectionForm />} />
        <Route path="/inspections/:id" element={<InspectionForm />} />
        <Route path="*" element={<Navigate to="/inspections/new" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
