// src/pages/Landing.tsx
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FaUsers, FaCodeBranch, FaBolt, FaSun, FaMoon, FaTwitter, FaGithub } from 'react-icons/fa';

interface FeatureProps {
  icon: React.ReactNode;
  title: string;
  desc: string;
}

interface AvatarProps {
  name: string;
  isActive?: boolean;
}

const Feature: React.FC<FeatureProps> = ({ icon, title, desc }) => (
  <div className="flex flex-col items-center p-6 rounded-xl hover:shadow-lg transition-all duration-300 bg-white dark:bg-gray-800 border border-gray-100 dark:border-gray-700 hover:-translate-y-1">
    <div className="text-3xl p-3 rounded-full bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400 mb-4">
      {icon}
    </div>
    <h3 className="text-xl font-semibold mb-3 text-gray-800 dark:text-gray-100">{title}</h3>
    <p className="text-gray-600 dark:text-gray-300 text-center">{desc}</p>
  </div>
);

const Avatar: React.FC<AvatarProps> = ({ name, isActive = false }) => (
  <div className="relative group">
    <div
      className={`flex items-center justify-center w-10 h-10 rounded-full text-white font-medium cursor-default transition-all duration-300 ${isActive ? 'bg-indigo-500 ring-2 ring-indigo-300 scale-110' : 'bg-indigo-400'}`}
      title={`${name} ${isActive ? 'is currently editing' : ''}`}
    >
      {name.charAt(0)}
    </div>
    <span className="absolute -bottom-6 left-1/2 transform -translate-x-1/2 text-xs bg-gray-800 text-white px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity duration-300 pointer-events-none">
      {name}
    </span>
  </div>
);

