(function () {
    'use strict';

    function $$(selector, root) {
        return Array.from((root || document).querySelectorAll(selector));
    }

    function markRevealTargets() {
        var selectors = [
            '.vac-section',
            '.vac-section-head',
            '.vac-art-card',
            '.card',
            '.tour-card',
            '.vac-exp-card',
            '.vac-blog-card',
            '.vac-focus-card',
            '.vac-trip-card',
            '.dept-block',
            '.split-section__media',
            '.vac-cart-item',
            '.vac-cart-empty',
            '.vac-cart-summary',
            '.admin-card',
            '.admin-panel',
            '.admin-kpi',
            '.admin-chart-container',
            '.vac-polaroid',
            '.vac-trust-item',
            '.vac-command-hero',
            '.vac-command-kpi',
            '.vac-command-panel',
            '.vac-action-tile',
            '.vac-user-command',
            '.vac-message-row',
            '.vac-notification-row',
            '.vac-chat-bubble',
            '.vac-stage-card',
            '.vac-shop-filter',
            '.vac-premium-art-card',
            '.vac-premium-exp-card',
            '.vac-premium-story-card',
            '.vac-home-polaroid',
            '.vac-home-proof-grid span'
        ];

        $$(selectors.join(',')).forEach(function (el) {
            if (el.closest('.vac-home-premium .vac-stage, .vac-home-premium .hero-golden, .vac-home-premium .stats-strip, .vac-home-premium .journey-section, .vac-home-premium .vac-intl-bar')) {
                return;
            }
            if (!el.hasAttribute('data-vac-reveal')) {
                el.setAttribute('data-vac-reveal', '');
            }
        });
    }

    function initLiveCharts() {
        $$('.vac-live-chart').forEach(function (chart) {
            var bars = $$('.vac-live-bar', chart);
            var values = bars.map(function (bar) {
                return Number(bar.getAttribute('data-value') || 0);
            });
            var max = Math.max(1, Math.max.apply(Math, values));
            bars.forEach(function (bar, index) {
                var value = values[index] || 0;
                var width = Math.max(value > 0 ? 8 : 0, Math.round((value / max) * 100));
                window.requestAnimationFrame(function () {
                    bar.style.setProperty('--bar-width', width + '%');
                });
            });
        });
    }

    function initCountUp() {
        var counters = $$('.vac-count-up[data-value]');
        if (!counters.length) return;
        var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

        function format(value) {
            return Math.round(value).toLocaleString();
        }

        function animate(el) {
            var target = Number(el.getAttribute('data-value') || 0);
            if (reduce || target <= 0) {
                el.textContent = format(target);
                return;
            }
            var start = performance.now();
            var duration = 900 + Math.min(target, 120) * 4;
            function frame(now) {
                var progress = Math.min(1, (now - start) / duration);
                var eased = 1 - Math.pow(1 - progress, 3);
                el.textContent = format(target * eased);
                if (progress < 1) {
                    requestAnimationFrame(frame);
                }
            }
            requestAnimationFrame(frame);
        }

        if (!('IntersectionObserver' in window)) {
            counters.forEach(animate);
            return;
        }

        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting || entry.target.dataset.counted === 'true') return;
                entry.target.dataset.counted = 'true';
                animate(entry.target);
                observer.unobserve(entry.target);
            });
        }, { threshold: 0.35 });

        counters.forEach(function (el) { observer.observe(el); });
    }

    function initReveal() {
        var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        var targets = $$('.reveal, [data-vac-reveal], .vac-split-text');
        if (!targets.length) return;

        if (reduce || !('IntersectionObserver' in window)) {
            targets.forEach(function (el) { el.classList.add('is-visible'); });
            return;
        }

        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add('is-visible');
                    // Once visible, we can stop observing
                    observer.unobserve(entry.target);
                }
            });
        }, { 
            rootMargin: '0px 0px -10% 0px', 
            threshold: 0.15 
        });

        targets.forEach(function (el) {
            var rect = el.getBoundingClientRect();
            if (rect.top < window.innerHeight * 0.92 && rect.bottom > 0) {
                el.classList.add('is-visible');
            }
            observer.observe(el);
        });
    }

    function initHeroParallax() {
        var hero = document.querySelector('.vac-commerce-hero, .vac-home-hero, .hero--premium, .page-header');
        var bg = document.querySelector('.vac-commerce-hero__bg img, .vac-home-hero__bg img, .hero__bg img');
        if (!hero || !bg) return;
        if (hero.querySelector('.vac-home-hero__slide')) return;
        var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        if (reduce) return;

        function tick() {
            var rect = hero.getBoundingClientRect();
            var progress = Math.max(-1, Math.min(1, rect.top / Math.max(1, window.innerHeight)));
            bg.style.transform = 'scale(1.06) translate3d(0,' + Math.round(progress * -26) + 'px,0)';
        }

        window.addEventListener('scroll', tick, { passive: true });
        window.addEventListener('resize', tick);
        tick();
    }

    function initClickableCards() {
        $$('.vac-clickable-card[data-card-url]').forEach(function (card) {
            card.setAttribute('tabindex', '0');
            card.setAttribute('role', 'link');

            function openCard(event) {
                var interactive = event.target.closest('a, button, input, select, textarea, form, details, summary');
                if (interactive) return;
                var url = card.getAttribute('data-card-url');
                if (url) window.location.href = url;
            }

            card.addEventListener('click', openCard);
            card.addEventListener('keydown', function (event) {
                if (event.key !== 'Enter' && event.key !== ' ') return;
                event.preventDefault();
                var url = card.getAttribute('data-card-url');
                if (url) window.location.href = url;
            });
        });
    }

    function initMagneticMedia() {
        var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        if (reduce) return;

        $$('.vac-art-card, .card--editorial, .tour-card, .vac-trip-card, .vac-blog-card, .vac-focus-card, .dept-block__media, .vac-home-hero__frame, .vac-command-kpi, .vac-action-tile, .vac-command-panel, .vac-stage-card, .vac-premium-art-card, .vac-premium-exp-card, .vac-premium-story-card').forEach(function (card) {
            card.addEventListener('pointermove', function (event) {
                var rect = card.getBoundingClientRect();
                var x = (event.clientX - rect.left) / rect.width - 0.5;
                var y = (event.clientY - rect.top) / rect.height - 0.5;
                card.style.setProperty('--tilt-x', (y * -4).toFixed(2) + 'deg');
                card.style.setProperty('--tilt-y', (x * 4).toFixed(2) + 'deg');
                card.style.setProperty('--glow-x', ((x + 0.5) * 100).toFixed(1) + '%');
                card.style.setProperty('--glow-y', ((y + 0.5) * 100).toFixed(1) + '%');
            });
            card.addEventListener('pointerleave', function () {
                card.style.setProperty('--tilt-x', '0deg');
                card.style.setProperty('--tilt-y', '0deg');
            });
        });
    }

    function initBackToTop() {
        if (document.querySelector('.back-to-top')) return;
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'back-to-top';
        btn.setAttribute('aria-label', 'Back to top');
        btn.innerHTML = '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M12 19V5"/><path d="M5 12l7-7 7 7"/></svg>';
        btn.style.position = 'fixed';
        btn.style.right = '24px';
        btn.style.bottom = '24px';
        btn.style.zIndex = '260';
        btn.style.opacity = '0';
        btn.style.pointerEvents = 'none';
        btn.addEventListener('click', function () {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
        document.body.appendChild(btn);

        function toggle() {
            var visible = window.scrollY > 600;
            btn.style.opacity = visible ? '1' : '0';
            btn.style.pointerEvents = visible ? 'auto' : 'none';
        }

        window.addEventListener('scroll', toggle, { passive: true });
        toggle();
    }

    function initCarousels() {
        $$('.vac-carousel-wrap').forEach(function (wrap) {
            var track = wrap.querySelector('.vac-carousel-track');
            var prevBtn = wrap.querySelector('[data-carousel-prev]');
            var nextBtn = wrap.querySelector('[data-carousel-next]');
            if (!track) return;

            var scrollAmount = 340;

            if (prevBtn) {
                prevBtn.addEventListener('click', function () {
                    track.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
                });
            }
            if (nextBtn) {
                nextBtn.addEventListener('click', function () {
                    track.scrollBy({ left: scrollAmount, behavior: 'smooth' });
                });
            }

            // Auto-scroll right-to-left every 4 seconds
            var autoInterval = setInterval(function () {
                var maxScroll = track.scrollWidth - track.clientWidth;
                if (track.scrollLeft >= maxScroll - 10) {
                    track.scrollTo({ left: 0, behavior: 'smooth' });
                } else {
                    track.scrollBy({ left: scrollAmount, behavior: 'smooth' });
                }
            }, 4000);

            // Pause auto-scroll on hover
            track.addEventListener('mouseenter', function () { clearInterval(autoInterval); });
            track.addEventListener('mouseleave', function () {
                autoInterval = setInterval(function () {
                    var maxScroll = track.scrollWidth - track.clientWidth;
                    if (track.scrollLeft >= maxScroll - 10) {
                        track.scrollTo({ left: 0, behavior: 'smooth' });
                    } else {
                        track.scrollBy({ left: scrollAmount, behavior: 'smooth' });
                    }
                }, 4000);
            });
        });
    }

    function initHeroRotation() {
        var hero = document.querySelector('.hero-golden--home');
        if (!hero) return;
        var title = hero.querySelector('.hero-golden__title');
        if (!title) return;

        var slides = Array.from(hero.querySelectorAll('.hero-golden__bg .hero-slide'));
        var dots = Array.from(hero.querySelectorAll('.hero-golden__slide-dot'));
        var words = ['WILDLIFE', 'CULTURE', 'ART', 'COMMUNITY'];
        var index = 0;
        var interval = 6000;

        title.style.transition = 'opacity 0.45s ease';

        if (slides.length === 1) {
            slides[0].classList.add('active');
            slides[0].style.opacity = '1';
        }

        function goTo(next) {
            index = ((next % words.length) + words.length) % words.length;

            if (slides.length > 1) {
                var slideIndex = index % slides.length;
                slides.forEach(function (s, i) {
                    s.classList.toggle('active', i === slideIndex);
                });
                dots.forEach(function (d, i) {
                    d.classList.toggle('active', i === slideIndex);
                });
            }

            title.style.opacity = '0';
            setTimeout(function () {
                title.textContent = words[index];
                title.style.opacity = '1';
            }, 400);
        }

        if (slides.length > 1) {
            slides.forEach(function (s) { s.style.transition = 'opacity 1.6s ease'; });
        }

        setInterval(function () {
            goTo(index + 1);
        }, interval);
    }

    function initNavScroll() {
        var nav = document.getElementById('main-nav');
        if (!nav) return;

        function checkScroll() {
            if (window.scrollY > 50) {
                nav.classList.add('is-scrolled');
                nav.classList.add('scrolled');
            } else {
                nav.classList.remove('is-scrolled');
                nav.classList.remove('scrolled');
            }
        }

        window.addEventListener('scroll', checkScroll, { passive: true });
        checkScroll();
    }

    function initCinematicHeroSlides() {
        var heroes = document.querySelectorAll('.vac-cinematic-hero:not(.hero-golden--home):not(.hero-golden--editorial-bg)');
        heroes.forEach(function(hero) {
            var slides = Array.from(hero.querySelectorAll('.hero-slide'));
            if (slides.length < 2) return;
            var current = 0;
            setInterval(function() {
                slides[current].classList.remove('active');
                current = (current + 1) % slides.length;
                slides[current].classList.add('active');
            }, 7000);
        });
    }

    function initCinematicParallax() {
        var heroes = document.querySelectorAll('.vac-cinematic-hero .vac-cinematic-hero__bg, .vac-cinematic-hero .hero-golden__bg');
        if (!heroes.length) return;
        var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        if (reduceMotion) return;

        function onScroll() {
            var y = window.scrollY || 0;
            heroes.forEach(function(bg) {
                var hero = bg.closest('.vac-cinematic-hero, .hero-golden');
                if (!hero) return;
                var rect = hero.getBoundingClientRect();
                if (rect.bottom < 0 || rect.top > window.innerHeight) return;
                var offset = Math.min(Math.max(y * 0.12, 0), 80);
                bg.style.transform = 'translate3d(0,' + offset + 'px,0)';
            });
        }
        window.addEventListener('scroll', onScroll, { passive: true });
        onScroll();
    }

    function initNavMegaMenu() {
        var items = document.querySelectorAll('.nav-item--mega');
        if (!items.length) return;
        items.forEach(function (item) {
            var link = item.querySelector('.nav-link');
            if (!link) return;
            link.addEventListener('click', function (e) {
                if (window.innerWidth <= 980) return;
                if (item.classList.contains('is-open')) return;
            });
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                document.querySelectorAll('.nav-item--mega.is-open').forEach(function (el) {
                    el.classList.remove('is-open');
                });
            }
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        initNavScroll();
        initNavMegaMenu();
        markRevealTargets();
        initReveal();
        initHeroParallax();
        initBackToTop();
        initClickableCards();
        initMagneticMedia();
        initLiveCharts();
        initCountUp();
        initCarousels();
        initHeroRotation();
        initCinematicHeroSlides();
        initCinematicParallax();
    });
})();
