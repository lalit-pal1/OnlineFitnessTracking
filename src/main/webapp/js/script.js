/**
 * ONLINE FITNESS TRACKING APPLICATION - CLIENT JAVASCRIPT (PART 4 ENHANCED)
 * Handles Dashboard Loading, Custom Exercises, Dynamic Sets, & Workout History
 */

document.addEventListener('DOMContentLoaded', () => {
    handleUrlAlerts();
    initMobileNavigation();
    init3DTiltEffects();
    initThreeJsHero();
    
    const dataPage = document.body.getAttribute('data-page');

    if (dataPage === 'admin-dashboard') {
        initAdminDashboard();
    } else if (dataPage === 'admin-users') {
        initAdminUsers();
    } else if (dataPage === 'admin-exercises') {
        initAdminExercises();
    } else if (dataPage === 'admin-nutrition') {
        initAdminNutrition();
    } else if (dataPage === 'admin-workouts') {
        initAdminWorkouts();
    } else if (dataPage === 'admin-progress') {
        initAdminProgress();
    } else if (dataPage === 'goals') {
        initGoalsPage();
    }

    // Auto load user dashboard data if on user-dashboard page
    if (document.getElementById('welcome-heading')) {
        loadUserDashboard();
    }

    // Auto initialize workout page if on workout.html
    if (document.getElementById('workout-form')) {
        initWorkoutPage();
    }

    // Auto initialize nutrition page if on nutrition.html
    if (document.getElementById('nutrition-form')) {
        initNutritionPage();
    }

    // Auto initialize progress page if on progress.html
    if (document.getElementById('progress-form')) {
        initProgressPage();
    }

    // Auto initialize goals page if on goals.html
    if (document.getElementById('goal-form')) {
        initGoalsPage();
    }
});

/* ============================================================================
   USER DASHBOARD MODULE
   ============================================================================ */

/**
 * Loads User Dashboard details via GET /user-dashboard endpoint
 */
function loadUserDashboard() {
    const currentDateElem = document.getElementById('current-date');
    if (currentDateElem) {
        const options = { weekday: 'long', year: 'numeric', month: 'short', day: 'numeric' };
        currentDateElem.textContent = new Date().toLocaleDateString('en-US', options);
    }

    fetch('user-dashboard', { cache: 'no-store' })
        .then(response => {
            if (response.status === 401) {
                window.location.href = 'login.html?error=unauthorized';
                throw new Error('Unauthorized');
            }
            if (!response.ok) {
                throw new Error('Server error (' + response.status + ') while loading dashboard data');
            }
            return response.json();
        })
        .then(data => {
            if (data.status === 'success') {
                populateDashboard(data);
            } else {
                showDashboardError(data.message || 'Unable to load profile metrics.');
            }
        })
        .catch(err => {
            console.error('Error loading dashboard:', err);
            if (err.message !== 'Unauthorized') {
                showDashboardError(err.message);
            }
        });
}

/**
 * Helper to display error message on dashboard alert banner
 */
function showDashboardError(msg) {
    const alertBox = document.getElementById('dashboard-alert');
    if (alertBox) {
        alertBox.textContent = '❌ ' + msg;
        alertBox.classList.remove('hidden');
    }
}

/**
 * Populates DOM elements with fetched user profile and BMI data
 */
function populateDashboard(data) {
    // Welcome Banner
    const welcomeHeading = document.getElementById('welcome-heading');
    if (welcomeHeading) welcomeHeading.textContent = `Welcome, ${data.name}!`;

    // Summary Stat Cards
    const statWeight = document.getElementById('stat-weight');
    if (statWeight) statWeight.textContent = `${data.weight} kg`;

    const statBmi = document.getElementById('stat-bmi');
    if (statBmi) statBmi.textContent = data.formattedBmi;

    const statBmiCategory = document.getElementById('stat-bmi-category');
    if (statBmiCategory) {
        statBmiCategory.textContent = data.bmiCategory;
        statBmiCategory.className = 'bmi-badge';
        
        switch (data.bmiCategory.toLowerCase()) {
            case 'underweight':
                statBmiCategory.classList.add('badge-underweight');
                break;
            case 'normal':
                statBmiCategory.classList.add('badge-normal');
                break;
            case 'overweight':
                statBmiCategory.classList.add('badge-overweight');
                break;
            case 'obese':
                statBmiCategory.classList.add('badge-obese');
                break;
        }
    }

    const statGoal = document.getElementById('stat-goal');
    if (statGoal) statGoal.textContent = data.fitnessGoal;

    const statAgeGender = document.getElementById('stat-age-gender');
    if (statAgeGender) statAgeGender.textContent = `${data.age} yrs • ${data.gender}`;

    // Profile Card Sidebar
    const profileInitials = document.getElementById('profile-initials');
    if (profileInitials && data.name) {
        profileInitials.textContent = data.name.charAt(0).toUpperCase();
    }

    const profileName = document.getElementById('profile-name');
    if (profileName) profileName.textContent = data.name;

    const profileEmail = document.getElementById('profile-email');
    if (profileEmail) profileEmail.textContent = data.email;

    const detailAge = document.getElementById('detail-age');
    if (detailAge) detailAge.textContent = `${data.age} years`;

    const detailGender = document.getElementById('detail-gender');
    if (detailGender) detailGender.textContent = data.gender;

    const detailHeight = document.getElementById('detail-height');
    if (detailHeight) detailHeight.textContent = `${data.height} cm`;

    const detailWeight = document.getElementById('detail-weight');
    if (detailWeight) detailWeight.textContent = `${data.weight} kg`;

    const detailGoal = document.getElementById('detail-goal');
    if (detailGoal) detailGoal.textContent = data.fitnessGoal;
}

/* ============================================================================
   WORKOUT TRACKING MODULE (PART 4 ENHANCED)
   ============================================================================ */

/**
 * Initializes workout page: sets default date, exercise input mode, dynamic set rows, and history.
 */
function initWorkoutPage() {
    const workoutDateInput = document.getElementById('workoutDate');
    if (workoutDateInput && !workoutDateInput.value) {
        const today = new Date().toISOString().split('T')[0];
        workoutDateInput.value = today;
    }

    const currentDateElem = document.getElementById('current-date');
    if (currentDateElem) {
        const options = { weekday: 'long', year: 'numeric', month: 'short', day: 'numeric' };
        currentDateElem.textContent = new Date().toLocaleDateString('en-US', options);
    }

    toggleExerciseInputMode();
    handleSetCountChange();
    loadExercisesDropdown();
    loadWorkoutHistory();
}

/**
 * Toggles between Catalog Exercise dropdown and Custom Exercise text input.
 */
function toggleExerciseInputMode() {
    const exerciseTypeElems = document.getElementsByName('exerciseType');
    let selectedType = 'catalog';
    for (const radio of exerciseTypeElems) {
        if (radio.checked) {
            selectedType = radio.value;
            break;
        }
    }

    const catalogGroup = document.getElementById('catalog-exercise-group');
    const customGroup = document.getElementById('custom-exercise-group');
    const exerciseIdSelect = document.getElementById('exerciseId');
    const customExerciseInput = document.getElementById('customExerciseName');

    const toggleCatalogLabel = document.getElementById('toggle-catalog-label');
    const toggleCustomLabel = document.getElementById('toggle-custom-label');

    if (selectedType === 'catalog') {
        if (catalogGroup) catalogGroup.classList.remove('hidden');
        if (customGroup) customGroup.classList.add('hidden');
        if (exerciseIdSelect) exerciseIdSelect.required = true;
        if (customExerciseInput) customExerciseInput.required = false;

        if (toggleCatalogLabel) toggleCatalogLabel.classList.add('active');
        if (toggleCustomLabel) toggleCustomLabel.classList.remove('active');
    } else {
        if (catalogGroup) catalogGroup.classList.add('hidden');
        if (customGroup) customGroup.classList.remove('hidden');
        if (exerciseIdSelect) exerciseIdSelect.required = false;
        if (customExerciseInput) customExerciseInput.required = true;

        if (toggleCatalogLabel) toggleCatalogLabel.classList.remove('active');
        if (toggleCustomLabel) toggleCustomLabel.classList.add('active');
    }
}

/**
 * Handles set count change and dynamically regenerates set rows.
 */
function handleSetCountChange() {
    const numSetsInput = document.getElementById('numSets');
    if (!numSetsInput) return;

    let numSets = parseInt(numSetsInput.value);
    if (isNaN(numSets) || numSets < 1) numSets = 1;
    if (numSets > 30) numSets = 30;

    generateSetRows(numSets);
}

/**
 * Dynamically generates input table rows for Set 1 to Set N (preserving existing inputs).
 */
function generateSetRows(numSets) {
    const container = document.getElementById('set-rows-container');
    if (!container) return;

    // Preserve existing user inputs before re-rendering
    const existingValues = {};
    for (let i = 1; i <= 30; i++) {
        const repsElem = document.getElementById(`reps_${i}`);
        const weightElem = document.getElementById(`weight_${i}`);
        if (repsElem && weightElem) {
            existingValues[i] = {
                reps: repsElem.value,
                weight: weightElem.value
            };
        }
    }

    let tableHtml = `
        <table class="set-input-table">
            <thead>
                <tr>
                    <th style="width: 20%;">Set #</th>
                    <th style="width: 40%;">Reps (per set) *</th>
                    <th style="width: 40%;">Weight (kg) *</th>
                </tr>
            </thead>
            <tbody>`;

    for (let i = 1; i <= numSets; i++) {
        const prevReps = (existingValues[i] && existingValues[i].reps !== '') ? existingValues[i].reps : (i === 1 ? '10' : '');
        const prevWeight = (existingValues[i] && existingValues[i].weight !== '') ? existingValues[i].weight : (i === 1 ? '20' : '');

        tableHtml += `
            <tr>
                <td class="set-number-label">Set ${i}</td>
                <td>
                    <input type="number" id="reps_${i}" name="reps_${i}" value="${prevReps}" placeholder="e.g. 10" min="1" max="200" class="set-row-input" required>
                </td>
                <td>
                    <input type="number" step="0.5" id="weight_${i}" name="weight_${i}" value="${prevWeight}" placeholder="e.g. 20.0" min="0" max="500" class="set-row-input" required>
                </td>
            </tr>`;
    }

    tableHtml += `</tbody></table>`;
    container.innerHTML = tableHtml;
}

/**
 * Fetches available exercises from DB and populates dropdown
 */
function loadExercisesDropdown() {
    const selectElem = document.getElementById('exerciseId');
    if (!selectElem) return;

    fetch('workout?action=exercises', { cache: 'no-store' })
        .then(res => {
            if (res.status === 401) {
                window.location.href = 'login.html?error=unauthorized';
                throw new Error('Unauthorized');
            }
            return res.json();
        })
        .then(data => {
            if (data.status === 'success' && Array.isArray(data.exercises)) {
                selectElem.innerHTML = '<option value="">-- Select Exercise --</option>';
                data.exercises.forEach(ex => {
                    const opt = document.createElement('option');
                    opt.value = ex.exerciseId;
                    opt.textContent = `${ex.exerciseName} (${ex.muscleGroup})`;
                    selectElem.appendChild(opt);
                });
            }
        })
        .catch(err => console.error('Error loading exercises dropdown:', err));
}

/**
 * Fetches user's logged workout history from DB and populates table & quick stats
 */
function loadWorkoutHistory() {
    const container = document.getElementById('workout-history-container');
    const countElem = document.getElementById('history-count');
    const statWorkoutsElem = document.getElementById('stat-total-workouts');
    const statSetsElem = document.getElementById('stat-total-sets');
    const statVolumeElem = document.getElementById('stat-total-volume');

    if (!container) return;

    fetch('workout?action=list', { cache: 'no-store' })
        .then(res => {
            if (res.status === 401) {
                window.location.href = 'login.html?error=unauthorized';
                throw new Error('Unauthorized');
            }
            return res.json();
        })
        .then(data => {
            if (data.status === 'success' && Array.isArray(data.workouts)) {
                const workouts = data.workouts;

                // Update Quick Stats Bar
                if (statWorkoutsElem) statWorkoutsElem.textContent = data.totalWorkouts || 0;
                if (statSetsElem) statSetsElem.textContent = data.totalSets || 0;
                if (statVolumeElem) statVolumeElem.textContent = `${data.totalVolume || 0} kg`;
                if (countElem) countElem.textContent = `${workouts.length} Session${workouts.length === 1 ? '' : 's'}`;

                if (workouts.length === 0) {
                    container.innerHTML = `
                        <div class="placeholder-box">
                            <div class="placeholder-icon">🏋️‍♂️</div>
                            <h4>No Workouts Logged Yet</h4>
                            <p>Complete the form above to record your first workout session.</p>
                        </div>`;
                    return;
                }

                let tableHtml = `
                    <div class="history-table-wrapper">
                    <table class="workout-history-table">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>Exercise</th>
                                <th>Muscle</th>
                                <th>Date</th>
                                <th>Sets</th>
                                <th>Reps</th>
                                <th>Weight (kg)</th>
                                <th>Total Volume</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>`;

                workouts.forEach((w, idx) => {
                    const isCustom = !w.exerciseId || w.exerciseId === 0 || w.exerciseId === 'null';
                    const muscleClass = isCustom ? 'muscle-tag custom-tag' : 'muscle-tag';

                    tableHtml += `
                        <tr>
                            <td><strong>${idx + 1}</strong></td>
                            <td>
                                <div class="exercise-name-cell">${escapeHtml(w.exerciseName)}</div>
                            </td>
                            <td>
                                <span class="${muscleClass}">${escapeHtml(w.muscleGroup)}</span>
                            </td>
                            <td>${formatDate(w.workoutDate)}</td>
                            <td><strong>${w.totalSets}</strong></td>
                            <td>${escapeHtml(w.repsSummary)}</td>
                            <td>${escapeHtml(w.weightsSummary)}</td>
                            <td><span class="volume-badge">${w.totalVolume} kg</span></td>
                            <td>
                                <button onclick="deleteWorkout(${w.workoutId})" class="btn-danger-sm">Delete</button>
                            </td>
                        </tr>`;
                });

                tableHtml += `</tbody></table></div>`;
                container.innerHTML = tableHtml;
            }
        })
        .catch(err => console.error('Error loading workout history:', err));
}

/**
 * Submits the Add Workout form with dynamic set details via AJAX POST
 */
