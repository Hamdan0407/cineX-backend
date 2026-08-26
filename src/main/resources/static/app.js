const IMG_BASE_URL = "https://image.tmdb.org/t/p/w500";
const IMG_ORIGINAL_URL = "https://image.tmdb.org/t/p/original";

// Global State
let isLoggedIn = localStorage.getItem('cinex_logged_in') === 'true';
let userBookings = JSON.parse(localStorage.getItem('cinex_bookings') || '[]');
let currentMovie = null;
let currentCity = "Mumbai";
let currentTheatre = "";
let currentTime = "";
let selectedSeats = [];
let totalPrice = 0;

let moviesNowPlaying = [];
let moviesTrending = [];
let moviesUpcoming = [];

// DOM Load
document.addEventListener('DOMContentLoaded', () => {
    if (isLoggedIn) updateAuthUI();
    fetchAllMovies();
    renderHomeTheatres();
});

/* =========================================
   1. TMDB Backend Fetch & Render
========================================= */
async function fetchAllMovies() {
    const errorBanner = document.getElementById('api-error');
    const content = document.getElementById('movies-content');
    const skeletons = document.getElementById('loading-skeletons');
    
    errorBanner.style.display = 'none';
    content.style.display = 'none';
    skeletons.style.display = 'block';
    
    try {
        const [resNow, resTrend, resUp] = await Promise.all([
            fetch('/api/tmdb/now_playing'),
            fetch('/api/tmdb/trending'),
            fetch('/api/tmdb/upcoming')
        ]);
        
        if (!resNow.ok || !resTrend.ok || !resUp.ok) throw new Error("API Error");
        
        const dataNow = await resNow.json();
        const dataTrend = await resTrend.json();
        const dataUp = await resUp.json();
        
        moviesNowPlaying = dataNow.results.slice(0, 10);
        moviesTrending = dataTrend.results.slice(0, 10);
        moviesUpcoming = dataUp.results.slice(0, 10);
        
        skeletons.style.display = 'none';
        content.style.display = 'block';
        
        // Render hero banner from first trending
        if (moviesTrending.length > 0) renderHeroBanner(moviesTrending[0]);
        else if (moviesNowPlaying.length > 0) renderHeroBanner(moviesNowPlaying[0]);
        
        renderGrid('now-playing-grid', moviesNowPlaying);
        renderGrid('trending-grid', moviesTrending);
        renderGrid('upcoming-grid', moviesUpcoming);
        
    } catch (e) {
        console.error("Fetch Error:", e);
        errorBanner.style.display = 'block';
        skeletons.style.display = 'none';
    }
}

function renderHeroBanner(movie) {
    if (!movie) return;
    const hero = document.getElementById('hero-carousel');
    const bgUrl = movie.backdrop_path ? `${IMG_ORIGINAL_URL}${movie.backdrop_path}` : `${IMG_BASE_URL}${movie.poster_path}`;
    
    hero.innerHTML = `
        <img src="${bgUrl}" class="hero-img" alt="Banner" onload="this.classList.add('loaded')">
        <div style="position:absolute; bottom:0; left:0; right:0; background:linear-gradient(to top, rgba(15,17,21,1), transparent); height: 50%;"></div>
    `;
    hero.onclick = () => openMovieDetail(movie);
}

const GENRE_MAP = {
    28: "Action", 12: "Adventure", 16: "Animation", 35: "Comedy", 80: "Crime", 99: "Documentary", 18: "Drama", 10751: "Family", 14: "Fantasy", 36: "History", 27: "Horror", 10402: "Music", 9648: "Mystery", 10749: "Romance", 878: "Sci-Fi", 10770: "TV Movie", 53: "Thriller", 10752: "War", 37: "Western"
};

function getGenresString(genreIds) {
    if (!genreIds || genreIds.length === 0) return "Cinema";
    return genreIds.map(id => GENRE_MAP[id] || "Cinema").slice(0,3).join(", ");
}

