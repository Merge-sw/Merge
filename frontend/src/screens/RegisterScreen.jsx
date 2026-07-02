import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';

export default function RegisterScreen() {
  const navigate = useNavigate();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [uniEmail, setUniEmail] = useState("");
  const [emailError, setEmailError] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!uniEmail.endsWith('.edu') && !uniEmail.endsWith('.edu.ng')) {
      setEmailError(true);
      return;
    }
    setEmailError(false);

    const profile = {
      fullName,
      email,
      phone,
      uniEmail,
      total_xp: 1250,
      current_stage: "SCOUT"
    };
    localStorage.setItem('merge_student', JSON.stringify(profile));
    localStorage.setItem('merge_jwt', 'mock-jwt-token');

    navigate('/connect/github');
  };

  return (
    <div className="min-h-screen flex items-center justify-center font-body-md text-body-md p-margin-md bg-[#10131a] text-[#e1e2ec]">
      <main className="w-full max-w-[480px] flex flex-col items-center">
        <header className="w-full mb-10 text-center">
          <MergeLogo className="font-display font-extrabold uppercase tracking-widest text-[36px]" />
        </header>

        <div className="w-full bg-[#16181D] border border-outline-variant p-8 md:p-12">
          <form className="space-y-6" onSubmit={handleSubmit}>
            <div className="space-y-2">
              <label className="block font-label-caps text-label-caps text-on-surface-variant uppercase" htmlFor="full_name">Full Name</label>
              <div className="relative">
                <input 
                  className="w-full bg-[#09090B] border border-outline-variant text-on-surface font-mono-code text-mono-code p-3 focus:border-primary transition-colors duration-150 rounded-none outline-none" 
                  id="full_name" 
                  required
                  value={fullName}
                  onChange={e => setFullName(e.target.value)}
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="block font-label-caps text-label-caps text-on-surface-variant uppercase" htmlFor="email">Email Address</label>
              <div className="relative">
                <input 
                  className="w-full bg-[#09090B] border border-outline-variant text-on-surface font-mono-code text-mono-code p-3 focus:border-primary transition-colors duration-150 rounded-none outline-none" 
                  id="email" 
                  type="email" 
                  required
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="block font-label-caps text-label-caps text-on-surface-variant uppercase" htmlFor="phone">Phone Number</label>
              <div className="relative">
                <input 
                  className="w-full bg-[#09090B] border border-outline-variant text-on-surface font-mono-code text-mono-code p-3 focus:border-primary transition-colors duration-150 rounded-none outline-none" 
                  id="phone" 
                  type="tel" 
                  required
                  value={phone}
                  onChange={e => setPhone(e.target.value)}
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="block font-label-caps text-label-caps text-on-surface-variant uppercase" htmlFor="uni_email">University Email</label>
              <div className="relative">
                <input 
                  className={`w-full bg-[#09090B] text-on-surface font-mono-code text-mono-code p-3 transition-colors duration-150 rounded-none outline-none border ${
                    emailError ? "border-error focus:border-error" : "border-outline-variant focus:border-primary"
                  }`} 
                  id="uni_email" 
                  type="email" 
                  required
                  value={uniEmail}
                  onChange={e => {
                    setUniEmail(e.target.value);
                    if (emailError) setEmailError(false);
                  }}
                />
              </div>
              {emailError && (
                <div className="flex items-center space-x-2 pt-1">
                  <span className="text-error font-mono-code text-[11px]">Please enter a valid university email (.edu / .edu.ng)</span>
                </div>
              )}
            </div>

            <div className="pt-4">
              <button className="w-full bg-[#3B82F6] hover:bg-[#2563EB] text-white font-headline-md text-headline-md py-4 transition-colors duration-150 rounded-none uppercase tracking-wide" type="submit">
                Create Account
              </button>
            </div>
          </form>
        </div>

        <footer className="mt-8 text-center">
          <p className="font-body-md text-body-md text-on-surface-variant">
            Already have an account?{" "}
            <button className="text-primary hover:underline transition-all duration-150 font-semibold bg-transparent border-none p-0 cursor-pointer" onClick={() => navigate('/login')}>
              Log in
            </button>
          </p>
        </footer>
      </main>
    </div>
  );
}
