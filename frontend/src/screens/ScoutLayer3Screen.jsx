import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';
import ScoutProgressHeader from '../components/ScoutProgressHeader';

export default function ScoutLayer3Screen() {
  const navigate = useNavigate();
  const [code, setCode] = useState(
`function filterEvenNumbers(numbers) {
    // Write code here...
}`
  );
  const [selectedLanguage, setSelectedLanguage] = useState("javascript");

  const handleLanguageChange = (lang) => {
    setSelectedLanguage(lang);
    if (lang === "python") {
      setCode(
`def filter_even_numbers(numbers):
    # Write code here
    pass`
      );
    } else if (lang === "javascript") {
      setCode(
`function filterEvenNumbers(numbers) {
    // Write code here...
}`
      );
    } else if (lang === "java") {
      setCode(
`import java.util.List;
import java.util.stream.Collectors;

public class Solution {
    public static List<Integer> filterEvenNumbers(List<Integer> numbers) {
        // Write code here...
        return null;
    }
}`
      );
    } else {
      setCode(`// Implement filter evens logic`);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    localStorage.setItem('scout_layer3_code', code);
    localStorage.setItem('scout_layer3_language', selectedLanguage);

    navigate('/scout/complete');
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-start p-margin-md bg-[#10131a] text-[#e1e2ec] overflow-x-hidden pt-12 pb-24">
      <main className="w-full max-w-4xl flex flex-col items-center">
        <header className="w-full mb-8 text-center">
          <MergeLogo className="font-display font-extrabold uppercase tracking-widest text-[28px] mb-2 block" />
        </header>

        <ScoutProgressHeader currentStep={3} />

        <div className="w-full grid grid-cols-12 border border-outline-variant bg-[#16181D] shadow-2xl overflow-hidden">
          <section className="col-span-5 border-r border-outline-variant p-6 flex flex-col gap-6 text-left h-[480px] overflow-y-auto bg-surface-container-lowest">
            <div>
              <span className="font-mono-code text-[10px] text-primary uppercase tracking-widest block mb-1">TASK ID: baseline-filter-evens</span>
              <h2 className="font-headline-lg text-headline-lg text-on-surface uppercase">Filter Even Numbers</h2>
            </div>

            <div className="space-y-4 font-body-md text-body-md text-on-surface-variant leading-relaxed">
              <p>
                Write a function that accepts a list of integers and returns only the even numbers from that list, preserving their original order.
              </p>
              <p>
                If the input list is empty, return an empty list. Use the programming language of your choice.
              </p>
            </div>

            <div className="space-y-4 border-t border-outline-variant/30 pt-4">
              <div className="space-y-1">
                <span className="font-label-caps text-[10px] text-on-surface-variant uppercase">Example Input</span>
                <pre className="font-mono-code text-xs bg-[#09090B] border border-outline-variant/20 p-2 text-primary">[1, 2, 3, 4, 5, 6]</pre>
              </div>
              <div className="space-y-1">
                <span className="font-label-caps text-[10px] text-on-surface-variant uppercase">Example Output</span>
                <pre className="font-mono-code text-xs bg-[#09090B] border border-outline-variant/20 p-2 text-primary">[2, 4, 6]</pre>
              </div>
            </div>
          </section>

          <section className="col-span-7 flex flex-col h-[480px]">
            <div className="flex justify-between items-center border-b border-outline-variant px-4 py-2 bg-surface-container">
              <span className="font-mono-code text-[11px] text-on-surface-variant uppercase">workspace.src</span>
              
              <div className="flex items-center gap-2">
                <span className="font-label-caps text-[10px] text-on-surface-variant">LANGUAGE:</span>
                <select 
                  value={selectedLanguage}
                  onChange={(e) => handleLanguageChange(e.target.value)}
                  className="bg-[#09090B] border border-outline-variant text-on-surface font-mono-code text-xs px-2 py-1 outline-none focus:border-primary rounded-none"
                >
                  <option value="javascript">JavaScript</option>
                  <option value="python">Python</option>
                  <option value="java">Java</option>
                </select>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="flex flex-col flex-1">
              <textarea
                value={code}
                onChange={(e) => setCode(e.target.value)}
                className="w-full flex-1 bg-[#09090B] font-mono-code text-xs text-[#a78bfa] p-4 outline-none border-none resize-none leading-relaxed select-all"
                rows={15}
                required
              />

              <div className="border-t border-outline-variant p-4 bg-surface-container flex justify-end">
                <button 
                  type="submit"
                  className="bg-[#3B82F6] hover:bg-[#2563EB] text-white px-6 py-3 font-label-caps text-label-caps uppercase tracking-wider flex items-center gap-2 rounded-none transition-colors"
                >
                  <span>SUBMIT BASELINE CODE</span>
                  <span className="material-symbols-outlined text-[16px]">arrow_forward</span>
                </button>
              </div>
            </form>
          </section>
        </div>
      </main>
    </div>
  );
}
