/**
 * WealthOS RFC 4180 CSV & TSV Reader
 * Supports dynamic delimiter detection, escaped quotes (""), CRLF/LF,
 * multi-line quoted cells, and UTF-8 BOM removal.
 * Zero external dependencies.
 */

export function detectDelimiter(text: string): string {
  const lines = text.split(/\r?\n/).slice(0, 20).join('\n');
  let commaCount = 0;
  let tabCount = 0;
  let semicolonCount = 0;
  let inQuotes = false;

  for (let i = 0; i < lines.length; i++) {
    const c = lines[i];
    if (c === '"') {
      if (i + 1 < lines.length && lines[i + 1] === '"') {
        // escaped quote
      } else {
        inQuotes = !inQuotes;
      }
    } else if (!inQuotes) {
      if (c === ',') commaCount++;
      else if (c === '\t') tabCount++;
      else if (c === ';') semicolonCount++;
    }
  }

  if (tabCount > commaCount && tabCount > semicolonCount) return '\t';
  if (semicolonCount > commaCount && semicolonCount > tabCount) return ';';
  return ',';
}

export function readCsv(input: string, delimiter?: string): string[][] {
  const text = input.replace(/^\uFEFF/, '');
  if (!text.trim()) return [];

  const delim = delimiter || detectDelimiter(text);
  const rows: string[][] = [];
  let row: string[] = [];
  let cell = '';
  let inQuotes = false;
  let i = 0;

  function endCell() {
    row.push(cell.trim());
    cell = '';
  }

  function endRow() {
    endCell();
    if (!(row.length === 1 && row[0] === '')) {
      rows.push(row);
    }
    row = [];
  }

  while (i < text.length) {
    const c = text[i];
    if (inQuotes) {
      if (c === '"') {
        if (i + 1 < text.length && text[i + 1] === '"') {
          cell += '"';
          i++; // skip second quote
        } else {
          inQuotes = false;
        }
      } else {
        cell += c;
      }
    } else {
      if (c === '"') {
        if (cell.length === 0) {
          inQuotes = true;
        } else {
          cell += c;
        }
      } else if (c === delim) {
        endCell();
      } else if (c === '\r') {
        if (i + 1 < text.length && text[i + 1] === '\n') {
          i++;
        }
        endRow();
      } else if (c === '\n') {
        endRow();
      } else {
        cell += c;
      }
    }
    i++;
  }

  if (inQuotes) {
    throw new Error('Malformed CSV: unclosed quoted field');
  }

  if (cell.length > 0 || row.length > 0) {
    endRow();
  }

  return rows;
}
