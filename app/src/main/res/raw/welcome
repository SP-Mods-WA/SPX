// SPMods YouTube - Premium Welcome Screen

(function() {
    'use strict';
    
    function replaceSearchSection() {
        // "Try searching to get started" text එක තියෙන element එක හොයාගන්න
        var allElements = document.querySelectorAll('*');
        var trySearchingElement = null;
        
        for (var i = 0; i < allElements.length; i++) {
            var el = allElements[i];
            var text = el.textContent || '';
            
            if (text.includes('Try searching to get started')) {
                trySearchingElement = el.closest('ytm-browse') || 
                                     el.closest('[page-subtype="home"]') ||
                                     el.parentElement;
                break;
            }
        }
        
        if (!trySearchingElement) {
            trySearchingElement = document.querySelector('ytm-browse');
        }
        
        if (trySearchingElement && trySearchingElement.textContent.includes('Try searching')) {
            
            if (document.getElementById('ytpro-welcome')) {
                return true;
            }
            
            // Original content hide කරන්න
            var contentChildren = trySearchingElement.children;
            for (var j = 0; j < contentChildren.length; j++) {
                contentChildren[j].style.display = 'none';
            }
            
            // Main welcome container
            var welcomeContainer = document.createElement('div');
            welcomeContainer.id = 'ytpro-welcome';
            welcomeContainer.setAttribute('style', `
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: flex-start;
                min-height: 550px;
                padding: 40px 20px 20px 20px;
                text-align: center;
                background: linear-gradient(180deg, rgba(30, 30, 30, 0) 0%, rgba(15, 15, 15, 0.8) 100%);
            `);
            
            // SPMods branding header
            var brandHeader = document.createElement('div');
            brandHeader.setAttribute('style', `
                display: flex;
                align-items: center;
                justify-content: center;
                margin-bottom: 30px;
                gap: 10px;
            `);
            
            var modIcon = document.createElement('div');
            modIcon.setAttribute('style', `
                font-size: 28px;
            `);
            modIcon.textContent = '';
            
            var brandText = document.createElement('div');
            brandText.setAttribute('style', `
                font-size: 22px;
                font-weight: 700;
                background: linear-gradient(135deg, #00d4ff 0%, #0066ff 100%);
                -webkit-background-clip: text;
                -webkit-text-fill-color: transparent;
                background-clip: text;
                font-family: 'Roboto', Arial, sans-serif;
                letter-spacing: 1.5px;
            `);
            brandText.textContent = '';
            
            brandHeader.appendChild(modIcon);
            brandHeader.appendChild(brandText);
            
            // YouTube Pro logo section
            var logoSection = document.createElement('div');
            logoSection.setAttribute('style', `
                margin-bottom: 25px;
                position: relative;
            `);
            
            // Pro badge
            var proBadge = document.createElement('div');
            proBadge.setAttribute('style', `
                position: absolute;
                top: -5px;
                right: -15px;
                background: linear-gradient(135deg, #FFD700 0%, #FFA500 100%);
                color: #000;
                font-size: 11px;
                font-weight: 800;
                padding: 4px 10px;
                border-radius: 12px;
                box-shadow: 0 4px 12px rgba(255, 215, 0, 0.5);
                font-family: 'Roboto', Arial, sans-serif;
                z-index: 10;
            `);
            proBadge.textContent = 'PRO';
            
            var logoDiv = document.createElement('div');
            logoDiv.setAttribute('style', `
                width: 110px;
                height: 110px;
                background: linear-gradient(135deg, #FF0000 0%, #8B0000 100%);
                border-radius: 22px;
                display: flex;
                align-items: center;
                justify-content: center;
                box-shadow: 0 10px 30px rgba(255, 0, 0, 0.4);
                position: relative;
                animation: floatLogo 3s ease-in-out infinite;
                border: 3px solid rgba(255, 255, 255, 0.1);
            `);
            
            var playIcon = document.createElement('div');
            playIcon.setAttribute('style', `
                width: 0;
                height: 0;
                border-left: 35px solid white;
                border-top: 20px solid transparent;
                border-bottom: 20px solid transparent;
                margin-left: 8px;
            `);
            
            logoDiv.appendChild(playIcon);
            logoSection.appendChild(logoDiv);
            logoSection.appendChild(proBadge);
            
            // Main title
            var titleDiv = document.createElement('div');
            titleDiv.setAttribute('style', `
                font-size: 28px;
                font-weight: 700;
                color: #ffffff;
                margin-bottom: 8px;
                font-family: 'YouTube Sans', 'Roboto', Arial, sans-serif;
                text-shadow: 0 2px 10px rgba(0, 0, 0, 0.5);
            `);
            titleDiv.textContent = 'YouTube Premium';
            
            // Version badge
            var versionDiv = document.createElement('div');
            versionDiv.setAttribute('style', `
                display: inline-block;
                background: rgba(255, 255, 255, 0.1);
                color: #00d4ff;
                font-size: 12px;
                font-weight: 600;
                padding: 5px 15px;
                border-radius: 20px;
                margin-bottom: 20px;
                font-family: 'Roboto', Arial, sans-serif;
                border: 1px solid rgba(0, 212, 255, 0.3);
            `);
            versionDiv.textContent = 'Made With ❤️ by SPMods';
            
            // Premium features grid
            var featuresGrid = document.createElement('div');
            featuresGrid.setAttribute('style', `
                display: grid;
                grid-template-columns: 1fr 1fr;
                gap: 10px;
                width: 100%;
                max-width: 340px;
                margin-bottom: 25px;
            `);
            
            var features = [
                { icon: '🚫', title: 'No Ads', color: '#FF4444' },
                { icon: '⬇️', title: 'Downloads', color: '#4CAF50' },
                { icon: '🎵', title: 'BG Play', color: '#2196F3' },
                { icon: '🎬', title: 'HD Quality', color: '#FF9800' }
            ];
            
            features.forEach(function(feature) {
                var featureBox = document.createElement('div');
                featureBox.setAttribute('style', `
                    background: rgba(255, 255, 255, 0.05);
                    border-radius: 12px;
                    padding: 18px 12px;
                    text-align: center;
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    transition: transform 0.2s ease;
                `);
                
                var featureIcon = document.createElement('div');
                featureIcon.setAttribute('style', `
                    font-size: 32px;
                    margin-bottom: 8px;
                `);
                featureIcon.textContent = feature.icon;
                
                var featureTitle = document.createElement('div');
                featureTitle.setAttribute('style', `
                    font-size: 13px;
                    font-weight: 600;
                    color: ${feature.color};
                    font-family: 'Roboto', Arial, sans-serif;
                `);
                featureTitle.textContent = feature.title;
                
                featureBox.appendChild(featureIcon);
                featureBox.appendChild(featureTitle);
                featuresGrid.appendChild(featureBox);
            });
            
            // Info cards
            var infoContainer = document.createElement('div');
            infoContainer.setAttribute('style', `
                display: flex;
                flex-direction: column;
                gap: 10px;
                width: 100%;
                max-width: 340px;
                margin-bottom: 25px;
            `);
            
            var infos = [
                { label: '✨ Premium Unlocked', value: 'All features enabled' },
                { label: '🔓 Region Free', value: 'Access worldwide content' }
            ];
            
            infos.forEach(function(info) {
                var infoCard = document.createElement('div');
                infoCard.setAttribute('style', `
                    background: linear-gradient(135deg, rgba(0, 212, 255, 0.1) 0%, rgba(0, 102, 255, 0.1) 100%);
                    border-radius: 10px;
                    padding: 12px 16px;
                    text-align: left;
                    border: 1px solid rgba(0, 212, 255, 0.2);
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                `);
                
                var labelSpan = document.createElement('span');
                labelSpan.setAttribute('style', `
                    font-size: 13px;
                    font-weight: 600;
                    color: #ffffff;
                    font-family: 'Roboto', Arial, sans-serif;
                `);
                labelSpan.textContent = info.label;
                
                var valueSpan = document.createElement('span');
                valueSpan.setAttribute('style', `
                    font-size: 11px;
                    color: #00d4ff;
                    font-family: 'Roboto', Arial, sans-serif;
                `);
                valueSpan.textContent = info.value;
                
                infoCard.appendChild(labelSpan);
                infoCard.appendChild(valueSpan);
                infoContainer.appendChild(infoCard);
            });
            
            // Footer
            var footerDiv = document.createElement('div');
            footerDiv.setAttribute('style', `
                font-size: 11px;
                color: #666666;
                font-family: 'Roboto', Arial, sans-serif;
                margin-top: 10px;
            `);
            footerDiv.innerHTML = 'Try searching to get started';
            
            // Assemble everything
            welcomeContainer.appendChild(brandHeader);
            welcomeContainer.appendChild(logoSection);
            welcomeContainer.appendChild(titleDiv);
            welcomeContainer.appendChild(versionDiv);
            welcomeContainer.appendChild(featuresGrid);
            welcomeContainer.appendChild(infoContainer);
            welcomeContainer.appendChild(footerDiv);
            
            // Add animations
            if (!document.getElementById('ytpro-style')) {
                var style = document.createElement('style');
                style.id = 'ytpro-style';
                style.textContent = `
                    @keyframes fadeInUp {
                        from {
                            opacity: 0;
                            transform: translateY(30px);
                        }
                        to {
                            opacity: 1;
                            transform: translateY(0);
                        }
                    }
                    @keyframes floatLogo {
                        0%, 100% {
                            transform: translateY(0px);
                        }
                        50% {
                            transform: translateY(-10px);
                        }
                    }
                    #ytpro-welcome {
                        animation: fadeInUp 0.6s ease-out;
                    }
                    #ytpro-welcome > div {
                        animation: fadeInUp 0.7s ease-out backwards;
                    }
                    #ytpro-welcome > div:nth-child(1) { animation-delay: 0.1s; }
                    #ytpro-welcome > div:nth-child(2) { animation-delay: 0.2s; }
                    #ytpro-welcome > div:nth-child(3) { animation-delay: 0.3s; }
                    #ytpro-welcome > div:nth-child(4) { animation-delay: 0.4s; }
                    #ytpro-welcome > div:nth-child(5) { animation-delay: 0.5s; }
                    #ytpro-welcome > div:nth-child(6) { animation-delay: 0.6s; }
                    #ytpro-welcome > div:nth-child(7) { animation-delay: 0.7s; }
                `;
                document.head.appendChild(style);
            }
            
            trySearchingElement.appendChild(welcomeContainer);
            
            return true;
        }
        
        return false;
    }
    
    // Repeatedly try කරන්න
    var attempts = 0;
    var maxAttempts = 20;
    
    var checkInterval = setInterval(function() {
        attempts++;
        
        var success = replaceSearchSection();
        
        if (success) {
            clearInterval(checkInterval);
        } else if (attempts >= maxAttempts) {
            clearInterval(checkInterval);
        }
    }, 500);
    
    // Also watch for DOM changes
    var observer = new MutationObserver(function() {
        if (attempts < maxAttempts) {
            replaceSearchSection();
        }
    });
    
    observer.observe(document.body, {
        childList: true,
        subtree: true
    });
    
})();