function handleWorkoutSubmit(event) {
    event.preventDefault();

    const exerciseTypeElems = document.getElementsByName('exerciseType');
    let selectedType = 'catalog';
    for (const radio of exerciseTypeElems) {
        if (radio.checked) {
            selectedType = radio.value;
            break;
        }
    }

    const exerciseId = document.getElementById('exerciseId').value;
    const customExerciseName = document.getElementById('customExerciseName').value.trim();
    const workoutDate = document.getElementById('workoutDate').value;
    const numSets = parseInt(document.getElementById('numSets').value);

    // Validate Exercise Selection
    if (selectedType === 'catalog' && (!exerciseId || exerciseId === '')) {
        showWorkoutAlert('⚠️ Please select an exercise from the catalog.', 'error');
        return false;
    }

    if (selectedType === 'custom' && (!customExerciseName || customExerciseName === '')) {
        showWorkoutAlert('⚠️ Please enter a custom exercise name.', 'error');
        return false;
    }

    if (!workoutDate) {
        showWorkoutAlert('⚠️ Please select a workout date.', 'error');
        return false;
    }

    if (isNaN(numSets) || numSets <= 0) {
        showWorkoutAlert('⚠️ Number of sets must be a positive number.', 'error');
        return false;
    }

    // Prepare Form Data with set-by-set inputs
    const params = new URLSearchParams();
    params.append('action', 'add');
    params.append('exerciseType', selectedType);
    params.append('exerciseId', selectedType === 'catalog' ? exerciseId : '');
    params.append('customExerciseName', selectedType === 'custom' ? customExerciseName : '');
    params.append('workoutDate', workoutDate);
    params.append('numSets', numSets);

    // Validate and collect set details
    for (let i = 1; i <= numSets; i++) {
        const repsElem = document.getElementById(`reps_${i}`);
        const weightElem = document.getElementById(`weight_${i}`);

        if (!repsElem || !weightElem) {
            showWorkoutAlert(`⚠️ Missing input for Set #${i}.`, 'error');
            return false;
        }

        const reps = parseInt(repsElem.value);
        const weight = parseFloat(weightElem.value);

        if (isNaN(reps) || reps <= 0) {
            showWorkoutAlert(`⚠️ Reps for Set #${i} must be a positive integer greater than zero.`, 'error');
            return false;
        }

        if (isNaN(weight) || weight < 0) {
            showWorkoutAlert(`⚠️ Weight for Set #${i} cannot be negative.`, 'error');
            return false;
        }

        params.append(`reps_${i}`, reps);
        params.append(`weight_${i}`, weight);
    }

    fetch('workout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        if (data.status === 'success') {
            showWorkoutAlert(`✅ ${data.message}`, 'success');
            document.getElementById('workout-form').reset();
            
            // Re-apply defaults
            const workoutDateInput = document.getElementById('workoutDate');
            if (workoutDateInput) workoutDateInput.value = new Date().toISOString().split('T')[0];
            const numSetsInput = document.getElementById('numSets');
            if (numSetsInput) numSetsInput.value = 3;

            toggleExerciseInputMode();
            handleSetCountChange();
            loadWorkoutHistory();
        } else {
            showWorkoutAlert(`❌ ${data.message}`, 'error');
        }
    })
    .catch(err => {
        console.error('Error logging workout:', err);
        showWorkoutAlert('❌ Error logging workout. Please check your connection.', 'error');
    });

    return false;
}

/**
 * Deletes a workout entry via AJAX POST to /workout
 */
function deleteWorkout(workoutId) {
    if (!confirm('Are you sure you want to delete this workout entry?')) {
        return;
    }

    const params = new URLSearchParams({
        action: 'delete',
        workoutId: workoutId
    });

    fetch('workout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        if (data.status === 'success') {
            showWorkoutAlert(`✅ ${data.message}`, 'success');
            loadWorkoutHistory();
        } else {
            showWorkoutAlert(`❌ ${data.message}`, 'error');
        }
    })
    .catch(err => {
        console.error('Error deleting workout:', err);
        showWorkoutAlert('❌ Error deleting workout entry.', 'error');
    });
}

/**
 * Helper to display alert box on workout page
 */
function showWorkoutAlert(message, type) {
    const alertBox = document.getElementById('workout-alert');
    if (!alertBox) return;

    alertBox.textContent = message;
    alertBox.classList.remove('hidden', 'alert-error', 'alert-success');
    alertBox.classList.add(type === 'success' ? 'alert-success' : 'alert-error');
}

/**
 * Helper to format date strings nicely
 */
function formatDate(dateStr) {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
        const d = new Date(parts[0], parts[1] - 1, parts[2]);
        return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    }
    return dateStr;
}

/**
 * Helper to escape HTML characters
 */
function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

/* ============================================================================
   COMMON UTILITY FUNCTIONS (MODALS, ALERTS, FORM VALIDATION)
   ============================================================================ */

/**
 * Shows "Coming Soon" modal dialog for quick action cards
 */
function showComingSoonModal(moduleName) {
    const modal = document.getElementById('coming-soon-modal');
    const featureNameElem = document.getElementById('modal-feature-name');
    const moduleTitleElem = document.getElementById('modal-module-title');

    if (featureNameElem) featureNameElem.textContent = moduleName;
    if (moduleTitleElem) moduleTitleElem.textContent = `${moduleName} Module`;
    if (modal) modal.classList.remove('hidden');
}

/**
 * Closes "Coming Soon" modal dialog
 */
function closeComingSoonModal() {
    const modal = document.getElementById('coming-soon-modal');
    if (modal) modal.classList.add('hidden');
}

/**
 * Checks URL parameters for error or success codes and displays alert messages.
 */
function handleUrlAlerts() {
    const alertBox = document.getElementById('alert-box');
    if (!alertBox) return;

    const urlParams = new URLSearchParams(window.location.search);

    if (urlParams.get('registered') === 'true') {
        showAlert(alertBox, '✅ Registration successful! Please log in with your credentials.', 'success');
        return;
    }

    if (urlParams.get('loggedOut') === 'true') {
        showAlert(alertBox, 'ℹ️ You have been logged out successfully.', 'success');
        return;
    }

    const errorCode = urlParams.get('error');
    const msg = urlParams.get('msg');

    if (errorCode) {
        let message = 'An error occurred. Please try again.';
        
        switch (errorCode) {
            case 'invalid_credentials':
                message = msg 
                    ? '❌ Database Error during login: ' + decodeURIComponent(msg)
                    : '❌ Invalid email address or password.';
                break;
            case 'email_exists':
                message = '⚠️ An account with this email address already exists.';
                break;
            case 'missing_fields':
                message = '⚠️ Please fill out all required fields.';
                break;
            case 'invalid_numbers':
            case 'invalid_format':
                message = '⚠️ Age, height, and weight must be valid positive numbers.';
                break;
            case 'registration_failed':
                message = msg 
                    ? '❌ Database Error during registration: ' + decodeURIComponent(msg)
                    : '❌ Registration failed due to a database error. Please check your MySQL connection and credentials.';
                break;
            case 'unauthorized':
                message = '🔒 Access denied. Please log in first.';
                break;
        }

        showAlert(alertBox, message, 'error');
    }
}

/**
 * Helper to display alert box
 */
function showAlert(alertBox, message, type) {
    alertBox.textContent = message;
    alertBox.classList.remove('hidden', 'alert-error', 'alert-success');
    alertBox.classList.add(type === 'success' ? 'alert-success' : 'alert-error');
}

/**
 * Client-Side Validation for User Registration Form
 */
function validateRegisterForm() {
    const name = document.getElementById('name').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const age = parseInt(document.getElementById('age').value);
    const height = parseFloat(document.getElementById('height').value);
    const weight = parseFloat(document.getElementById('weight').value);
    const alertBox = document.getElementById('alert-box');

    if (!name || !email || !password) {
        showAlert(alertBox, 'Please complete all required text fields.', 'error');
        return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        showAlert(alertBox, 'Please enter a valid email address format (e.g. user@example.com).', 'error');
        return false;
    }

    if (password.length < 4) {
        showAlert(alertBox, 'Password must be at least 4 characters long.', 'error');
        return false;
    }

    if (isNaN(age) || age < 10 || age > 100) {
        showAlert(alertBox, 'Age must be a valid number between 10 and 100.', 'error');
        return false;
    }

    if (isNaN(height) || height < 50 || height > 250) {
        showAlert(alertBox, 'Height must be a valid number between 50 cm and 250 cm.', 'error');
        return false;
    }

    if (isNaN(weight) || weight < 20 || weight > 300) {
        showAlert(alertBox, 'Weight must be a valid number between 20 kg and 300 kg.', 'error');
        return false;
    }

    return true;
}

/**
 * Client-Side Validation for User Login Form
 */
function validateLoginForm() {
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const alertBox = document.getElementById('alert-box');

    if (!email || !password) {
        showAlert(alertBox, 'Please enter both your email address and password.', 'error');
        return false;
    }

    return true;
}

/**
 * Client-Side Validation for Admin Login Form
 */
function validateAdminLoginForm() {
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const alertBox = document.getElementById('alert-box');

    if (!email || !password) {
        showAlert(alertBox, 'Please enter administrator email and password.', 'error');
        return false;
    }

    return true;
}

/**
 * Helper to return YYYY-MM-DD in local browser timezone (prevents UTC timezone shift bugs)
 */
