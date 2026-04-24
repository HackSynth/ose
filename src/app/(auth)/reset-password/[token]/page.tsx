import { DecorativeBackground } from "@/components/decorative-background";
import { PasswordResetConfirmForm } from "@/components/password-reset-confirm-form";

export default async function ResetPasswordConfirmPage({ params }: { params: Promise<{ token: string }> }) {
  const { token } = await params;
  return (
    <main className="ose-page flex items-center justify-center px-5 py-12">
      <DecorativeBackground />
      <div className="relative z-10 flex w-full justify-center">
        <PasswordResetConfirmForm token={token} />
      </div>
    </main>
  );
}
