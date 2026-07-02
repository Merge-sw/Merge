import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';
import ScoutProgressHeader from '../components/ScoutProgressHeader';

export default function ScoutLayer1Screen() {
  const navigate = useNavigate();
  
  const questions = [
    { id: "q1", number: "01", text: "Tell us about yourself in your own words." },
    { id: "q2", number: "02", text: "Which university are you attending, and what year are you in?" },
    { id: "q3", number: "03", text: "What made you choose Computer Science or a related field?" },
    { id: "q4", number: "04", text: "Have you written code before? If yes, describe what you built." },
    { id: "q5", number: "05", text: "What does becoming a software engineer mean to you personally?" },
    { id: "q6", number: "06", text: "How many hours per week can you realistically commit to this programme?" },
    { id: "q7", number: "07", text: "What is your biggest worry about learning to code professionally?" },
    { id: "q8", number: "08", text: "What does your ideal engineering career look like in five years?" }
  ];

  const [answers, setAnswers] = useState({
    q1: "", q2: "", q3: "", q4: "", q5: "", q6: "", q7: "", q8: ""
  });

  const handleAnswerChange = (id, text) => {
    setAnswers(prev => ({ ...prev, [id]: text }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    localStorage.setItem('scout_layer1_responses', JSON.stringify(answers));

    const hasCodeExperience = answers.q4.trim().length > 15;
    localStorage.setItem('scout_has_prior_experience', hasCodeExperience ? 'true' : 'false');

    navigate('/scout/layer-2');
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-start p-margin-md bg-[#10131a] text-[#e1e2ec] overflow-x-hidden pt-12 pb-24">
      <main className="w-full max-w-2xl flex flex-col items-center">
        <header className="w-full mb-8 text-center">
          <MergeLogo className="font-display font-extrabold uppercase tracking-widest text-[28px] mb-2 block" />
        </header>

        <ScoutProgressHeader currentStep={1} />

        <div className="w-full bg-[#16181D] border border-outline-variant p-8 md:p-12 shadow-2xl">
          <div className="mb-8 border-b border-outline-variant pb-4 text-left">
            <h1 className="font-headline-lg text-headline-lg text-on-surface uppercase">Layer 1: Background Profile</h1>
            <p className="font-body-md text-on-surface-variant mt-1">Answer the following questions in your own words. Take your time; there is no active timer.</p>
          </div>

          <form className="space-y-6 text-left" onSubmit={handleSubmit}>
            {questions.map((q) => (
              <div key={q.id} className="space-y-2">
                <label className="block text-sm mb-2" htmlFor={q.id}>
                  <span className="font-mono-code text-mono-code text-primary uppercase mr-2">{q.number} /</span>
                  <span className="font-body-md text-body-md text-on-surface font-semibold">{q.text}</span>
                </label>
                <textarea
                  id={q.id}
                  required
                  rows={3}
                  className="w-full bg-[#09090B] border border-[#27272a] focus:border-[#3B82F6] text-on-surface font-mono-code text-mono-code p-3 rounded-none outline-none transition-colors duration-150 resize-y"
                  value={answers[q.id]}
                  onChange={e => handleAnswerChange(q.id, e.target.value)}
                  placeholder="Enter response..."
                />
              </div>
            ))}

            <div className="pt-6">
              <button 
                type="submit"
                className="w-full bg-[#3B82F6] hover:bg-[#2563EB] text-white font-label-caps text-label-caps h-[48px] uppercase tracking-wider flex items-center justify-center gap-2 rounded-none transition-colors"
              >
                <span>SUBMIT BACKGROUND PROFILE</span>
                <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
              </button>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
}