function getLocalDateString(d = new Date()) {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

/* ============================================================================
   NUTRITION TRACKING & AI SCANNER MODULE (PART 5.1 ENHANCED)
   ============================================================================ */

let selectedMealImageFile = null;

/**
 * Initializes nutrition page: sets default dates, default local times, loads daily summary totals and history.
 */
function initNutritionPage() {
    const today = getLocalDateString();

    const filterDateInput = document.getElementById('nutritionFilterDate');
    if (filterDateInput && !filterDateInput.value) {
        filterDateInput.value = today;
    }

    const logDateInput = document.getElementById('nutritionDate');
    if (logDateInput && !logDateInput.value) {
        logDateInput.value = today;
    }

    const aiDateInput = document.getElementById('ai-nutritionDate');
    if (aiDateInput && !aiDateInput.value) {
        aiDateInput.value = today;
    }

    const now = new Date();
    const timeStr = String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0');

    const mealTimeInput = document.getElementById('mealTime');
    if (mealTimeInput && !mealTimeInput.value) {
        mealTimeInput.value = timeStr;
    }

    const aiMealTimeInput = document.getElementById('ai-mealTime');
    if (aiMealTimeInput && !aiMealTimeInput.value) {
        aiMealTimeInput.value = timeStr;
    }

    const currentDateElem = document.getElementById('current-date');
    if (currentDateElem) {
        const options = { weekday: 'long', year: 'numeric', month: 'short', day: 'numeric' };
        currentDateElem.textContent = new Date().toLocaleDateString('en-US', options);
    }

    handleMealTypeChange();
    loadNutritionData();
}

/**
 * Loads nutrition logs and daily totals for selected date.
 */
function loadNutritionData() {
    const filterDateInput = document.getElementById('nutritionFilterDate');
    const selectedDate = filterDateInput ? filterDateInput.value : '';

    loadNutritionHistory(selectedDate);
}

/**
 * Handles date filter input change event.
 */
function handleDateFilterChange() {
    loadNutritionData();
}

/**
 * Resets date filter to today's date.
 */
function resetDateFilterToToday() {
    const filterDateInput = document.getElementById('nutritionFilterDate');
    if (filterDateInput) {
        filterDateInput.value = getLocalDateString();
        loadNutritionData();
    }
}

/**
 * Clears date filter and fetches nutrition history across all recorded dates.
 */
function showAllNutritionHistory() {
    const filterDateInput = document.getElementById('nutritionFilterDate');
    if (filterDateInput) {
        filterDateInput.value = '';
    }
    loadNutritionHistory('');
}

/**
 * Clears the Add Food form inputs.
 */
function clearNutritionForm() {
    const form = document.getElementById('nutrition-form');
    if (form) {
        form.reset();
        const logDateInput = document.getElementById('nutritionDate');
        if (logDateInput) {
            logDateInput.value = getLocalDateString();
        }
        const now = new Date();
        const timeStr = String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0');
        const mealTimeInput = document.getElementById('mealTime');
        if (mealTimeInput) mealTimeInput.value = timeStr;
    }
}

/**
 * Triggers hidden file input for AI meal photo upload.
 */
function triggerFileInput() {
    const fileInput = document.getElementById('mealImageInput');
    if (fileInput) fileInput.click();
}

/**
 * Handles meal photo selection from file input.
 */
function handleMealImageSelect(event) {
    const file = event.target.files[0];
    if (!file) return;

    // Check MIME type or filename extension
    const isImageMime = file.type && file.type.match('image.*');
    const isImageExt = /\.(jpe?g|png|webp|gif|bmp)$/i.test(file.name);

    if (!isImageMime && !isImageExt) {
        showNutritionAlert('⚠️ Please select a valid image file (JPG, PNG, or WEBP).', 'error');
        return;
    }

    if (file.size > 5 * 1024 * 1024) {
        showNutritionAlert('⚠️ Image file size exceeds 5 MB limit. Please select a smaller photo.', 'error');
        return;
    }

    selectedMealImageFile = file;

    const dropzoneContent = document.getElementById('dropzone-content');
    const previewBox = document.getElementById('image-preview-container');
    const previewImg = document.getElementById('meal-image-preview');
    const filenameElem = document.getElementById('preview-filename');
    const filesizeElem = document.getElementById('preview-filesize');

    const reader = new FileReader();
    reader.onload = function(e) {
        if (previewImg) previewImg.src = e.target.result;
        if (filenameElem) filenameElem.textContent = file.name;
        if (filesizeElem) filesizeElem.textContent = (file.size / 1024).toFixed(1) + ' KB';

        if (dropzoneContent) {
            dropzoneContent.style.display = 'none';
            dropzoneContent.classList.add('hidden');
        }
        if (previewBox) {
            previewBox.style.display = 'block';
            previewBox.classList.remove('hidden');
        }
    };
    reader.readAsDataURL(file);
}

/**
 * Clears and removes selected meal image.
 */
function removeMealImage() {
    selectedMealImageFile = null;
    const fileInput = document.getElementById('mealImageInput');
    if (fileInput) fileInput.value = '';

    const dropzoneContent = document.getElementById('dropzone-content');
    const previewBox = document.getElementById('image-preview-container');
    const loadingBox = document.getElementById('ai-loading-box');
    const aiResultsCard = document.getElementById('ai-results-card');

    if (dropzoneContent) {
        dropzoneContent.style.display = 'block';
        dropzoneContent.classList.remove('hidden');
    }
    if (previewBox) {
        previewBox.style.display = 'none';
        previewBox.classList.add('hidden');
    }
    if (loadingBox) {
        loadingBox.style.display = 'none';
        loadingBox.classList.add('hidden');
    }
    if (aiResultsCard) {
        aiResultsCard.style.display = 'none';
        aiResultsCard.classList.add('hidden');
    }
}

/**
 * Sends selected meal image to backend /nutrition?action=analyzeImage for AI estimation.
 */
function analyzeMealImage() {
    if (!selectedMealImageFile) {
        showNutritionAlert('⚠️ Please select a meal photo first.', 'error');
        return;
    }

    const previewBox = document.getElementById('image-preview-container');
    const loadingBox = document.getElementById('ai-loading-box');
    const aiResultsCard = document.getElementById('ai-results-card');

    if (previewBox) {
        previewBox.style.display = 'none';
        previewBox.classList.add('hidden');
    }
    if (loadingBox) {
        loadingBox.style.display = 'block';
        loadingBox.classList.remove('hidden');
    }
    if (aiResultsCard) {
        aiResultsCard.style.display = 'none';
        aiResultsCard.classList.add('hidden');
    }

    const formData = new FormData();
    formData.append('action', 'analyzeImage');
    formData.append('mealImage', selectedMealImageFile);

    fetch('nutrition', {
        method: 'POST',
        body: formData
    })
    .then(res => res.json())
    .then(data => {
        if (loadingBox) {
            loadingBox.style.display = 'none';
            loadingBox.classList.add('hidden');
        }
        if (previewBox) {
            previewBox.style.display = 'block';
            previewBox.classList.remove('hidden');
        }

        if (data.status === 'success' && data.result) {
            displayAIResult(data.result);
        } else {
            showNutritionAlert(`ℹ️ ${data.message || 'AI Food Scanner is currently unavailable. You can still enter nutrition manually.'}`, 'error');
        }
    })
    .catch(err => {
        console.error('Error analyzing meal image:', err);
        if (loadingBox) {
            loadingBox.style.display = 'none';
            loadingBox.classList.add('hidden');
        }
        if (previewBox) {
            previewBox.style.display = 'block';
            previewBox.classList.remove('hidden');
        }
        showNutritionAlert('ℹ️ AI Food Scanner is currently unavailable. You can still enter nutrition manually.', 'error');
    });
}

/**
 * Displays pre-filled editable AI result card.
 */
function displayAIResult(res) {
    const aiResultsCard = document.getElementById('ai-results-card');
    const detectedItemsElem = document.getElementById('ai-detected-items');

    const foodNameInput = document.getElementById('ai-foodName');
    const caloriesInput = document.getElementById('ai-calories');
    const proteinInput = document.getElementById('ai-protein');
    const carbsInput = document.getElementById('ai-carbs');
    const fatsInput = document.getElementById('ai-fats');

    if (detectedItemsElem) {
        detectedItemsElem.innerHTML = '';
        if (Array.isArray(res.items) && res.items.length > 0) {
            res.items.forEach(item => {
                const tag = document.createElement('span');
                tag.className = 'detected-tag';
                tag.textContent = `${item.name || 'FoodItem'} (~${item.portion || 'portion'})`;
                detectedItemsElem.appendChild(tag);
            });
        } else {
            const tag = document.createElement('span');
            tag.className = 'detected-tag';
            tag.textContent = res.foodName || 'Meal Photo';
            detectedItemsElem.appendChild(tag);
        }
    }

    if (foodNameInput) foodNameInput.value = res.foodName || 'Analyzed Meal';
    if (caloriesInput) caloriesInput.value = res.calories || 0;
    if (proteinInput) proteinInput.value = res.protein || 0;
    if (carbsInput) carbsInput.value = res.carbs || 0;
    if (fatsInput) fatsInput.value = res.fats || 0;

    const now = new Date();
    const timeStr = String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0');
    const aiMealTimeInput = document.getElementById('ai-mealTime');
    if (aiMealTimeInput) aiMealTimeInput.value = timeStr;

    const aiDateInput = document.getElementById('ai-nutritionDate');
    const filterDateInput = document.getElementById('nutritionFilterDate');
    if (aiDateInput) {
        aiDateInput.value = (filterDateInput && filterDateInput.value) ? filterDateInput.value : getLocalDateString();
    }

    if (aiResultsCard) {
        aiResultsCard.style.display = 'block';
        aiResultsCard.classList.remove('hidden');
        aiResultsCard.scrollIntoView({ behavior: 'smooth' });
    }
}

/**
 * Submits the user-reviewed AI nutrition entry.
 */
function saveAIAnalyzedNutrition(event) {
    event.preventDefault();

    const foodName = document.getElementById('ai-foodName').value.trim();
    const calories = parseFloat(document.getElementById('ai-calories').value);
    const protein = parseFloat(document.getElementById('ai-protein').value);
    const carbs = parseFloat(document.getElementById('ai-carbs').value);
    const fats = parseFloat(document.getElementById('ai-fats').value);
    const mealTypeSelect = document.getElementById('ai-mealType');
    const customMealNameInput = document.getElementById('ai-customMealName');
    const mealTime = document.getElementById('ai-mealTime').value;
    const nutritionDate = document.getElementById('ai-nutritionDate').value;

    let mealType = mealTypeSelect ? mealTypeSelect.value : 'Lunch';
    if (mealType === 'Other' && customMealNameInput && customMealNameInput.value.trim() !== '') {
        mealType = customMealNameInput.value.trim();
    }

    submitNutritionEntry(foodName, calories, protein, carbs, fats, nutritionDate, mealType, mealTime, selectedMealImageFile ? selectedMealImageFile.name : null);
    return false;
}

/**
 * Handles meal type selection changes on AI and manual forms to toggle custom meal name input.
 */
function handleMealTypeChange() {
    const mealTypeSelect = document.getElementById('mealType');
    const customGroup = document.getElementById('custom-meal-group');
    if (mealTypeSelect && customGroup) {
        if (mealTypeSelect.value === 'Other') {
            customGroup.style.display = 'block';
            customGroup.classList.remove('hidden');
        } else {
            customGroup.style.display = 'none';
            customGroup.classList.add('hidden');
        }
    }
}

function handleAiMealTypeChange() {
    const mealTypeSelect = document.getElementById('ai-mealType');
    const customGroup = document.getElementById('ai-customMealGroup');
    if (mealTypeSelect && customGroup) {
        if (mealTypeSelect.value === 'Other') {
            customGroup.style.display = 'block';
            customGroup.classList.remove('hidden');
        } else {
            customGroup.style.display = 'none';
            customGroup.classList.add('hidden');
        }
    }
}

/**
 * Shared helper to send add nutrition request via AJAX POST.
 */
function submitNutritionEntry(foodName, calories, protein, carbs, fats, nutritionDate, mealType, mealTime, imageName) {
    if (!foodName) {
        showNutritionAlert('⚠️ Food name is required.', 'error');
        return;
    }

    if (isNaN(calories) || calories < 0 || isNaN(protein) || protein < 0 || isNaN(carbs) || carbs < 0 || isNaN(fats) || fats < 0) {
        showNutritionAlert('⚠️ Calories, protein, carbs, and fats must be valid non-negative numbers.', 'error');
        return;
    }

    if (!nutritionDate) {
        showNutritionAlert('⚠️ Log date is required.', 'error');
        return;
    }

    const params = new URLSearchParams({
        action: 'add',
        foodName: foodName,
        calories: calories,
        protein: protein,
        carbs: carbs,
        fats: fats,
        nutritionDate: nutritionDate,
        mealType: mealType || 'Other',
        mealTime: mealTime || '',
        imageName: imageName || ''
    });

    fetch('nutrition', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        if (data.status === 'success') {
            showNutritionAlert(`✅ ${data.message}`, 'success');
            clearNutritionForm();
            removeMealImage();

            const filterDateInput = document.getElementById('nutritionFilterDate');
            if (filterDateInput) {
                filterDateInput.value = nutritionDate;
            }
            loadNutritionData();
        } else {
            showNutritionAlert(`❌ ${data.message}`, 'error');
        }
    })
    .catch(err => {
        console.error('Error logging food entry:', err);
        showNutritionAlert('❌ Error logging food entry. Please check connection.', 'error');
    });
}

/**
 * Handles submission of manual Add Food form.
 */
function handleNutritionSubmit(event) {
    event.preventDefault();

    const foodName = document.getElementById('foodName').value.trim();
    const calories = parseFloat(document.getElementById('calories').value);
    const protein = parseFloat(document.getElementById('protein').value);
    const carbs = parseFloat(document.getElementById('carbs').value);
    const fats = parseFloat(document.getElementById('fats').value);
    const nutritionDate = document.getElementById('nutritionDate').value;
    const mealTypeSelect = document.getElementById('mealType');
    const customMealNameInput = document.getElementById('customMealName');
    const mealTime = document.getElementById('mealTime').value;

    let mealType = mealTypeSelect ? mealTypeSelect.value : 'Other';
    if (mealType === 'Other' && customMealNameInput && customMealNameInput.value.trim() !== '') {
        mealType = customMealNameInput.value.trim();
    }

    submitNutritionEntry(foodName, calories, protein, carbs, fats, nutritionDate, mealType, mealTime, null);
    return false;
}

/**
 * Helper to render meal category badge with custom styling.
 */
function getMealTypeBadge(type) {
    if (!type) return '<span class="meal-badge badge-other">Other</span>';
    const lower = type.toLowerCase();
    if (lower.includes('breakfast')) return `<span class="meal-badge badge-breakfast">🌅 ${escapeHtml(type)}</span>`;
    if (lower.includes('lunch')) return `<span class="meal-badge badge-lunch">☀️ ${escapeHtml(type)}</span>`;
    if (lower.includes('dinner')) return `<span class="meal-badge badge-dinner">🌙 ${escapeHtml(type)}</span>`;
    if (lower.includes('snack')) return `<span class="meal-badge badge-snack">🍌 ${escapeHtml(type)}</span>`;
    if (lower.includes('workout')) return `<span class="meal-badge badge-workout">⚡ ${escapeHtml(type)}</span>`;
    return `<span class="meal-badge badge-other">📌 ${escapeHtml(type)}</span>`;
}

/**
 * Helper to format 24h time HH:mm:ss to 12-hour AM/PM format.
 */
function formatTime12H(timeStr) {
    if (!timeStr) return '--';
    const parts = timeStr.split(':');
    if (parts.length >= 2) {
        let hours = parseInt(parts[0]);
        const minutes = parts[1];
        const ampm = hours >= 12 ? 'PM' : 'AM';
        hours = hours % 12;
        hours = hours ? hours : 12;
        return `${String(hours).padStart(2, '0')}:${minutes} ${ampm}`;
    }
    return timeStr;
}

/**
 * Fetches user nutrition history logs & daily totals from GET /nutrition
 */
function loadNutritionHistory(dateStr) {
    const container = document.getElementById('nutrition-history-container');
    const countElem = document.getElementById('history-count');
    const subtitleElem = document.getElementById('history-subtitle');

    const calElem = document.getElementById('summary-calories');
    const proElem = document.getElementById('summary-protein');
    const carbElem = document.getElementById('summary-carbs');
    const fatElem = document.getElementById('summary-fats');

    if (!container) return;

    let url = 'nutrition?action=list';
    if (dateStr) {
        url += '&date=' + encodeURIComponent(dateStr);
    }

    fetch(url, { cache: 'no-store' })
        .then(res => {
            if (res.status === 401) {
                window.location.href = 'login.html?error=unauthorized';
                throw new Error('Unauthorized');
            }
            return res.json();
        })
        .then(data => {
            if (data.status === 'success') {
                if (calElem) calElem.textContent = `${data.totalCalories || 0} kcal`;
                if (proElem) proElem.textContent = `${data.totalProtein || 0} g`;
                if (carbElem) carbElem.textContent = `${data.totalCarbs || 0} g`;
                if (fatElem) fatElem.textContent = `${data.totalFats || 0} g`;

                const logs = data.logs || [];
                if (countElem) countElem.textContent = `${logs.length} Entr${logs.length === 1 ? 'y' : 'ies'}`;
                if (subtitleElem) {
                    subtitleElem.textContent = dateStr 
                        ? `Logged meals and snacks for ${formatDate(dateStr)}.` 
                        : 'Logged meals and snacks across all recorded dates.';
                }

                if (logs.length === 0) {
                    container.innerHTML = `
                        <div class="placeholder-box">
                            <div class="placeholder-icon">🥗</div>
                            <h4>No Food Entries Found</h4>
                            <p>No nutrition logs recorded for ${dateStr ? formatDate(dateStr) : 'this period'}.</p>
                        </div>`;
                    return;
                }

                let tableHtml = `
                    <div class="history-table-wrapper">
                    <table class="workout-history-table">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Meal</th>
                                <th>Food</th>
                                <th>Calories</th>
                                <th>Protein</th>
                                <th>Carbs</th>
                                <th>Fats</th>
                                <th>Time</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>`;

                logs.forEach(n => {
                    tableHtml += `
                        <tr>
                            <td>${formatDate(n.nutritionDate)}</td>
                            <td>${getMealTypeBadge(n.mealType)}</td>
                            <td><strong>${escapeHtml(n.foodName)}</strong></td>
                            <td><span class="macro-badge badge-calories">${n.calories} kcal</span></td>
                            <td><span class="macro-badge badge-protein">${n.protein} g</span></td>
                            <td><span class="macro-badge badge-carbs">${n.carbs} g</span></td>
                            <td><span class="macro-badge badge-fats">${n.fats} g</span></td>
                            <td><strong>${formatTime12H(n.mealTime)}</strong></td>
                            <td>
                                <button onclick="deleteNutrition(${n.nutritionId})" class="btn-danger-sm">Delete</button>
                            </td>
                        </tr>`;
                });

                tableHtml += `</tbody></table></div>`;
                container.innerHTML = tableHtml;
            } else {
                showNutritionAlert(`❌ ${data.message || 'Error loading nutrition history.'}`, 'error');
            }
        })
        .catch(err => console.error('Error loading nutrition history:', err));
}

/**
 * Deletes a food entry via AJAX POST to /nutrition
 */
