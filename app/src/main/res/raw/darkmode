// Hide YouTube Mobile Bottom Navigation Bar ONLY

(function() {
    'use strict';
    
    console.log('🚫 Hiding YouTube Bottom Bar...');
    
    // ✅ Hide bottom navigation bar only
    function hideBottomBar() {
        // CSS injection
        if (!document.getElementById('ytpro-hide-bottom')) {
            const style = document.createElement('style');
            style.id = 'ytpro-hide-bottom';
            style.textContent = `
                ytm-pivot-bar-renderer {
                    display: none !important;
                    visibility: hidden !important;
                    height: 0 !important;
                    opacity: 0 !important;
                }
                
                body, ytm-app, #page-manager {
                    padding-bottom: 0 !important;
                }
            `;
            document.head.appendChild(style);
        }
        
        // Remove elements
        const bars = document.querySelectorAll('ytm-pivot-bar-renderer');
        bars.forEach(bar => {
            bar.style.display = 'none';
            bar.remove();
        });
    }
    
    // Run immediately
    hideBottomBar();
    
    // Watch for new elements
    new MutationObserver(hideBottomBar).observe(document.body, {
        childList: true,
        subtree: true
    });
    
    // Backup check every 2 seconds
    setInterval(hideBottomBar, 2000);
    
    console.log('✅ Bottom bar hidden!');
    
})();
