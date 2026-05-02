import { Suspense } from "react";
import { DecorativeBackground } from "@/components/decorative-background";
import { LoginForm } from "@/components/login-form";

export default function LoginPage() {
  return (
    <main className="ose-page flex items-center justify-center px-5 py-12">
      <DecorativeBackground />
      <div className="relative z-10 flex w-full justify-center">
        <Suspense>
          <LoginForm />
        </Suspense>
      </div>
    </main>
  );
}