function deleteNutrition(nutritionId) {
    if (!confirm('Are you sure you want to delete this food entry?')) {
        return;
    }

    const params = new URLSearchParams({
        action: 'delete',
        nutritionId: nutritionId
    });

    fetch('nutrition', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        if (data.status === 'success') {
            showNutritionAlert(`✅ ${data.message}`, 'success');
            loadNutritionData();
        } else {
            showNutritionAlert(`❌ ${data.message}`, 'error');
        }
    })
    .catch(err => {
        console.error('Error deleting nutrition entry:', err);
        showNutritionAlert('❌ Error deleting food entry.', 'error');
    });
}

/**
 * Helper to display alert box on nutrition page
 */
function showNutritionAlert(message, type) {
    const alertBox = document.getElementById('nutrition-alert');
    if (!alertBox) return;

    alertBox.textContent = message;
    alertBox.classList.remove('hidden', 'alert-error', 'alert-success');
    alertBox.classList.add(type === 'success' ? 'alert-success' : 'alert-error');
}

/* ============================================================================
   PROGRESS TRACKING MODULE (PART 6 ENHANCED)
   ============================================================================ */

/**
 * Initializes progress page: sets default local date, header date, and loads progress history & charts.
 */
function initProgressPage() {
    const today = getLocalDateString();

    const progressDateInput = document.getElementById('progressDate');
    if (progressDateInput && !progressDateInput.value) {
        progressDateInput.value = today;
    }

    const currentDateElem = document.getElementById('current-date');
    if (currentDateElem) {
        const options = { weekday: 'long', year: 'numeric', month: 'short', day: 'numeric' };
        currentDateElem.textContent = new Date().toLocaleDateString('en-US', options);
    }

    loadProgressData();
}

/**
 * Clears the Add Progress form.
 */
function clearProgressForm() {
    const form = document.getElementById('progress-form');
    if (form) {
        form.reset();
        const progressDateInput = document.getElementById('progressDate');
        if (progressDateInput) {
            progressDateInput.value = getLocalDateString();
        }
    }
}

/**
 * Loads progress data, populates summary cards, renders table and line charts.
 */
function loadProgressData() {
    const container = document.getElementById('progress-history-container');
    const countElem = document.getElementById('history-count');

    const weightElem = document.getElementById('stat-progress-weight');
    const bmiElem = document.getElementById('stat-progress-bmi');
    const bmiCatElem = document.getElementById('stat-progress-bmi-category');
    const goalElem = document.getElementById('stat-progress-goal');

    if (!container) return;

    fetch('progress', { cache: 'no-store' })
        .then(res => {
            if (res.status === 401) {
                window.location.href = 'login.html?error=unauthorized';
                throw new Error('Unauthorized');
            }
            return res.json();
        })
        .then(data => {
            if (data.status === 'success') {
                if (weightElem) weightElem.textContent = `${data.currentWeight || 0.0} kg`;
                if (bmiElem) bmiElem.textContent = data.currentBmi || '--';
                
                if (bmiCatElem) {
                    bmiCatElem.textContent = data.bmiCategory || 'N/A';
                    bmiCatElem.className = 'bmi-badge';
                    const catLower = (data.bmiCategory || '').toLowerCase();
                    if (catLower.includes('underweight')) bmiCatElem.classList.add('badge-underweight');
                    else if (catLower.includes('normal')) bmiCatElem.classList.add('badge-normal');
                    else if (catLower.includes('overweight')) bmiCatElem.classList.add('badge-overweight');
                    else if (catLower.includes('obese')) bmiCatElem.classList.add('badge-obese');
                }

                if (goalElem) goalElem.textContent = data.fitnessGoal || 'Maintenance';

                const logs = data.logs || [];
                if (countElem) countElem.textContent = `${logs.length} Entr${logs.length === 1 ? 'y' : 'ies'}`;

                // Render Charts
                renderWeightChart(logs);
                renderBMIChart(logs);

                if (logs.length === 0) {
                    container.innerHTML = `
                        <div class="placeholder-box">
                            <div class="placeholder-icon">📈</div>
                            <h4>No Progress Data Yet</h4>
                            <p>No progress records found. Add your first progress entry to start tracking.</p>
                        </div>`;
                    return;
                }

                let tableHtml = `
                    <div class="history-table-wrapper">
                    <table class="workout-history-table">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>Date</th>
                                <th>Weight (kg)</th>
                                <th>BMI</th>
                                <th>Status</th>
                                <th>Notes</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>`;

                // Display table in reverse chronological order (newest first)
                const reverseLogs = [...logs].reverse();
                reverseLogs.forEach((p, idx) => {
                    tableHtml += `
                        <tr>
                            <td><strong>${reverseLogs.length - idx}</strong></td>
                            <td>${formatDate(p.progressDate)}</td>
                            <td><span class="volume-badge">${p.weight} kg</span></td>
                            <td><strong>${p.bmi}</strong></td>
                            <td>${getBmiBadge(p.bmiCategory)}</td>
                            <td>${escapeHtml(p.notes || '--')}</td>
                            <td>
                                <button onclick="deleteProgressEntry(${p.progressId})" class="btn-danger-sm">Delete</button>
                            </td>
                        </tr>`;
                });

                tableHtml += `</tbody></table></div>`;
                container.innerHTML = tableHtml;
            } else {
                showProgressAlert(`❌ ${data.message || 'Error loading progress data.'}`, 'error');
            }
        })
        .catch(err => console.error('Error loading progress data:', err));
}

/**
 * Handles submission of Add Progress form.
 */
function handleProgressSubmit(event) {
    event.preventDefault();

    const weightStr = document.getElementById('progressWeight').value;
    const progressDate = document.getElementById('progressDate').value;
    const notes = document.getElementById('progressNotes').value.trim();

    const weight = parseFloat(weightStr);
    if (isNaN(weight) || weight < 20 || weight > 300) {
        showProgressAlert('⚠️ Please enter a valid weight between 20 kg and 300 kg.', 'error');
        return false;
    }

    if (!progressDate) {
        showProgressAlert('⚠️ Please select a log date.', 'error');
        return false;
    }

    const params = new URLSearchParams({
        action: 'add',
        weight: weight,
        progressDate: progressDate,
        notes: notes
    });

    fetch('progress', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        if (data.status === 'success') {
            showProgressAlert(`✅ ${data.message}`, 'success');
            clearProgressForm();
            loadProgressData();
        } else {
            showProgressAlert(`❌ ${data.message}`, 'error');
        }
    })
    .catch(err => {
        console.error('Error logging progress entry:', err);
        showProgressAlert('❌ Error logging progress entry. Please check connection.', 'error');
    });

    return false;
}

/**
 * Deletes a progress record via AJAX POST.
 */
function deleteProgressEntry(progressId) {
    if (!confirm('Are you sure you want to delete this progress record?')) {
        return;
    }

    const params = new URLSearchParams({
        action: 'delete',
        progressId: progressId
    });

    fetch('progress', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        if (data.status === 'success') {
            showProgressAlert(`✅ ${data.message}`, 'success');
            loadProgressData();
        } else {
            showProgressAlert(`❌ ${data.message}`, 'error');
        }
    })
    .catch(err => {
        console.error('Error deleting progress record:', err);
        showProgressAlert('❌ Error deleting progress record.', 'error');
    });
}

/**
 * Helper to display alert box on progress page.
 */
function showProgressAlert(message, type) {
    const alertBox = document.getElementById('progress-alert');
    if (!alertBox) return;

    alertBox.textContent = message;
    alertBox.classList.remove('hidden', 'alert-error', 'alert-success');
    alertBox.classList.add(type === 'success' ? 'alert-success' : 'alert-error');
}

/**
 * Returns styled HTML badge for BMI status category.
 */
function getBmiBadge(category) {
    if (!category) return '<span class="bmi-badge">N/A</span>';
    const catLower = category.toLowerCase();
    if (catLower.includes('underweight')) return `<span class="bmi-badge badge-underweight">Underweight</span>`;
    if (catLower.includes('normal')) return `<span class="bmi-badge badge-normal">Normal</span>`;
    if (catLower.includes('overweight')) return `<span class="bmi-badge badge-overweight">Overweight</span>`;
    if (catLower.includes('obese')) return `<span class="bmi-badge badge-obese">Obese</span>`;
    return `<span class="bmi-badge">${escapeHtml(category)}</span>`;
}

/**
 * Renders Weight Progress Line Chart on Canvas.
 */
function renderWeightChart(logs) {
    const dates = logs.map(p => p.progressDate);
    const weights = logs.map(p => parseFloat(p.weight));
    drawSmoothLineChart('weightChartCanvas', dates, weights, 'kg', '#10b981', 'rgba(16, 185, 129, 0.25)');
}

/**
 * Renders BMI Progress Line Chart on Canvas.
 */
function renderBMIChart(logs) {
    const dates = logs.map(p => p.progressDate);
    const bmis = logs.map(p => parseFloat(p.bmi));
    drawSmoothLineChart('bmiChartCanvas', dates, bmis, 'BMI', '#0ea5e9', 'rgba(14, 165, 233, 0.25)');
}

/**
 * Pure HTML5 Canvas Drawing Utility for Smooth Responsive Line Charts.
 */
function drawSmoothLineChart(canvasId, labels, values, unit, strokeColor, fillColor) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;

    const parent = canvas.parentElement;
    const rect = parent ? parent.getBoundingClientRect() : canvas.getBoundingClientRect();
    const width = rect.width > 0 ? rect.width : 600;
    const height = 240;

    canvas.width = width * 2;
    canvas.height = height * 2;
    canvas.style.width = width + 'px';
    canvas.style.height = height + 'px';

    const ctx = canvas.getContext('2d');
    ctx.scale(2, 2);
    ctx.clearRect(0, 0, width, height);

    if (!values || values.length === 0) {
        ctx.fillStyle = '#94a3b8';
        ctx.font = '500 13px Poppins, sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('No progress data yet. Add your first progress entry to start tracking.', width / 2, height / 2);
        return;
    }

    const padding = { top: 35, right: 40, bottom: 40, left: 55 };
    const chartWidth = width - padding.left - padding.right;
    const chartHeight = height - padding.top - padding.bottom;

    let minVal = Math.min(...values);
    let maxVal = Math.max(...values);

    if (minVal === maxVal) {
        minVal = Math.max(0, minVal - 5);
        maxVal = maxVal + 5;
    } else {
        const diff = maxVal - minVal;
        minVal = Math.max(0, minVal - diff * 0.2);
        maxVal = maxVal + diff * 0.2;
    }

    // Grid lines & Y-axis labels
    const gridRows = 4;
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.07)';
    ctx.lineWidth = 1;
    ctx.fillStyle = '#94a3b8';
    ctx.font = '400 11px Poppins, sans-serif';
    ctx.textAlign = 'right';
    ctx.textBaseline = 'middle';

    for (let i = 0; i <= gridRows; i++) {
        const y = padding.top + (chartHeight / gridRows) * i;
        const val = maxVal - ((maxVal - minVal) / gridRows) * i;

        ctx.beginPath();
        ctx.moveTo(padding.left, y);
        ctx.lineTo(width - padding.right, y);
        ctx.stroke();

        ctx.fillText(val.toFixed(1) + (unit ? ' ' + unit : ''), padding.left - 8, y);
    }

    // Calculate X and Y coordinates for points
    const points = values.map((val, idx) => {
        const x = values.length === 1 
            ? padding.left + chartWidth / 2 
            : padding.left + (chartWidth / (values.length - 1)) * idx;
        const y = padding.top + chartHeight - ((val - minVal) / (maxVal - minVal)) * chartHeight;
        return { x, y, val, label: labels[idx] };
    });

    // Draw Filled Area under Line
    if (points.length > 1) {
        const gradient = ctx.createLinearGradient(0, padding.top, 0, height - padding.bottom);
        gradient.addColorStop(0, fillColor || 'rgba(16, 185, 129, 0.25)');
        gradient.addColorStop(1, 'rgba(16, 185, 129, 0.0)');

        ctx.beginPath();
        ctx.moveTo(points[0].x, points[0].y);
        for (let i = 1; i < points.length; i++) {
            ctx.lineTo(points[i].x, points[i].y);
        }
        ctx.lineTo(points[points.length - 1].x, height - padding.bottom);
        ctx.lineTo(points[0].x, height - padding.bottom);
        ctx.closePath();
        ctx.fillStyle = gradient;
        ctx.fill();
    }

    // Draw Connecting Line
    ctx.beginPath();
    ctx.strokeStyle = strokeColor || '#10b981';
    ctx.lineWidth = 3;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    points.forEach((pt, i) => {
        if (i === 0) ctx.moveTo(pt.x, pt.y);
        else ctx.lineTo(pt.x, pt.y);
    });
    ctx.stroke();

    // Draw Dots, X Labels, and Value Labels
    points.forEach((pt) => {
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, 6, 0, Math.PI * 2);
        ctx.fillStyle = strokeColor || '#10b981';
        ctx.fill();

        ctx.beginPath();
        ctx.arc(pt.x, pt.y, 3, 0, Math.PI * 2);
        ctx.fillStyle = '#0f172a';
        ctx.fill();

        ctx.fillStyle = '#f8fafc';
        ctx.font = '600 11px Poppins, sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(pt.val.toFixed(1) + (unit ? ' ' + unit : ''), pt.x, pt.y - 12);

        ctx.fillStyle = '#94a3b8';
        ctx.font = '400 11px Poppins, sans-serif';
        ctx.fillText(formatDate(pt.label), pt.x, height - padding.bottom + 18);
    });
}

/* ============================================================================
   ADMIN PANEL MANAGEMENT MODULE (PART 7 ENHANCED)
   ============================================================================ */

/**
 * Helper to display alert box on Admin pages
 */
function showAdminAlert(message, type = 'error') {
    const alertBox = document.getElementById('alertBox');
    if (!alertBox) return;

    alertBox.textContent = message;
    alertBox.classList.remove('hidden', 'alert-error', 'alert-success');
    alertBox.classList.add(type === 'success' ? 'alert-success' : 'alert-error');
}

/* ----------------------------------------------------------------------------
   1. ADMIN DASHBOARD MODULE
   ---------------------------------------------------------------------------- */
