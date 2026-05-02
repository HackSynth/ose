import { DecorativeBackground } from "@/components/decorative-background";
import { RegisterForm } from "@/components/register-form";

export default function RegisterPage() {
  return (
    <main className="ose-page flex items-center justify-center px-5 py-12">
      <DecorativeBackground />
      <div className="relative z-10 flex w-full justify-center">
        <RegisterForm />
      </div>
    </main>
  );
}

