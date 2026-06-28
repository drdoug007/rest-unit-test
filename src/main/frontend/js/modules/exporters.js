import { exportBtn, exportOptions, exportPdfBtn, exportWordBtn, state } from './core.js';

export function setupExporters() {
    if (exportBtn) exportBtn.onclick = (e) => {
        e.stopPropagation();
        exportOptions.style.display = exportOptions.style.display === 'block' ? 'none' : 'block';
    };

    if (exportPdfBtn) exportPdfBtn.onclick = async () => {
        exportOptions.style.display = 'none';
        const element = document.getElementById('report-content');
        
        // Force light mode for PDF export
        const isDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
        if (isDarkMode) {
            document.documentElement.classList.add('light-mode');
            const hljsStyle = document.getElementById('hljs-style');
            if (hljsStyle) hljsStyle.href = 'lib/highlight/styles/github.min.css';
        }
        
        const opt = {
            margin: 10,
            filename: (state.currentTestName || 'test-report') + '.pdf',
            image: { type: 'jpeg', quality: 0.98 },
            html2canvas: { scale: 2, useCORS: true, logging: false },
            jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
            pagebreak: { mode: ['avoid-all', 'css', 'legacy'] }
        };

        try {
            await html2pdf().set(opt).from(element).save();
        } finally {
            if (isDarkMode) {
                document.documentElement.classList.remove('light-mode');
                const hljsStyle = document.getElementById('hljs-style');
                if (hljsStyle) hljsStyle.href = 'lib/highlight/styles/github-dark.min.css';
            }
        }
    };

    if (exportWordBtn) exportWordBtn.onclick = () => {
        exportOptions.style.display = 'none';
        const element = document.getElementById('report-content');
        const header = "<html xmlns:o='urn:schemas-microsoft-com:office:office' " +
            "xmlns:w='urn:schemas-microsoft-com:office:word' " +
            "xmlns='http://www.w3.org/TR/REC-html40'>" +
            "<head><meta charset='utf-8'><title>Export HTML to Word</title></head><body>";
        const footer = "</body></html>";
        const sourceHTML = header + element.innerHTML + footer;
        
        const source = 'data:application/vnd.ms-word;charset=utf-8,' + encodeURIComponent(sourceHTML);
        const fileDownload = document.createElement("a");
        document.body.appendChild(fileDownload);
        fileDownload.href = source;
        fileDownload.download = (state.currentTestName || 'test-report') + '.doc';
        fileDownload.click();
        document.body.removeChild(fileDownload);
    };
}
