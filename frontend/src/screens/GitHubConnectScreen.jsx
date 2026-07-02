import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';

export default function GitHubConnectScreen() {
  const navigate = useNavigate();
  const [connecting, setConnecting] = useState(false);

  const handleConnect = () => {
    setConnecting(true);
    setTimeout(() => {
      navigate('/setup/gemini');
    }, 1200);
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-6 relative bg-[#10131a] text-[#e1e2ec] overflow-hidden">
      <main className="relative z-10 w-full max-w-md">
        <div className="bg-[#16181D] border border-outline-variant p-12 flex flex-col items-center text-center space-y-8">
          <div className="inline-block bg-surface-container-high px-3 py-1.5 border border-outline-variant">
            <MergeLogo className="font-display font-extrabold uppercase tracking-widest text-[13px]" />
          </div>

          <div className="flex items-center justify-center space-x-6 py-4 opacity-80">
            <div className="w-12 h-12 border border-outline-variant flex items-center justify-center bg-surface-container-lowest">
              <span className="material-symbols-outlined text-on-surface-variant text-[22px]">terminal</span>
            </div>
            <div className="h-px w-8 bg-outline-variant"></div>
            <div className="w-12 h-12 border border-outline-variant flex items-center justify-center bg-surface-container-lowest">
              <span className="material-symbols-outlined text-primary text-[22px]">hub</span>
            </div>
          </div>

          <div className="space-y-4">
            <h1 className="font-headline-lg text-headline-lg text-on-surface">Initialize Integration</h1>
            <p className="font-body-lg text-body-lg text-on-surface-variant leading-relaxed">
              Connecting GitHub will create a public portfolio repository on your account. This repository becomes your real engineering portfolio, tracking every contribution and milestone.
            </p>
          </div>

          <div className="w-full flex flex-col items-center space-y-6 pt-4">
            <button 
              disabled={connecting}
              onClick={handleConnect}
              className="w-full bg-[#3b82f6] text-white font-label-caps text-body-md py-4 transition-all hover:bg-blue-600 active:scale-[0.98] focus:ring-2 focus:ring-white focus:outline-none flex items-center justify-center space-x-2 group rounded-none disabled:opacity-85 disabled:cursor-wait"
            >
              {connecting ? (
                <>
                  <span className="uppercase tracking-widest">Redirecting...</span>
                  <span className="material-symbols-outlined text-sm animate-spin">sync</span>
                </>
              ) : (
                <>
                  <span className="uppercase tracking-widest">Connect GitHub</span>
                  <span className="material-symbols-outlined text-sm group-hover:translate-x-1 transition-transform">arrow_forward</span>
                </>
              )}
            </button>

            <button 
              onClick={() => navigate('/setup/gemini')}
              className="font-label-caps text-label-caps text-on-surface-variant uppercase hover:text-on-surface transition-colors bg-transparent border-none p-0 cursor-pointer"
            >
              Skip for now
            </button>
          </div>
        </div>

        <div className="mt-8 flex justify-between items-center px-2 opacity-30">
          <div className="flex space-x-4">
            <span className="font-mono-code text-[10px] uppercase">STATUS: AWAITING_AUTH</span>
            <span className="font-mono-code text-[10px] uppercase">VERSION: 2.4.0-STABLE</span>
          </div>
          <span className="font-mono-code text-[10px] uppercase tracking-tighter">&copy; 2026 MERGE</span>
        </div>
      </main>
    </div>
  );
}