function initAdminDashboard() {
    fetch('admin-dashboard', { cache: 'no-store' })
        .then(res => {
            if (res.status === 401) {
                window.location.href = 'admin-login.html?error=unauthorized';
                throw new Error('Unauthorized');
            }
            if (!res.ok) throw new Error('Server error (' + res.status + ')');
            return res.json();
        })
        .then(data => {
            if (data.status === 'success') {
                const s = data.stats || {};
                const totalUsersElem = document.getElementById('totalUsers');
                if (totalUsersElem) totalUsersElem.textContent = s.totalUsers || 0;
                const totalExercisesElem = document.getElementById('totalExercises');
                if (totalExercisesElem) totalExercisesElem.textContent = s.totalExercises || 0;
                const totalWorkoutsElem = document.getElementById('totalWorkouts');
                if (totalWorkoutsElem) totalWorkoutsElem.textContent = s.totalWorkouts || 0;
                const totalNutritionElem = document.getElementById('totalNutrition');
                if (totalNutritionElem) totalNutritionElem.textContent = s.totalNutrition || 0;
                const totalProgressElem = document.getElementById('totalProgress');
                if (totalProgressElem) totalProgressElem.textContent = s.totalProgress || 0;
                const workoutsTodayElem = document.getElementById('workoutsToday');
                if (workoutsTodayElem) workoutsTodayElem.textContent = s.workoutsToday || 0;
                const nutritionTodayElem = document.getElementById('nutritionToday');
                if (nutritionTodayElem) nutritionTodayElem.textContent = s.nutritionToday || 0;
                const progressThisMonthElem = document.getElementById('progressThisMonth');
                if (progressThisMonthElem) progressThisMonthElem.textContent = s.progressThisMonth || 0;

                // Render Recent Users Stream
                const usersElem = document.getElementById('recentUsersList');
                if (usersElem) {
                    if (data.recentUsers && data.recentUsers.length > 0) {
                        usersElem.innerHTML = data.recentUsers.map(u => `
                            <div style="background: #0f172a; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center;">
                                <div>
                                    <strong style="color: var(--text-white); display: block; font-size: 0.9rem;">${escapeHtml(u.name)}</strong>
                                    <span style="color: var(--text-light); font-size: 0.8rem;">${escapeHtml(u.email)}</span>
                                </div>
                                <span class="history-count">ID #${u.userId}</span>
                            </div>
                        `).join('');
                    } else {
                        usersElem.innerHTML = `<p style="color: var(--text-light); font-size: 0.88rem;">No users registered yet.</p>`;
                    }
                }

                // Render Recent Workouts Stream
                const workoutsElem = document.getElementById('recentWorkoutsList');
                if (workoutsElem) {
                    if (data.recentWorkouts && data.recentWorkouts.length > 0) {
                        workoutsElem.innerHTML = data.recentWorkouts.map(w => `
                            <div style="background: #0f172a; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center;">
                                <div>
                                    <strong style="color: var(--primary); display: block; font-size: 0.9rem;">${escapeHtml(w.exerciseName)}</strong>
                                    <span style="color: var(--text-light); font-size: 0.8rem;">by ${escapeHtml(w.userName)} (${formatDate(w.workoutDate)})</span>
                                </div>
                                <span class="user-badge">Workout</span>
                            </div>
                        `).join('');
                    } else {
                        workoutsElem.innerHTML = `<p style="color: var(--text-light); font-size: 0.88rem;">No workouts logged yet.</p>`;
                    }
                }

                // Render Recent Nutrition Stream
                const nutritionElem = document.getElementById('recentNutritionList');
                if (nutritionElem) {
                    if (data.recentNutrition && data.recentNutrition.length > 0) {
                        nutritionElem.innerHTML = data.recentNutrition.map(n => `
                            <div style="background: #0f172a; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center;">
                                <div>
                                    <strong style="color: #c4b5fd; display: block; font-size: 0.9rem;">${escapeHtml(n.foodName)}</strong>
                                    <span style="color: var(--text-light); font-size: 0.8rem;">by ${escapeHtml(n.userName)} • ${n.calories} kcal</span>
                                </div>
                                <span class="meal-badge badge-other">${escapeHtml(n.mealType || 'Meal')}</span>
                            </div>
                        `).join('');
                    } else {
                        nutritionElem.innerHTML = `<p style="color: var(--text-light); font-size: 0.88rem;">No nutrition entries logged yet.</p>`;
                    }
                }
            }
        })
        .catch(err => {
            console.error('Error loading admin dashboard:', err);
            if (err.message !== 'Unauthorized') {
                showAdminAlert('❌ Failed to load admin dashboard statistics.');
            }
        });
}

/* ----------------------------------------------------------------------------
   2. ADMIN USER MANAGEMENT MODULE
   ---------------------------------------------------------------------------- */
function initAdminUsers() {
    const btnSearch = document.getElementById('btnSearchUsers');
    const btnClear = document.getElementById('btnClearUserSearch');
    const inputSearch = document.getElementById('userSearchInput');

    if (btnSearch && inputSearch) {
        btnSearch.addEventListener('click', () => loadAdminUsers(inputSearch.value));
        inputSearch.addEventListener('keyup', (e) => {
            if (e.key === 'Enter') loadAdminUsers(inputSearch.value);
        });
    }

    if (btnClear && inputSearch) {
        btnClear.addEventListener('click', () => {
            inputSearch.value = '';
            loadAdminUsers('');
        });
    }

    // Modal listeners
    const modalProfile = document.getElementById('userProfileModal');
    const btnCloseProfile1 = document.getElementById('btnCloseProfileModal');
    const btnCloseProfile2 = document.getElementById('btnCloseProfileModalBottom');
    if (btnCloseProfile1) btnCloseProfile1.addEventListener('click', () => modalProfile.classList.add('hidden'));
    if (btnCloseProfile2) btnCloseProfile2.addEventListener('click', () => modalProfile.classList.add('hidden'));

    const modalDelete = document.getElementById('deleteUserModal');
    const btnCancelDelete = document.getElementById('btnCancelDeleteUser');
    const btnConfirmDelete = document.getElementById('btnConfirmDeleteUser');
    if (btnCancelDelete) btnCancelDelete.addEventListener('click', () => modalDelete.classList.add('hidden'));
    if (btnConfirmDelete) {
        btnConfirmDelete.addEventListener('click', () => {
            const targetId = document.getElementById('deleteTargetUserId').value;
            if (targetId) executeDeleteUser(targetId);
        });
    }

    loadAdminUsers('');
}

function loadAdminUsers(searchQuery = '') {
    const url = 'admin-users' + (searchQuery ? '?search=' + encodeURIComponent(searchQuery) : '');
    const tableBody = document.getElementById('usersTableBody');
    const countBadge = document.getElementById('userCountBadge');

    fetch(url, { cache: 'no-store' })
        .then(res => {
            if (res.status === 401) {
                window.location.href = 'admin-login.html?error=unauthorized';
                throw new Error('Unauthorized');
            }
            return res.json();
        })
        .then(data => {
            if (data.status === 'success' && Array.isArray(data.users)) {
                const users = data.users;
                if (countBadge) countBadge.textContent = `${users.length} User${users.length === 1 ? '' : 's'}`;

                if (users.length === 0) {
                    tableBody.innerHTML = `
                        <tr>
                            <td colspan="10" style="text-align: center; color: var(--text-light); padding: 30px;">
                                No registered users found.
                            </td>
                        </tr>`;
                    return;
                }

                tableBody.innerHTML = users.map(u => `
                    <tr>
                        <td><strong>#${u.userId}</strong></td>
                        <td style="font-weight: 600;">${escapeHtml(u.name)}</td>
                        <td>${escapeHtml(u.email)}</td>
                        <td>${u.age || '-'}</td>
                        <td>${escapeHtml(u.gender || '-')}</td>
                        <td>${u.height ? u.height + ' cm' : '-'}</td>
                        <td>${u.weight ? u.weight + ' kg' : '-'}</td>
                        <td><span class="user-badge">${escapeHtml(u.fitnessGoal || 'General')}</span></td>
                        <td>${formatDate(u.createdAt ? u.createdAt.split(' ')[0] : '')}</td>
                        <td style="text-align: right; white-space: nowrap;">
                            <button type="button" class="btn btn-outline btn-sm" onclick="openUserProfileModal(${u.userId})" style="padding: 4px 10px; font-size: 0.8rem; margin-right: 6px;">View Profile</button>
                            <button type="button" class="btn-danger-sm" onclick="openDeleteUserModal(${u.userId}, '${escapeHtml(u.name)}')">Delete</button>
                        </td>
                    </tr>
                `).join('');
            } else {
                showAdminAlert(`❌ ${data.message || 'Failed to load users.'}`);
            }
        })
        .catch(err => console.error('Error loading admin users:', err));
}

function openUserProfileModal(userId) {
    const modal = document.getElementById('userProfileModal');
    const content = document.getElementById('modalProfileContent');
    const modalName = document.getElementById('modalUserName');

    content.innerHTML = `<p style="color: var(--text-light);">Loading user profile metrics...</p>`;
    modal.classList.remove('hidden');

    fetch('admin-users?userId=' + userId, { cache: 'no-store' })
        .then(res => res.json())
        .then(data => {
            if (data.status === 'success') {
                const u = data.user;
                if (modalName) modalName.textContent = `User Profile: ${u.name}`;

                content.innerHTML = `
                    <div style="background: #0f172a; padding: 15px; border-radius: 10px; border: 1px solid var(--border-color); margin-bottom: 15px;">
                        <div style="display: flex; gap: 15px; align-items: center; margin-bottom: 12px;">
                            <div class="profile-avatar" style="width: 50px; height: 50px; font-size: 1.4rem; margin: 0;">${u.name.charAt(0).toUpperCase()}</div>
                            <div>
                                <strong style="color: var(--text-white); font-size: 1.1rem; display: block;">${escapeHtml(u.name)}</strong>
                                <span style="color: var(--text-light); font-size: 0.88rem;">${escapeHtml(u.email)}</span>
                            </div>
                        </div>
                        <div class="profile-details-list" style="border-top: 1px solid var(--border-color); padding-top: 10px;">
                            <div class="detail-row"><span class="detail-label">User ID:</span><span class="detail-value">#${u.userId}</span></div>
                            <div class="detail-row"><span class="detail-label">Age & Gender:</span><span class="detail-value">${u.age} yrs • ${escapeHtml(u.gender)}</span></div>
                            <div class="detail-row"><span class="detail-label">Height & Weight:</span><span class="detail-value">${u.height} cm • ${u.weight} kg</span></div>
                            <div class="detail-row"><span class="detail-label">Fitness Goal:</span><span class="detail-valueHighlight">${escapeHtml(u.fitnessGoal)}</span></div>
                            <div class="detail-row"><span class="detail-label">Registration Date:</span><span class="detail-value">${formatDate(u.createdAt ? u.createdAt.split(' ')[0] : '')}</span></div>
                        </div>
                    </div>

                    <h4 style="color: var(--text-white); margin-bottom: 10px;">Activity Metrics Summary</h4>
                    <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 15px;">
                        <div style="background: #0f172a; padding: 12px; border-radius: 8px; text-align: center; border: 1px solid var(--border-color);">
                            <span style="font-size: 0.75rem; color: var(--text-light); text-transform: uppercase;">Workouts</span>
                            <h3 style="color: var(--primary); margin-top: 4px;">${data.totalWorkouts}</h3>
                        </div>
                        <div style="background: #0f172a; padding: 12px; border-radius: 8px; text-align: center; border: 1px solid var(--border-color);">
                            <span style="font-size: 0.75rem; color: var(--text-light); text-transform: uppercase;">Nutrition</span>
                            <h3 style="color: #c4b5fd; margin-top: 4px;">${data.totalNutrition}</h3>
                        </div>
                        <div style="background: #0f172a; padding: 12px; border-radius: 8px; text-align: center; border: 1px solid var(--border-color);">
                            <span style="font-size: 0.75rem; color: var(--text-light); text-transform: uppercase;">Progress</span>
                            <h3 style="color: var(--secondary); margin-top: 4px;">${data.totalProgress}</h3>
                        </div>
                    </div>

                    <div style="background: #0f172a; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); font-size: 0.88rem;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
                            <span style="color: var(--text-light);">Latest Workout Date:</span>
                            <strong style="color: var(--text-white);">${formatDate(data.lastWorkoutDate)}</strong>
                        </div>
                        <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
                            <span style="color: var(--text-light);">Latest Nutrition Date:</span>
                            <strong style="color: var(--text-white);">${formatDate(data.lastNutritionDate)}</strong>
                        </div>
                        <div style="display: flex; justify-content: space-between;">
                            <span style="color: var(--text-light);">Latest Weight / BMI Log:</span>
                            <strong style="color: var(--primary);">${data.latestWeight > 0 ? data.latestWeight + ' kg (BMI: ' + data.latestBmi + ')' : 'No entries'}</strong>
                        </div>
                    </div>
                `;
            } else {
                content.innerHTML = `<p style="color: var(--danger);">❌ ${data.message || 'Failed to fetch user metrics.'}</p>`;
            }
        })
        .catch(err => {
            console.error('Error fetching user profile:', err);
            content.innerHTML = `<p style="color: var(--danger);">❌ Server error loading profile details.</p>`;
        });
}

function openDeleteUserModal(userId, userName) {
    const modal = document.getElementById('deleteUserModal');
    const msg = document.getElementById('deleteUserMsg');
    const targetInput = document.getElementById('deleteTargetUserId');

    targetInput.value = userId;
    msg.textContent = `Are you sure you want to permanently delete user account "${userName}" (#${userId})? All associated workouts, nutrition, and progress entries will be removed.`;
    modal.classList.remove('hidden');
}

function executeDeleteUser(userId) {
    const params = new URLSearchParams({
        action: 'delete',
        userId: userId
    });

    fetch('admin-users', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        document.getElementById('deleteUserModal').classList.add('hidden');
        if (data.status === 'success') {
            showAdminAlert(`✅ ${data.message}`, 'success');
            loadAdminUsers();
        } else {
            showAdminAlert(`❌ ${data.message}`);
        }
    })
    .catch(err => {
        console.error('Error deleting user:', err);
        showAdminAlert('❌ Error deleting user account.');
    });
}

/* ----------------------------------------------------------------------------
   3. ADMIN EXERCISE MANAGEMENT MODULE
   ---------------------------------------------------------------------------- */
