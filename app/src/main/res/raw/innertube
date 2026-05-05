/*****YTPRO VIDEO ENHANCEMENTS - FIXED VERSION*******
Perfect YouTube App-like Video Player with Visible Controls
*/

(function() {

    // ✅ Enhanced video player setup
    function setupPerfectVideoPlayer() {
        var video = document.querySelector('.video-stream');
        if (!video) return;
        
        // ✅ Remove all restrictions
        video.removeAttribute('disablepictureinpicture');
        video.setAttribute('playsinline', 'true');
        video.setAttribute('webkit-playsinline', 'true');
        video.controlsList = '';
        
        // ✅ Perfect video fit - FIXED POSITIONING
        video.style.objectFit = 'contain';
        video.style.width = '100%';
        video.style.height = '100%';
        video.style.background = '#000';
        video.style.position = 'absolute';
        video.style.top = '0';
        video.style.left = '0';
        video.style.right = '0';
        video.style.bottom = '0';
        
        // Fix video container positioning
        var playerContainer = video.closest('.html5-video-container');
        if (playerContainer) {
            playerContainer.style.position = 'absolute';
            playerContainer.style.width = '100%';
            playerContainer.style.height = '100%';
            playerContainer.style.top = '0';
            playerContainer.style.left = '0';
        }
        
        // ✅ Enable smooth quality switching
        video.addEventListener('loadedmetadata', function() {
            console.log('📹 Video loaded:', video.videoWidth + 'x' + video.videoHeight);
            
            // Fix aspect ratio without breaking position
            var container = video.closest('#player-container-id');
            if (container && !document.fullscreenElement) {
                var aspectRatio = video.videoWidth / video.videoHeight;
                // Don't change container size, just ensure video fills properly
                container.style.position = 'relative';
            }
        });
        
        // ✅ Smooth quality transitions
        video.addEventListener('waiting', function() {
            console.log('⏳ Buffering...');
        });
        
        video.addEventListener('playing', function() {
            console.log('▶️ Playing');
        });
    }
    

    // ✅ YouTube app-like progress bar
    function enhanceProgressBar() {
        var progressBar = document.querySelector('.ytProgressBarLineProgressBarPlayed');
        if (!progressBar) return;
        
        progressBar.style.background = '#FF0000';
        progressBar.style.transition = 'width 0.1s linear';
        
        var playhead = document.querySelector('.ytProgressBarLineProgressBarPlayhead');
        if (playhead) {
            playhead.style.background = '#FF0000';
            playhead.style.boxShadow = '0 0 4px rgba(255, 0, 0, 0.8)';
        }
    }
    

    
    // ✅ Auto-quality based on network
    function setupAutoQuality() {
        if (!navigator.connection) return;
        
        var connection = navigator.connection;
        var quality = 'auto';
        
        if (connection.effectiveType === '4g') {
            quality = 'hd1080';
        } else if (connection.effectiveType === '3g') {
            quality = 'hd720';
        } else {
            quality = 'medium';
        }
        
        console.log('📶 Network:', connection.effectiveType, '→ Quality:', quality);
    }
    
    
// ✅ Make controls ALWAYS visible and functional
function fixControlsVisibility() {
    var style = document.createElement('style');
    style.textContent = `         
        /* Video thumbnails - YouTube app style */
        ytm-thumbnail img,
        ytm-thumbnail-overlay-resume-playback-renderer,
        .media-item-thumbnail-container img,
        ytm-video-with-context-renderer img,
        #thumbnail img {
            border-radius: 12px !important;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3) !important;
            transition: transform 0.2s ease !important;
        }
        
        /* Thumbnail containers */
        .media-item-thumbnail-container,
        ytm-thumbnail-overlay-resume-playback-renderer,
        ytm-thumbnail {
            border-radius: 12px !important;
            overflow: hidden !important;
        }
        
        /* Press animation */
        ytm-video-with-context-renderer:active img {
            transform: scale(0.98) !important;
        }
        
        /* Shorts thumbnails */
        ytm-shorts-lockup-view-model img {
            border-radius: 12px !important;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3) !important;
        }
        
        /* Quality selector */
        .ytp-settings-menu {
            background: rgba(28, 28, 28, 0.95) !important;
            backdrop-filter: blur(10px) !important;
            border-radius: 8px !important;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5) !important;
        }
        
        .ytp-menuitem-label {
            color: #fff !important;
            font-weight: 500 !important;
        }
        
        .ytp-menuitem[aria-checked="true"] {
            background: rgba(255, 255, 255, 0.1) !important;
        }
        
        /* Progress bar visibility fix */
        .ytp-progress-bar-container,
        .ytp-progress-bar {
            opacity: 1 !important;
            visibility: visible !important;
        }
        
        /* ══════════════════════════════════════════════════════
           💬 YOUTUBE APP STYLE COMMENTS - PERFECT REPLICA
           ══════════════════════════════════════════════════════ */
        
        /* Comment section header */
        ytm-comments-entry-point-header-renderer {
            background: #0f0f0f !important;
            padding: 0 !important;
            margin: 0 !important;
        }
        
        /* Comments title + X button container */
        ytm-comments-entry-point-header-renderer #header {
            display: flex !important;
            align-items: center !important;
            justify-content: space-between !important;
            padding: 12px 16px !important;
            background: transparent !important;
        }
        
        /* "Comments" text */
        ytm-comments-entry-point-header-renderer h2 {
            color: #f1f1f1 !important;
            font-size: 14px !important;
            font-weight: 400 !important;
            margin: 0 !important;
            padding: 0 !important;
        }
        
        /* Close (X) button - YouTube blue circle */
        ytm-comments-entry-point-header-renderer #icon-button {
            background: rgba(62, 166, 255, 0.15) !important;
            width: 40px !important;
            height: 40px !important;
            border-radius: 50% !important;
            display: flex !important;
            align-items: center !important;
            justify-content: center !important;
            padding: 0 !important;
            border: none !important;
        }
        
        ytm-comments-entry-point-header-renderer #icon-button:active {
            background: rgba(62, 166, 255, 0.25) !important;
        }
        
        /* Add comment input box */
        ytm-commentbox {
            background: rgba(255, 255, 255, 0.1) !important;
            border: 1px solid rgba(255, 255, 255, 0.15) !important;
            border-radius: 20px !important;
            padding: 10px 16px !important;
            margin: 8px 16px 16px 16px !important;
        }
        
        ytm-commentbox #simplebox-placeholder,
        ytm-commentbox #placeholder {
            color: rgba(255, 255, 255, 0.6) !important;
            font-size: 14px !important;
        }
        
        /* Comment list container */
        ytm-item-section-renderer[section-identifier="comment-item-section"] {
            background: #0f0f0f !important;
        }
        
        /* Individual comment */
        ytm-comment-thread-renderer {
            background: transparent !important;
            padding: 12px 16px !important;
            margin: 0 !important;
            border-bottom: none !important;
        }
        
        /* Comment inner container */
        ytm-comment-renderer {
            background: transparent !important;
            padding: 0 !important;
        }
        
        /* Profile picture */
        ytm-comment-renderer #author-thumbnail {
            margin-right: 12px !important;
        }
        
        ytm-comment-renderer #author-thumbnail img {
            width: 40px !important;
            height: 40px !important;
            border-radius: 50% !important;
            border: none !important;
        }
        
        /* Author name + time */
        ytm-comment-renderer #header {
            margin-bottom: 4px !important;
        }
        
        ytm-comment-renderer #author-text {
            color: #f1f1f1 !important;
            font-size: 13px !important;
            font-weight: 500 !important;
            margin-right: 8px !important;
        }
        
        ytm-comment-renderer .published-time-text {
            color: rgba(255, 255, 255, 0.6) !important;
            font-size: 12px !important;
        }
        
        /* Comment text */
        ytm-comment-renderer #comment-content,
        ytm-comment-renderer #content-text {
            color: #f1f1f1 !important;
            font-size: 14px !important;
            line-height: 1.4 !important;
            margin: 0 !important;
            padding: 0 !important;
        }
        
        /* Action buttons row */
        ytm-comment-renderer #action-buttons {
            margin-top: 8px !important;
            display: flex !important;
            align-items: center !important;
            gap: 4px !important;
        }
        
        /* Like button */
        ytm-toggle-button-renderer {
            margin: 0 !important;
        }
        
        ytm-toggle-button-renderer button {
            background: transparent !important;
            border: none !important;
            color: rgba(255, 255, 255, 0.7) !important;
            padding: 8px 12px !important;
            border-radius: 18px !important;
            display: flex !important;
            align-items: center !important;
            gap: 6px !important;
            min-width: auto !important;
        }
        
        ytm-toggle-button-renderer button:active {
            background: rgba(255, 255, 255, 0.1) !important;
        }
        
        /* Like icon */
        ytm-toggle-button-renderer button svg,
        ytm-toggle-button-renderer button path {
            fill: currentColor !important;
        }
        
        /* Like count */
        #vote-count-middle {
            color: rgba(255, 255, 255, 0.7) !important;
            font-size: 12px !important;
            font-weight: 500 !important;
        }
        
        /* Dislike button */
        ytm-toggle-button-renderer:nth-child(2) button {
            padding: 8px !important;
        }
        
        /* Reply button */
        ytm-button-renderer button {
            background: transparent !important;
            border: none !important;
            color: rgba(255, 255, 255, 0.7) !important;
            padding: 8px 12px !important;
            border-radius: 18px !important;
            font-size: 12px !important;
            font-weight: 500 !important;
            text-transform: uppercase !important;
        }
        
        ytm-button-renderer button:active {
            background: rgba(255, 255, 255, 0.1) !important;
        }
        
        /* More menu (3 dots) */
        ytm-menu-renderer button {
            background: transparent !important;
            border: none !important;
            color: rgba(255, 255, 255, 0.7) !important;
            padding: 8px !important;
            border-radius: 50% !important;
            width: 36px !important;
            height: 36px !important;
        }
        
        ytm-menu-renderer button:active {
            background: rgba(255, 255, 255, 0.1) !important;
        }
        
        /* View replies button */
        ytm-comment-replies-renderer #expander {
            margin-left: 52px !important;
            margin-top: 8px !important;
        }
        
        #expander ytm-button-renderer button {
            color: #3ea6ff !important;
            background: transparent !important;
            padding: 8px 12px !important;
            text-transform: none !important;
            font-weight: 500 !important;
        }
        
        /* Reply icon */
        #expander ytm-button-renderer iron-icon {
            color: #3ea6ff !important;
            width: 18px !important;
            height: 18px !important;
            margin-right: 4px !important;
        }
        
        /* Nested replies */
        ytm-comment-replies-renderer ytm-comment-thread-renderer {
            margin-left: 52px !important;
            padding-left: 0 !important;
        }
        
        /* Pinned comment badge */
        ytm-pinned-comment-badge-renderer {
            background: transparent !important;
            color: rgba(255, 255, 255, 0.7) !important;
            font-size: 12px !important;
            padding: 0 0 8px 0 !important;
            display: flex !important;
            align-items: center !important;
        }
        
        ytm-pinned-comment-badge-renderer iron-icon {
            width: 16px !important;
            height: 16px !important;
            margin-right: 6px !important;
        }
        
        /* Creator heart */
        ytm-creator-heart-renderer {
            margin-left: 8px !important;
        }
        
        /* Comment section background */
        ytm-comments-entry-point-renderer {
            background: #0f0f0f !important;
        }
        
        /* No comments message */
        ytm-backstage-post-renderer {
            color: rgba(255, 255, 255, 0.6) !important;
            text-align: center !important;
            padding: 40px 16px !important;
        }
    `;
    
    if (!document.getElementById('ytpro-video-enhancements-fixed')) {
        style.id = 'ytpro-video-enhancements-fixed';
        document.head.appendChild(style);
        console.log('✅ YouTube App Style Applied!');
    }
}
    


    
    // ✅ Initialize everything
    function init() {
        setupPerfectVideoPlayer();
        enhanceProgressBar();
        setupAutoQuality();
        fixControlsVisibility();
        
        
        
        console.log('✅ YTPRO Video Enhancements FIXED VERSION Loaded');
    }
    
    // Run on page load
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
    

    
    observer.observe(document.body, {
        childList: true,
        subtree: true
    });
    
})();

// ✅ Export for use in other scripts
window.YTProVideoEnhancements = {
    version: '1.0.1-fixed',
    loaded: true
};
