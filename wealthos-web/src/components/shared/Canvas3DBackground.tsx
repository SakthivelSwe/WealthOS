import { useEffect, useRef } from 'react';

export function Canvas3DBackground() {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animationFrameId: number;
    let width = (canvas.width = window.innerWidth);
    let height = (canvas.height = window.innerHeight);

    const handleResize = () => {
      if (!canvas) return;
      width = canvas.width = window.innerWidth;
      height = canvas.height = window.innerHeight;
    };
    window.addEventListener('resize', handleResize);

    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const isMobile = window.innerWidth < 768;

    if (prefersReducedMotion || isMobile) {
      // Draw static calm vignette without expensive continuous requestAnimationFrame loop
      ctx.fillStyle = '#06080e';
      ctx.fillRect(0, 0, width, height);
      const gradient = ctx.createRadialGradient(width / 2, height / 3, 50, width / 2, height / 2, Math.max(width, height) * 0.7);
      gradient.addColorStop(0, 'rgba(16, 185, 129, 0.05)');
      gradient.addColorStop(1, 'rgba(6, 8, 14, 0)');
      ctx.fillStyle = gradient;
      ctx.fillRect(0, 0, width, height);
      return () => {
        window.removeEventListener('resize', handleResize);
      };
    }

    // Mouse coordinates with smoothing
    const mouse = { x: width / 2, y: height / 2, targetX: width / 2, targetY: height / 2 };
    const handleMouseMove = (e: MouseEvent) => {
      mouse.targetX = e.clientX;
      mouse.targetY = e.clientY;
    };
    window.addEventListener('mousemove', handleMouseMove);

    // Glowing 3D Particles - restrained Quiet Wealth count and palette
    const PARTICLE_COUNT = 24;
    const particles = Array.from({ length: PARTICLE_COUNT }, () => ({
      x: Math.random() * width,
      y: Math.random() * height,
      z: Math.random() * 800 + 200, // 3D depth
      vx: (Math.random() - 0.5) * 0.25,
      vy: (Math.random() - 0.5) * 0.25,
      radius: Math.random() * 1.5 + 0.8,
      color: ['rgba(16, 185, 129, ', 'rgba(52, 211, 153, ', 'rgba(148, 163, 184, '][Math.floor(Math.random() * 3)],
      alpha: Math.random() * 0.25 + 0.05,
    }));

    // Floating subtle ambient orbs
    const orbs = [
      { x: width * 0.25, y: height * 0.2, radius: 260, color: 'rgba(16, 185, 129, 0.06)', vx: 0.15, vy: 0.1, phase: 0 },
      { x: width * 0.75, y: height * 0.3, radius: 280, color: 'rgba(5, 150, 105, 0.05)', vx: -0.1, vy: 0.15, phase: Math.PI / 2 },
      { x: width * 0.5, y: height * 0.8, radius: 240, color: 'rgba(15, 118, 110, 0.05)', vx: 0.1, vy: -0.1, phase: Math.PI },
    ];

    let t = 0;

    const render = () => {
      t += 0.008;

      // Smooth mouse easing
      mouse.x += (mouse.targetX - mouse.x) * 0.05;
      mouse.y += (mouse.targetY - mouse.y) * 0.05;

      // Dark obsidian canvas clear
      ctx.fillStyle = '#06080e';
      ctx.fillRect(0, 0, width, height);

      // 1. Draw glowing aurora nebulae
      orbs.forEach((orb, i) => {
        orb.phase += 0.006;
        const currentX = orb.x + Math.sin(orb.phase) * 60 + (mouse.x - width / 2) * 0.04 * (i % 2 === 0 ? 1 : -1);
        const currentY = orb.y + Math.cos(orb.phase) * 50 + (mouse.y - height / 2) * 0.04 * (i % 2 === 0 ? -1 : 1);

        const gradient = ctx.createRadialGradient(currentX, currentY, 0, currentX, currentY, orb.radius);
        gradient.addColorStop(0, orb.color);
        gradient.addColorStop(0.7, orb.color.replace(/[\d\.]+\)$/, '0.04)'));
        gradient.addColorStop(1, 'rgba(6, 8, 14, 0)');

        ctx.fillStyle = gradient;
        ctx.beginPath();
        ctx.arc(currentX, currentY, orb.radius, 0, Math.PI * 2);
        ctx.fill();
      });

      // 2. Cursor dynamic spotlight beam
      const cursorGlow = ctx.createRadialGradient(mouse.x, mouse.y, 0, mouse.x, mouse.y, 380);
      cursorGlow.addColorStop(0, 'rgba(99, 179, 237, 0.09)');
      cursorGlow.addColorStop(0.5, 'rgba(139, 92, 246, 0.04)');
      cursorGlow.addColorStop(1, 'rgba(6, 8, 14, 0)');
      ctx.fillStyle = cursorGlow;
      ctx.beginPath();
      ctx.arc(mouse.x, mouse.y, 380, 0, Math.PI * 2);
      ctx.fill();

      // 3. 3D Floating particle constellation with depth projection
      const fov = 400;
      particles.forEach((p) => {
        p.x += p.vx;
        p.y += p.vy;

        // Wrap around edges
        if (p.x < 0) p.x = width;
        if (p.x > width) p.x = 0;
        if (p.y < 0) p.y = height;
        if (p.y > height) p.y = 0;

        // 3D perspective projection
        const scale = fov / (fov + p.z);
        const projX = (p.x - width / 2) * scale + width / 2 + (mouse.x - width / 2) * 0.02 * scale;
        const projY = (p.y - height / 2) * scale + height / 2 + (mouse.y - height / 2) * 0.02 * scale;
        const projRadius = p.radius * scale;

        ctx.fillStyle = `${p.color}${p.alpha * scale})`;
        ctx.beginPath();
        ctx.arc(projX, projY, Math.max(0.5, projRadius), 0, Math.PI * 2);
        ctx.fill();
      });

      // 4. Subtle cyber grid lines at 4% opacity
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.025)';
      ctx.lineWidth = 1;
      const gridSize = 64;
      const offsetX = (mouse.x * 0.02) % gridSize;
      const offsetY = (mouse.y * 0.02) % gridSize;

      ctx.beginPath();
      for (let x = offsetX; x < width; x += gridSize) {
        ctx.moveTo(x, 0);
        ctx.lineTo(x, height);
      }
      for (let y = offsetY; y < height; y += gridSize) {
        ctx.moveTo(0, y);
        ctx.lineTo(width, y);
      }
      ctx.stroke();

      animationFrameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      cancelAnimationFrame(animationFrameId);
      window.removeEventListener('resize', handleResize);
      window.removeEventListener('mousemove', handleMouseMove);
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100vw',
        height: '100vh',
        pointerEvents: 'none',
        zIndex: 0,
      }}
    />
  );
}
