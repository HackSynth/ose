import * as React from "react";
import { cn } from "@/lib/utils";

const Input = React.forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(({ className, type, ...props }, ref) => (
  <input type={type} className={cn("ose-input flex h-12 w-full text-sm placeholder:text-muted", className)} ref={ref} {...props} />
));
Input.displayName = "Input";

export { Input };
