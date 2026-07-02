import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';
import ScoutProgressHeader from '../components/ScoutProgressHeader';

export default function ScoutLayer2Screen() {
  const navigate = useNavigate();

  const problems = [
    { id: "p1", number: "01", text: "You're asked to build a system for a library to track borrowed books. Before writing any code, walk through how you'd break this problem into smaller pieces." },
    { id: "p2", number: "02", text: "A user reports 'the app is slow.' Describe your step-by-step process for figuring out why, without writing any code." },
    { id: "p3", number: "03", text: "You need to sort a stack of 100 numbered exam papers by student ID as quickly as possible, by hand. Describe the approach you'd take and why." },
    { id: "p4", number: "04", text: "Explain what a 'function' is to someone who has never programmed, using an analogy from everyday life." }
  ];

  const [answers, setAnswers] = useState({
    p1: "", p2: "", p3: "", p4: ""
  });

  const handleAnswerChange = (id, text) => {
    setAnswers(prev => ({ ...prev, [id]: text }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    localStorage.setItem('scout_layer2_responses', JSON.stringify(answers));

    const hasCodeExperience = localStorage.getItem('scout_has_prior_experience') === 'true';
    if (hasCodeExperience) {
      navigate('/scout/layer-3');
    } else {
      navigate('/scout/complete');
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-start p-margin-md bg-[#10131a] text-[#e1e2ec] overflow-x-hidden pt-12 pb-24">
      <main className="w-full max-w-2xl flex flex-col items-center">
        <header className="w-full mb-8 text-center">
          <MergeLogo className="font-display font-extrabold uppercase tracking-widest text-[28px] mb-2 block" />
        </header>

        <ScoutProgressHeader currentStep={2} />

        <div className="w-full bg-[#16181D] border border-outline-variant p-8 md:p-12 shadow-2xl">
          <div className="mb-8 border-b border-outline-variant pb-4 text-left">
            <h1 className="font-headline-lg text-headline-lg text-on-surface uppercase">Layer 2: Conceptual Probe</h1>
            <p className="font-body-md text-on-surface-variant mt-1">Answer the following reasoning problems in your own words. Think through the constraints before writing.</p>
          </div>

          <form className="space-y-6 text-left" onSubmit={handleSubmit}>
            {problems.map((p) => (
              <div key={p.id} className="space-y-2">
                <label className="block text-sm mb-2" htmlFor={p.id}>
                  <span className="font-mono-code text-mono-code text-primary uppercase mr-2">{p.number} /</span>
                  <span className="font-body-md text-body-md text-on-surface font-semibold">{p.text}</span>
                </label>
                <textarea
                  id={p.id}
                  required
                  rows={4}
                  className="w-full bg-[#09090B] border border-[#27272a] focus:border-[#3B82F6] text-on-surface font-mono-code text-mono-code p-3 rounded-none outline-none transition-colors duration-150 resize-y"
                  value={answers[p.id]}
                  onChange={e => handleAnswerChange(p.id, e.target.value)}
                  placeholder="Decompose approach..."
                />
              </div>
            ))}

            <div className="pt-6">
              <button 
                type="submit"
                className="w-full bg-[#3B82F6] hover:bg-[#2563EB] text-white font-label-caps text-label-caps h-[48px] uppercase tracking-wider flex items-center justify-center gap-2 rounded-none transition-colors"
              >
                <span>SUBMIT CONCEPTUAL PROBE</span>
                <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
              </button>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
}
