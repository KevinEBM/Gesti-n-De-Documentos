import { RouterProvider } from "@tanstack/react-router";
import { QueryClient } from "@tanstack/react-query";
import { router } from "./router";

const queryClient = new QueryClient();

function App() {
  return <RouterProvider router={router} context={{ queryClient }} />;
}

export default App;