function renderGrid(gridId, movies) {
    const grid = document.getElementById(gridId);
    grid.innerHTML = '';
    
    movies.forEach(m => {
        const posterUrl = m.poster_path ? `${IMG_BASE_URL}${m.poster_path}` : 'images/poster_placeholder.png';
        const lang = m.original_language === 'hi' ? 'Hindi' : 'English';
        
        const card = document.createElement('div');
        card.className = 'movie-card';
        card.onclick = () => openMovieDetail(m);
        card.innerHTML = `
            <div class="card-img-wrap">
                <img src="${posterUrl}" alt="${m.title}" loading="lazy">
            </div>
            <div class="card-info">
                <h3>${m.title}</h3>
                <p>UA • ${lang}</p>
            </div>
        `;
        grid.appendChild(card);
    });
}

window.applyFilters = function() {
    const query = document.getElementById('search-input').value.toLowerCase();
    
    const filterLogic = (m) => m.title.toLowerCase().includes(query);
    
    const fNow = moviesNowPlaying.filter(filterLogic);
    const fTrend = moviesTrending.filter(filterLogic);
    const fUp = moviesUpcoming.filter(filterLogic);
    
    renderGrid('now-playing-grid', fNow);
    renderGrid('trending-grid', fTrend);
    renderGrid('upcoming-grid', fUp);
    
    const noResults = document.getElementById('no-results');
    const content = document.getElementById('movies-content');
    
    if (fNow.length === 0 && fTrend.length === 0 && fUp.length === 0) {
        content.style.display = 'none';
        noResults.style.display = 'block';
    } else {
        content.style.display = 'block';
        noResults.style.display = 'none';
        
        document.getElementById('now-playing-grid').parentElement.style.display = fNow.length ? 'block' : 'none';
        document.getElementById('trending-grid').parentElement.style.display = fTrend.length ? 'block' : 'none';
        document.getElementById('upcoming-grid').parentElement.style.display = fUp.length ? 'block' : 'none';
    }
}

/* =========================================
   2. Theatres Data
========================================= */
const THEATRES_DB = {
    "Mumbai": [
        { name: "PVR ICON: Palladium, Lower Parel", distance: "2.4 km", features: "M-Ticket, Food & Beverage", times: [{t:"10:30 AM", s:"available"}, {t:"01:15 PM", s:"fast-filling"}, {t:"04:45 PM", s:"almost-full"}, {t:"08:30 PM", s:"available"}] },
        { name: "INOX: Nariman Point", distance: "5.1 km", features: "M-Ticket", times: [{t:"11:00 AM", s:"available"}, {t:"02:30 PM", s:"fast-filling"}, {t:"06:15 PM", s:"available"}] },
        { name: "Cinepolis: Andheri West", distance: "8.2 km", features: "F&B, Recliners", times: [{t:"12:15 PM", s:"almost-full"}, {t:"03:45 PM", s:"fast-filling"}, {t:"07:30 PM", s:"almost-full"}] }
    ],
    "Delhi NCR": [
        { name: "PVR: Select CityWalk", distance: "3.2 km", features: "IMAX", times: [{t:"10:00 AM", s:"available"}, {t:"01:00 PM", s:"fast-filling"}, {t:"05:30 PM", s:"available"}] }
    ],
    "Bengaluru": [
        { name: "PVR: Forum Mall, Koramangala", distance: "4.1 km", features: "IMAX 3D", times: ["09:30 AM", "12:45 PM", "04:15 PM", "08:00 PM"].map(t=>({t, s:Math.random()>0.5?'fast-filling':'available'})) },
        { name: "INOX: Mantri Square", distance: "6.5 km", features: "Recliners", times: ["10:15 AM", "01:30 PM", "06:45 PM", "09:30 PM"].map(t=>({t, s:Math.random()>0.5?'almost-full':'available'})) }
    ],
    "Hyderabad": [
        { name: "AMB Cinemas: Gachibowli", distance: "5.5 km", features: "Superplex, Laser", times: ["10:00 AM", "01:45 PM", "05:15 PM", "08:45 PM"].map(t=>({t, s:Math.random()>0.5?'fast-filling':'available'})) }
    ],
    "Chennai": [
        { name: "Sathyam Cinemas", distance: "2.1 km", features: "Dolby Atmos, VIP", times: ["10:30 AM", "02:15 PM", "06:00 PM", "09:45 PM"].map(t=>({t, s:Math.random()>0.5?'fast-filling':'available'})) }
    ]
};

