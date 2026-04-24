import { Loader2 } from "lucide-react";
import { Card } from "@/components/ui/card";

export default function Loading() {
  return (
    <main className="mx-auto mt-8 max-w-7xl">
      <Card className="flex min-h-64 items-center justify-center p-10 text-center hover:translate-y-0">
        <div>
          <Loader2 className="mx-auto mb-4 h-9 w-9 animate-spin text-primary" />
          <p className="text-lg font-black text-navy">正在准备温暖的学习空间...</p>
          <p className="mt-2 font-semibold text-muted">马上就好</p>
        </div>
      </Card>
    </main>
  );
}