function initAdminExercises() {
    const btnOpenAdd = document.getElementById('btnOpenAddExerciseModal');
    const formModal = document.getElementById('exerciseFormModal');
    const btnCloseForm = document.getElementById('btnCloseExerciseModal');
    const btnCancelForm = document.getElementById('btnCancelExerciseForm');
    const form = document.getElementById('exerciseForm');

    if (btnOpenAdd) {
        btnOpenAdd.addEventListener('click', () => {
            document.getElementById('exerciseModalTitle').textContent = 'Add New Exercise';
            document.getElementById('modalExerciseAction').value = 'add';
            document.getElementById('modalExerciseId').value = '';
            form.reset();
            formModal.classList.remove('hidden');
        });
    }

    if (btnCloseForm) btnCloseForm.addEventListener('click', () => formModal.classList.add('hidden'));
    if (btnCancelForm) btnCancelForm.addEventListener('click', () => formModal.classList.add('hidden'));

    if (form) {
        form.addEventListener('submit', (e) => {
            e.preventDefault();
            saveExercise();
        });
    }

    // Filter listeners
    const btnFilter = document.getElementById('btnFilterExercises');
    const btnReset = document.getElementById('btnResetExerciseFilter');
    const inputSearch = document.getElementById('exerciseSearchInput');
    const selectMuscle = document.getElementById('exerciseMuscleFilter');

    if (btnFilter) {
        btnFilter.addEventListener('click', () => loadAdminExercises(inputSearch.value, selectMuscle.value));
    }
    if (btnReset) {
        btnReset.addEventListener('click', () => {
            inputSearch.value = '';
            selectMuscle.value = 'ALL';
            loadAdminExercises('', 'ALL');
        });
    }

    // Delete modal listeners
    const modalDelete = document.getElementById('deleteExerciseModal');
    const btnCancelDelete = document.getElementById('btnCancelDeleteExercise');
    const btnConfirmDelete = document.getElementById('btnConfirmDeleteExercise');

    if (btnCancelDelete) btnCancelDelete.addEventListener('click', () => modalDelete.classList.add('hidden'));
    if (btnConfirmDelete) {
        btnConfirmDelete.addEventListener('click', () => {
            const targetId = document.getElementById('deleteTargetExerciseId').value;
            if (targetId) executeDeleteExercise(targetId);
        });
    }

    loadAdminExercises('', 'ALL');
}

function loadAdminExercises(searchQuery = '', muscleGroup = 'ALL') {
    let url = 'admin-exercises?';
    if (searchQuery) url += 'search=' + encodeURIComponent(searchQuery) + '&';
    if (muscleGroup) url += 'muscleGroup=' + encodeURIComponent(muscleGroup);

    const tableBody = document.getElementById('exercisesTableBody');

    fetch(url, { cache: 'no-store' })
        .then(res => {
            if (res.status === 401) {
                window.location.href = 'admin-login.html?error=unauthorized';
                throw new Error('Unauthorized');
            }
            return res.json();
        })
        .then(data => {
            if (data.status === 'success' && Array.isArray(data.exercises)) {
                const list = data.exercises;
                if (list.length === 0) {
                    tableBody.innerHTML = `
                        <tr>
                            <td colspan="6" style="text-align: center; color: var(--text-light); padding: 30px;">
                                No exercises found in catalog matching filters.
                            </td>
                        </tr>`;
                    return;
                }

                tableBody.innerHTML = list.map(e => `
                    <tr>
                        <td><strong>#${e.exerciseId}</strong></td>
                        <td style="font-weight: 600;">${escapeHtml(e.exerciseName)}</td>
                        <td><span class="muscle-tag">${escapeHtml(e.muscleGroup)}</span></td>
                        <td style="color: var(--text-light); font-size: 0.88rem;">${escapeHtml(e.description || 'No description provided.')}</td>
                        <td>${formatDate(e.createdAt ? e.createdAt.split(' ')[0] : '')}</td>
                        <td style="text-align: right; white-space: nowrap;">
                            <button type="button" class="btn btn-outline btn-sm" onclick="openEditExerciseModal(${e.exerciseId}, '${escapeHtml(e.exerciseName)}', '${escapeHtml(e.muscleGroup)}', '${escapeHtml(e.description)}')" style="padding: 4px 10px; font-size: 0.8rem; margin-right: 6px;">Edit</button>
                            <button type="button" class="btn-danger-sm" onclick="openDeleteExerciseModal(${e.exerciseId}, '${escapeHtml(e.exerciseName)}')">Delete</button>
                        </td>
                    </tr>
                `).join('');
            } else {
                showAdminAlert(`❌ ${data.message || 'Failed to load exercise catalog.'}`);
            }
        })
        .catch(err => console.error('Error loading exercises catalog:', err));
}

function openEditExerciseModal(id, name, muscle, description) {
    document.getElementById('exerciseModalTitle').textContent = 'Edit Exercise';
    document.getElementById('modalExerciseAction').value = 'update';
    document.getElementById('modalExerciseId').value = id;
    document.getElementById('modalExerciseName').value = name;
    document.getElementById('modalMuscleGroup').value = muscle;
    document.getElementById('modalDescription').value = description;

    document.getElementById('exerciseFormModal').classList.remove('hidden');
}

function saveExercise() {
    const action = document.getElementById('modalExerciseAction').value;
    const id = document.getElementById('modalExerciseId').value;
    const name = document.getElementById('modalExerciseName').value.trim();
    const muscle = document.getElementById('modalMuscleGroup').value;
    const desc = document.getElementById('modalDescription').value.trim();

    if (!name || !muscle) {
        showAdminAlert('⚠️ Exercise name and target muscle group are required.');
        return;
    }

    const params = new URLSearchParams({
        action: action,
        exerciseId: id,
        exerciseName: name,
        muscleGroup: muscle,
        description: desc
    });

    fetch('admin-exercises', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        document.getElementById('exerciseFormModal').classList.add('hidden');
        if (data.status === 'success') {
            showAdminAlert(`✅ ${data.message}`, 'success');
            loadAdminExercises();
        } else {
            showAdminAlert(`❌ ${data.message}`);
        }
    })
    .catch(err => {
        console.error('Error saving exercise:', err);
        showAdminAlert('❌ Error saving exercise.');
    });
}

function openDeleteExerciseModal(exerciseId, exerciseName) {
    const modal = document.getElementById('deleteExerciseModal');
    const msg = document.getElementById('deleteExerciseMsg');
    const targetInput = document.getElementById('deleteTargetExerciseId');

    targetInput.value = exerciseId;
    msg.textContent = `Are you sure you want to delete exercise "${exerciseName}" (#${exerciseId}) from the master catalog?`;
    modal.classList.remove('hidden');
}

function executeDeleteExercise(exerciseId) {
    const params = new URLSearchParams({
        action: 'delete',
        exerciseId: exerciseId
    });

    fetch('admin-exercises', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        document.getElementById('deleteExerciseModal').classList.add('hidden');
        if (data.status === 'success') {
            showAdminAlert(`✅ ${data.message}`, 'success');
            loadAdminExercises();
        } else {
            showAdminAlert(`❌ ${data.message}`);
        }
    })
    .catch(err => {
        console.error('Error deleting exercise:', err);
        showAdminAlert('❌ Error deleting exercise.');
    });
}

/* ----------------------------------------------------------------------------
   4. ADMIN NUTRITION MANAGEMENT MODULE
   ---------------------------------------------------------------------------- */
function initAdminNutrition() {
    const btnFilter = document.getElementById('btnFilterNutrition');
    const btnReset = document.getElementById('btnResetNutritionFilter');
    const userFilter = document.getElementById('nutritionUserFilter');
    const dateFilter = document.getElementById('nutritionDateFilter');
    const searchInput = document.getElementById('nutritionSearchInput');

    if (btnFilter) {
        btnFilter.addEventListener('click', () => loadAdminNutrition());
    }
    if (btnReset) {
        btnReset.addEventListener('click', () => {
            if (userFilter) userFilter.value = 'ALL';
            if (dateFilter) dateFilter.value = '';
            if (searchInput) searchInput.value = '';
            loadAdminNutrition();
        });
    }

    // Modal listeners
    const modalDelete = document.getElementById('deleteNutritionModal');
    const btnCancelDelete = document.getElementById('btnCancelDeleteNutrition');
    const btnConfirmDelete = document.getElementById('btnConfirmDeleteNutrition');

    if (btnCancelDelete) btnCancelDelete.addEventListener('click', () => modalDelete.classList.add('hidden'));
    if (btnConfirmDelete) {
        btnConfirmDelete.addEventListener('click', () => {
            const targetId = document.getElementById('deleteTargetNutritionId').value;
            if (targetId) executeDeleteNutrition(targetId);
        });
    }

    loadAdminNutrition();
}

function loadAdminNutrition() {
    const userVal = document.getElementById('nutritionUserFilter') ? document.getElementById('nutritionUserFilter').value : 'ALL';
    const dateVal = document.getElementById('nutritionDateFilter') ? document.getElementById('nutritionDateFilter').value : '';
    const searchVal = document.getElementById('nutritionSearchInput') ? document.getElementById('nutritionSearchInput').value : '';

    let url = 'admin-nutrition?';
    if (userVal && userVal !== 'ALL') url += 'userId=' + encodeURIComponent(userVal) + '&';
    if (dateVal) url += 'date=' + encodeURIComponent(dateVal) + '&';
    if (searchVal) url += 'search=' + encodeURIComponent(searchVal);

    const tableBody = document.getElementById('nutritionTableBody');
    const countBadge = document.getElementById('nutritionCountBadge');

    fetch(url, { cache: 'no-store' })
        .then(res => {
            if (res.status === 401) {
                window.location.href = 'admin-login.html?error=unauthorized';
                throw new Error('Unauthorized');
            }
            return res.json();
        })
        .then(data => {
            if (data.status === 'success') {
                // Populate Users Dropdown if first time
                const userSelect = document.getElementById('nutritionUserFilter');
                if (userSelect && Array.isArray(data.users) && userSelect.options.length <= 1) {
                    data.users.forEach(u => {
                        const opt = document.createElement('option');
                        opt.value = u.userId;
                        opt.textContent = `${u.name} (${u.email})`;
                        userSelect.appendChild(opt);
                    });
                }

                const logs = data.logs || [];
                if (countBadge) countBadge.textContent = `${logs.length} Log${logs.length === 1 ? '' : 's'}`;

                if (logs.length === 0) {
                    tableBody.innerHTML = `
                        <tr>
                            <td colspan="11" style="text-align: center; color: var(--text-light); padding: 30px;">
                                No nutrition entries found matching filters.
                            </td>
                        </tr>`;
                    return;
                }

                tableBody.innerHTML = logs.map(n => `
                    <tr>
                        <td><strong>#${n.nutritionId}</strong></td>
                        <td>
                            <strong style="color: var(--text-white); display: block;">${escapeHtml(n.userName)}</strong>
                            <span style="color: var(--text-light); font-size: 0.8rem;">#${n.userId}</span>
                        </td>
                        <td style="font-weight: 600; color: #c4b5fd;">${escapeHtml(n.foodName)}</td>
                        <td><span class="meal-badge badge-other">${escapeHtml(n.mealType || 'Meal')}</span></td>
                        <td>${n.mealTime || '-'}</td>
                        <td><span class="macro-badge badge-calories">${n.calories} kcal</span></td>
                        <td><span class="macro-badge badge-protein">${n.protein}g P</span></td>
                        <td><span class="macro-badge badge-carbs">${n.carbs}g C</span></td>
                        <td><span class="macro-badge badge-fats">${n.fats}g F</span></td>
                        <td>${formatDate(n.nutritionDate)}</td>
                        <td style="text-align: right;">
                            <button type="button" class="btn-danger-sm" onclick="openDeleteNutritionModal(${n.nutritionId}, '${escapeHtml(n.foodName)}', '${escapeHtml(n.userName)}')">Delete</button>
                        </td>
                    </tr>
                `).join('');
            } else {
                showAdminAlert(`❌ ${data.message || 'Failed to load nutrition logs.'}`);
            }
        })
        .catch(err => console.error('Error loading admin nutrition logs:', err));
}

function openDeleteNutritionModal(nutritionId, foodName, userName) {
    const modal = document.getElementById('deleteNutritionModal');
    const msg = document.getElementById('deleteNutritionMsg');
    const targetInput = document.getElementById('deleteTargetNutritionId');

    targetInput.value = nutritionId;
    msg.textContent = `Are you sure you want to delete nutrition log "${foodName}" (#${nutritionId}) logged by ${userName}?`;
    modal.classList.remove('hidden');
}

function executeDeleteNutrition(nutritionId) {
    const params = new URLSearchParams({
        action: 'delete',
        nutritionId: nutritionId
    });

    fetch('admin-nutrition', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        document.getElementById('deleteNutritionModal').classList.add('hidden');
        if (data.status === 'success') {
            showAdminAlert(`✅ ${data.message}`, 'success');
            loadAdminNutrition();
        } else {
            showAdminAlert(`❌ ${data.message}`);
        }
    })
    .catch(err => {
        console.error('Error deleting nutrition record:', err);
        showAdminAlert('❌ Error deleting nutrition record.');
    });
}

/* ----------------------------------------------------------------------------
   5. ADMIN WORKOUT MANAGEMENT MODULE
   ---------------------------------------------------------------------------- */
let adminWorkoutsData = [];

function initAdminWorkouts() {
    const btnFilter = document.getElementById('btnFilterWorkouts');
    const btnReset = document.getElementById('btnResetWorkoutFilter');
    const userFilter = document.getElementById('workoutUserFilter');
    const dateFilter = document.getElementById('workoutDateFilter');
    const searchInput = document.getElementById('workoutSearchInput');

    if (btnFilter) {
        btnFilter.addEventListener('click', () => loadAdminWorkouts());
    }
    if (btnReset) {
        btnReset.addEventListener('click', () => {
            if (userFilter) userFilter.value = 'ALL';
            if (dateFilter) dateFilter.value = '';
            if (searchInput) searchInput.value = '';
            loadAdminWorkouts();
        });
    }

    // Modal listeners
    const modalSets = document.getElementById('workoutSetsModal');
    const btnCloseSets1 = document.getElementById('btnCloseSetsModal');
    const btnCloseSets2 = document.getElementById('btnCloseSetsModalBottom');
    if (btnCloseSets1) btnCloseSets1.addEventListener('click', () => modalSets.classList.add('hidden'));
    if (btnCloseSets2) btnCloseSets2.addEventListener('click', () => modalSets.classList.add('hidden'));

    const modalDelete = document.getElementById('deleteWorkoutModal');
    const btnCancelDelete = document.getElementById('btnCancelDeleteWorkout');
    const btnConfirmDelete = document.getElementById('btnConfirmDeleteWorkout');
    if (btnCancelDelete) btnCancelDelete.addEventListener('click', () => modalDelete.classList.add('hidden'));
    if (btnConfirmDelete) {
        btnConfirmDelete.addEventListener('click', () => {
            const targetId = document.getElementById('deleteTargetWorkoutId').value;
            if (targetId) executeDeleteWorkout(targetId);
        });
    }

    loadAdminWorkouts();
}

