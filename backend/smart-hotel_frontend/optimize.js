const fs = require('fs');
const path = require('path');

const ROOT_DIR = __dirname;

const cssLinksToInject = (basePath) => `
  <!-- CÁC FILE CSS ĐƯỢC CHÈN SẴN ĐỂ TRÁNH FOUC (FLASH OF UNSTYLED CONTENT) -->
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&family=Playfair+Display:ital,wght@0,600;0,700;1,600&display=swap" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
  <link rel="stylesheet" href="${basePath}style.css" />
  <style>
    .navbar-nav .nav-link.active-menu {
      font-weight: 700 !important;
      color: #d4af37 !important; /* Màu vàng Gold */
      background-color: rgba(212, 175, 55, 0.1); /* Nền mờ */
      border-radius: 8px; /* Bo tròn góc */
      padding: 8px 16px !important; /* Tạo khoảng trống xung quanh chữ */
      transition: 0.3s all ease-in-out;
    }
  </style>
`;

function walkDir(dir, callback) {
    fs.readdirSync(dir).forEach(f => {
        let dirPath = path.join(dir, f);
        let isDirectory = fs.statSync(dirPath).isDirectory();
        
        // Skip .git or other irrelevant dirs
        if (isDirectory && !f.startsWith('.')) {
            walkDir(dirPath, callback);
        } else if (!isDirectory) {
            callback(path.join(dir, f));
        }
    });
}

function optimizeHtmlFile(filePath) {
    if (!filePath.endsWith('.html')) return;
    
    let content = fs.readFileSync(filePath, 'utf8');
    
    // Determine relative path to root for style.css
    const relativePath = path.relative(path.dirname(filePath), ROOT_DIR);
    const basePath = relativePath ? relativePath.replace(/\\/g, '/') + '/' : '';

    // If it already has our injected block, skip CSS injection
    if (!content.includes('CÁC FILE CSS ĐƯỢC CHÈN SẴN')) {
        const injectedCSS = cssLinksToInject(basePath);
        
        if (content.includes('</head>')) {
            content = content.replace('</head>', injectedCSS + '\n</head>');
        }
    }
    
    // Optimize images with loading="lazy"
    content = content.replace(/<img\s+([^>]*?)>/gi, (match, p1) => {
        if (!p1.includes('loading=')) {
            return `<img loading="lazy" ${p1}>`;
        }
        return match;
    });

    fs.writeFileSync(filePath, content, 'utf8');
    console.log('Optimized: ' + filePath);
}

walkDir(ROOT_DIR, optimizeHtmlFile);
console.log('Done optimization script!');
