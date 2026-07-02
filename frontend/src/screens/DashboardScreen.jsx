import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';

export default function DashboardScreen() {
  const navigate = useNavigate();
  const [xp, setXp] = useState(1250);

  useEffect(() => {
    const studentRaw = localStorage.getItem('merge_student');
    if (studentRaw) {
      try {
        const student = JSON.parse(studentRaw);
        if (student.total_xp) setXp(student.total_xp);
      } catch(e) {}
    }
  }, []);

  const handleResumeTask = () => {
    if (xp > 1250) {
      navigate('/build-workspace');
    } else {
      navigate('/session/start');
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('merge_jwt');
    localStorage.removeItem('merge_student');
    localStorage.removeItem('merge_gemini_token');
    navigate('/register');
  };

  const remainingXp = Math.max(0, 1500 - xp);

  return (
    <div className="min-h-screen bg-background text-on-surface">
      <header className="fixed top-0 w-full h-[48px] bg-surface flex justify-between items-center px-margin-md border-b border-outline-variant z-50">
        <div className="flex items-center gap-4">
          <span className="cursor-pointer" onClick={() => navigate('/dashboard')}>
            <MergeLogo className="font-display font-bold uppercase tracking-widest text-[20px]" />
          </span>
        </div>
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-4 text-on-surface-variant">
            <button 
              className="hover:bg-surface-container-highest p-1 transition-colors active:opacity-80 text-primary" 
              onClick={() => navigate('/identity')} 
              title="Verified Engineering Identity & Portfolio"
            >
              <span className="material-symbols-outlined">workspace_premium</span>
            </button>
            <button className="hover:bg-surface-container-highest p-1 transition-colors active:opacity-80" title="Help Guide">
              <span className="material-symbols-outlined">help</span>
            </button>
            <button className="hover:bg-surface-container-highest p-1 transition-colors active:opacity-80" onClick={handleLogout} title="Log Out / Settings">
              <span className="material-symbols-outlined">settings</span>
            </button>

          </div>
        </div>
      </header>

      <main className="pt-[48px] min-h-screen">
        <div className="grid grid-cols-12 min-h-[calc(100vh-48px)]">
          <section className="col-span-8 functional-border border-l-0 border-t-0 p-margin-md flex flex-col gap-10">
            <div>
              <span className="font-label-caps text-label-caps text-primary mb-2 block">WEEKLY MOMENTUM STATE</span>
              <h1 className="font-display text-[80px] font-extrabold leading-none tracking-tighter text-on-surface mb-4">VELOCITY</h1>
              <p className="font-body-lg text-body-lg text-on-surface-variant max-w-lg">Maintaining high-density throughput across core systems.</p>
            </div>

            <div className="functional-border bg-surface-container p-6">
              <div className="flex justify-between items-end mb-6">
                <div>
                  <span className="font-label-caps text-label-caps text-on-surface-variant block mb-1">CURRENT MODULE</span>
                  <h2 className="font-headline-lg text-headline-lg text-on-surface">CloudStream Networks</h2>
                  <span className="font-label-caps text-[10px] text-on-surface-variant hover:text-primary transition-colors block mt-1 cursor-pointer">VIEW IN STAGE</span>
                  <span className="font-mono-code text-mono-code text-primary">Circuit Breaker Mechanism</span>
                </div>
                <div className="text-right">
                  <span className="font-label-caps text-label-caps text-on-surface-variant block mb-1">FORMATION STATUS</span>
                  <span className="px-2 py-1 bg-primary/10 border border-primary text-primary font-label-caps text-[10px]">
                    {xp > 1250 ? "COMPLETED" : "ACTIVE DRILL"}
                  </span>
                </div>
              </div>

              <div className="grid grid-cols-3 gap-gutter bg-outline-variant functional-border overflow-hidden">
                <div className="bg-surface-container-low p-4 flex items-center gap-3 opacity-60">
                  <span className="font-mono-code text-mono-code text-on-surface-variant">01</span>
                  <span className="font-body-md text-body-md text-on-surface-variant">Explanation</span>
                  <span className="material-symbols-outlined text-primary text-sm ml-auto" style={{ fontVariationSettings: "'FILL' 1" }}>check_circle</span>
                </div>

                <div className="bg-surface-container-low p-4 flex items-center gap-3 opacity-60">
                  <span className="font-mono-code text-mono-code text-on-surface-variant">02</span>
                  <span className="font-body-md text-body-md text-on-surface-variant">Drill 1</span>
                  <span className="material-symbols-outlined text-primary text-sm ml-auto" style={{ fontVariationSettings: "'FILL' 1" }}>check_circle</span>
                </div>

                <div className={`p-4 flex items-center gap-3 transition-colors duration-150 ${xp > 1250 ? "bg-surface-container-low opacity-60" : "bg-surface-container-high border-l-2 border-primary"}`}>
                  <span className={`font-mono-code text-mono-code ${xp > 1250 ? "text-on-surface-variant" : "text-primary"}`}>03</span>
                  <span className={`font-body-md text-body-md ${xp > 1250 ? "text-on-surface-variant" : "text-on-surface font-bold"}`}>Drill 2</span>
                  {xp > 1250 ? (
                    <span className="material-symbols-outlined text-primary text-sm ml-auto" style={{ fontVariationSettings: "'FILL' 1" }}>check_circle</span>
                  ) : (
                    <span className="material-symbols-outlined text-primary text-sm ml-auto animate-pulse">keyboard_arrow_right</span>
                  )}
                </div>
              </div>
            </div>

            <div>
              <button 
                onClick={handleResumeTask}
                className="bg-[#3B82F6] hover:bg-[#2563EB] text-white px-10 py-5 font-display text-headline-md font-bold tracking-tight transition-all active:scale-[0.98] w-full md:w-auto"
              >
                {xp > 1250 ? "ENTER BUILD WORKSPACE" : "RESUME TASK"}
              </button>
            </div>
          </section>

          <section className="col-span-4 bg-surface-container-low functional-border border-r-0 border-t-0 p-margin-md flex flex-col gap-8">
            <div className="space-y-6">
              <div>
                <span className="font-label-caps text-label-caps text-on-surface-variant block mb-2">ENGINEER STAGE</span>
                <div className="flex items-center gap-3">
                  <h3 className="font-display text-headline-lg font-bold text-on-surface">CADET</h3>
                  <span className="px-2 py-0.5 border border-primary/40 bg-primary/5 text-primary font-label-caps text-[10px]">ON PACE</span>
                </div>
              </div>
              
              <div className="space-y-2">
                <div className="flex justify-between font-mono-code text-mono-code">
                  <span className="text-on-surface-variant">PROGRESSION</span>
                  <span className="text-on-surface">{xp.toLocaleString()} / 1,500 XP</span>
                </div>
                {remainingXp === 0 ? (
                  <button 
                    onClick={() => navigate('/promote')}
                    className="w-full mt-2 bg-gradient-to-r from-blue-700 to-blue-500 hover:from-blue-600 hover:to-blue-400 text-white py-2.5 font-label-caps text-[10px] uppercase tracking-widest animate-pulse transition-all cursor-pointer rounded-none border-none"
                  >
                    Promotion Gateway Unlocked
                  </button>
                ) : (
                  <p className="font-label-caps text-label-caps text-on-surface-variant">{remainingXp.toLocaleString()} XP TO NEXT STAGE</p>
                )}
              </div>
            </div>

            <div className="h-px bg-outline-variant"></div>

            <div className="space-y-4">
              <span className="font-label-caps text-label-caps text-on-surface-variant block">ACTIVE MISSIONS</span>
              <div className="functional-border overflow-hidden zebra-list">
                <div className="p-4 flex flex-col gap-1 group cursor-pointer hover:bg-surface-container-highest transition-colors">
                  <div className="flex justify-between items-start">
                    <span className="font-body-md text-body-md font-bold text-on-surface group-hover:text-primary transition-colors">JWT Refresh Token Flow</span>
                    <span className="px-2 py-0.5 border border-primary/30 text-primary font-label-caps text-[9px] bg-primary/5">DONE</span>
                  </div>
                  <span className="font-mono-code text-[11px] text-on-surface-variant">DUE OCT 12</span>
                </div>

                <div className="p-4 flex flex-col gap-1 group cursor-pointer border-t border-outline-variant/30 hover:bg-surface-container-highest transition-colors">
                  <div className="flex justify-between items-start">
                    <span className="font-body-md text-body-md font-bold text-on-surface group-hover:text-primary transition-colors">Secret Management Strategy</span>
                    <span className={`px-2 py-0.5 border font-label-caps text-[9px] ${xp > 1250 ? "border-primary/30 text-primary bg-primary/5" : "border-secondary/30 text-secondary bg-secondary/5"}`}>
                      {xp > 1250 ? "DONE" : "IN PROGRESS"}
                    </span>
                  </div>
                  <span className="font-mono-code text-[11px] text-on-surface-variant">DUE OCT 12</span>
                </div>

                <div className="p-4 flex flex-col gap-1 group cursor-pointer border-t border-outline-variant/30 hover:bg-surface-container-highest transition-colors">
                  <div className="flex justify-between items-start">
                    <span className="font-body-md text-body-md font-bold text-on-surface group-hover:text-primary transition-colors">Redis Cache Invalidation</span>
                    <span className="px-2 py-0.5 border border-on-surface-variant/30 text-on-surface-variant font-label-caps text-[9px]">NOT STARTED</span>
                  </div>
                  <span className="font-mono-code text-[11px] text-on-surface-variant">DUE OCT 15</span>
                </div>

                <div className="p-4 flex flex-col gap-1 group cursor-pointer border-t border-outline-variant/30 hover:bg-surface-container-highest transition-colors">
                  <div className="flex justify-between items-start">
                    <span className="font-body-md text-body-md font-bold text-on-surface group-hover:text-primary transition-colors">Load Balancer Health Check</span>
                    <span className="px-2 py-0.5 border border-on-surface-variant/30 text-on-surface-variant font-label-caps text-[9px]">NOT STARTED</span>
                  </div>
                  <span className="font-mono-code text-[11px] text-on-surface-variant">DUE OCT 16</span>
                </div>
              </div>
            </div>
          </section>
        </div>

        <section className="p-margin-md bg-surface functional-border border-x-0 border-b-0">
          <div className="max-w-screen-2xl mx-auto">
            <div className="flex items-center gap-4 mb-6">
              <h2 className="font-label-caps text-label-caps text-primary tracking-widest">COHORT ACTIVITY STREAM</h2>
              <span className="font-label-caps text-[10px] text-on-surface-variant ml-4">VIEW COHORT</span>
              <span onClick={() => navigate('/peer-review')} className="font-label-caps text-[10px] text-on-surface-variant hover:text-primary transition-colors ml-4 cursor-pointer">PEER REVIEW GATEWAY</span>
              <div className="flex-grow h-px bg-outline-variant"></div>
            </div>


            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div className="functional-border p-4 bg-surface-container-low flex items-start gap-4 hover:bg-surface-container-highest transition-colors duration-150">
                <div className="w-10 h-10 bg-surface-container-highest functional-border flex items-center justify-center flex-shrink-0">
                  <span className="material-symbols-outlined text-primary">person</span>
                </div>
                <div className="flex flex-col">
                  <span className="font-body-md text-body-md text-on-surface">
                    <span className="font-bold">Elena Rossi</span> passed a Drill
                  </span>
                  <span className="font-mono-code text-[11px] text-on-surface-variant uppercase mt-1">2m ago</span>
                </div>
              </div>

              <div className="functional-border p-4 bg-[#16181d] flex items-start gap-4 hover:bg-surface-container-highest transition-colors duration-150">
                <div className="w-10 h-10 bg-surface-container-highest functional-border flex items-center justify-center flex-shrink-0">
                  <span className="material-symbols-outlined text-secondary">terminal</span>
                </div>
                <div className="flex flex-col">
                  <span className="font-body-md text-body-md text-on-surface">
                    <span className="font-bold">Marcus Wright</span> pushed a commit
                  </span>
                  <span className="font-mono-code text-[11px] text-on-surface-variant uppercase mt-1">5m ago</span>
                </div>
              </div>

              <div className="functional-border p-4 bg-surface-container-low flex items-start gap-4 hover:bg-surface-container-highest transition-colors duration-150">
                <div className="w-10 h-10 bg-surface-container-highest functional-border flex items-center justify-center flex-shrink-0">
                  <span className="material-symbols-outlined text-primary-container">package_2</span>
                </div>
                <div className="flex flex-col">
                  <span className="font-body-md text-body-md text-on-surface">
                    <span className="font-bold">David Park</span> completed a Build
                  </span>
                  <span className="font-mono-code text-[11px] text-on-surface-variant uppercase mt-1">12m ago</span>
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className="panel-header bg-surface border-t border-outline-variant py-4 justify-center text-center">
        <p className="font-label-caps text-[10px] text-on-surface-variant">
          &copy; 2026 Merge Platform. Confidential &bull; Semicolon Africa Capstone Project.
        </p>
      </footer>
    </div>
  );
}
