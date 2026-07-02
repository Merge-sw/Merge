import React from 'react';
import { useNavigate } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';

export default function ScoutIntroScreen() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen flex flex-col items-center justify-center p-6 relative bg-[#10131a] text-[#e1e2ec] overflow-hidden">
      <main className="relative z-10 w-full max-w-md flex flex-col items-center">
        <header className="mb-10 text-center">
          <MergeLogo className="font-display font-extrabold uppercase tracking-widest text-[36px]" />
        </header>

        <div className="w-full bg-[#16181D] border border-outline-variant p-12 flex flex-col items-center text-center space-y-8">
          <div className="space-y-4">
            <h1 className="font-label-caps text-[22px] font-extrabold text-primary tracking-widest uppercase block">
              SCOUT ASSESSMENT
            </h1>
            <p className="font-body-lg text-body-lg text-on-surface-variant leading-relaxed">
              Before we build anything, let's find out where you actually are.
            </p>
          </div>

          <div className="w-full pt-4">
            <button 
              onClick={() => navigate('/scout/layer-1')}
              className="w-full bg-[#3b82f6] text-white font-label-caps text-body-md py-4 transition-all hover:bg-blue-600 active:scale-[0.98] flex items-center justify-center space-x-2 rounded-none"
            >
              <span className="uppercase tracking-widest">START PROFILE INTAKE</span>
              <span className="material-symbols-outlined text-sm">arrow_forward</span>
            </button>
          </div>
        </div>

        <div className="mt-8 w-full flex justify-between items-center px-2 opacity-30">
          <div className="flex space-x-4">
            <span className="font-mono-code text-[10px] uppercase">STATUS: PENDING_INTAKE</span>
            <span className="font-mono-code text-[10px] uppercase">VER: 2026_FALL</span>
          </div>
          <span className="font-mono-code text-[10px] uppercase tracking-tighter">&copy; 2026 MERGE</span>
        </div>
      </main>
    </div>
  );
}