function renderHomeTheatres() {
    const list = document.getElementById('home-theatres-list');
    const theatres = THEATRES_DB[currentCity] || THEATRES_DB["Mumbai"];
    
    list.innerHTML = theatres.map(t => `
        <div class="home-theatre-card" onclick="alert('Please select a movie first to book tickets at ${t.name}')">
            <div class="ht-info">
                <h4>${t.name}</h4>
                <p>${t.features}</p>
            </div>
            <div class="ht-distance">${t.distance}</div>
        </div>
    `).join('');
}

/* =========================================
   3. Dropdown & City Selection
========================================= */
window.toggleDropdown = (e) => {
    e.stopPropagation();
    document.getElementById('city-dropdown').classList.toggle('open');
}
window.selectCity = (option) => {
    currentCity = option.innerText.trim();
    document.getElementById('selected-city').innerText = currentCity;
    document.querySelectorAll('.trending-city').forEach(el => el.innerText = currentCity);
    document.querySelectorAll('.dropdown-options .option').forEach(opt => opt.classList.remove('active'));
    option.classList.add('active');
    document.getElementById('city-dropdown').classList.remove('open');
    renderHomeTheatres();
    if(currentMovie) renderDetailTheatres(); // Re-render if detail view is open
}
document.addEventListener('click', (e) => {
    const dropdown = document.getElementById('city-dropdown');
    if (dropdown && !dropdown.contains(e.target)) dropdown.classList.remove('open');
});

/* =========================================
   4. Movie Detail Page Overlay
========================================= */
window.openMovieDetail = (movie) => {
    currentMovie = movie;
    const view = document.getElementById('movie-detail-view');
    const bgUrl = movie.backdrop_path ? `${IMG_ORIGINAL_URL}${movie.backdrop_path}` : `${IMG_BASE_URL}${movie.poster_path}`;
    const posterUrl = movie.poster_path ? `${IMG_BASE_URL}${movie.poster_path}` : '';
    
    document.getElementById('detail-hero').style.background = `linear-gradient(90deg, #1a1a1a 20%, rgba(26,26,26,0.8) 50%, rgba(26,26,26,0) 100%), url(${bgUrl}) right top / cover no-repeat`;
    document.getElementById('detail-poster-img').src = posterUrl;
    document.getElementById('detail-title').innerText = movie.title;
    
    // Proper rating bind
    const rating = movie.vote_average ? movie.vote_average.toFixed(1) : "NR";
    const votesCount = movie.vote_count ? `${(movie.vote_count / 1000).toFixed(1)}K` : "0";
    document.getElementById('detail-score').innerText = rating;
    document.getElementById('detail-votes').innerText = `(${votesCount} Votes)`;
    
    document.getElementById('detail-lang').innerText = movie.original_language === 'hi' ? 'Hindi' : 'English';
    document.getElementById('detail-genres').innerText = getGenresString(movie.genre_ids);
    document.getElementById('detail-release').innerText = movie.release_date || "Coming Soon";
    document.getElementById('detail-desc').innerText = movie.overview || "No description available for this movie.";
    
    generateDetailDates();
    renderDetailTheatres();
    
    document.body.style.overflow = 'hidden';
    view.classList.add('active');
    window.scrollTo(0,0);
    view.scrollTop = 0;
}

window.closeMovieDetail = () => {
    document.getElementById('movie-detail-view').classList.remove('active');
    document.body.style.overflow = 'auto';
}

window.scrollToShowtimes = () => {
    document.getElementById('showtimes-section').scrollIntoView({ behavior: 'smooth' });
}

function generateDetailDates() {
    const selector = document.getElementById('detail-dates');
    const today = new Date();
    const days = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
    let html = '';
    for(let i=0; i<4; i++) {
        let d = new Date(today);
        d.setDate(today.getDate() + i);
        let dayStr = i === 0 ? "TODAY" : i === 1 ? "TOM" : days[d.getDay()];
        html += `<div class="date-item ${i===0?'active':''}" onclick="document.querySelectorAll('.date-item').forEach(e=>e.classList.remove('active')); this.classList.add('active');">
            <span class="day">${dayStr}</span><span class="date">${d.getDate()}</span>
        </div>`;
    }
    selector.innerHTML = html;
}

