import { basicSetup } from "codemirror";
import { EditorState, Compartment, RangeSet, StateField, StateEffect } from "@codemirror/state";
import { javascript } from "@codemirror/lang-javascript";
import { sql } from "@codemirror/lang-sql";
import { keymap, gutter, GutterMarker, EditorView, Decoration } from "@codemirror/view";
import { indentWithTab } from "@codemirror/commands";
import { oneDark } from "@codemirror/theme-one-dark";
import { state, sourceEditor, setEditor, editor as cmEditor } from './core.js';
import { runSingleRequest } from './test-runner.js';
import { setBreakpoints } from './debugger.js';

class RunMarker extends GutterMarker {
    constructor(requestLine, lineIndex) {
        super();
        this.requestLine = requestLine;
        this.lineIndex = lineIndex;
    }
    toDOM() {
        const span = document.createElement("span");
        span.className = "play-button";
        span.textContent = "▶";
        span.title = "Run this request";
        span.onclick = (e) => {
            e.stopPropagation();
            runSingleRequest(this.requestLine, this.lineIndex);
        };
        return span;
    }
}

const runGutter = gutter({
    class: "cm-run-gutter",
    renderEmptyElements: false,
    markers(view) {
        let markers = [];
        const requestLineRegex = /^(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD|TRACE) /;
        for (let {from, to} of view.visibleRanges) {
            for (let pos = from; pos <= to;) {
                let line = view.state.doc.lineAt(pos);
                if (requestLineRegex.test(line.text.trim())) {
                    markers.push(new RunMarker(line.text.trim(), line.number - 1).range(line.from));
                }
                pos = line.to + 1;
            }
        }
        return RangeSet.of(markers);
    }
});

class BreakpointMarker extends GutterMarker {
    toDOM() {
        const div = document.createElement("div");
        div.className = "breakpoint-marker";
        return div;
    }
}

class DebugMarker extends GutterMarker {
    toDOM() {
        const div = document.createElement("div");
        div.className = "debug-marker";
        div.textContent = "➔";
        return div;
    }
}

export const toggleBreakpoint = StateEffect.define();

const breakpointState = StateField.define({
    create() { return RangeSet.empty; },
    update(breakpoints, tr) {
        breakpoints = breakpoints.map(tr.changes);
        for (let e of tr.effects) {
            if (e.is(toggleBreakpoint)) {
                let {pos, on} = e.value;
                let exists = false;
                breakpoints.between(pos, pos, (from, to, value) => { exists = true; });
                if (exists) {
                    breakpoints = breakpoints.update({filter: (from, to, value) => from !== pos});
                } else {
                    breakpoints = breakpoints.update({add: [new BreakpointMarker().range(pos)]});
                }
            }
        }
        // Update backend with new breakpoints
        if (tr.effects.some(e => e.is(toggleBreakpoint))) {
            setTimeout(() => {
                const currentBreakpoints = {};
                breakpoints.between(0, tr.state.doc.length, (from, to, value) => {
                    const line = tr.state.doc.lineAt(from);
                    currentBreakpoints[line.number] = true;
                });
                setBreakpoints(state.currentTestName, currentBreakpoints);
            }, 0);
        }
        return breakpoints;
    },
    provide: f => gutter({
        class: "cm-breakpoint-gutter",
        markers: v => v.state.field(f),
        domEventHandlers: {
            mousedown(view, line) {
                view.dispatch({
                    effects: toggleBreakpoint.of({pos: line.from})
                });
                return true;
            }
        }
    })
});

const debugLineEffect = StateEffect.define();
const debugLineField = StateField.define({
    create() { return RangeSet.empty; },
    update(lines, tr) {
        lines = lines.map(tr.changes);
        for (let e of tr.effects) {
            if (e.is(debugLineEffect)) {
                if (e.value === null) {
                    lines = RangeSet.empty;
                } else {
                    const line = tr.state.doc.line(e.value + 1);
                    lines = RangeSet.of([Decoration.line({class: "cm-debug-line"}).range(line.from)]);
                }
            }
        }
        return lines;
    },
    provide: f => [
        EditorView.decorations.from(f),
        gutter({
            class: "cm-debug-gutter",
            markers: v => {
                const rangeSet = v.state.field(f);
                let markers = [];
                rangeSet.between(0, v.state.doc.length, (from, to, value) => {
                    markers.push(new DebugMarker().range(from));
                });
                return RangeSet.of(markers);
            }
        })
    ]
});

const languageConf = new Compartment();
const themeConf = new Compartment();
const readOnlyConf = new Compartment();

export function initEditor(content = '', readOnly = false) {
    const isDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    
    const view = new EditorView({
        state: EditorState.create({
            doc: content,
            extensions: [
                basicSetup,
                keymap.of([indentWithTab]),
                readOnlyConf.of(EditorState.readOnly.of(readOnly)),
                // We'll use javascript as default for now, but in a real scenario we'd use the http parser if available
                languageConf.of(javascript()), 
                themeConf.of(isDarkMode ? oneDark : []),
                runGutter,
                breakpointState,
                debugLineField,
            ]
        }),
        parent: sourceEditor
    });
    setEditor(view);
    window.toggleBreakpointAtLine = toggleBreakpointAtLine;
}

export function updateEditorTheme() {
    if (!cmEditor) return;
    const isDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    cmEditor.dispatch({
        effects: themeConf.reconfigure(isDarkMode ? oneDark : [])
    });
}

export function highlightDebugLine(line) {
    if (!cmEditor) return;
    cmEditor.dispatch({
        effects: debugLineEffect.of(line)
    });
    if (line !== null) {
        const pos = cmEditor.state.doc.line(line + 1).from;
        cmEditor.dispatch({
            selection: {anchor: pos},
            scrollIntoView: true
        });
    }
}

export function setReadOnly(readOnly) {
    if (!cmEditor) return;
    cmEditor.dispatch({
        effects: readOnlyConf.reconfigure(EditorState.readOnly.of(readOnly))
    });
}

export function toggleBreakpointAtLine(line) {
    if (!cmEditor) return;
    const lineObj = cmEditor.state.doc.line(line);
    cmEditor.dispatch({
        effects: toggleBreakpoint.of({pos: lineObj.from})
    });
}
