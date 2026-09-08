import React from 'react';

interface LogoProps {
  size?: number | string;
  className?: string;
  showText?: boolean;
}

export const Logo: React.FC<LogoProps> = ({ size = 120, className = '', showText = true }) => {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 500 500"
      width={size}
      height={size}
      className={className}
    >
      <defs>
        <linearGradient id="saServeGrad" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="#2563eb" />
          <stop offset="100%" stopColor="#06b6d4" />
        </linearGradient>
        <filter id="saServeGlow" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="6" result="blur" />
          <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
      </defs>

      {/* Background Canvas */}
      <rect width="500" height="500" rx="36" fill="#090d16" />

      {/* Clean Vertical Top-to-Bottom S-Loop */}
      <g transform="translate(250, 160)" filter="url(#saServeGlow)">
        <path
          d="M 0,-95 
             C 55,-95 75,-48 35,-15 
             C 2,-3 -2,3 -35,15 
             C -75,48 -55,95 0,95 
             C 55,95 75,48 35,15 
             C 2,3 -2,-3 -35,-15 
             C -75,-48 -55,-95 0,-95 Z"
          fill="none"
          stroke="url(#saServeGrad)"
          strokeWidth="18"
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        {/* Streamline Orbital Nodes */}
        <circle cx="0" cy="-95" r="7" fill="#2563eb" />
        <circle cx="0" cy="95" r="7" fill="#06b6d4" />
      </g>

      {/* Typography */}
      {showText && (
        <>
          <text
            x="250"
            y="380"
            textAnchor="middle"
            fontFamily="'Space Grotesk', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
            fontSize="44"
            fontWeight="800"
            fill="#ffffff"
            letterSpacing="1"
          >
            SaServe
          </text>
          <text
            x="250"
            y="415"
            textAnchor="middle"
            fontFamily="'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
            fontSize="14"
            fontWeight="600"
            fill="#06b6d4"
            letterSpacing="2"
            textTransform="uppercase"
          >
            Connecting You with Trusted Services
          </text>
        </>
      )}
    </svg>
  );
};

export default Logo;