function renderDetailTheatres() {
    const container = document.getElementById('detail-theatres');
    const theatres = THEATRES_DB[currentCity] || THEATRES_DB["Mumbai"];
    
    container.innerHTML = theatres.map(t => `
        <div class="theatre-row">
            <div class="theatre-info-col">
                <h4><svg viewBox="0 0 24 24" width="16" height="16" stroke="#f84464" stroke-width="2" fill="none"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"></path><line x1="7" y1="7" x2="7.01" y2="7"></line></svg> ${t.name}</h4>
                <div class="t-meta"><span>${t.features}</span></div>
            </div>
            <div class="theatre-times-col">
                ${t.times.map(timeObj => {
                    return `<button class="time-btn ${timeObj.s === 'fast-filling' || timeObj.s === 'almost-full' ? 'fast-filling' : ''}" 
                            onclick="openSeatSelection('${t.name}', '${timeObj.t}', '${timeObj.s}')">
                        ${timeObj.t}
                        <span class="t-desc">${timeObj.s === 'almost-full' ? 'Almost Full' : (timeObj.s === 'fast-filling' ? 'Filling Fast' : 'Available')}</span>
                    </button>`;
                }).join('')}
            </div>
        </div>
    `).join('');
}


/* =========================================
   5. Seat Selection (Realistic Matrix)
========================================= */
window.openSeatSelection = (theatre, time, status) => {
    currentTheatre = theatre;
    currentTime = time;
    
    document.getElementById('seat-movie-title').innerText = currentMovie.title;
    document.getElementById('seat-theatre-meta').innerText = `${theatre} | Today, ${time}`;
    
    generateSeatMap(status);
    
    document.getElementById('seat-selection-view').classList.add('active');
}

window.closeSeatSelection = () => {
    document.getElementById('seat-selection-view').classList.remove('active');
    selectedSeats = [];
    updateSeatFooter();
}

function generateSeatMap(status) {
    const container = document.getElementById('seat-map-container');
    container.innerHTML = '';
    selectedSeats = [];
    updateSeatFooter();
    
    let prob = status === 'almost-full' ? 0.75 : (status === 'fast-filling' ? 0.4 : 0.15);
    
    const tiers = [
        { name: "EXECUTIVE - ₹250", rows: ['A','B','C'], colsLeft: 5, colsRight: 5, price: 250 },
        { name: "CLUB - ₹150", rows: ['D','E','F','G'], colsLeft: 7, colsRight: 7, price: 150 }
    ];
    
    tiers.forEach(tier => {
        const group = document.createElement('div');
        group.className = 'seat-tier-group';
        group.innerHTML = `<div class="tier-price-label">${tier.name}</div>`;
        
        tier.rows.forEach(r => {
            const rowDiv = document.createElement('div');
            rowDiv.className = 'seat-row';
            rowDiv.innerHTML = `<div class="row-label">${r}</div>`;
            
            // Left Block
            for(let i=1; i<=tier.colsLeft; i++) {
                rowDiv.appendChild(createSeat(r, i, tier.price, prob));
            }
            // Aisle
            rowDiv.innerHTML += `<div class="seat-gap"></div>`;
            // Right Block
            for(let i=tier.colsLeft+1; i<=tier.colsLeft+tier.colsRight; i++) {
                rowDiv.appendChild(createSeat(r, i, tier.price, prob));
            }
            
            group.appendChild(rowDiv);
        });
        container.appendChild(group);
    });
}

function createSeat(row, num, price, prob) {
    const isOcc = Math.random() < prob;
    const seat = document.createElement('div');
    seat.className = `seat-col ${isOcc ? 'occupied' : ''}`;
    seat.innerText = num; 
    const sId = `${row}${num}`;
    
    if(!isOcc) {
        seat.onclick = () => {
            seat.classList.toggle('selected');
            if(seat.classList.contains('selected')) {
                selectedSeats.push({ id: sId, price });
            } else {
                selectedSeats = selectedSeats.filter(s => s.id !== sId);
            }
            updateSeatFooter();
        };
    }
    return seat;
}

