import { reportContent } from './core.js';

const dashboardContent = document.getElementById('dashboard-content');
const recentRunsTbody = document.getElementById('recent-runs-tbody');
const successRatesChart = document.getElementById('success-rates-chart');
const trendsChart = document.getElementById('trends-chart');
const failurePatternsList = document.getElementById('failure-patterns-list');
const slowestTestsList = document.getElementById('slowest-tests-list');

export async function showDashboard() {
    reportContent.style.display = 'none';
    dashboardContent.style.display = 'block';
    
    // Clear previous content
    recentRunsTbody.innerHTML = '<tr><td colspan="7" style="text-align: center;">Loading...</td></tr>';
    successRatesChart.innerHTML = 'Loading...';
    trendsChart.innerHTML = 'Loading...';
    failurePatternsList.innerHTML = 'Loading...';
    slowestTestsList.innerHTML = 'Loading...';

    try {
        const statsResponse = await fetch('/api/history/stats');
        const stats = await statsResponse.json();
        
        const recentResponse = await fetch('/api/history/recent');
        const recentRuns = await recentResponse.json();

        renderSuccessRates(stats.successRates);
        renderTrends(stats.trends);
        renderFailurePatterns(stats.failurePatterns);
        renderSlowestTests(stats.slowestTests);
        renderRecentRuns(recentRuns);

    } catch (error) {
        console.error('Error loading dashboard:', error);
        dashboardContent.innerHTML += `<p class="error">Error loading dashboard stats: ${error.message}</p>`;
    }
}

function renderSuccessRates(data) {
    if (!data || data.length === 0) {
        successRatesChart.innerHTML = '<p>No data available</p>';
        return;
    }
    let html = '<ul class="stats-list">';
    data.forEach(item => {
        const rate = parseFloat(item.successRate || 0).toFixed(1);
        html += `<li>
            <div>
                <strong>${item.name}</strong> (${item.count} runs)
                <div class="success-rate-bar">
                    <div class="success-rate-fill" style="width: ${rate}%"></div>
                </div>
            </div>
            <span>${rate}%</span>
        </li>`;
    });
    html += '</ul>';
    successRatesChart.innerHTML = html;
}

function renderTrends(data) {
    if (!data || data.length === 0) {
        trendsChart.innerHTML = '<p>No data available</p>';
        return;
    }
    
    const maxDuration = Math.max(...data.map(d => d.avgDuration), 1);
    let html = '<div style="margin-top: 10px;">';
    data.slice(-10).forEach(item => {
        const width = (item.avgDuration / maxDuration) * 100;
        html += `<div class="trend-item">
            <span class="trend-label">${item.date}</span>
            <div class="trend-bar" style="width: ${width}%" title="${item.avgDuration.toFixed(0)} ms"></div>
            <span style="font-size: 0.8em;">${item.avgDuration.toFixed(0)}ms</span>
        </div>`;
    });
    html += '</div>';
    trendsChart.innerHTML = html;
}

function renderFailurePatterns(data) {
    if (!data || data.length === 0) {
        failurePatternsList.innerHTML = '<p>No failures recorded</p>';
        return;
    }
    let html = '<ul class="stats-list">';
    data.forEach(item => {
        html += `<li>
            <div style="max-width: 80%; word-break: break-all;">${item.error}</div>
            <strong>${item.count}</strong>
        </li>`;
    });
    html += '</ul>';
    failurePatternsList.innerHTML = html;
}

function renderSlowestTests(data) {
    if (!data || data.length === 0) {
        slowestTestsList.innerHTML = '<p>No data available</p>';
        return;
    }
    let html = '<ul class="stats-list">';
    data.forEach(item => {
        html += `<li>
            <div>${item.name}</div>
            <strong>${item.avgResponseTime.toFixed(0)} ms</strong>
        </li>`;
    });
    html += '</ul>';
    slowestTestsList.innerHTML = html;
}

function renderRecentRuns(data) {
    if (!data || data.length === 0) {
        recentRunsTbody.innerHTML = '<tr><td colspan="7" style="text-align: center;">No runs recorded</td></tr>';
        return;
    }
    
    recentRunsTbody.innerHTML = data.map(run => `
        <tr>
            <td>${run.executionTime.replace('T', ' ').substring(0, 19)}</td>
            <td>${run.testFileName}</td>
            <td>${run.environment || '-'}</td>
            <td>${run.totalTests}</td>
            <td style="color: var(--success-color); font-weight: bold;">${run.passedTests}</td>
            <td style="color: ${run.failedTests > 0 ? 'var(--error-color)' : 'inherit'}; font-weight: bold;">${run.failedTests}</td>
            <td>${run.durationMs} ms</td>
        </tr>
    `).join('');
}
