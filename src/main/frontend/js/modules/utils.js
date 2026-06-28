import { marked } from 'marked';
import * as jsYaml from 'js-yaml';
import hljs from 'highlight.js';

export function escapeHtml(text) {
    if (typeof text !== 'string') return text;
    return text.replace(/&/g, '&amp;')
               .replace(/</g, '&lt;')
               .replace(/>/g, '&gt;')
               .replace(/"/g, '&quot;')
               .replace(/'/g, '&#39;');
}

export function showAssertionToast(message) {
    let toast = document.querySelector('.assertion-toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.className = 'assertion-toast';
        document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.classList.add('show');
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}
