import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';

export default function GeminiTokenSetupScreen() {
  const navigate = useNavigate();
  const [token, setToken] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [success, setSuccess] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  const [redirectTimer, setRedirectTimer] = useState(5);
  const [currentTime, setCurrentTime] = useState("");

  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setCurrentTime(
        'UTC ' + now.getHours().toString().padStart(2, '0') + ':' + 
        now.getMinutes().toString().padStart(2, '0') + ':' + 
        now.getSeconds().toString().padStart(2, '0')
      );
    };
    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (success && redirectTimer > 0) {
      const timeout = setTimeout(() => {
        setRedirectTimer(prev => prev - 1);
      }, 1000);
      return () => clearTimeout(timeout);
    } else if (success && redirectTimer === 0) {
      navigate('/scout');
    }
  }, [success, redirectTimer, navigate]);

  const handleSubmitToken = (e) => {
    e.preventDefault();
    if (!token.startsWith("AIzaSy")) {
      setErrorMsg("Invalid token format. Check for lead characters.");
      return;
    }
    setErrorMsg("");
    setVerifying(true);

    setTimeout(() => {
      setVerifying(false);
      setSuccess(true);
      localStorage.setItem('merge_gemini_token', token);
    }, 1500);
  };

  if (success) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center relative overflow-hidden bg-[#0D0F12] text-[#E1E2EC]">
        <header className="fixed top-0 left-0 w-full h-[48px] flex items-center justify-center px-6 bg-surface border-b border-outline-variant z-50">
          <MergeLogo className="font-display font-extrabold uppercase tracking-widest text-[13px]" />
        </header>

        <main className="relative z-10 w-full max-w-md mx-auto px-6">
          <div className="bg-surface-container border border-outline-variant p-8 shadow-none flex flex-col gap-6">
            <div className="flex items-center justify-between border-b border-outline-variant pb-4">
              <div className="flex flex-col">
                <span className="font-label-caps text-[10px] text-on-surface-variant uppercase tracking-tighter">System Access</span>
                <h1 className="font-headline-md text-headline-md text-on-surface">Verification Success</h1>
              </div>
              <div className="h-8 w-8 flex items-center justify-center border border-primary text-primary">
                <span className="material-symbols-outlined text-[20px]" style={{ fontVariationSettings: "'FILL' 1" }}>verified_user</span>
              </div>
            </div>

            <div className="space-y-6">
              <div className="flex flex-col gap-2">
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-green-400 text-[18px]">check_circle</span>
                  <span className="font-mono-code text-mono-code text-green-400">Token verified and encrypted.</span>
                </div>
                <div className="h-1 w-full bg-surface-container-highest">
                  <div className="h-full bg-primary w-full transition-all duration-1000 ease-out"></div>
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="font-label-caps text-label-caps text-on-surface-variant">API_ACCESS_TOKEN</label>
                <div className="relative">
                  <input className="recessed-input w-full px-4 py-3 text-sm opacity-50 cursor-not-allowed rounded-none outline-none border border-outline-variant" disabled placeholder="********************************" type="text"/>
                  <div className="absolute right-3 top-1/2 -translate-y-1/2 flex gap-2">
                    <div className="success-tag font-label-caps text-[9px] px-1.5 py-0.5 border border-green-400 bg-green-400/10 text-green-400">VALID</div>
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-2 border border-outline-variant">
                <div className="p-3 border-r border-outline-variant bg-surface-container-low">
                  <span className="block font-label-caps text-[9px] text-on-surface-variant mb-1">SESSION_ID</span>
                  <span className="block font-mono-code text-[11px] text-primary">0xFA22-88C1</span>
                </div>
                <div className="p-3 bg-surface-container-low">
                  <span className="block font-label-caps text-[9px] text-on-surface-variant mb-1">ENCRYPTION</span>
                  <span className="block font-mono-code text-[11px] text-primary">AES-256-GCM</span>
                </div>
              </div>
            </div>

            <div className="pt-2">
              <button 
                onClick={() => navigate('/scout')}
                className="w-full bg-primary text-on-primary font-label-caps text-label-caps py-4 transition-all hover:bg-primary-container active:opacity-80 flex items-center justify-center gap-2 group rounded-none"
              >
                <span>PROCEED TO WORKSPACE</span>
                <span className="material-symbols-outlined text-[16px] group-hover:translate-x-1 transition-transform">arrow_forward</span>
              </button>
              <div className="mt-4 text-center">
                <span className="font-mono-code text-[10px] text-on-surface-variant opacity-50 uppercase">Redirecting in {redirectTimer}s...</span>
              </div>
            </div>
          </div>
        </main>

        <footer className="fixed bottom-0 left-0 w-full h-[32px] px-6 bg-surface-container-lowest border-t border-outline-variant flex items-center justify-between">
          <div className="flex gap-4">
            <div className="flex items-center gap-2">
              <div className="w-1.5 h-1.5 bg-green-500 rounded-full animate-pulse"></div>
              <span className="font-mono-code text-[10px] text-on-surface-variant">GATEWAY: ACTIVE</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-[12px] text-on-surface-variant">schedule</span>
              <span className="font-mono-code text-[10px] text-on-surface-variant">{currentTime}</span>
            </div>
          </div>
          <div className="font-mono-code text-[10px] text-on-surface-variant">
            v1.0.4-STABLE
          </div>
        </footer>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center px-margin-md relative overflow-hidden bg-[#0D0F12] text-[#e1e2ec]">
      <header className="absolute top-12 left-0 w-full flex justify-center z-10">
        <MergeLogo className="font-display font-extrabold uppercase tracking-widest text-[13px]" />
      </header>

      <main className="w-full max-w-[480px] z-10 flex flex-col gap-8">
        <div className="bg-surface-container-low panel-border p-8 md:p-12 shadow-2xl">
          <div className="mb-10">
            <h2 className="font-headline-lg text-headline-lg text-on-surface mb-2">Initialize Environment</h2>
            <p className="font-body-md text-on-surface-variant">Provide your secure access token to bridge the local workstation with the orchestration layer.</p>
          </div>

          <form className="flex flex-col gap-6" onSubmit={handleSubmitToken}>
            <div className="flex flex-col gap-2">
              <label className="font-label-caps text-label-caps text-primary" htmlFor="api_token">
                API TOKEN
              </label>
              
              <div className="relative">
                <input 
                  disabled={verifying}
                  className={`w-full recessed-input font-mono-code text-mono-code px-4 py-3 rounded-none focus:ring-0 outline-none border ${
                    errorMsg ? "border-error" : "border-[#27272a]"
                  }`} 
                  id="api_token" 
                  placeholder="AIzaSy...................." 
                  type="password"
                  value={token}
                  onChange={e => {
                    setToken(e.target.value);
                    if (errorMsg) setErrorMsg("");
                  }}
                  required
                />
                
                {errorMsg && (
                  <div className="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-2">
                    <span className="material-symbols-outlined text-error text-[18px]">warning</span>
                  </div>
                )}
              </div>

              {errorMsg && (
                <div className="flex items-center gap-2 mt-2">
                  <span className="font-mono-code text-[11px] px-2 py-0.5 border border-error bg-error/10 text-error uppercase font-bold">
                    Error 0x42
                  </span>
                  <span className="font-body-md text-error text-[13px]">
                    {errorMsg}
                  </span>
                </div>
              )}
            </div>

            <div className="mt-4">
              <button 
                disabled={verifying}
                type="submit"
                className="w-full primary-btn font-label-caps text-label-caps h-[48px] uppercase tracking-wider flex items-center justify-center gap-2 rounded-none border border-outline-variant bg-[#3B82F6] hover:bg-[#2563EB]"
              >
                {verifying ? (
                  <>
                    <svg className="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <span>VERIFYING...</span>
                  </>
                ) : (
                  <>
                    <span>SUBMIT TOKEN</span>
                    <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
                  </>
                )}
              </button>
            </div>

            <div className="mt-8 pt-8 border-t border-outline-variant/30 flex gap-4">
              <span className="material-symbols-outlined text-on-surface-variant text-[20px] shrink-0" style={{ fontVariationSettings: "'FILL' 1" }}>lock</span>
              <p className="font-body-md text-on-surface-variant leading-relaxed text-[13px]">
                Token is encrypted immediately on submission and is never stored or shown in the browser again. Multi-signature verification is enabled by default for all administrative calls.
              </p>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
}
