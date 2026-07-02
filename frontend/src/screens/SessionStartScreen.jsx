import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';

export default function SessionStartScreen() {
  const navigate = useNavigate();
  
  // Screen views: "mood_selector" | "audio_player" | "success"
  const [viewMode, setViewMode] = useState("mood_selector");
  const [selectedMood, setSelectedMood] = useState("");
  
  // Audio playback states
  const [playing, setPlaying] = useState(false);
  const [progress, setProgress] = useState(0);
  const [timeElapsed, setTimeElapsed] = useState("0:00");
  const [timeTotal, setTimeTotal] = useState("0:45"); // Shortened for quick demo, default ~5 mins in real
  
  const speechUtteranceRef = useRef(null);
  const playbackTimerRef = useRef(null);
  const durationSeconds = 45; // Simulated track duration

  const mockAudioScript = 
    "Hello David. I detect you are feeling exhausted today. Let's adjust our pace. " +
    "Instead of loading code edits right away, we are going to conduct a conceptual reinforcement session on distributed systems resilience. " +
    "A circuit breaker is designed to fail fast, shielding downstream databases and preventing connection cascade pools from exhausting. " +
    "Remember, in production, handling timeouts gracefully is not a nice-to-have, it's a structural requirement. " +
    "Relax, absorb the concepts, and let's compile our energy for the next building stage.";

  useEffect(() => {
    return () => {
      stopSpeech();
    };
  }, []);

  const handleMoodSelect = (mood) => {
    setSelectedMood(mood);
    if (mood === "EXHAUSTED") {
      setViewMode("audio_player");
      initializeSpeech();
    } else {
      // For FRESH or OKAY, route directly to workspace (where session starts)
      localStorage.setItem('merge_session_mood', mood);
      navigate('/workspace');
    }
  };

  const initializeSpeech = () => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel(); // Reset active speech
      const utterance = new SpeechSynthesisUtterance(mockAudioScript);
      utterance.rate = 0.95; // Slightly slower, relaxing pace
      utterance.pitch = 1.0;
      
      utterance.onend = () => {
        setPlaying(false);
        setProgress(100);
        setTimeElapsed("0:45");
        if (playbackTimerRef.current) clearInterval(playbackTimerRef.current);
      };

      speechUtteranceRef.current = utterance;
    } else {
      console.warn("Speech synthesis is not supported by this browser.");
    }
  };

  const startSpeech = () => {
    if (!speechUtteranceRef.current) return;
    
    setPlaying(true);
    window.speechSynthesis.speak(speechUtteranceRef.current);

    // Track simulated player slider
    let elapsed = Math.floor((progress / 100) * durationSeconds);
    
    playbackTimerRef.current = setInterval(() => {
      elapsed++;
      const currentProgress = (elapsed / durationSeconds) * 100;
      setProgress(currentProgress);
      
      const mins = Math.floor(elapsed / 60);
      const secs = elapsed % 60;
      setTimeElapsed(`${mins}:${secs < 10 ? '0' : ''}${secs}`);

      if (elapsed >= durationSeconds) {
        clearInterval(playbackTimerRef.current);
      }
    }, 1000);
  };

  const pauseSpeech = () => {
    setPlaying(false);
    window.speechSynthesis.pause();
    if (playbackTimerRef.current) clearInterval(playbackTimerRef.current);
  };

  const resumeSpeech = () => {
    setPlaying(true);
    window.speechSynthesis.resume();
    
    let elapsed = Math.floor((progress / 100) * durationSeconds);
    playbackTimerRef.current = setInterval(() => {
      elapsed++;
      const currentProgress = (elapsed / durationSeconds) * 100;
      setProgress(currentProgress);
      
      const mins = Math.floor(elapsed / 60);
      const secs = elapsed % 60;
      setTimeElapsed(`${mins}:${secs < 10 ? '0' : ''}${secs}`);

      if (elapsed >= durationSeconds) {
        clearInterval(playbackTimerRef.current);
      }
    }, 1000);
  };

  const togglePlayback = () => {
    if (playing) {
      pauseSpeech();
    } else {
      if (window.speechSynthesis.paused) {
        resumeSpeech();
      } else {
        startSpeech();
      }
    }
  };

  const stopSpeech = () => {
    window.speechSynthesis.cancel();
    if (playbackTimerRef.current) clearInterval(playbackTimerRef.current);
    setPlaying(false);
  };

  const handleCompleteAudio = () => {
    stopSpeech();
    
    // Simulate awarding XP locally
    const studentRaw = localStorage.getItem('merge_student');
    if (studentRaw) {
      try {
        const student = JSON.parse(studentRaw);
        student.total_xp = (student.total_xp || 0) + 5; // Audiocast listen XP
        localStorage.setItem('merge_student', JSON.stringify(student));
      } catch(e) {}
    }
    
    setViewMode("success");
  };

  return (
    <div className="min-h-screen flex flex-col bg-background text-on-surface relative font-body-md text-body-md select-text">
      {/* Header */}
      <header className="h-[48px] bg-surface border-b border-outline-variant flex justify-between items-center px-margin-md z-40">
        <div className="flex items-center gap-4">
          <button 
            onClick={() => {
              stopSpeech();
              navigate('/dashboard');
            }}
            className="flex items-center gap-1.5 hover:text-primary transition-colors text-on-surface-variant font-label-caps text-label-caps"
          >
            <span className="material-symbols-outlined text-[16px]">arrow_back</span>
            <span>Abort Session</span>
          </button>
        </div>
        <div className="flex items-center gap-4">
          <span className="px-2 py-0.5 border border-[#ffb95f] bg-[#ffb95f]/5 text-[#ffb95f] font-mono-code text-[10px] uppercase">
            ENGAGEMENT ORCHESTRATOR
          </span>
        </div>
      </header>

      {/* Main View Area */}
      {viewMode === "mood_selector" && (
        <main className="flex-1 max-w-md mx-auto w-full px-6 py-16 flex flex-col justify-center text-center space-y-10 animate-fade-in">
          <div className="space-y-3">
            <span className="font-mono-code text-[10px] text-primary uppercase tracking-widest block">SESSION INTAKE</span>
            <h1 className="font-display text-[28px] font-extrabold text-on-surface uppercase tracking-tight">
              Select Current Mood
            </h1>
            <p className="text-xs text-on-surface-variant leading-relaxed max-w-xs mx-auto">
              How are you approaching your engineering formation today?
            </p>
          </div>

          <div className="flex flex-col gap-4">
            <button 
              onClick={() => handleMoodSelect("FRESH")}
              className="group border border-outline-variant hover:border-primary bg-[#16181D] hover:bg-surface-container-high p-6 text-left flex items-center justify-between transition-all rounded-none"
            >
              <div className="space-y-1">
                <span className="font-label-caps text-sm text-on-surface font-bold group-hover:text-primary transition-colors uppercase">Fresh / Focused</span>
                <p className="text-[11px] text-on-surface-variant">Ready for full exercises, code drills, and conceptual checks.</p>
              </div>
              <span className="material-symbols-outlined text-primary text-[28px] group-hover:translate-x-1.5 transition-transform">bolt</span>
            </button>

            <button 
              onClick={() => handleMoodSelect("OKAY")}
              className="group border border-outline-variant hover:border-primary bg-[#16181D] hover:bg-surface-container-high p-6 text-left flex items-center justify-between transition-all rounded-none"
            >
              <div className="space-y-1">
                <span className="font-label-caps text-sm text-on-surface font-bold group-hover:text-primary transition-colors uppercase">Okay / standard</span>
                <p className="text-[11px] text-on-surface-variant">Skips syntax warmups. Proceeds directly to Core Drills.</p>
              </div>
              <span className="material-symbols-outlined text-primary text-[28px] group-hover:translate-x-1.5 transition-transform">arrow_forward</span>
            </button>

            <button 
              onClick={() => handleMoodSelect("EXHAUSTED")}
              className="group border border-[#ffb95f]/50 hover:border-[#ffb95f] bg-[#16181D] hover:bg-[#ffb95f]/5 p-6 text-left flex items-center justify-between transition-all rounded-none"
            >
              <div className="space-y-1">
                <span className="font-label-caps text-sm text-on-surface font-bold text-[#ffb95f] uppercase">Exhausted / Stressed</span>
                <p className="text-[11px] text-on-surface-variant font-normal">Locks code editor. Serves 5m conceptual reinforcement audio.</p>
              </div>
              <span className="material-symbols-outlined text-[#ffb95f] text-[28px] group-hover:translate-x-1.5 transition-transform">headphones</span>
            </button>
          </div>
        </main>
      )}

      {viewMode === "audio_player" && (
        <main className="flex-grow flex items-center justify-center p-6 select-none animate-fade-in">
          <div className="max-w-sm w-full border border-outline-variant bg-[#16181D] p-10 text-center space-y-8 relative shadow-2xl">
            {/* Pulsing glow animation */}
            <div className={`absolute inset-0 bg-[#ffb95f]/5 transition-opacity duration-1000 ${playing ? 'opacity-100' : 'opacity-0'} pointer-events-none`}></div>
            
            <div className="space-y-2 relative z-10">
              <span className="font-mono-code text-[10px] text-[#ffb95f] uppercase tracking-widest block font-bold">REINFORCEMENT AUDIOCAST</span>
              <h2 className="font-headline-lg text-on-surface uppercase">Distributed Resilience</h2>
              <span className="font-mono-code text-[10px] text-on-surface-variant">GEMINI COACHING ENGINE</span>
            </div>

            {/* Simulated wave graphics */}
            <div className="h-16 flex items-center justify-center gap-1 relative z-10">
              {[...Array(9)].map((_, idx) => (
                <div 
                  key={idx} 
                  className={`w-1.5 rounded-full bg-[#ffb95f] transition-all duration-300 ${
                    playing 
                      ? `h-${[6, 12, 16, 10, 14, 8, 12, 6, 10][idx]} animate-pulse` 
                      : 'h-1.5'
                  }`}
                  style={{ animationDelay: `${idx * 0.1}s`, height: playing ? undefined : '6px' }}
                ></div>
              ))}
            </div>

            {/* Timeline slider */}
            <div className="space-y-2 relative z-10">
              <div className="h-1 w-full bg-surface-container-highest border border-outline-variant/35 overflow-hidden">
                <div 
                  className="h-full bg-[#ffb95f] transition-all duration-300"
                  style={{ width: `${progress}%` }}
                ></div>
              </div>
              
              <div className="flex justify-between font-mono-code text-[10px] text-on-surface-variant">
                <span>{timeElapsed}</span>
                <span>{timeTotal}</span>
              </div>
            </div>

            {/* Play controls */}
            <div className="flex items-center justify-center gap-8 relative z-10">
              <button 
                onClick={togglePlayback}
                className="w-16 h-16 rounded-full border border-[#ffb95f]/40 hover:border-[#ffb95f] bg-[#ffb95f]/5 text-[#ffb95f] flex items-center justify-center transition-all hover:scale-105 active:scale-95"
              >
                <span className="material-symbols-outlined text-[32px]">
                  {playing ? 'pause' : 'play_arrow'}
                </span>
              </button>
            </div>

            {/* Continue Actions */}
            <div className="pt-2 relative z-10">
              <button
                disabled={progress < 100}
                onClick={handleCompleteAudio}
                className={`w-full py-4 font-label-caps text-label-caps uppercase tracking-wider transition-colors rounded-none ${
                  progress >= 100 
                    ? 'bg-[#ffb95f] hover:bg-[#ee9800] text-black font-extrabold cursor-pointer' 
                    : 'bg-surface-container-high border border-outline-variant/35 text-on-surface-variant/40 cursor-not-allowed text-[10px]'
                }`}
              >
                {progress >= 100 ? "Complete Audiocast & Continue" : "Finish Listening to Unlock Continue"}
              </button>
            </div>
          </div>
        </main>
      )}

      {viewMode === "success" && (
        <main className="fixed inset-0 bg-[#0D0F12] flex items-center justify-center p-6 z-50 animate-fade-in select-none">
          <div className="max-w-md w-full border border-outline-variant bg-[#16181D] p-12 text-center space-y-8">
            <div className="space-y-2">
              <span className="font-mono-code text-[10px] text-primary uppercase tracking-widest block">Engagement Milestone</span>
              <h1 className="font-display text-[72px] font-extrabold text-[#ffb95f] leading-none tracking-tight">+5 XP</h1>
              <span className="font-label-caps text-label-caps text-on-surface-variant block">AWARDED FOR LISTENING</span>
            </div>

            <p className="text-xs text-on-surface-variant leading-relaxed max-w-xs mx-auto">
              Your conceptual session is logged in the profile registry. Take a moment to recover before your next building workspace launches.
            </p>

            <div className="pt-4">
              <button 
                onClick={() => navigate('/dashboard')}
                className="w-full bg-[#ffb95f] hover:bg-[#ee9800] text-black font-extrabold py-4 font-label-caps text-label-caps uppercase tracking-widest transition-colors rounded-none"
              >
                Launch Overview Dashboard
              </button>
            </div>
          </div>
        </main>
      )}
    </div>
  );
}