function loadAdminWorkouts() {
    const userVal = document.getElementById('workoutUserFilter') ? document.getElementById('workoutUserFilter').value : 'ALL';
    const dateVal = document.getElementById('workoutDateFilter') ? document.getElementById('workoutDateFilter').value : '';
    const searchVal = document.getElementById('workoutSearchInput') ? document.getElementById('workoutSearchInput').value : '';

    let url = 'admin-workouts?';
    if (userVal && userVal !== 'ALL') url += 'userId=' + encodeURIComponent(userVal) + '&';
    if (dateVal) url += 'date=' + encodeURIComponent(dateVal) + '&';
    if (searchVal) url += 'search=' + encodeURIComponent(searchVal);

    const tableBody = document.getElementById('workoutsTableBody');
    const countBadge = document.getElementById('workoutCountBadge');

    fetch(url, { cache: 'no-store' })
        .then(res => {
            if (res.status === 401) {
                window.location.href = 'admin-login.html?error=unauthorized';
                throw new Error('Unauthorized');
            }
            return res.json();
        })
        .then(data => {
            if (data.status === 'success') {
                // Populate Users Dropdown if first time
                const userSelect = document.getElementById('workoutUserFilter');
                if (userSelect && Array.isArray(data.users) && userSelect.options.length <= 1) {
                    data.users.forEach(u => {
                        const opt = document.createElement('option');
                        opt.value = u.userId;
                        opt.textContent = `${u.name} (${u.email})`;
                        userSelect.appendChild(opt);
                    });
                }

                adminWorkoutsData = data.workouts || [];
                if (countBadge) countBadge.textContent = `${adminWorkoutsData.length} Session${adminWorkoutsData.length === 1 ? '' : 's'}`;

                if (adminWorkoutsData.length === 0) {
                    tableBody.innerHTML = `
                        <tr>
                            <td colspan="8" style="text-align: center; color: var(--text-light); padding: 30px;">
                                No workout sessions found matching filters.
                            </td>
                        </tr>`;
                    return;
                }

                tableBody.innerHTML = adminWorkoutsData.map(w => `
                    <tr>
                        <td><strong>#${w.workoutId}</strong></td>
                        <td>
                            <strong style="color: var(--text-white); display: block;">${escapeHtml(w.userName)}</strong>
                            <span style="color: var(--text-light); font-size: 0.8rem;">#${w.userId}</span>
                        </td>
                        <td style="font-weight: 600; color: var(--primary);">${escapeHtml(w.exerciseName)}</td>
                        <td><span class="muscle-tag">${escapeHtml(w.muscleGroup)}</span></td>
                        <td>${formatDate(w.workoutDate)}</td>
                        <td><strong>${w.totalSets}</strong></td>
                        <td><span class="volume-badge">${w.totalVolume} kg</span></td>
                        <td style="text-align: right; white-space: nowrap;">
                            <button type="button" class="btn btn-outline btn-sm" onclick="openWorkoutSetsModal(${w.workoutId})" style="padding: 4px 10px; font-size: 0.8rem; margin-right: 6px;">View Sets</button>
                            <button type="button" class="btn-danger-sm" onclick="openDeleteWorkoutModal(${w.workoutId}, '${escapeHtml(w.exerciseName)}', '${escapeHtml(w.userName)}')">Delete</button>
                        </td>
                    </tr>
                `).join('');
            } else {
                showAdminAlert(`❌ ${data.message || 'Failed to load workout logs.'}`);
            }
        })
        .catch(err => console.error('Error loading admin workouts:', err));
}

function openWorkoutSetsModal(workoutId) {
    const workout = adminWorkoutsData.find(w => w.workoutId === workoutId);
    if (!workout) return;

    const modal = document.getElementById('workoutSetsModal');
    const title = document.getElementById('modalWorkoutTitle');
    const content = document.getElementById('modalSetsContent');

    if (title) title.textContent = `${workout.exerciseName} - Set Breakdown`;

    let setsHtml = `
        <div style="margin-bottom: 15px; background: #0f172a; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color);">
            <p style="margin: 0; color: var(--text-light); font-size: 0.9rem;">Logged by <strong style="color: var(--text-white);">${escapeHtml(workout.userName)}</strong> on ${formatDate(workout.workoutDate)}</p>
            <p style="margin: 4px 0 0 0; color: var(--primary); font-weight: 600; font-size: 0.9rem;">Total Calculated Volume: ${workout.totalVolume} kg</p>
        </div>
        <div class="set-details-table-wrapper">
            <table class="set-input-table">
                <thead>
                    <tr>
                        <th>Set #</th>
                        <th>Reps</th>
                        <th>Weight (kg)</th>
                        <th>Set Volume</th>
                    </tr>
                </thead>
                <tbody>`;

    workout.sets.forEach(s => {
        const setVol = s.reps * s.weight;
        setsHtml += `
            <tr>
                <td class="set-number-label">Set ${s.setNumber}</td>
                <td><strong>${s.reps}</strong> reps</td>
                <td>${s.weight} kg</td>
                <td style="color: var(--primary); font-weight: 600;">${setVol} kg</td>
            </tr>`;
    });

    setsHtml += `</tbody></table></div>`;
    content.innerHTML = setsHtml;
    modal.classList.remove('hidden');
}

function openDeleteWorkoutModal(workoutId, exerciseName, userName) {
    const modal = document.getElementById('deleteWorkoutModal');
    const msg = document.getElementById('deleteWorkoutMsg');
    const targetInput = document.getElementById('deleteTargetWorkoutId');

    targetInput.value = workoutId;
    msg.textContent = `Are you sure you want to delete workout "${exerciseName}" (#${workoutId}) logged by ${userName}? All set-by-set details will be deleted.`;
    modal.classList.remove('hidden');
}

function executeDeleteWorkout(workoutId) {
    const params = new URLSearchParams({
        action: 'delete',
        workoutId: workoutId
    });

    fetch('admin-workouts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        document.getElementById('deleteWorkoutModal').classList.add('hidden');
        if (data.status === 'success') {
            showAdminAlert(`✅ ${data.message}`, 'success');
            loadAdminWorkouts();
        } else {
            showAdminAlert(`❌ ${data.message}`);
        }
    })
    .catch(err => {
        console.error('Error deleting workout:', err);
        showAdminAlert('❌ Error deleting workout session.');
    });
}

/* ----------------------------------------------------------------------------
   6. ADMIN PROGRESS MANAGEMENT MODULE
   ---------------------------------------------------------------------------- */
function initAdminProgress() {
    const btnFilter = document.getElementById('btnFilterProgress');
    const btnReset = document.getElementById('btnResetProgressFilter');
    const userFilter = document.getElementById('progressUserFilter');
    const dateFilter = document.getElementById('progressDateFilter');
    const searchInput = document.getElementById('progressSearchInput');

    if (btnFilter) {
        btnFilter.addEventListener('click', () => loadAdminProgress());
    }
    if (btnReset) {
        btnReset.addEventListener('click', () => {
            if (userFilter) userFilter.value = 'ALL';
            if (dateFilter) dateFilter.value = '';
            if (searchInput) searchInput.value = '';
            loadAdminProgress();
        });
    }

    // Modal listeners
    const modalDelete = document.getElementById('deleteProgressModal');
    const btnCancelDelete = document.getElementById('btnCancelDeleteProgress');
    const btnConfirmDelete = document.getElementById('btnConfirmDeleteProgress');

    if (btnCancelDelete) btnCancelDelete.addEventListener('click', () => modalDelete.classList.add('hidden'));
    if (btnConfirmDelete) {
        btnConfirmDelete.addEventListener('click', () => {
            const targetId = document.getElementById('deleteTargetProgressId').value;
            if (targetId) executeDeleteProgress(targetId);
        });
    }

    loadAdminProgress();
}

function loadAdminProgress() {
    const userVal = document.getElementById('progressUserFilter') ? document.getElementById('progressUserFilter').value : 'ALL';
    const dateVal = document.getElementById('progressDateFilter') ? document.getElementById('progressDateFilter').value : '';
    const searchVal = document.getElementById('progressSearchInput') ? document.getElementById('progressSearchInput').value : '';

    let url = 'admin-progress?';
    if (userVal && userVal !== 'ALL') url += 'userId=' + encodeURIComponent(userVal) + '&';
    if (dateVal) url += 'date=' + encodeURIComponent(dateVal) + '&';
    if (searchVal) url += 'search=' + encodeURIComponent(searchVal);

    const tableBody = document.getElementById('progressTableBody');
    const countBadge = document.getElementById('progressCountBadge');

    fetch(url, { cache: 'no-store' })
        .then(res => {
            if (res.status === 401) {
                window.location.href = 'admin-login.html?error=unauthorized';
                throw new Error('Unauthorized');
            }
            return res.json();
        })
        .then(data => {
            if (data.status === 'success') {
                // Populate Users Dropdown if first time
                const userSelect = document.getElementById('progressUserFilter');
                if (userSelect && Array.isArray(data.users) && userSelect.options.length <= 1) {
                    data.users.forEach(u => {
                        const opt = document.createElement('option');
                        opt.value = u.userId;
                        opt.textContent = `${u.name} (${u.email})`;
                        userSelect.appendChild(opt);
                    });
                }

                const list = data.progress || [];
                if (countBadge) countBadge.textContent = `${list.length} Entr${list.length === 1 ? 'y' : 'ies'}`;

                if (list.length === 0) {
                    tableBody.innerHTML = `
                        <tr>
                            <td colspan="8" style="text-align: center; color: var(--text-light); padding: 30px;">
                                No progress records found matching filters.
                            </td>
                        </tr>`;
                    return;
                }

                tableBody.innerHTML = list.map(p => `
                    <tr>
                        <td><strong>#${p.progressId}</strong></td>
                        <td>
                            <strong style="color: var(--text-white); display: block;">${escapeHtml(p.userName)}</strong>
                            <span style="color: var(--text-light); font-size: 0.8rem;">#${p.userId}</span>
                        </td>
                        <td style="font-weight: 600; color: var(--text-white);">${p.weight} kg</td>
                        <td style="font-weight: 600; color: var(--secondary);">${p.bmi}</td>
                        <td>${getBmiBadge(p.bmiCategory)}</td>
                        <td>${formatDate(p.progressDate)}</td>
                        <td style="color: var(--text-light); font-size: 0.88rem;">${escapeHtml(p.notes || '-')}</td>
                        <td style="text-align: right;">
                            <button type="button" class="btn-danger-sm" onclick="openDeleteProgressModal(${p.progressId}, '${p.weight}', '${escapeHtml(p.userName)}')">Delete</button>
                        </td>
                    </tr>
                `).join('');
            } else {
                showAdminAlert(`❌ ${data.message || 'Failed to load progress records.'}`);
            }
        })
        .catch(err => console.error('Error loading admin progress records:', err));
}

function openDeleteProgressModal(progressId, weight, userName) {
    const modal = document.getElementById('deleteProgressModal');
    const msg = document.getElementById('deleteProgressMsg');
    const targetInput = document.getElementById('deleteTargetProgressId');

    targetInput.value = progressId;
    msg.textContent = `Are you sure you want to delete progress record #${progressId} (${weight} kg) logged by ${userName}?`;
    modal.classList.remove('hidden');
}

function executeDeleteProgress(progressId) {
    const params = new URLSearchParams({
        action: 'delete',
        progressId: progressId
    });

    fetch('admin-progress', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        document.getElementById('deleteProgressModal').classList.add('hidden');
        if (data.status === 'success') {
            showAdminAlert(`✅ ${data.message}`, 'success');
            loadAdminProgress();
        } else {
            showAdminAlert(`❌ ${data.message}`);
        }
    })
    .catch(err => {
        console.error('Error deleting progress record:', err);
        showAdminAlert('❌ Error deleting progress record.');
    });
}

/* ============================================================================
   USER MY GOALS MODULE (PART 8 ENHANCED)
   ============================================================================ */

let currentActiveGoalObj = null;

function initGoalsPage() {
    const targetDateInput = document.getElementById('targetDate');
    if (targetDateInput && !targetDateInput.value) {
        const futureDate = new Date();
        futureDate.setDate(futureDate.getDate() + 30);
        targetDateInput.value = getLocalDateString(futureDate);
    }

    const currentDateElem = document.getElementById('current-date');
    if (currentDateElem) {
        const options = { weekday: 'long', year: 'numeric', month: 'short', day: 'numeric' };
        currentDateElem.textContent = new Date().toLocaleDateString('en-US', options);
    }

    // Modal and button event listeners
    const btnEdit = document.getElementById('btnEditGoal');
    const btnDelete = document.getElementById('btnDeleteGoal');
    const btnCancelEdit = document.getElementById('btnCancelEditGoal');

    if (btnEdit) {
        btnEdit.addEventListener('click', () => {
            if (currentActiveGoalObj) populateGoalFormForEdit(currentActiveGoalObj);
        });
    }

    if (btnDelete) {
        btnDelete.addEventListener('click', () => {
            if (currentActiveGoalObj) openDeleteGoalModal(currentActiveGoalObj.goalId, currentActiveGoalObj.goalType, currentActiveGoalObj.targetWeight);
        });
    }

    if (btnCancelEdit) {
        btnCancelEdit.addEventListener('click', () => resetGoalForm());
    }

    const modalDelete = document.getElementById('delete-goal-modal');
    const btnCancelDeleteModal = document.getElementById('btnCancelDeleteGoalModal');
    const btnConfirmDeleteModal = document.getElementById('btnConfirmDeleteGoalModal');

    if (btnCancelDeleteModal) btnCancelDeleteModal.addEventListener('click', () => modalDelete.classList.add('hidden'));
    if (btnConfirmDeleteModal) {
        btnConfirmDeleteModal.addEventListener('click', () => {
            const targetId = document.getElementById('deleteTargetGoalId').value;
            if (targetId) executeDeleteGoal(targetId);
        });
    }

    loadGoalsData();
}

