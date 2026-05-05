// YouTube "You" Page - Simple Sign In Screen

(function() {
    'use strict';
    
    function replaceYouPageSignIn() {
        // "You" page එකේ ද කියලා check කරන්න
        var isYouPage = window.location.pathname.includes('/account') ||
                       window.location.pathname.includes('/you') ||
                       window.location.hash.includes('account');
        
        // Bottom navigation එකේ You tab selected ද කියලත් check කරන්න
        var youTabSelected = document.querySelector('ytm-pivot-bar-item-renderer.pivot-account') ||
                            document.querySelector('[tab-identifier="FEwhat_to_watch"].tab-selected') ||
                            document.querySelector('ytm-pivot-bar-item-renderer:last-child[aria-selected="true"]');
        
        if (!isYouPage && !youTabSelected) {
            return false;
        }
        
        // "You're not signed in" text හොයාගන්න
        var pageText = document.body.innerText || document.body.textContent;
        var hasSignInPrompt = pageText.includes('You\'re not signed in') ||
                             pageText.includes('Sign in now to upload') ||
                             pageText.includes('not signed in');
        
        if (!hasSignInPrompt) {
            return false;
        }
        
        // Main content container හොයාගන්න
        var container = document.querySelector('ytm-browse') ||
                       document.querySelector('ytm-account-item-section-renderer') ||
                       document.querySelector('#page-manager');
        
        if (!container) {
            return false;
        }
        
        // දැනටමත් custom screen එකක් තියෙනවද
        if (document.getElementById('spmods-you-screen')) {
            return true;
        }
        
        // Original content hide කරන්න
        var children = container.children;
        for (var i = 0; i < children.length; i++) {
            children[i].style.display = 'none';
        }
        
        // Custom simple screen
        var customScreen = document.createElement('div');
        customScreen.id = 'spmods-you-screen';
        customScreen.setAttribute('style', `
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            min-height: 60vh;
            padding: 40px 20px;
            text-align: center;
        `);
        
        // Simple icon (emoji)
        var iconDiv = document.createElement('div');
        iconDiv.setAttribute('style', `
            font-size: 80px;
            margin-bottom: 25px;
        `);
        iconDiv.textContent = '👤';
        
        // Title
        var titleDiv = document.createElement('div');
        titleDiv.setAttribute('style', `
            font-size: 22px;
            font-weight: 500;
            color: #ffffff;
            margin-bottom: 10px;
            font-family: 'Roboto', Arial, sans-serif;
        `);
        titleDiv.textContent = 'You\'re not signed in';
        
        // Description
        var descDiv = document.createElement('div');
        descDiv.setAttribute('style', `
            font-size: 14px;
            color: #aaaaaa;
            max-width: 300px;
            margin-bottom: 25px;
            line-height: 1.5;
            font-family: 'Roboto', Arial, sans-serif;
        `);
        descDiv.textContent = 'Sign in now to upload, save, and comment on videos';
        
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
        `);
        signinBtn.textContent = 'Sign in';
        
        signinBtn.addEventListener('click', function() {
            window.location.href = 'https://accounts.google.com/ServiceLogin?service=youtube';
        });
        
        // Assemble
        customScreen.appendChild(iconDiv);
        customScreen.appendChild(titleDiv);
        customScreen.appendChild(descDiv);
        customScreen.appendChild(signinBtn);
        
        container.appendChild(customScreen);
        
        return true;
    }
    
    // Repeatedly check කරන්න
    var attempts = 0;
    var maxAttempts = 25;
    
    var checkInterval = setInterval(function() {
        attempts++;
        
        var success = replaceYouPageSignIn();
        
        if (success) {
            clearInterval(checkInterval);
        } else if (attempts >= maxAttempts) {
            clearInterval(checkInterval);
        }
    }, 500);
    
    // URL changes watch කරන්න
    var lastUrl = location.href;
    new MutationObserver(function() {
        var currentUrl = location.href;
        if (currentUrl !== lastUrl) {
            lastUrl = currentUrl;
            
            if (currentUrl.includes('account') || currentUrl.includes('you')) {
                attempts = 0;
                setTimeout(function() {
                    checkInterval = setInterval(function() {
                        attempts++;
                        var success = replaceYouPageSignIn();
                        if (success || attempts >= maxAttempts) {
                            clearInterval(checkInterval);
                        }
                    }, 500);
                }, 300);
            }
        }
    }).observe(document.body, {
        childList: true,
        subtree: true
    });
    
    // Bottom navigation clicks ත් watch කරන්න
    document.addEventListener('click', function(e) {
        var clickedYouTab = e.target.closest('ytm-pivot-bar-item-renderer:last-child') ||
                           e.target.closest('[aria-label*="You"]') ||
                           e.target.closest('.pivot-account');
        
        if (clickedYouTab) {
            setTimeout(function() {
                attempts = 0;
                checkInterval = setInterval(function() {
                    attempts++;
                    var success = replaceYouPageSignIn();
                    if (success || attempts >= maxAttempts) {
                        clearInterval(checkInterval);
                    }
                }, 500);
            }, 300);
        }
    });
    
})();