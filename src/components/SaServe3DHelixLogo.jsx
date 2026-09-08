import React, { useEffect, useRef } from 'react';
import * as THREE from 'three';

/**
 * SaServe3DHelixLogo - Interactive 3D Speed Helix Logo Component
 * 
 * Features:
 * - 100% 3D WebGL canvas: Unobstructed ascending spiral vortex (Speed Helix)
 * - Primary metallic royal blue ribbon + complementary cyan accent tracer strand
 * - Completely hollow center with ZERO center obstructions or flat 2D shapes
 * - Ambient floating energy particles matching the brand palette
 * - Interactive cursor-following tilt physics with smooth lerp damping
 * - Clean WebGL resource disposal and resize handling
 */
export default function SaServe3DHelixLogo({
  size = 240,               // Diameter in pixels (square)
  primaryColor = '#2563eb', // Brand primary (Royal Blue)
  accentColor = '#06b6d4',  // Service accent (Cyan)
  speed = 1.0,              // Orbit speed multiplier
  interactive = true,       // Enable cursor tilt
  showText = true,          // Display "SaServe" text next to logo
  defaultMode = 'helix',    // Helix mode
  className = ''
}) {
  const containerRef = useRef(null);

  // Normalize props that might be passed as strings (e.g. size="{240}" or showText="{true}")
  const numericSize = typeof size === 'string' ? (parseInt(size.replace(/[{}]/g, ''), 10) || 240) : size;
  const isShowText = typeof showText === 'string' ? (showText === 'true' || showText === '{true}') : Boolean(showText);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    // 1. Scene & Camera Setup
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100);
    camera.position.z = 16;

    // 2. High-Performance WebGL Renderer
    const renderer = new THREE.WebGLRenderer({
      alpha: true,
      antialias: true,
      powerPreference: 'high-performance'
    });
    renderer.setSize(numericSize, numericSize);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    container.appendChild(renderer.domElement);

    // 3. Lighting Setup
    const ambientLight = new THREE.AmbientLight(0xffffff, 1.2);
    scene.add(ambientLight);

    const pointLight1 = new THREE.PointLight(primaryColor, 3.5, 50);
    pointLight1.position.set(10, 10, 10);
    scene.add(pointLight1);

    const pointLight2 = new THREE.PointLight(accentColor, 2.5, 50);
    pointLight2.position.set(-10, -10, -10);
    scene.add(pointLight2);

    // 4. Logo Group Setup (Speed Helix Geometry)
    const logoGroup = new THREE.Group();
    scene.add(logoGroup);

    // 5. Materials
    const primaryMat = new THREE.MeshStandardMaterial({
      color: new THREE.Color(primaryColor),
      metalness: 0.85,
      roughness: 0.18,
      emissive: new THREE.Color(primaryColor),
      emissiveIntensity: 0.35
    });

    const accentMat = new THREE.MeshStandardMaterial({
      color: new THREE.Color(accentColor),
      metalness: 0.9,
      roughness: 0.12,
      emissive: new THREE.Color(accentColor),
      emissiveIntensity: 0.45
    });

    // Primary Speed Helix Spiral Ribbon (continuous ascending 3D vortex, NO center obstructions)
    const primaryHelixPoints = [];
    const accentHelixPoints = [];
    const segments = 140;

    for (let i = 0; i <= segments; i++) {
      const t = i / segments;
      const angle = t * Math.PI * 6; // 3 full orbital turns
      const radius = 1.0 + t * 2.2;
      const y = (t - 0.5) * 6.5;

      // Primary helix path
      primaryHelixPoints.push(
        new THREE.Vector3(Math.cos(angle) * radius, y, Math.sin(angle) * radius)
      );

      // Complementary accent tracer trailing closely behind
      accentHelixPoints.push(
        new THREE.Vector3(
          Math.cos(angle + 0.35) * (radius * 0.92),
          y,
          Math.sin(angle + 0.35) * (radius * 0.92)
        )
      );
    }

    const primaryCurve = new THREE.CatmullRomCurve3(primaryHelixPoints);
    const loopGeo = new THREE.TubeGeometry(primaryCurve, 120, 0.34, 16, false);
    const loopMesh = new THREE.Mesh(loopGeo, primaryMat);
    logoGroup.add(loopMesh);

    // Secondary smooth cyan accent tracer strand
    const tracerCurve = new THREE.CatmullRomCurve3(accentHelixPoints);
    const tracerGeo = new THREE.TubeGeometry(tracerCurve, 120, 0.12, 12, false);
    const tracerMesh = new THREE.Mesh(tracerGeo, accentMat);
    logoGroup.add(tracerMesh);

    // 6. Glowing Orbital Energy Particles
    const particleCount = 65;
    const pGeo = new THREE.BufferGeometry();
    const pPositions = new Float32Array(particleCount * 3);
    const p1 = new THREE.Color(primaryColor);
    const p2 = new THREE.Color(accentColor);
    const pColors = new Float32Array(particleCount * 3);

    for (let i = 0; i < particleCount; i++) {
      pPositions[i * 3] = (Math.random() - 0.5) * 16;
      pPositions[i * 3 + 1] = (Math.random() - 0.5) * 16;
      pPositions[i * 3 + 2] = (Math.random() - 0.5) * 16;

      const mix = p1.clone().lerp(p2, Math.random());
      pColors[i * 3] = mix.r;
      pColors[i * 3 + 1] = mix.g;
      pColors[i * 3 + 2] = mix.b;
    }
    pGeo.setAttribute('position', new THREE.BufferAttribute(pPositions, 3));
    pGeo.setAttribute('color', new THREE.BufferAttribute(pColors, 3));

    const pMat = new THREE.PointsMaterial({
      size: 0.22,
      vertexColors: true,
      transparent: true,
      opacity: 0.7,
      blending: THREE.AdditiveBlending
    });
    const particleSystem = new THREE.Points(pGeo, pMat);
    scene.add(particleSystem);

    let mouseX = 0;
    let mouseY = 0;
    let targetRotX = 0;
    let targetRotY = 0;

    const handleMouseMove = (e) => {
      if (!interactive) return;
      const rect = container.getBoundingClientRect();
      mouseX = ((e.clientX - rect.left) / rect.width) * 2 - 1;
      mouseY = -((e.clientY - rect.top) / rect.height) * 2 + 1;
      targetRotY = mouseX * 1.2;
      targetRotX = mouseY * 0.8;
    };

    const handleMouseLeave = () => {
      targetRotX = 0;
      targetRotY = 0;
    };

    if (interactive) {
      container.addEventListener('mousemove', handleMouseMove);
      container.addEventListener('mouseleave', handleMouseLeave);
    }

    // Animation Loop
    let animId;
    const clock = new THREE.Clock();

    const animate = () => {
      animId = requestAnimationFrame(animate);
      const delta = clock.getElapsedTime() * speed;

      // Smooth mouse follow lerp
      logoGroup.rotation.y += (targetRotY - logoGroup.rotation.y) * 0.08;
      logoGroup.rotation.x += (targetRotX - logoGroup.rotation.x) * 0.08;

      // Continuous gentle orbit & vertical hover float
      logoGroup.rotation.y += 0.015 * speed;
      logoGroup.position.y = Math.sin(delta * 1.5) * 0.25;

      particleSystem.rotation.y = delta * 0.05;

      renderer.render(scene, camera);
    };

    animate();

    return () => {
      cancelAnimationFrame(animId);
      if (interactive) {
        container.removeEventListener('mousemove', handleMouseMove);
        container.removeEventListener('mouseleave', handleMouseLeave);
      }
      loopGeo.dispose();
      tracerGeo.dispose();
      pGeo.dispose();
      primaryMat.dispose();
      accentMat.dispose();
      pMat.dispose();
      renderer.dispose();
      if (container.contains(renderer.domElement)) {
        container.removeChild(renderer.domElement);
      }
    };
  }, [numericSize, primaryColor, accentColor, speed, interactive, defaultMode]);

  return (
    <div className={`inline-flex items-center gap-3 select-none ${className}`}>
      {/* 3D WebGL Canvas Viewport */}
      <div 
        ref={containerRef} 
        style={{ width: `${numericSize}px`, height: `${numericSize}px` }}
        className="relative flex items-center justify-center cursor-grab active:cursor-grabbing transition-transform hover:scale-105 duration-300"
        title="SaServe 3D Speed Helix - Interactive WebGL Logo"
      />

      {/* Brand Typography */}
      {isShowText && (
        <div className="flex flex-col justify-center">
          <span className="text-2xl font-black tracking-tight text-white font-sans leading-tight">
            Sa<span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-cyan-400 to-teal-300">Serve</span>
          </span>
          <span className="text-[10px] font-semibold uppercase tracking-wider text-cyan-400/90 mt-0.5">
            Connecting You with Trusted Services
          </span>
        </div>
      )}
    </div>
  );
}
