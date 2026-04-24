import { DecorativeBackground } from "@/components/decorative-background";
import { PasswordResetRequestForm } from "@/components/password-reset-request-form";

export default function ResetPasswordRequestPage() {
  return (
    <main className="ose-page flex items-center justify-center px-5 py-12">
      <DecorativeBackground />
      <div className="relative z-10 flex w-full justify-center">
        <PasswordResetRequestForm />
      </div>
    </main>
  );
}
