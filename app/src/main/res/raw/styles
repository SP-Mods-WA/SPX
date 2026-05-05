/* YouTube Share Link Box - Share Menu Version */
(function() {
    
    function createShareMenu() {
        var existing = document.getElementById('share-overlay');
        if (existing) existing.remove();
        
        var overlay = document.createElement('div');
        overlay.id = 'share-overlay';
        overlay.innerHTML = `
            <div class="share-backdrop"></div>
            <div class="share-sheet">
                <div class="share-handle"></div>
                <div class="share-header">
                    <span class="share-title">Share</span>
                </div>
                <div class="share-link-box">
                    <div class="share-url" id="share-url-text"></div>
                    <button class="copy-btn" id="copy-btn">Copy</button>
                </div>
            </div>
        `;
        
        document.body.appendChild(overlay);
        document.getElementById('share-url-text').textContent = window.location.href;
        
        document.getElementById('copy-btn').addEventListener('click', copyUrl);
        overlay.querySelector('.share-backdrop').addEventListener('click', closeShare);
        
        setTimeout(() => overlay.classList.add('active'), 10);
    }
    
    function copyUrl() {
        var btn = document.getElementById('copy-btn');
        navigator.clipboard.writeText(window.location.href).then(function() {
            btn.textContent = 'Copied';
            btn.style.background = '#065fd4';
            btn.style.borderColor = '#065fd4';
            btn.style.color = '#fff';
            
            setTimeout(function() {
                btn.textContent = 'Copy';
                btn.style.background = 'transparent';
                btn.style.borderColor = '#3ea6ff';
                btn.style.color = '#3ea6ff';
            }, 1500);
        });
    }
    
    function closeShare() {
        var overlay = document.getElementById('share-overlay');
        if (overlay) {
            overlay.classList.remove('active');
            setTimeout(() => overlay.remove(), 300);
        }
    }
    
    function injectStyles() {
        var style = document.createElement('style');
        style.textContent = `
            #share-overlay {
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                z-index: 999999;
                display: flex;
                align-items: flex-end;
                justify-content: center;
            }
            
            .share-backdrop {
                position: absolute;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0);
                transition: background 0.3s ease;
            }
            
            #share-overlay.active .share-backdrop {
                background: rgba(0, 0, 0, 0.8);
            }
            
            .share-sheet {
                position: relative;
                width: 100%;
                max-width: 600px;
                background: #212121;
                border-radius: 16px 16px 0 0;
                transform: translateY(100%);
                transition: transform 0.3s ease;
                padding-bottom: 24px;
            }
            
            #share-overlay.active .share-sheet {
                transform: translateY(0);
            }
            
            .share-handle {
                width: 32px;
                height: 4px;
                background: #606060;
                border-radius: 2px;
                margin: 12px auto;
            }
            
            .share-header {
                padding: 0 16px 16px;
                border-bottom: 1px solid #3a3a3a;
            }
            
            .share-title {
                color: #fff;
                font-size: 16px;
                font-weight: 500;
                font-family: Roboto, Arial, sans-serif;
            }
            
            .share-link-box {
                padding: 12px 16px;
                background: #3a3a3a;
                margin: 16px 16px;
                border-radius: 8px;
                display: flex;
                align-items: center;
                gap: 12px;
            }
            
            .share-url {
                flex: 1;
                color: #3ea6ff;
                font-size: 13px;
                font-family: 'Roboto Mono', 'Courier New', monospace;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
            
            .copy-btn {
                background: transparent;
                border: 1px solid #3ea6ff;
                color: #3ea6ff;
                padding: 6px 16px;
                border-radius: 18px;
                font-size: 13px;
                font-weight: 500;
                cursor: pointer;
                font-family: Roboto, Arial, sans-serif;
                transition: all 0.2s ease;
                outline: none;
            }
            
            .copy-btn:active {
                background: rgba(62, 166, 255, 0.1);
                transform: scale(0.95);
            }
        `;
        
        document.head.appendChild(style);
    }
    
    function init() {
        injectStyles();
        
        document.addEventListener('click', function(e) {
            if (e.target.closest('[aria-label*="Share"]') || 
                e.target.closest('.share-button')) {
                e.preventDefault();
                e.stopPropagation();
                createShareMenu();
            }
        }, true);
    }
    
    window.share = createShareMenu;
    
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
    
})();
