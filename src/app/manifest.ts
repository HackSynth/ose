import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "OSE 软考备考",
    short_name: "OSE",
    description: "Open Software Exam 软考备考系统",
    start_url: "/dashboard",
    display: "standalone",
    background_color: "#FFF8F0",
    theme_color: "#E8642C",
  };
}
