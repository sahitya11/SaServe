import React from 'react';
import SaServe3DHelixLogo from './SaServe3DHelixLogo';

/**
 * SaServe Page Header / Navbar Component
 *
 * Integrates the official 3D WebGL Speed Helix Logo directly into the header.
 */
export default function Navbar() {
  return (
    <header className="fixed top-0 left-0 right-0 z-50 bg-[#090d16]/90 backdrop-blur-md border-b border-white/10 shadow-2xl">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-20 flex items-center justify-between gap-6">
        {/* Left: SaServe 3D Speed Helix Logo */}
        <div className="flex items-center shrink-0">
          <SaServe3DHelixLogo
            defaultMode="helix"
            showText={true}
            size={240}
            className="cursor-pointer"
          />
        </div>

        {/* Center: Desktop Navigation Links */}
        <nav className="hidden md:flex items-center gap-6">
          <a
            href="#services"
            className="text-sm font-semibold text-slate-300 hover:text-white hover:text-cyan-400 transition-colors"
          >
            Services
          </a>
          <a
            href="#categories"
            className="text-sm font-semibold text-slate-300 hover:text-white hover:text-cyan-400 transition-colors"
          >
            Categories
          </a>
          <a
            href="#specialists"
            className="text-sm font-semibold text-slate-300 hover:text-white hover:text-cyan-400 transition-colors"
          >
            Specialists
          </a>
          <a
            href="#bookings"
            className="text-sm font-semibold text-slate-300 hover:text-white hover:text-cyan-400 transition-colors"
          >
            My Bookings
          </a>
          <a
            href="#support"
            className="text-sm font-semibold text-slate-300 hover:text-white hover:text-cyan-400 transition-colors"
          >
            Help & Support
          </a>
        </nav>

        {/* Right: Actions */}
        <div className="flex items-center gap-4">
          <button className="hidden sm:inline-flex items-center justify-center px-4 py-2 rounded-xl text-xs font-semibold text-cyan-400 bg-cyan-500/10 border border-cyan-500/30 hover:bg-cyan-500/20 transition-all">
            Find Specialist
          </button>
          <button className="inline-flex items-center justify-center px-4 py-2 rounded-xl text-xs font-bold text-white bg-gradient-to-r from-blue-600 to-cyan-500 hover:from-blue-500 hover:to-cyan-400 shadow-lg shadow-blue-500/25 transition-all">
            Book Now
          </button>
        </div>
      </div>
    </header>
  );
}
