import { highlightDebugLine } from './editor.js';

let socket = null;
let currentBreakpoints = {};

export function initDebugger() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    socket = new WebSocket(`${protocol}//${window.location.host}/ws/debugger`);

    socket.onmessage = (event) => {
        const message = JSON.parse(event.data);
        handleDebuggerMessage(message);
    };

    socket.onopen = () => {
        console.log('Debugger WebSocket connected');
    };

    socket.onclose = () => {
        console.log('Debugger WebSocket disconnected');
        // Try to reconnect?
    };
}

function handleDebuggerMessage(message) {
    switch (message.type) {
        case 'paused':
            console.log('Execution paused:', message);
            showDebuggerControls(true, message.variables);
            highlightDebugLine(message.line - 1); // GraalVM uses 1-based lines
            break;
        case 'resumed':
            console.log('Execution resumed');
            showDebuggerControls(false);
            highlightDebugLine(null);
            break;
        case 'finished':
            console.log('Execution finished');
            showDebuggerControls(false);
            highlightDebugLine(null);
            break;
    }
}

export function setBreakpoints(testName, breakpoints) {
    currentBreakpoints = breakpoints;
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({
            type: 'setBreakpoints',
            testName: testName,
            breakpoints: breakpoints
        }));
    }
}

export function resume() {
    sendCommand('resume');
}

export function stepOver() {
    sendCommand('stepOver');
}

export function stepInto() {
    sendCommand('stepInto');
}

export function stepOut() {
    sendCommand('stepOut');
}

export function stop() {
    sendCommand('stop');
}

function sendCommand(type) {
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ type: type }));
    }
}

function showDebuggerControls(show, variables) {
    const controls = document.getElementById('debugger-controls');
    const variablesPanel = document.getElementById('debugger-variables');
    const variablesList = document.getElementById('variables-list');

    if (controls) {
        controls.style.display = show ? 'flex' : 'none';
    }

    if (variablesPanel) {
        variablesPanel.style.display = (show && variables) ? 'block' : 'none';
    }

    if (show && variables && variablesList) {
        variablesList.innerHTML = '';
        Object.entries(variables).forEach(([name, value]) => {
            const item = document.createElement('div');
            item.className = 'variable-item';
            
            const nameEl = document.createElement('span');
            nameEl.className = 'variable-name';
            nameEl.textContent = name + ':';
            
            const valueEl = document.createElement('span');
            valueEl.className = 'variable-value';
            valueEl.textContent = String(value);
            
            item.appendChild(nameEl);
            item.appendChild(valueEl);
            variablesList.appendChild(item);
        });
    }
}
