import { state, setEditor, sourceEditor } from './modules/core.js';
import { initEnvironments } from './modules/env-manager.js';
import { fetchTests } from './modules/test-runner.js';
import { setupVisualAssertionBuilder } from './modules/assertion-builder.js';
import { setupImporters } from './modules/importers.js';
import { setupExporters } from './modules/exporters.js';
import { setupGlobalVars } from './modules/global-vars.js';
import { setupInit } from './modules/init.js';
import { initEditor } from './modules/editor.js';
import hljs from 'highlight.js';
import http from 'highlight.js/lib/languages/http';
import javascript from 'highlight.js/lib/languages/javascript';
import json from 'highlight.js/lib/languages/json';
import xml from 'highlight.js/lib/languages/xml';
import sql from 'highlight.js/lib/languages/sql';

// Register highlight.js languages
hljs.registerLanguage('http', http);
hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('json', json);
hljs.registerLanguage('xml', xml);
hljs.registerLanguage('sql', sql);

document.addEventListener('DOMContentLoaded', () => {
    console.log('Rest Unit Test Runner Initializing (Modular)...');
    
    // Initialize CodeMirror if sourceEditor exists
    if (sourceEditor) {
        initEditor("");
    }
    
    // Initialize modules
    initEnvironments();
    fetchTests();
    setupVisualAssertionBuilder();
    setupImporters();
    setupExporters();
    setupGlobalVars();
    setupInit();

    // Re-expose state or global functions if necessary for legacy inline scripts 
    window.RestUnitTest = {
        state: state,
        fetchTests: fetchTests
    };
});
