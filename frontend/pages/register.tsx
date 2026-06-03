import { RegisterCard } from '@/components/ui/register-card';
import { BackgroundGradientAnimation } from '@/components/ui/background-gradient-animation';

export default function RegisterPage() {
  return (
    <BackgroundGradientAnimation interactive>
      <div className="absolute inset-0 z-50 flex items-center justify-center px-4 py-8 overflow-y-auto">
        <div className="w-full max-w-sm" style={{ perspective: 1500 }}>
          <RegisterCard />
        </div>
      </div>
    </BackgroundGradientAnimation>
  );
}
