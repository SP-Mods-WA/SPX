// YouTube Subscriptions - Wait for Content Load Version

(function() {
    'use strict';
    
    var contentCheckComplete = false;
    
    function replaceSubscriptionsEmpty() {
        // Subscriptions page එකේ ද කියලා check කරන්න
        var isSubscriptionsPage = window.location.pathname.includes('/feed/subscriptions') ||
                                 window.location.hash.includes('subscriptions');
        
        if (!isSubscriptionsPage) {
            // Custom screen එක remove කරන්න if exists
            var existingScreen = document.getElementById('spmods-subs-screen');
            if (existingScreen) {
                existingScreen.remove();
            }
            contentCheckComplete = false;
            return false;
        }
        
        // දැනටමත් custom screen එකක් තියෙනවනම් return
        if (document.getElementById('spmods-subs-screen')) {
            return true;
        }
        
        // Check if user is logged in and has actual subscriptions content
        var hasActualContent = false;
        
        // Video thumbnails තියෙනවද බලන්න
        var videoThumbnails = document.querySelectorAll('ytm-thumbnail-overlay-time-status-renderer, ytm-video-with-context-renderer, ytm-compact-video-renderer, ytm-rich-item-renderer');
        if (videoThumbnails.length >= 1) {
            hasActualContent = true;
        }
        
        // Video titles තියෙනවද
        var videoTitles = document.querySelectorAll('#video-title, .compact-media-item-headline, [class*="video-title"]');
        if (videoTitles.length >= 1) {
            hasActualContent = true;
        }
        
        // Feed items
        var feedItems = document.querySelectorAll('ytm-item-section-renderer, ytm-rich-item-renderer');
        if (feedItems.length >= 1) {
            hasActualContent = true;
        }
        
        // If has actual content, mark as complete and don't show custom screen
        if (hasActualContent) {
            contentCheckComplete = true;
            return false;
        }
        
        // Content check complete නැත්නම් තව wait කරන්න (don't show screen yet)
        if (!contentCheckComplete) {
            return false;
        }
        
        // Login required / empty state messages හොයාගන්න
        var pageText = document.body.innerText || document.body.textContent;
        var hasEmptyState = pageText.includes('Sign in to see') || 
                           pageText.includes('subscriptions') && pageText.includes('sign') ||
                           pageText.includes('No subscriptions') ||
                           pageText.includes('Don\'t miss');
        
        // Page එක කෙටි නම් හෝ empty state message එකක් තියෙනවනම්
        var isReallyEmpty = pageText.length < 1000 || hasEmptyState;
        
        if (!isReallyEmpty) {
            return false;
        }
        
        // Main content container හොයාගන්න
        var container = document.querySelector('ytm-browse') ||
                       document.querySelector('ytm-section-list-renderer') ||
                       document.querySelector('#page-manager');
        
        if (!container) {
            return false;
        }
        
        // Original content hide කරන්න
        var children = container.children;
        for (var i = 0; i < children.length; i++) {
            children[i].style.display = 'none';
        }
        
        // Simple login screen
        var customScreen = document.createElement('div');
        customScreen.id = 'spmods-subs-screen';
        customScreen.setAttribute('style', `
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            min-height: 60vh;
            padding: 40px 20px;
            text-align: center;
        `);
        
        // Icon
        var iconDiv = document.createElement('div');
        iconDiv.setAttribute('style', `
            font-size: 80px;
            margin-bottom: 25px;
        `);
        iconDiv.textContent = '📺';
        
        // Title
        var titleDiv = document.createElement('div');
        titleDiv.setAttribute('style', `
            font-size: 22px;
            font-weight: 600;
            color: #ffffff;
            margin-bottom: 10px;
            font-family: 'Roboto', Arial, sans-serif;
        `);
        titleDiv.textContent = 'Sign in to see subscriptions';
        
        // Description
        var descDiv = document.createElement('div');
        descDiv.setAttribute('style', `
            font-size: 14px;
            color: #aaaaaa;
            max-width: 300px;
            margin-bottom: 30px;
            line-height: 1.5;
            font-family: 'Roboto', Arial, sans-serif;
        `);
        descDiv.textContent = 'Follow your favorite channels and get updates when they post new content.';
        
        // Sign in button
        var signinBtn = document.createElement('button');
        signinBtn.setAttribute('style', `
            background: #3ea6ff;
            color: white;
            border: none;
            font-size: 14px;
            font-weight: 500;
            padding: 10px 20px;
            border-radius: 18px;
            font-family: 'Roboto', Arial, sans-serif;
            cursor: pointer;
            margin-bottom: 40px;
        `);
        signinBtn.textContent = 'Sign in';
        
        signinBtn.addEventListener('click', function() {
            var youTab = document.querySelector('ytm-pivot-bar-item-renderer[tab-identifier="FEwhat_to_watch"]') ||
                        document.querySelector('a[href*="account"]') ||
                        document.querySelector('[aria-label*="You"]') ||
                        document.querySelector('ytm-pivot-bar-item-renderer:last-child');
            
            if (youTab) {
                youTab.click();
            } else {
                window.location.href = '/account';
            }
        });
        
        // Info text
        var infoDiv = document.createElement('div');
        infoDiv.setAttribute('style', `
            font-size: 13px;
            color: #888888;
            font-family: 'Roboto', Arial, sans-serif;
        `);
        infoDiv.textContent = 'Don\'t have an account? Create one for free';
        
        // Assemble
        customScreen.appendChild(iconDiv);
        customScreen.appendChild(titleDiv);
        customScreen.appendChild(descDiv);
        customScreen.appendChild(signinBtn);
        customScreen.appendChild(infoDiv);
        
        container.appendChild(customScreen);
        
        return true;
    }
    
    // Wait කරන function - content load වෙනකන්
    function waitAndCheck() {
        var attempts = 0;
        var maxAttempts = 30; // 30 attempts = 3 seconds wait
        
        var checkInterval = setInterval(function() {
            attempts++;
            
            var isSubscriptionsPage = window.location.pathname.includes('/feed/subscriptions') ||
                                     window.location.hash.includes('subscriptions');
            
            if (!isSubscriptionsPage) {
                clearInterval(checkInterval);
                return;
            }
            
            // Check for content
            var videoThumbnails = document.querySelectorAll('ytm-thumbnail-overlay-time-status-renderer, ytm-video-with-context-renderer, ytm-compact-video-renderer, ytm-rich-item-renderer');
            var videoTitles = document.querySelectorAll('#video-title, .compact-media-item-headline, [class*="video-title"]');
            var feedItems = document.querySelectorAll('ytm-item-section-renderer, ytm-rich-item-renderer');
            
            var hasContent = videoThumbnails.length >= 1 || videoTitles.length >= 1 || feedItems.length >= 1;
            
            if (hasContent) {
                // Content loaded! Mark as complete and stop checking
                contentCheckComplete = true;
                clearInterval(checkInterval);
                return;
            }
            
            // Max attempts reached - assume no content
            if (attempts >= maxAttempts) {
                contentCheckComplete = true;
                clearInterval(checkInterval);
                replaceSubscriptionsEmpty();
            }
        }, 100); // Check every 100ms
    }
    
    // URL changes watch කරන්න
    var lastUrl = location.href;
    new MutationObserver(function() {
        var currentUrl = location.href;
        if (currentUrl !== lastUrl) {
            lastUrl = currentUrl;
            
            if (currentUrl.includes('subscriptions')) {
                // Remove existing screen
                var existingScreen = document.getElementById('spmods-subs-screen');
                if (existingScreen) {
                    existingScreen.remove();
                }
                
                // Reset and wait for content
                contentCheckComplete = false;
                setTimeout(waitAndCheck, 300);
            }
        }
    }).observe(document.body, {
        childList: true,
        subtree: true
    });
    
    // Initial load
    setTimeout(waitAndCheck, 500);
    
})();
