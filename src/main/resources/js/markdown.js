export class Markdown {

    generateMarkdownTable(columns, data) {
        if (!columns || !Array.isArray(columns) || columns.length === 0) {
            return "No data available";
        }
        if (!data || !Array.isArray(data)) {
            data = [];
        }
        // 1. Create the Header Row: | id | name | email |
        const header = `| ${columns.join(' | ')} |`;

        // 2. Create the Separator Row: | :--- | :--- | :--- |
        const separator = `| ${columns.map(() => ':---').join(' | ')} |`;

        // 3. Create the Data Rows
        const rows = data.map(row => {
            const values = columns.map(col => {
                const val = row[col];
                // Convert null/undefined to empty string for clean markdown
                return (val !== null && val !== undefined) ? val : '';
            });
            return `| ${values.join(' | ')} |`;
        });

        // Join everything with newlines
        return [header, separator, ...rows].join('\n');
    }

    heading(level, text) {
        client.markdown("#".repeat(level) + " " + text + "\n");
    }

    raw(text) {
        client.markdown(text + "\n");
    }

    codeBlock(language, code, prettyPrint) {
        if (prettyPrint && language.toLowerCase() === 'sql') {
            code = this.formatSql(code);
        }
        client.markdown("```" + language + "\n" + code + "\n```\n");
    }

    formatSql(sql) {
        if (!sql) return sql;

        // Basic SQL formatter
        let formatted = sql
            .replace(/\s+/g, ' ')
            .replace(/\b(SELECT|FROM|WHERE|AND|OR|ORDER BY|GROUP BY|HAVING|LIMIT|INSERT INTO|VALUES|UPDATE|SET|DELETE FROM|JOIN|LEFT JOIN|RIGHT JOIN|INNER JOIN|OUTER JOIN|ON|UNION|UNION ALL)\b/gi, '\n$1')
            .replace(/\b(SET|VALUES)\b/gi, '$1\n ')
            .replace(/,/g, ',\n ')
            .trim();

        // Indent lines that don't start with a keyword
        const keywords = /^(SELECT|FROM|WHERE|AND|OR|ORDER BY|GROUP BY|HAVING|LIMIT|INSERT INTO|VALUES|UPDATE|SET|DELETE FROM|JOIN|LEFT JOIN|RIGHT JOIN|INNER JOIN|OUTER JOIN|ON|UNION|UNION ALL)/i;
        formatted = formatted.split('\n').map(line => {
            line = line.trim();
            if (line.length > 0 && !keywords.test(line)) {
                return '  ' + line;
            }
            return line;
        }).filter(line => line.trim().length > 0).join('\n');

        return formatted;
    }

    table(columns, data) {
        client.markdown(this.generateMarkdownTable(columns, data) + "\n");
    }

}

export const markdowner = new Markdown();