const Home: React.FC = () => {
  const [darkMode, setDarkMode] = useState<boolean>(false);
  const [mounted, setMounted] = useState<boolean>(false);

  useEffect(() => {
    // Check system preference or saved preference
    const isDark = localStorage.getItem('darkMode') === 'true' || 
      (window.matchMedia('(prefers-color-scheme: dark)').matches && localStorage.getItem('darkMode') !== 'false');
    setDarkMode(isDark);
    setMounted(true);
  }, []);

  useEffect(() => {
    if (mounted) {
      localStorage.setItem('darkMode', darkMode.toString());
    }
  }, [darkMode, mounted]);

  if (!mounted) return null;

  return (
    <div className={darkMode ? 'dark' : ''}>
      <div className="bg-gradient-to-br from-indigo-50 via-white to-blue-50 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900 min-h-screen text-gray-800 dark:text-gray-100 transition-colors duration-300">

        {/* Navigation */}
        <nav className="flex justify-between items-center px-6 py-4 md:px-12">
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 rounded-full bg-indigo-600 flex items-center justify-center">
              <FaBolt className="text-white text-sm" />
            </div>
            <span className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-indigo-600 to-purple-600">
              LiveText
            </span>
          </div>
          <button
            onClick={() => setDarkMode(!darkMode)}
            className="p-2 rounded-full bg-white dark:bg-gray-700 shadow-md hover:shadow-lg transition-all duration-300"
            aria-label="Toggle dark mode"
          >
            {darkMode ? <FaSun className="text-yellow-500" /> : <FaMoon className="text-indigo-700" />}
          </button>
        </nav>

        {/* Hero Section */}
        <section className="text-center py-20 px-6 max-w-4xl mx-auto">
          <h1 className="text-4xl md:text-5xl lg:text-6xl font-extrabold mb-6 leading-tight">
            Collaborate <span className="text-indigo-600 dark:text-indigo-400">Instantly</span>. 
            Write <span className="text-indigo-600 dark:text-indigo-400">Together</span>.
          </h1>
          <p className="text-lg md:text-xl mb-8 max-w-2xl mx-auto text-gray-600 dark:text-gray-300">
            LiveText is your real-time text editor for seamless team writing, coding, and brainstorming. No lag. No limits.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
            <Link to="/signup" className="px-8 py-3.5 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 transition-all duration-300 transform hover:-translate-y-0.5 shadow-md hover:shadow-lg font-medium text-center">
              Get Started - It's Free
            </Link>
            <Link to="/login" className="px-8 py-3.5 border border-indigo-600 text-indigo-600 dark:text-indigo-400 dark:border-indigo-400 rounded-xl hover:bg-indigo-50 dark:hover:bg-indigo-900/30 transition-all duration-300 font-medium text-center">
              Sign in
            </Link>
          </div>
        </section>

        {/* Live Preview */}
        <section className="px-6 py-12 flex justify-center">
          <div className="bg-white dark:bg-gray-800 shadow-xl rounded-2xl p-6 w-full max-w-3xl border border-gray-100 dark:border-gray-700">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center space-x-3">
                <div className="flex -space-x-2">
                  <Avatar name="Sara" isActive={true} />
                  <Avatar name="Omar" />
                  <Avatar name="You" />
                </div>
                <span className="text-sm text-gray-500 dark:text-gray-400">3 people editing</span>
              </div>
              <div className="text-xs px-3 py-1 bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400 rounded-full">
                Connected
              </div>
            </div>
            <div className="border border-gray-200 dark:border-gray-700 rounded-xl p-4 text-left text-sm font-mono bg-gray-50 dark:bg-gray-900 overflow-hidden">
              <div className="flex mb-2">
                <div className="w-3 h-3 rounded-full bg-red-500 mr-2"></div>
                <div className="w-3 h-3 rounded-full bg-yellow-500 mr-2"></div>
                <div className="w-3 h-3 rounded-full bg-green-500"></div>
              </div>
              <div className="pl-4 border-l-2 border-indigo-400 dark:border-indigo-500">
                <p className="text-gray-400 dark:text-gray-500 mb-3">// Real-time editing preview</p>
                <p><span className="text-purple-500 dark:text-purple-400">function</span> <span className="text-blue-500 dark:text-blue-400">greet</span>() {'{'}</p>
                <p className="pl-4"><span className="text-blue-500 dark:text-blue-400">return</span> <span className="text-yellow-500 dark:text-yellow-400">"Hello, team!"</span>;</p>
                <p>{'}'}</p>
                <div className="mt-4 flex">
                  <div className="animate-pulse h-4 w-32 bg-indigo-200 dark:bg-indigo-900 rounded"></div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Features Section */}
        <section className="py-16 px-6 bg-gradient-to-r from-indigo-50 to-blue-50 dark:from-gray-900 dark:to-gray-800">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-center mb-4 text-gray-800 dark:text-gray-100">Why Teams Love LiveText</h2>
            <p className="text-lg text-center mb-12 max-w-2xl mx-auto text-gray-600 dark:text-gray-300">
              Everything you need for productive collaboration, all in one place.
            </p>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              <Feature 
                icon={<FaUsers />} 
                title="Live Collaboration" 
                desc="Edit together with typing indicators and presence avatars in real-time." 
              />
              <Feature 
                icon={<FaCodeBranch />} 
                title="Version History" 
                desc="Track every change and revert with confidence. Never lose your work again." 
              />
              <Feature 
                icon={<FaBolt />} 
                title="Blazing Fast" 
                desc="Built with cutting-edge WebSockets for instant updates across all devices." 
              />
            </div>
          </div>
        </section>

        {/* Testimonials */}
        <section className="py-16 px-6 bg-white dark:bg-gray-800">
          <div className="max-w-4xl mx-auto text-center">
            <h2 className="text-3xl font-bold mb-12 text-gray-800 dark:text-gray-100">What Our Users Say</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <div className="bg-gray-50 dark:bg-gray-700 p-6 rounded-xl border border-gray-100 dark:border-gray-600">
                <div className="flex items-center mb-4">
                  <div className="w-12 h-12 rounded-full bg-indigo-500 flex items-center justify-center text-white font-bold mr-4">JD</div>
                  <div>
                    <h4 className="font-semibold">Jane Doe</h4>
                    <p className="text-sm text-gray-500 dark:text-gray-400">Software Engineer</p>
                  </div>
                </div>
                <p className="text-gray-600 dark:text-gray-300 italic">"LiveText has transformed how our remote team collaborates on documentation. The real-time editing is flawless!"</p>
              </div>
              <div className="bg-gray-50 dark:bg-gray-700 p-6 rounded-xl border border-gray-100 dark:border-gray-600">
                <div className="flex items-center mb-4">
                  <div className="w-12 h-12 rounded-full bg-green-500 flex items-center justify-center text-white font-bold mr-4">JS</div>
                  <div>
                    <h4 className="font-semibold">John Smith</h4>
                    <p className="text-sm text-gray-500 dark:text-gray-400">Product Manager</p>
                  </div>
                </div>
                <p className="text-gray-600 dark:text-gray-300 italic">"The version history feature saved us countless hours when we needed to revert to an earlier draft. Incredible tool!"</p>
              </div>
            </div>
          </div>
        </section>

        {/* Call to Action */}
        <section className="text-center py-20 px-6 bg-gradient-to-br from-indigo-600 to-purple-600 dark:from-indigo-800 dark:to-purple-800 text-white">
          <h2 className="text-3xl md:text-4xl font-bold mb-4">Ready to write together?</h2>
          <p className="text-lg mb-8 max-w-2xl mx-auto opacity-90">Start your first document in seconds. No sign-up required.</p>
<Link to="/signup" className="px-8 py-4 bg-white text-indigo-600 rounded-xl hover:bg-gray-100 transition-all duration-300 transform hover:-translate-y-0.5 shadow-lg hover:shadow-xl font-semibold inline-block">
              Get started
            </Link>
        </section>

        {/* Footer */}
        <footer className="text-center py-10 px-6 bg-gray-50 dark:bg-gray-900 text-gray-600 dark:text-gray-400">
          <div className="max-w-4xl mx-auto">
            <div className="flex items-center justify-center space-x-2 mb-4">
              <div className="w-6 h-6 rounded-full bg-indigo-600 flex items-center justify-center">
                <FaBolt className="text-white text-xs" />
              </div>
              <span className="font-bold">LiveText</span>
            </div>
            <p className="mb-6">© 2025 LiveText. Built for creators, by creators.</p>
            <div className="flex justify-center space-x-6">
              <a href="#" className="hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors duration-300">
                <FaTwitter className="text-xl" />
              </a>
              <a href="#" className="hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors duration-300">
                <FaGithub className="text-xl" />
              </a>
            </div>
          </div>
        </footer>
      </div>
    </div>
  );
};

export default Home;