function updateSeatFooter() {
    const footer = document.getElementById('seat-footer');
    const btn = document.getElementById('btn-pay');
    
    totalPrice = selectedSeats.reduce((sum, s) => sum + s.price, 0);
    
    if (selectedSeats.length > 0) {
        footer.classList.add('active');
        document.getElementById('checkout-amount').innerText = `₹${totalPrice} (${selectedSeats.length} Tickets)`;
        document.getElementById('checkout-total').innerText = `₹${totalPrice}`;
        btn.disabled = false;
    } else {
        footer.classList.remove('active');
        btn.disabled = true;
    }
}


/* =========================================
   6. Auth & Payments
========================================= */
window.openLoginModal = () => document.getElementById('login-modal-overlay').classList.add('active');
window.closeLoginModal = () => document.getElementById('login-modal-overlay').classList.remove('active');

window.simulateLogin = () => {
    const btn = document.querySelector('.auth-submit');
    const originalText = btn.innerText;
    btn.innerText = "Verifying...";
    setTimeout(() => {
        isLoggedIn = true;
        localStorage.setItem('cinex_logged_in', 'true');
        closeLoginModal();
        updateAuthUI();
        btn.innerText = originalText;
    }, 1000);
}

function updateAuthUI() {
    const authSection = document.getElementById('auth-section');
    authSection.innerHTML = `
        <div class="user-badge" onclick="openHistoryModal()">
            <div class="avatar-circle">H</div>
            Hi, Hamdaan
        </div>
    `;
}

window.openHistoryModal = () => {
    const list = document.getElementById('booking-list');
    if(userBookings.length === 0) {
        list.innerHTML = '<p style="text-align:center; padding:2rem; color:#999;">No bookings found.</p>';
    } else {
        list.innerHTML = userBookings.map(b => `
            <div class="booking-card">
                <img src="${b.poster}" alt="Poster">
                <div class="b-info">
                    <h4>${b.movie}</h4>
                    <p>${b.theatre} | ${b.time}</p>
                    <p>${b.seats.length} Tickets (${b.seats.join(', ')})</p>
                    <div class="b-tag">CONFIRMED</div>
                </div>
            </div>
        `).join('');
    }
    document.getElementById('history-modal-overlay').classList.add('active');
}
window.closeHistoryModal = () => document.getElementById('history-modal-overlay').classList.remove('active');

window.openPaymentFlow = () => {
    if (!isLoggedIn) {
        openLoginModal();
        return;
    }
    document.getElementById('payment-amount').innerText = `₹${totalPrice}`;
    document.getElementById('payment-error').style.display = 'none';
    document.getElementById('payment-modal-overlay').classList.add('active');
}
window.closePaymentModal = () => document.getElementById('payment-modal-overlay').classList.remove('active');

window.selectPayMethod = (elem) => {
    document.querySelectorAll('.pay-method').forEach(m => m.classList.remove('active'));
    elem.classList.add('active');
}

window.simulatePayment = () => {
    const btn = document.getElementById('btn-process-pay');
    const err = document.getElementById('payment-error');
    err.style.display = 'none';
    
    btn.innerText = "Processing Bank Request...";
    btn.disabled = true;
    
    setTimeout(() => {
        if (Math.random() < 0.2) {
            // 20% chance to fail
            err.style.display = 'block';
            err.innerText = "Payment Failed: Bank server timeout. Please try again.";
            btn.innerText = "Retry Payment";
            btn.disabled = false;
        } else {
            // Success
            userBookings.unshift({
                movie: currentMovie.title,
                poster: `${IMG_BASE_URL}${currentMovie.poster_path}`,
                theatre: `${currentTheatre} (${currentCity})`,
                time: currentTime,
                seats: selectedSeats.map(s => s.id)
            });
            localStorage.setItem('cinex_bookings', JSON.stringify(userBookings));
            
            btn.innerText = "Success! 🎉";
            btn.style.background = "#10b981";
            btn.style.borderColor = "#10b981";
            
            setTimeout(() => {
                closePaymentModal();
                closeSeatSelection();
                closeMovieDetail();
                btn.innerText = "Pay Securely";
                btn.style.background = "";
                btn.style.borderColor = "";
                btn.disabled = false;
                openHistoryModal();
            }, 1500);
        }
    }, 1500);
}
