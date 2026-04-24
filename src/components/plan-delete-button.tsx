"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { showToast } from "@/lib/toast-client";
import { cn } from "@/lib/utils";

type PlanDeleteButtonProps = {
  planId: string;
  afterDelete?: "refresh" | "redirect";
  className?: string;
};

export function PlanDeleteButton({ planId, afterDelete = "refresh", className }: PlanDeleteButtonProps) {
  const router = useRouter();
  const [deleting, setDeleting] = useState(false);

  async function deletePlan() {
    if (deleting) return;
    if (!confirm("确定要删除这个学习计划吗？此操作不可撤销。")) return;

    setDeleting(true);
    try {
      const response = await fetch(`/api/plan/${planId}`, { method: "DELETE" });
      if (!response.ok) {
        showToast({ title: "删除失败", description: response.status === 404 ? "计划不存在或无权限访问。" : "请稍后再试。" });
        return;
      }
      showToast({ title: "计划已删除" });
      if (afterDelete === "redirect") {
        router.push("/plan");
      }
      router.refresh();
    } catch {
      showToast({ title: "删除失败", description: "网络异常，请稍后再试。" });
    } finally {
      if (afterDelete !== "redirect") setDeleting(false);
    }
  }

  return (
    <Button
      type="button"
      variant="secondary"
      size="sm"
      onClick={deletePlan}
      disabled={deleting}
      className={cn("border border-red-200 bg-white text-red-600 shadow-sm hover:bg-red-50 hover:text-red-700", className)}
    >
      <Trash2 className="h-4 w-4" />
      {deleting ? "删除中..." : "删除计划"}
    </Button>
  );
}