function loadGoalsData() {
    fetch('goals', { cache: 'no-store' })
        .then(res => {
            if (res.status === 401) {
                window.location.href = 'login.html?error=unauthorized';
                throw new Error('Unauthorized');
            }
            return res.json();
        })
        .then(data => {
            if (data.status === 'success') {
                const active = data.activeGoal;
                currentActiveGoalObj = active;

                const activeCard = document.getElementById('active-goal-card');
                const dispCurrentWeight = document.getElementById('disp-current-weight');
                const dispTargetWeight = document.getElementById('disp-target-weight');
                const dispTargetDate = document.getElementById('disp-target-date');
                const dispProgressPercent = document.getElementById('disp-progress-percent');
                const dispProgressBar = document.getElementById('disp-progress-bar');
                const dispStartSub = document.getElementById('disp-start-weight-sub');
                const dispStatusMsg = document.getElementById('disp-status-msg');
                const dispDailyCalories = document.getElementById('disp-daily-calories');
                const dispDailyProtein = document.getElementById('disp-daily-protein');
                const dispNotesBox = document.getElementById('disp-notes-box');
                const dispNotesText = document.getElementById('disp-notes-text');
                const goalTypeBadge = document.getElementById('goal-type-badge');
                const goalTitleDisplay = document.getElementById('goal-title-display');

                if (dispCurrentWeight) dispCurrentWeight.textContent = `${data.currentWeight.toFixed(1)} kg`;

                if (active) {
                    if (goalTypeBadge) goalTypeBadge.textContent = active.goalType;
                    if (goalTitleDisplay) goalTitleDisplay.textContent = `Target: ${active.targetWeight.toFixed(1)} kg`;
                    if (dispTargetWeight) dispTargetWeight.textContent = `${active.targetWeight.toFixed(1)} kg`;
                    if (dispTargetDate) dispTargetDate.textContent = formatDate(active.targetDate);
                    if (dispDailyCalories) dispDailyCalories.textContent = active.dailyCalories > 0 ? `${active.dailyCalories} kcal` : 'Not Set';
                    if (dispDailyProtein) dispDailyProtein.textContent = active.dailyProtein > 0 ? `${active.dailyProtein} g` : 'Not Set';

                    const pct = data.progressPercentage || 0;
                    if (dispProgressPercent) dispProgressPercent.textContent = `${pct.toFixed(1)}%`;
                    if (dispProgressBar) dispProgressBar.style.width = `${pct.toFixed(1)}%`;
                    if (dispStartSub) dispStartSub.textContent = `Start Weight: ${data.startWeight.toFixed(1)} kg`;
                    if (dispStatusMsg) dispStatusMsg.textContent = data.statusMessage || '';

                    if (dispNotesText && dispNotesBox) {
                        if (active.notes && active.notes.trim()) {
                            dispNotesText.textContent = active.notes;
                            dispNotesBox.classList.remove('hidden');
                        } else {
                            dispNotesBox.classList.add('hidden');
                        }
                    }

                    if (activeCard) activeCard.classList.remove('hidden');
                } else {
                    if (goalTypeBadge) goalTypeBadge.textContent = 'No Active Goal';
                    if (goalTitleDisplay) goalTitleDisplay.textContent = 'Set Your Goal Below';
                    if (dispTargetWeight) dispTargetWeight.textContent = '-- kg';
                    if (dispTargetDate) dispTargetDate.textContent = '--';
                    if (dispDailyCalories) dispDailyCalories.textContent = '--';
                    if (dispDailyProtein) dispDailyProtein.textContent = '--';
                    if (dispProgressPercent) dispProgressPercent.textContent = '0%';
                    if (dispProgressBar) dispProgressBar.style.width = '0%';
                    if (dispStartSub) dispStartSub.textContent = 'No active target defined.';
                    if (dispStatusMsg) dispStatusMsg.textContent = 'Please fill out the form below to create a goal.';
                    if (dispNotesBox) dispNotesBox.classList.add('hidden');
                }

                // Render Goal History in Sidebar
                const historyContainer = document.getElementById('goal-history-list');
                if (historyContainer) {
                    const history = data.history || [];
                    if (history.length === 0) {
                        historyContainer.innerHTML = `<p style="color: var(--text-light); font-size: 0.88rem;">No past goals recorded.</p>`;
                    } else {
                        historyContainer.innerHTML = history.map(g => {
                            const isCurrent = (active && active.goalId === g.goalId);
                            const badgeClass = isCurrent ? 'badge-active' : (g.status === 'COMPLETED' ? 'bmi-badge badge-normal' : 'bmi-badge');
                            return `
                                <div style="background: #0f172a; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); font-size: 0.88rem;">
                                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                                        <strong style="color: var(--text-white);">${escapeHtml(g.goalType)}</strong>
                                        <span class="${badgeClass}">${isCurrent ? 'ACTIVE' : escapeHtml(g.status || 'PAST')}</span>
                                    </div>
                                    <div style="color: var(--text-light); font-size: 0.82rem;">
                                        Target: <strong style="color: var(--primary);">${g.targetWeight} kg</strong> by ${formatDate(g.targetDate)}
                                    </div>
                                </div>
                            `;
                        }).join('');
                    }
                }
            } else {
                showGoalAlert(`❌ ${data.message || 'Failed to load goals data.'}`);
            }
        })
        .catch(err => console.error('Error loading goals data:', err));
}

function handleGoalFormSubmit(event) {
    event.preventDefault();

    const action = document.getElementById('goalAction').value;
    const goalId = document.getElementById('goalId').value;
    const goalType = document.getElementById('goalType').value;
    const targetWeight = parseFloat(document.getElementById('targetWeight').value);
    const targetDate = document.getElementById('targetDate').value;
    const dailyCalories = document.getElementById('dailyCalories').value;
    const dailyProtein = document.getElementById('dailyProtein').value;
    const notes = document.getElementById('notes').value.trim();

    if (isNaN(targetWeight) || targetWeight < 20 || targetWeight > 300) {
        showGoalAlert('⚠️ Please enter a valid target weight between 20 kg and 300 kg.');
        return false;
    }

    if (!targetDate) {
        showGoalAlert('⚠️ Please select a target date.');
        return false;
    }

    const params = new URLSearchParams({
        action: action,
        goalId: goalId,
        goalType: goalType,
        targetWeight: targetWeight,
        targetDate: targetDate,
        dailyCalories: dailyCalories,
        dailyProtein: dailyProtein,
        notes: notes
    });

    fetch('goals', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        if (data.status === 'success') {
            showGoalAlert(`✅ ${data.message}`, 'success');
            resetGoalForm();
            loadGoalsData();
        } else {
            showGoalAlert(`❌ ${data.message}`);
        }
    })
    .catch(err => {
        console.error('Error saving goal:', err);
        showGoalAlert('❌ Error saving goal. Please check connection.');
    });

    return false;
}

function populateGoalFormForEdit(goal) {
    document.getElementById('goal-form-title').textContent = '✏️ Edit Active Fitness Goal';
    document.getElementById('goalAction').value = 'update';
    document.getElementById('goalId').value = goal.goalId;
    document.getElementById('goalType').value = goal.goalType;
    document.getElementById('targetWeight').value = goal.targetWeight;
    document.getElementById('targetDate').value = goal.targetDate;
    document.getElementById('dailyCalories').value = goal.dailyCalories > 0 ? goal.dailyCalories : '';
    document.getElementById('dailyProtein').value = goal.dailyProtein > 0 ? goal.dailyProtein : '';
    document.getElementById('notes').value = goal.notes || '';
    document.getElementById('btnSaveGoal').textContent = 'Update Goal';
    document.getElementById('btnCancelEditGoal').classList.remove('hidden');

    document.getElementById('goal-form').scrollIntoView({ behavior: 'smooth' });
}

function resetGoalForm() {
    document.getElementById('goal-form-title').textContent = '➕ Set New Active Fitness Goal';
    document.getElementById('goalAction').value = 'add';
    document.getElementById('goalId').value = '';
    document.getElementById('goal-form').reset();
    document.getElementById('btnSaveGoal').textContent = 'Set Active Goal';
    document.getElementById('btnCancelEditGoal').classList.add('hidden');

    const futureDate = new Date();
    futureDate.setDate(futureDate.getDate() + 30);
    document.getElementById('targetDate').value = getLocalDateString(futureDate);
}

function openDeleteGoalModal(goalId, goalType, targetWeight) {
    const modal = document.getElementById('delete-goal-modal');
    const msg = document.getElementById('delete-goal-msg');
    const targetInput = document.getElementById('deleteTargetGoalId');

    targetInput.value = goalId;
    msg.textContent = `Are you sure you want to delete your active goal "${goalType} (${targetWeight} kg)"?`;
    modal.classList.remove('hidden');
}

function executeDeleteGoal(goalId) {
    const params = new URLSearchParams({
        action: 'delete',
        goalId: goalId
    });

    fetch('goals', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        document.getElementById('delete-goal-modal').classList.add('hidden');
        if (data.status === 'success') {
            showGoalAlert(`✅ ${data.message}`, 'success');
            resetGoalForm();
            loadGoalsData();
        } else {
            showGoalAlert(`❌ ${data.message}`);
        }
    })
    .catch(err => {
        console.error('Error deleting goal:', err);
        showGoalAlert('❌ Error deleting goal.');
    });
}

function showGoalAlert(message, type = 'error') {
    const alertBox = document.getElementById('goal-alert');
    if (!alertBox) return;

    alertBox.textContent = message;
    alertBox.classList.remove('hidden', 'alert-error', 'alert-success');
    alertBox.classList.add(type === 'success' ? 'alert-success' : 'alert-error');
}

function handleGoalTypeChange() {
    // Optional helper if needed when goal type selection changes
}

/* ============================================================================
   MOBILE NAVIGATION & 3D VISUAL EFFECTS
   ============================================================================ */

/**
 * Initializes mobile hamburger menu toggle & backdrop events
 */
function initMobileNavigation() {
    const mobileMenuBtn = document.getElementById('mobileMenuBtn') || document.querySelector('.mobile-menu-btn') || document.querySelector('.hamburger-btn');
    const mobileNavDrawer = document.getElementById('mobileNavDrawer') || document.querySelector('.mobile-nav-drawer');
    const mobileNavBackdrop = document.getElementById('mobileNavBackdrop') || document.querySelector('.mobile-nav-backdrop');

    if (!mobileMenuBtn || !mobileNavDrawer) return;

    function toggleMenu() {
        const isOpen = mobileNavDrawer.classList.contains('active') || mobileNavDrawer.classList.contains('is-open');
        if (isOpen) {
            closeMenu();
        } else {
            openMenu();
        }
    }

    function openMenu() {
        mobileMenuBtn.classList.add('active', 'is-active');
        mobileNavDrawer.classList.add('active', 'is-open');
        if (mobileNavBackdrop) mobileNavBackdrop.classList.add('active', 'is-open');
        document.body.style.overflow = 'hidden';
    }

    function closeMenu() {
        mobileMenuBtn.classList.remove('active', 'is-active');
        mobileNavDrawer.classList.remove('active', 'is-open');
        if (mobileNavBackdrop) mobileNavBackdrop.classList.remove('active', 'is-open');
        document.body.style.overflow = '';
    }

    mobileMenuBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        toggleMenu();
    });

    if (mobileNavBackdrop) {
        mobileNavBackdrop.addEventListener('click', closeMenu);
    }

    const drawerLinks = mobileNavDrawer.querySelectorAll('a');
    drawerLinks.forEach(link => {
        link.addEventListener('click', () => {
            closeMenu();
        });
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeMenu();
        }
    });
}

/**
 * Initializes interactive 3D Tilt effect on glass cards
 */
function init3DTiltEffects() {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

    const cards = document.querySelectorAll('.stat-card, .dashboard-card, .auth-card, .hero-3d-card, .feature-card-3d');
    
    cards.forEach(card => {
        card.addEventListener('mousemove', (e) => {
            const rect = card.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;
            const centerX = rect.width / 2;
            const centerY = rect.height / 2;
            const rotateX = ((y - centerY) / centerY) * -6;
            const rotateY = ((x - centerX) / centerX) * 6;

            card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateZ(8px)`;
        });

        card.addEventListener('mouseleave', () => {
            card.style.transform = '';
        });
    });
}

/**
 * Initializes Three.js canvas for 3D Hero section on index.html
 */
function initThreeJsHero() {
    const canvasContainer = document.getElementById('hero-three-canvas');
    if (!canvasContainer || typeof THREE === 'undefined') return;

    try {
        const scene = new THREE.Scene();
        const camera = new THREE.PerspectiveCamera(75, canvasContainer.clientWidth / canvasContainer.clientHeight, 0.1, 1000);
        const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });

        renderer.setSize(canvasContainer.clientWidth, canvasContainer.clientHeight);
        renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        canvasContainer.appendChild(renderer.domElement);

        const geometry = new THREE.BufferGeometry();
        const count = 350;
        const positions = new Float32Array(count * 3);
        const colors = new Float32Array(count * 3);

        for (let i = 0; i < count * 3; i += 3) {
            positions[i] = (Math.random() - 0.5) * 15;
            positions[i + 1] = (Math.random() - 0.5) * 15;
            positions[i + 2] = (Math.random() - 0.5) * 15;

            const isGreen = Math.random() > 0.4;
            colors[i] = isGreen ? 0.06 : 0.23;
            colors[i + 1] = isGreen ? 0.72 : 0.51;
            colors[i + 2] = isGreen ? 0.50 : 0.96;
        }

        geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
        geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3));

        const material = new THREE.PointsMaterial({
            size: 0.06,
            vertexColors: true,
            transparent: true,
            opacity: 0.8
        });

        const particles = new THREE.Points(geometry, material);
        scene.add(particles);

        camera.position.z = 5;

        function animate() {
            requestAnimationFrame(animate);
            particles.rotation.x += 0.0008;
            particles.rotation.y += 0.0012;
            renderer.render(scene, camera);
        }
        animate();

        window.addEventListener('resize', () => {
            if (!canvasContainer) return;
            camera.aspect = canvasContainer.clientWidth / canvasContainer.clientHeight;
            camera.updateProjectionMatrix();
            renderer.setSize(canvasContainer.clientWidth, canvasContainer.clientHeight);
        });
    } catch (e) {
        console.warn('Three.js canvas init fallback:', e);
    }
}



