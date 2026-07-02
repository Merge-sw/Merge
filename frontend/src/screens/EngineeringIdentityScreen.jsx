import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';

export default function EngineeringIdentityScreen() {
  const navigate = useNavigate();
  
  // Data states
  const [student, setStudent] = useState(null);
  const [activeSection, setActiveSection] = useState("identity"); // "identity" | "competencies"
  
  // Load local student profile details on mount
  useEffect(() => {
    const studentRaw = localStorage.getItem('merge_student');
    if (studentRaw) {
      try {
        setStudent(JSON.parse(studentRaw));
      } catch(e) {}
    } else {
      // Fallback defaults
      setStudent({
        fullName: "David Park",
        email: "david@unilag.edu.ng",
        current_stage: "CADET",
        total_xp: 1350
      });
    }
  }, []);

  // Mock datasets mapping to the planned backend API schema
  const mockIdentity = {
    rank: "Rank #14 / 220",
    percentile: "Top 8%",
    githubRepo: "github.com/davidpark-cs/merge-portfolio",
    momentumHistory: [
      { week: "Week 1", state: "COMPILING", xp: 150 },
      { week: "Week 2", state: "BUILDING", xp: 450 },
      { week: "Week 3", state: "DEPLOYING", xp: 550 },
      { week: "Week 4", state: "DEPLOYING", xp: 200 }
    ],
    badges: [
      { id: "b1", title: "Fall 2025 Gold Medalist", desc: "Finished in the top 10% of the UNILAG CS Cohort by XP.", type: "gold" },
      { id: "b2", title: "Clean Code Champion", desc: "Passed three consecutive Builds with a perfect Clean Code rating.", type: "gold" },
      { id: "b3", title: "Git Push Velocity", desc: "Pushed 15+ automated commits to portfolio repository.", type: "silver" }
    ]
  };

  const mockCompetencies = [
    {
      id: "comp-1",
      name: "Software Design & Architecture",
      sfiaCode: "DESN",
      level: 3,
      status: "VERIFIED",
      description: "Applies design patterns, component frameworks, and design principles to build robust web application structures.",
      evidence: [
        { type: "COMMIT", hash: "f9b8c2d", desc: "Implemented circular routing guards in LoadBalancer component", url: "https://github.com/davidpark-cs/merge-portfolio/commit/f9b8c2d" },
        { type: "SUBMISSION", hash: "Build #1", desc: "Submitted CloudStream networks capstone project with clean SOLID layout", url: "#" }
      ]
    },
    {
      id: "comp-2",
      name: "Testing & Debugging",
      sfiaCode: "TEST",
      level: 3,
      status: "VERIFIED",
      description: "Designs structured unit tests and mocks targeting boundary edge failures and service latency recovery.",
      evidence: [
        { type: "COMMIT", hash: "a84d91e", desc: "Added integration tests asserting timeout recovery states", url: "https://github.com/davidpark-cs/merge-portfolio/commit/a84d91e" }
      ]
    },
    {
      id: "comp-3",
      name: "Code Quality",
      sfiaCode: "QUMG",
      level: 2,
      status: "IN_PROGRESS",
      description: "Maintains high readability, semantic naming metrics, and conforms to lint-free guidelines without functional redundancy.",
      evidence: [
        { type: "SUBMISSION", hash: "Drill 2", desc: "Completed isHalfOpen circuit breaker cooldown assessment", url: "#" }
      ]
    },
    {
      id: "comp-4",
      name: "Problem Solving & Analysis",
      sfiaCode: "PROS",
      level: 2,
      status: "IN_PROGRESS",
      description: "Decomposes conceptual specs into operational pipelines, sorting lists, and structural iteration loops.",
      evidence: []
    }
  ];

  const currentStage = student ? student.current_stage || "CADET" : "CADET";
  const totalXp = student ? student.total_xp || 1350 : 1350;

  return (
    <div className="min-h-screen flex flex-col bg-background text-on-surface relative font-body-md text-body-md select-text">
      {/* Header */}
      <header className="h-[48px] bg-surface border-b border-outline-variant flex justify-between items-center px-margin-md z-40">
        <div className="flex items-center gap-4">
          <button 
            onClick={() => navigate('/dashboard')}
            className="flex items-center gap-1.5 hover:text-primary transition-colors text-on-surface-variant font-label-caps text-label-caps"
          >
            <span className="material-symbols-outlined text-[16px]">arrow_back</span>
            <span>Dashboard</span>
          </button>
        </div>
        <div className="flex items-center gap-4">
          <span className="px-2 py-0.5 border border-primary bg-primary/5 text-primary font-mono-code text-[10px] uppercase">
            verified engineering identity
          </span>
        </div>
      </header>

      {/* Profile Header Block */}
      <section className="bg-surface-container-low border-b border-outline-variant py-10 px-margin-md">
        <div className="max-w-4xl mx-auto flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
          <div className="flex items-center gap-5 text-left">
            {/* User Initials Avatar */}
            <div className="w-16 h-16 bg-surface-container-highest border-2 border-primary flex items-center justify-center font-display font-extrabold text-2xl text-primary">
              {student ? student.fullName.split(' ').map(n => n[0]).join('') : "DP"}
            </div>
            
            <div className="space-y-1">
              <h1 className="font-display text-[26px] font-extrabold text-on-surface tracking-tight leading-none">
                {student ? student.fullName : "David Park"}
              </h1>
              <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-on-surface-variant font-mono-code">
                <span>{student ? student.email : "david@unilag.edu.ng"}</span>
                <span className="text-outline-variant">&#8226;</span>
                <span className="text-primary font-bold uppercase">STAGE: {currentStage}</span>
              </div>
            </div>
          </div>

          <div className="flex gap-4">
            <div className="border border-outline-variant p-4 bg-surface-container-lowest text-center min-w-28">
              <span className="font-mono-code text-[9px] text-on-surface-variant uppercase block mb-0.5">Rank Position</span>
              <span className="font-mono-code text-sm font-bold text-on-surface">{mockIdentity.rank.split(' / ')[0]}</span>
            </div>
            <div className="border border-outline-variant p-4 bg-surface-container-lowest text-center min-w-28">
              <span className="font-mono-code text-[9px] text-on-surface-variant uppercase block mb-0.5">Formation XP</span>
              <span className="font-mono-code text-sm font-bold text-primary">{totalXp} XP</span>
            </div>
          </div>
        </div>
      </section>

      {/* Main Section */}
      <main className="flex-grow max-w-4xl w-full mx-auto px-margin-md py-8">
        {/* Navigation Tabs */}
        <div className="flex border-b border-outline-variant/60 mb-8">
          <button 
            onClick={() => setActiveSection("identity")}
            className={`font-label-caps text-label-caps uppercase py-2.5 px-4 border-b-2 transition-all ${
              activeSection === "identity" 
                ? "border-primary text-primary" 
                : "border-transparent text-on-surface-variant hover:text-on-surface"
            }`}
          >
            Engineering Portfolio
          </button>
          <button 
            onClick={() => setActiveSection("competencies")}
            className={`font-label-caps text-label-caps uppercase py-2.5 px-4 border-b-2 transition-all ${
              activeSection === "competencies" 
                ? "border-primary text-primary" 
                : "border-transparent text-on-surface-variant hover:text-on-surface"
            }`}
          >
            SFIA Competency Matrix
          </button>
        </div>

        {/* Tab 1: Engineering Identity */}
        {activeSection === "identity" && (
          <div className="grid grid-cols-1 md:grid-cols-12 gap-8 text-left">
            {/* Left Col: Badges & Links */}
            <div className="md:col-span-7 space-y-6">
              {/* GitHub Link Card */}
              <div className="border border-outline-variant bg-[#16181D] p-5">
                <span className="font-mono-code text-[9px] text-primary uppercase tracking-widest block mb-2">VERIFIED PORTFOLIO REPOSITORY</span>
                <div className="flex justify-between items-center gap-4">
                  <span className="font-mono-code text-xs text-on-surface font-semibold truncate">
                    {mockIdentity.githubRepo}
                  </span>
                  <a 
                    href={`https://${mockIdentity.githubRepo}`} 
                    target="_blank" 
                    rel="noreferrer"
                    className="flex items-center gap-1.5 border border-outline-variant hover:bg-surface-container-high px-3 py-1.5 font-label-caps text-[10px] uppercase text-on-surface transition-colors"
                  >
                    <span>View Repo</span>
                    <span className="material-symbols-outlined text-xs">open_in_new</span>
                  </a>
                </div>
              </div>

              {/* Season Badges Section */}
              <div className="space-y-4">
                <h3 className="font-label-caps text-label-caps text-on-surface uppercase">Earned Season Badges</h3>
                
                <div className="grid grid-cols-1 gap-4">
                  {mockIdentity.badges.map(b => (
                    <div key={b.id} className="border border-outline-variant/60 p-4 bg-surface-container-lowest flex items-start gap-4">
                      <div className={`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 border ${
                        b.type === 'gold' 
                          ? 'bg-yellow-500/10 border-yellow-500 text-yellow-500' 
                          : 'bg-zinc-400/10 border-zinc-400 text-zinc-400'
                      }`}>
                        <span className="material-symbols-outlined text-[20px]">workspace_premium</span>
                      </div>
                      <div className="space-y-1">
                        <span className="font-body-md text-body-md font-bold text-on-surface block">
                          {b.title}
                        </span>
                        <p className="text-xs text-on-surface-variant leading-relaxed">
                          {b.desc}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Right Col: Momentum Velocity */}
            <div className="md:col-span-5 space-y-6">
              <div className="border border-outline-variant bg-[#16181D] p-5 space-y-4">
                <span className="font-mono-code text-[9px] text-primary uppercase tracking-widest block">COHORT POSITION METRICS</span>
                
                <div className="space-y-2">
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-on-surface-variant">Percentile Threshold</span>
                    <span className="font-mono-code font-bold text-green-400">{mockIdentity.percentile}</span>
                  </div>
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-on-surface-variant">Class Standing</span>
                    <span className="font-mono-code font-bold text-on-surface">{mockIdentity.rank}</span>
                  </div>
                </div>

                <div className="h-px bg-outline-variant/40 my-2"></div>

                <span className="font-mono-code text-[9px] text-on-surface-variant uppercase tracking-widest block mb-1">
                  Weekly Momentum Timeline
                </span>
                
                <div className="space-y-3">
                  {mockIdentity.momentumHistory.map((h, idx) => (
                    <div key={idx} className="flex justify-between items-center text-xs font-mono-code">
                      <span className="text-on-surface-variant">{h.week}</span>
                      <span className={`px-2 py-0.5 text-[9px] uppercase border ${
                        h.state === 'DEPLOYING' 
                          ? 'bg-green-500/10 border-green-500/30 text-green-400' 
                          : h.state === 'BUILDING' 
                            ? 'bg-blue-500/10 border-blue-500/30 text-blue-400' 
                            : 'bg-zinc-500/10 border-zinc-500/30 text-zinc-400'
                      }`}>
                        {h.state}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tab 2: Competency Matrix */}
        {activeSection === "competencies" && (
          <div className="space-y-6 text-left">
            <div className="bg-surface-container border border-outline-variant p-5 text-xs text-on-surface-variant leading-relaxed mb-6">
              <p>
                Merge competencies map directly to **Skills Framework for the Information Age (SFIA)** standards. Each competency level is verified cryptographically by linking passed sandbox code execution commits directly to your GitHub repository credentials.
              </p>
            </div>

            <div className="space-y-4">
              {mockCompetencies.map(c => (
                <div key={c.id} className="border border-outline-variant bg-[#16181D] p-6 space-y-4">
                  {/* Title Bar */}
                  <div className="flex justify-between items-start gap-4">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-display font-bold text-headline-md text-on-surface">
                          {c.name}
                        </span>
                        <span className="px-1.5 py-0.5 bg-surface-container border border-outline-variant text-[9px] font-mono-code text-on-surface-variant">
                          SFIA: {c.sfiaCode}
                        </span>
                      </div>
                      <p className="text-xs text-on-surface-variant leading-relaxed">
                        {c.description}
                      </p>
                    </div>
                    
                    <div className="text-right flex-shrink-0">
                      <span className={`px-2 py-1 text-[10px] font-mono-code font-bold uppercase border block mb-1 ${
                        c.status === 'VERIFIED' 
                          ? 'bg-green-500/10 border-green-500/30 text-green-400' 
                          : 'bg-blue-500/10 border-blue-500/30 text-blue-400'
                      }`}>
                        {c.status}
                      </span>
                      <span className="text-[10px] font-mono-code text-on-surface-variant">
                        TARGET LEVEL {c.level}
                      </span>
                    </div>
                  </div>

                  {/* Evidence List */}
                  {c.evidence.length > 0 && (
                    <div className="border-t border-outline-variant/30 pt-3 space-y-2">
                      <span className="font-mono-code text-[9px] text-primary uppercase block">Verified Sandbox Evidence</span>
                      
                      <div className="divide-y divide-outline-variant/20 bg-surface-container-lowest border border-outline-variant/40">
                        {c.evidence.map((ev, idx) => (
                          <div key={idx} className="p-3 flex justify-between items-center text-xs gap-4 font-mono-code">
                            <div className="flex items-center gap-2 truncate">
                              <span className="px-1.5 py-0.5 bg-surface-container text-[9px] border border-outline-variant text-on-surface-variant uppercase font-mono-code">
                                {ev.type}
                              </span>
                              <span className="text-on-surface truncate">{ev.desc}</span>
                            </div>
                            
                            {ev.url !== "#" ? (
                              <a 
                                href={ev.url} 
                                target="_blank" 
                                rel="noreferrer" 
                                className="flex items-center gap-1 hover:text-primary transition-colors text-[10px] text-on-surface-variant uppercase font-label-caps flex-shrink-0"
                              >
                                <span>Commit</span>
                                <span className="material-symbols-outlined text-[10px]">open_in_new</span>
                              </a>
                            ) : (
                              <span className="text-[9px] text-on-surface-variant uppercase font-mono-code flex-shrink-0">Passed Gate</span>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
