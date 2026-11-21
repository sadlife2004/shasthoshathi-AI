/**
 * Minimal, standards-compliant ISO/IEC 18004 QR Code encoder (Byte mode, ECC Level M, Versions 1..10).
 * Zero external dependencies — renders crisply onto any HTML5 <canvas>.
 */
(function (global) {
  "use strict";

  // ECC Level M parameters for QR Versions 1..10:
  // [version, totalCodewords, ecPerBlock, numBlocksGroup1, dataPerBlockGroup1, numBlocksGroup2, dataPerBlockGroup2, alignCoords]
  const VERSION_SPECS = [
    null,
    [1, 26, 10, 1, 16, 0, 0, []],
    [2, 44, 16, 1, 28, 0, 0, [6, 18]],
    [3, 70, 26, 1, 44, 0, 0, [6, 22]],
    [4, 100, 18, 2, 32, 0, 0, [6, 26]],
    [5, 134, 24, 2, 43, 0, 0, [6, 30]],
    [6, 172, 16, 4, 27, 0, 0, [6, 34]],
    [7, 196, 18, 4, 31, 0, 0, [6, 22, 38]],
    [8, 242, 22, 2, 38, 2, 39, [6, 24, 42]],
    [9, 292, 22, 3, 36, 2, 37, [6, 26, 46]],
    [10, 346, 26, 4, 43, 1, 44, [6, 28, 50]]
  ];

  // Version information bits for versions 7..10 (18-bit BCH(18,6))
  const VERSION_BITS = {
    7: 0x07c94,
    8: 0x085bc,
    9: 0x09a99,
    10: 0x0a4d3
  };

  // Precomputed GF(256) log & exp tables (primitive polynomial 0x11d)
  const GF_EXP = new Uint8Array(512);
  const GF_LOG = new Uint8Array(256);
  (function initGF() {
    let x = 1;
    for (let i = 0; i < 255; i++) {
      GF_EXP[i] = x;
      GF_LOG[x] = i;
      x <<= 1;
      if (x & 0x100) x ^= 0x11d;
    }
    for (let i = 255; i < 512; i++) {
      GF_EXP[i] = GF_EXP[i - 255];
    }
  })();

  function gfMul(a, b) {
    if (a === 0 || b === 0) return 0;
    return GF_EXP[GF_LOG[a] + GF_LOG[b]];
  }

  function rsGeneratorPoly(degree) {
    let poly = [1];
    for (let i = 0; i < degree; i++) {
      const next = new Array(poly.length + 1).fill(0);
      const factor = GF_EXP[i];
      for (let j = 0; j < poly.length; j++) {
        next[j] ^= poly[j];
        next[j + 1] ^= gfMul(poly[j], factor);
      }
      poly = next;
    }
    return poly;
  }

  function rsComputeRemainder(data, ecCount) {
    const gen = rsGeneratorPoly(ecCount);
    const rem = new Uint8Array(ecCount);
    for (let i = 0; i < data.length; i++) {
      const factor = data[i] ^ rem[0];
      for (let j = 0; j < ecCount - 1; j++) {
        rem[j] = rem[j + 1] ^ gfMul(gen[j + 1], factor);
      }
      rem[ecCount - 1] = gfMul(gen[ecCount], factor);
    }
    return rem;
  }

  function toUtf8Bytes(str) {
    if (typeof TextEncoder !== "undefined") {
      return Array.from(new TextEncoder().encode(str));
    }
    const bytes = [];
    for (let i = 0; i < str.length; i++) {
      const code = str.charCodeAt(i);
      if (code < 0x80) bytes.push(code);
      else if (code < 0x800) bytes.push(0xc0 | (code >> 6), 0x80 | (code & 0x3f));
      else bytes.push(0xe0 | (code >> 12), 0x80 | ((code >> 6) & 0x3f), 0x80 | (code & 0x3f));
    }
    return bytes;
  }

  function pickVersion(byteLen) {
    for (let v = 1; v <= 10; v++) {
      const spec = VERSION_SPECS[v];
      const dataCapacity = spec[3] * spec[4] + spec[5] * spec[6];
      const countBits = v <= 9 ? 8 : 16;
      const neededBits = 4 + countBits + byteLen * 8;
      if (neededBits <= dataCapacity * 8) return v;
    }
    throw new Error("QR payload too long (max 213 bytes)");
  }

  function buildCodewords(bytes, version) {
    const spec = VERSION_SPECS[version];
    const ecPerBlock = spec[2];
    const g1Blocks = spec[3], g1Data = spec[4];
    const g2Blocks = spec[5], g2Data = spec[6];
    const totalDataBytes = g1Blocks * g1Data + g2Blocks * g2Data;

    const bits = [];
    function pushBits(val, len) {
      for (let i = len - 1; i >= 0; i--) bits.push((val >>> i) & 1);
    }

    // Mode 0100 (Byte)
    pushBits(0b0100, 4);
    pushBits(bytes.length, version <= 9 ? 8 : 16);
    for (let i = 0; i < bytes.length; i++) pushBits(bytes[i], 8);

    // Terminator (up to 4 zero bits)
    const maxBits = totalDataBytes * 8;
    const termLen = Math.min(4, maxBits - bits.length);
    for (let i = 0; i < termLen; i++) bits.push(0);
    while (bits.length % 8 !== 0) bits.push(0);

    const dataBytes = [];
    for (let i = 0; i < bits.length; i += 8) {
      let b = 0;
      for (let j = 0; j < 8; j++) b = (b << 1) | bits[i + j];
      dataBytes.push(b);
    }

    // Pad bytes 0xEC, 0x11
    let padToggle = true;
    while (dataBytes.length < totalDataBytes) {
      dataBytes.push(padToggle ? 0xec : 0x11);
      padToggle = !padToggle;
    }

    // Split into blocks and compute RS ECC
    const dataBlocks = [];
    const ecBlocks = [];
    let offset = 0;
    for (let i = 0; i < g1Blocks; i++) {
      const blk = dataBytes.slice(offset, offset + g1Data);
      offset += g1Data;
      dataBlocks.push(blk);
      ecBlocks.push(rsComputeRemainder(blk, ecPerBlock));
    }
    for (let i = 0; i < g2Blocks; i++) {
      const blk = dataBytes.slice(offset, offset + g2Data);
      offset += g2Data;
      dataBlocks.push(blk);
      ecBlocks.push(rsComputeRemainder(blk, ecPerBlock));
    }

    // Interleave data codewords, then ECC codewords
    const result = [];
    const maxDataLen = Math.max(g1Data, g2Data);
    for (let i = 0; i < maxDataLen; i++) {
      for (let b = 0; b < dataBlocks.length; b++) {
        if (i < dataBlocks[b].length) result.push(dataBlocks[b][i]);
      }
    }
    for (let i = 0; i < ecPerBlock; i++) {
      for (let b = 0; b < ecBlocks.length; b++) {
        result.push(ecBlocks[b][i]);
      }
    }
    return result;
  }

  function formatInfoBits(mask) {
    // ECC Level M = 00
    const data = (0b00 << 3) | mask;
    let rem = data << 10;
    const poly = 0b10100110111;
    for (let i = 14; i >= 10; i--) {
      if ((rem >>> i) & 1) rem ^= poly << (i - 10);
    }
    return ((data << 10) | rem) ^ 0b101010000010010;
  }

  function createMatrix(text) {
    const bytes = toUtf8Bytes(text);
    const version = pickVersion(bytes.length);
    const size = version * 4 + 17;
    const codewords = buildCodewords(bytes, version);

    const modules = Array.from({ length: size }, () => new Int8Array(size));
    const isFunc = Array.from({ length: size }, () => new Uint8Array(size));

    function setFunc(r, c, dark) {
      if (r >= 0 && r < size && c >= 0 && c < size) {
        modules[r][c] = dark ? 1 : 0;
        isFunc[r][c] = 1;
      }
    }

    // Finder patterns + separators
    function drawFinder(top, left) {
      for (let dr = -1; dr <= 7; dr++) {
        for (let dc = -1; dc <= 7; dc++) {
          const r = top + dr, c = left + dc;
          if (r < 0 || r >= size || c < 0 || c >= size) continue;
          const dark =
            (dr >= 0 && dr <= 6 && (dc === 0 || dc === 6)) ||
            (dc >= 0 && dc <= 6 && (dr === 0 || dr === 6)) ||
            (dr >= 2 && dr <= 4 && dc >= 2 && dc <= 4);
          setFunc(r, c, dark);
        }
      }
    }
    drawFinder(0, 0);
    drawFinder(0, size - 7);
    drawFinder(size - 7, 0);

    // Alignment patterns
    const coords = VERSION_SPECS[version][7];
    for (let i = 0; i < coords.length; i++) {
      for (let j = 0; j < coords.length; j++) {
        const cr = coords[i], cc = coords[j];
        if ((cr === 6 && cc === 6) || (cr === 6 && cc === size - 7) || (cr === size - 7 && cc === 6)) {
          continue;
        }
        for (let dr = -2; dr <= 2; dr++) {
          for (let dc = -2; dc <= 2; dc++) {
            const dist = Math.max(Math.abs(dr), Math.abs(dc));
            setFunc(cr + dr, cc + dc, dist !== 1);
          }
        }
      }
    }

    // Timing patterns
    for (let i = 8; i < size - 8; i++) {
      if (!isFunc[6][i]) setFunc(6, i, i % 2 === 0);
      if (!isFunc[i][6]) setFunc(i, 6, i % 2 === 0);
    }

    // Dark module & reserve format info areas
    setFunc(size - 8, 8, true);
    for (let i = 0; i < 9; i++) {
      if (i !== 6) {
        isFunc[8][i] = 1;
        isFunc[i][8] = 1;
      }
    }
    for (let i = 0; i < 8; i++) {
      isFunc[8][size - 1 - i] = 1;
      isFunc[size - 1 - i][8] = 1;
    }

    // Version info for v >= 7
    if (version >= 7) {
      const vBits = VERSION_BITS[version];
      for (let i = 0; i < 18; i++) {
        const bit = ((vBits >>> i) & 1) === 1;
        const r = Math.floor(i / 3);
        const c = size - 11 + (i % 3);
        setFunc(r, c, bit);
        setFunc(c, r, bit);
      }
    }

    // Place data bits in upward/downward zigzag columns
    let bitIdx = 0;
    const totalBits = codewords.length * 8;
    let upward = true;
    for (let right = size - 1; right >= 1; right -= 2) {
      if (right === 6) right = 5;
      for (let vert = 0; vert < size; vert++) {
        const r = upward ? size - 1 - vert : vert;
        for (let j = 0; j < 2; j++) {
          const c = right - j;
          if (!isFunc[r][c]) {
            let dark = false;
            if (bitIdx < totalBits) {
              dark = ((codewords[bitIdx >>> 3] >>> (7 - (bitIdx & 7))) & 1) === 1;
              bitIdx++;
            }
            modules[r][c] = dark ? 1 : 0;
          }
        }
      }
      upward = !upward;
    }

    // Evaluate all 8 masks and choose lowest penalty
    function maskBit(mask, r, c) {
      switch (mask) {
        case 0: return (r + c) % 2 === 0;
        case 1: return r % 2 === 0;
        case 2: return c % 3 === 0;
        case 3: return (r + c) % 3 === 0;
        case 4: return (Math.floor(r / 2) + Math.floor(c / 3)) % 2 === 0;
        case 5: return ((r * c) % 2) + ((r * c) % 3) === 0;
        case 6: return (((r * c) % 2) + ((r * c) % 3)) % 2 === 0;
        case 7: return (((r + c) % 2) + ((r * c) % 3)) % 2 === 0;
        default: return false;
      }
    }

    function applyFormat(grid, mask) {
      const f = formatInfoBits(mask);
      for (let i = 0; i <= 5; i++) grid[8][i] = (f >>> (14 - i)) & 1;
      grid[8][7] = (f >>> 8) & 1;
      grid[8][8] = (f >>> 7) & 1;
      grid[7][8] = (f >>> 6) & 1;
      for (let i = 0; i <= 5; i++) grid[5 - i][8] = (f >>> (5 - i)) & 1;
      for (let i = 0; i < 8; i++) grid[8][size - 1 - i] = (f >>> i) & 1;
      for (let i = 0; i < 7; i++) grid[size - 1 - i][8] = (f >>> (14 - i)) & 1;
    }

    function penalty(grid) {
      let score = 0;
      for (let r = 0; r < size; r++) {
        let runColor = grid[r][0], runLen = 1;
        for (let c = 1; c < size; c++) {
          if (grid[r][c] === runColor) {
            runLen++;
            if (runLen === 5) score += 3;
            else if (runLen > 5) score += 1;
          } else {
            runColor = grid[r][c];
            runLen = 1;
          }
        }
      }
      for (let c = 0; c < size; c++) {
        let runColor = grid[0][c], runLen = 1;
        for (let r = 1; r < size; r++) {
          if (grid[r][c] === runColor) {
            runLen++;
            if (runLen === 5) score += 3;
            else if (runLen > 5) score += 1;
          } else {
            runColor = grid[r][c];
            runLen = 1;
          }
        }
      }
      for (let r = 0; r < size - 1; r++) {
        for (let c = 0; c < size - 1; c++) {
          const v = grid[r][c];
          if (v === grid[r][c + 1] && v === grid[r + 1][c] && v === grid[r + 1][c + 1]) {
            score += 3;
          }
        }
      }
      return score;
    }

    let bestGrid = null;
    let bestScore = Infinity;
    for (let m = 0; m < 8; m++) {
      const candidate = modules.map((row, r) => {
        const copy = new Int8Array(row);
        for (let c = 0; c < size; c++) {
          if (!isFunc[r][c] && maskBit(m, r, c)) copy[c] ^= 1;
        }
        return copy;
      });
      applyFormat(candidate, m);
      const s = penalty(candidate);
      if (s < bestScore) {
        bestScore = s;
        bestGrid = candidate;
      }
    }

    return { size, modules: bestGrid };
  }

  function renderToCanvas(canvas, text, options) {
    const opts = options || {};
    const pxSize = opts.size || 240;
    const quiet = opts.quietZone != null ? opts.quietZone : 4;
    const fg = opts.fg || "#0b3b36";
    const bg = opts.bg || "#ffffff";

    const qr = createMatrix(text);
    const totalModules = qr.size + quiet * 2;
    const scale = Math.max(2, Math.floor(pxSize / totalModules));
    const actualSize = totalModules * scale;

    canvas.width = actualSize;
    canvas.height = actualSize;
    const ctx = canvas.getContext("2d");
    ctx.fillStyle = bg;
    ctx.fillRect(0, 0, actualSize, actualSize);

    ctx.fillStyle = fg;
    for (let r = 0; r < qr.size; r++) {
      for (let c = 0; c < qr.size; c++) {
        if (qr.modules[r][c]) {
          ctx.fillRect((c + quiet) * scale, (r + quiet) * scale, scale, scale);
        }
      }
    }
    return qr;
  }

  global.ShasthoQR = {
    createMatrix: createMatrix,
    renderToCanvas: renderToCanvas
  };
})(typeof window !== "undefined" ? window : globalThis);
