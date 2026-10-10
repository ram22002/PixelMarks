package com.pixelmarks.core.pcserver

/**
 * Built-in, responsive HTML/CSS/JavaScript companion client served by the local Android device.
 * Follows the PixelMarks Warm Editorial Archival design language (parchment, charcoal, terracotta).
 * Completely self-contained: no CDNs, remote web fonts, analytics, telemetry, or external trackers.
 */
internal const val PC_CLIENT_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>PixelMarks PC Connect</title>
  <style>
    :root {
      --bg-parchment: #F8F5EE;
      --bg-parchment-deep: #F0EAE1;
      --card-bg: #FFFFFF;
      --charcoal: #2A2624;
      --charcoal-soft: #3D3734;
      --muted-charcoal: #68615C;
      --light-charcoal: #8E8680;
      --terracotta: #C85A32;
      --terracotta-dark: #A94723;
      --terracotta-soft: #F9EFEB;
      --accent-gold: #C28935;
      --accent-gold-soft: #FCF5EC;
      --border: #E5DFD7;
      --border-light: #F0ECE6;
      --success: #2E7D32;
      --success-bg: #E8F5E9;
      --error-bg: #FDECEA;
      --error-text: #C62828;
      --shadow-sm: 0 1px 3px rgba(42, 38, 36, 0.05);
      --shadow-md: 0 4px 14px rgba(42, 38, 36, 0.08);
      --shadow-lg: 0 10px 28px rgba(42, 38, 36, 0.12);
      --radius: 10px;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background-color: var(--bg-parchment);
      color: var(--charcoal);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      line-height: 1.5;
      padding: 24px 16px;
      min-height: 100vh;
      -webkit-font-smoothing: antialiased;
    }
    .container {
      max-width: 1200px;
      margin: 0 auto;
    }

    /* Accessibility Focus States */
    a:focus-visible, button:focus-visible, input:focus-visible, select:focus-visible {
      outline: 2px solid var(--terracotta);
      outline-offset: 2px;
    }

    /* Header & Branding */
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-bottom: 20px;
      border-bottom: 2px solid var(--terracotta);
      margin-bottom: 24px;
      flex-wrap: wrap;
      gap: 16px;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 14px;
      text-decoration: none;
      color: inherit;
    }
    .brand-icon {
      width: 44px;
      height: 44px;
      background: var(--terracotta);
      color: #FFFFFF;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-family: Georgia, "Newsreader", serif;
      font-weight: 700;
      font-size: 24px;
      box-shadow: var(--shadow-sm);
      flex-shrink: 0;
    }
    .brand-title {
      font-family: Georgia, "Newsreader", "Times New Roman", serif;
      font-size: 28px;
      font-weight: 700;
      letter-spacing: -0.5px;
      color: var(--charcoal);
      line-height: 1.1;
    }
    .brand-subtitle {
      color: var(--muted-charcoal);
      font-size: 13px;
      margin-top: 2px;
      letter-spacing: 0.2px;
    }
    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }
    .status-badge {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 6px 14px;
      border-radius: 20px;
      font-size: 13px;
      font-weight: 500;
      background: var(--card-bg);
      border: 1px solid var(--border);
      color: var(--muted-charcoal);
    }
    .status-badge.connected {
      background: var(--success-bg);
      border-color: #C8E6C9;
      color: var(--success);
    }
    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #9E9E9E;
      display: inline-block;
    }
    .status-badge.connected .status-dot {
      background: var(--success);
      box-shadow: 0 0 0 3px rgba(46, 125, 50, 0.2);
    }

    /* Buttons */
    .btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      padding: 9px 16px;
      font-size: 13px;
      font-weight: 600;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.15s ease;
      text-decoration: none;
      border: 1px solid transparent;
      white-space: nowrap;
      font-family: inherit;
    }
    .btn-primary {
      background: var(--terracotta);
      color: #FFFFFF;
      border: none;
    }
    .btn-primary:hover { background: var(--terracotta-dark); }
    .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
    .btn-secondary {
      background: var(--card-bg);
      color: var(--charcoal);
      border-color: var(--border);
    }
    .btn-secondary:hover {
      background: var(--bg-parchment-deep);
      border-color: #D6CEC4;
    }
    .btn-subtle {
      background: transparent;
      color: var(--muted-charcoal);
      border: none;
      padding: 6px 10px;
    }
    .btn-subtle:hover {
      color: var(--charcoal);
      background: var(--bg-parchment-deep);
    }
    .btn-sm {
      padding: 5px 10px;
      font-size: 12px;
      border-radius: 6px;
    }
    .btn-block { width: 100%; padding: 13px; font-size: 15px; }

    /* Pairing Card */
    .pairing-wrapper {
      max-width: 500px;
      margin: 40px auto;
    }
    .card {
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 36px 30px;
      box-shadow: var(--shadow-md);
    }
    .pairing-header {
      text-align: center;
      margin-bottom: 24px;
    }
    .pairing-title {
      font-family: Georgia, "Newsreader", serif;
      font-size: 26px;
      font-weight: 700;
      color: var(--charcoal);
      margin-bottom: 8px;
    }
    .pairing-desc {
      color: var(--muted-charcoal);
      font-size: 14px;
      line-height: 1.5;
    }
    .network-notice {
      background: var(--bg-parchment-deep);
      border-left: 3px solid var(--terracotta);
      padding: 10px 14px;
      border-radius: 0 6px 6px 0;
      font-size: 12px;
      color: var(--charcoal-soft);
      margin: 18px 0;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .pin-container {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 8px;
      margin: 20px 0 14px;
    }
    .pin-input {
      width: 52px;
      height: 60px;
      text-align: center;
      font-family: Georgia, serif;
      font-size: 28px;
      font-weight: 700;
      color: var(--charcoal);
      border: 2px solid var(--border);
      border-radius: 8px;
      background: #FAFAFA;
      outline: none;
      transition: all 0.2s ease;
    }
    .pin-input:focus {
      border-color: var(--terracotta);
      background: #FFFFFF;
      box-shadow: 0 0 0 3px rgba(200, 90, 50, 0.15);
    }
    .pin-input.has-value {
      border-color: var(--charcoal);
      background: #FFFFFF;
    }
    .pin-divider {
      font-size: 24px;
      font-weight: 700;
      color: var(--light-charcoal);
      user-select: none;
    }
    .pin-hint {
      text-align: center;
      font-size: 12px;
      color: var(--muted-charcoal);
      margin-bottom: 24px;
    }

    /* Status & Alerts */
    .msg-box {
      padding: 14px 16px;
      border-radius: 8px;
      font-size: 13px;
      margin-top: 18px;
      display: none;
      align-items: center;
      gap: 10px;
      line-height: 1.4;
    }
    .msg-box.visible { display: flex; }
    .msg-error { background: var(--error-bg); color: var(--error-text); border: 1px solid #F5C6CB; }
    .msg-info { background: #E8F0FE; color: #174EA6; border: 1px solid #D2E3FC; }

    /* Waiting Box */
    .waiting-box {
      background: var(--terracotta-soft);
      border: 1px solid #F2DDD3;
      border-radius: 8px;
      padding: 20px;
      margin-top: 20px;
      text-align: center;
      display: none;
    }
    .waiting-box.visible { display: block; }
    .spinner {
      width: 30px;
      height: 30px;
      border: 3px solid rgba(200, 90, 50, 0.2);
      border-top-color: var(--terracotta);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 12px;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* Dashboard Layout */
    .dashboard-layout {
      display: grid;
      grid-template-columns: 280px 1fr;
      gap: 24px;
      align-items: start;
    }
    @media (max-width: 960px) {
      .dashboard-layout { grid-template-columns: 1fr; }
    }

    /* Sidebar */
    .sidebar {
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 18px 14px;
      box-shadow: var(--shadow-sm);
    }
    .sidebar-section-title {
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 1px;
      text-transform: uppercase;
      color: var(--muted-charcoal);
      padding: 6px 12px;
      margin-bottom: 4px;
    }
    .nav-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 9px 12px;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 500;
      color: var(--charcoal);
      cursor: pointer;
      transition: all 0.15s ease;
      margin-bottom: 3px;
      user-select: none;
    }
    .nav-item:hover { background: var(--bg-parchment-deep); }
    .nav-item.active {
      background: var(--terracotta-soft);
      color: var(--terracotta);
      font-weight: 600;
    }
    .nav-left {
      display: flex;
      align-items: center;
      gap: 10px;
      min-width: 0;
    }
    .nav-icon {
      width: 18px;
      height: 18px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .nav-dot {
      width: 9px;
      height: 9px;
      border-radius: 50%;
      flex-shrink: 0;
    }
    .nav-name {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .nav-count {
      font-size: 12px;
      padding: 2px 7px;
      border-radius: 12px;
      background: var(--bg-parchment-deep);
      color: var(--muted-charcoal);
      font-weight: 600;
      flex-shrink: 0;
    }
    .nav-item.active .nav-count {
      background: var(--terracotta);
      color: #FFFFFF;
    }

    /* Sub-collections in sidebar */
    .nav-subitem {
      padding-left: 28px;
      font-size: 13px;
    }

    /* Main Explorer Area */
    .explorer-main {
      min-width: 0;
    }

    /* Collection Dossier Banner (Active Collection Header) */
    .collection-dossier {
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 20px 24px;
      margin-bottom: 18px;
      box-shadow: var(--shadow-sm);
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;
    }
    .dossier-left {
      display: flex;
      align-items: center;
      gap: 16px;
      min-width: 0;
    }
    .dossier-badge {
      width: 48px;
      height: 48px;
      border-radius: 10px;
      background: var(--terracotta-soft);
      color: var(--terracotta);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      font-size: 22px;
    }
    .dossier-meta {
      min-width: 0;
    }
    .dossier-breadcrumb {
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.8px;
      color: var(--muted-charcoal);
      margin-bottom: 2px;
    }
    .dossier-title {
      font-family: Georgia, "Newsreader", serif;
      font-size: 22px;
      font-weight: 700;
      color: var(--charcoal);
      line-height: 1.2;
    }
    .dossier-subtitle {
      font-size: 12px;
      color: var(--muted-charcoal);
      margin-top: 4px;
    }
    .dossier-subcollections {
      width: 100%;
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
      margin-top: 8px;
      padding-top: 12px;
      border-top: 1px dashed var(--border);
    }
    .subcol-pill {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 10px;
      background: var(--bg-parchment-deep);
      border-radius: 16px;
      font-size: 12px;
      color: var(--charcoal);
      cursor: pointer;
      text-decoration: none;
      transition: all 0.15s ease;
    }
    .subcol-pill:hover {
      background: var(--border);
    }

    /* Explorer Toolbar */
    .explorer-toolbar {
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 14px 18px;
      margin-bottom: 16px;
      box-shadow: var(--shadow-sm);
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .toolbar-row {
      display: flex;
      gap: 12px;
      align-items: center;
      flex-wrap: wrap;
    }
    .search-wrapper {
      position: relative;
      flex: 1;
      min-width: 220px;
    }
    .search-input {
      width: 100%;
      padding: 9px 36px 9px 12px;
      font-size: 14px;
      border: 1px solid var(--border);
      border-radius: 8px;
      outline: none;
      background: #FAFAFA;
      transition: all 0.2s ease;
      font-family: inherit;
    }
    .search-input:focus {
      background: #FFFFFF;
      border-color: var(--terracotta);
      box-shadow: 0 0 0 3px rgba(200, 90, 50, 0.12);
    }
    .search-clear {
      position: absolute;
      right: 10px;
      top: 50%;
      transform: translateY(-50%);
      background: none;
      border: none;
      font-size: 16px;
      color: var(--light-charcoal);
      cursor: pointer;
      display: none;
      padding: 4px;
    }
    .sort-select {
      padding: 9px 12px;
      font-size: 13px;
      font-weight: 500;
      border: 1px solid var(--border);
      border-radius: 8px;
      background: #FAFAFA;
      color: var(--charcoal);
      outline: none;
      cursor: pointer;
      font-family: inherit;
    }
    .view-toggle-group {
      display: flex;
      background: var(--bg-parchment-deep);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 2px;
    }
    .view-toggle-btn {
      padding: 6px 10px;
      border-radius: 6px;
      border: none;
      background: transparent;
      color: var(--muted-charcoal);
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      font-weight: 500;
      gap: 4px;
      transition: all 0.15s ease;
    }
    .view-toggle-btn.active {
      background: var(--card-bg);
      color: var(--charcoal);
      box-shadow: var(--shadow-sm);
    }
    .toolbar-info {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 12px;
      color: var(--muted-charcoal);
      padding-top: 8px;
      border-top: 1px solid var(--border-light);
    }

    /* Views: Grid Cards Mode */
    .bookmarks-container.view-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 16px;
    }
    .card-item {
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 18px 20px;
      box-shadow: var(--shadow-sm);
      transition: all 0.18s ease;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      gap: 14px;
    }
    .card-item:hover {
      border-color: #D6CEC4;
      box-shadow: var(--shadow-md);
      transform: translateY(-1px);
    }
    .card-top {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .domain-strip {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 11px;
      color: var(--light-charcoal);
      margin-bottom: 2px;
    }
    .domain-tag {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      color: var(--muted-charcoal);
      font-weight: 500;
    }
    .favicon-avatar {
      width: 16px;
      height: 16px;
      border-radius: 4px;
      background: var(--bg-parchment-deep);
      color: var(--charcoal);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 10px;
      font-weight: 700;
      flex-shrink: 0;
    }
    .card-title {
      font-family: Georgia, "Newsreader", serif;
      font-size: 18px;
      font-weight: 700;
      color: var(--charcoal);
      text-decoration: none;
      line-height: 1.3;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      transition: color 0.15s ease;
    }
    .card-title:hover { color: var(--terracotta); }
    .card-desc {
      font-size: 13px;
      color: var(--charcoal-soft);
      line-height: 1.45;
      margin-top: 4px;
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .card-badges {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-wrap: wrap;
      margin-top: 8px;
    }
    .badge-pill {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 2px 8px;
      border-radius: 4px;
      background: var(--bg-parchment-deep);
      font-size: 11px;
      color: var(--charcoal-soft);
      font-weight: 500;
    }
    .badge-pill.clickable {
      cursor: pointer;
    }
    .badge-pill.clickable:hover {
      background: var(--border);
    }
    .card-actions {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding-top: 12px;
      border-top: 1px solid var(--border-light);
      gap: 8px;
    }
    .card-actions-left {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    /* Views: Compact List Mode */
    .bookmarks-container.view-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .list-item {
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 12px 18px;
      box-shadow: var(--shadow-sm);
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      transition: all 0.15s ease;
    }
    .list-item:hover {
      border-color: #D6CEC4;
      background: #FCFAF7;
    }
    .list-left {
      display: flex;
      align-items: center;
      gap: 14px;
      min-width: 0;
      flex: 1;
    }
    .list-text {
      min-width: 0;
      flex: 1;
    }
    .list-title {
      font-family: Georgia, "Newsreader", serif;
      font-size: 16px;
      font-weight: 700;
      color: var(--charcoal);
      text-decoration: none;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      display: block;
    }
    .list-title:hover { color: var(--terracotta); }
    .list-sub {
      font-size: 12px;
      color: var(--muted-charcoal);
      display: flex;
      align-items: center;
      gap: 10px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      margin-top: 2px;
    }
    .list-actions {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-shrink: 0;
    }

    /* Empty & Loading States */
    .empty-state {
      background: var(--card-bg);
      border: 1px dashed var(--border);
      border-radius: var(--radius);
      padding: 48px 24px;
      text-align: center;
      color: var(--muted-charcoal);
    }
    .empty-icon {
      font-size: 44px;
      margin-bottom: 12px;
      opacity: 0.7;
    }
    .empty-title {
      font-family: Georgia, "Newsreader", serif;
      font-size: 20px;
      color: var(--charcoal);
      margin-bottom: 6px;
      font-weight: 700;
    }

    /* Modal Dialog */
    .modal-backdrop {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(42, 38, 36, 0.45);
      backdrop-filter: blur(2px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 16px;
    }
    .modal-dialog {
      background: var(--card-bg);
      border-radius: var(--radius);
      border: 1px solid var(--border);
      box-shadow: var(--shadow-lg);
      width: 100%;
      max-width: 560px;
      max-height: 90vh;
      overflow-y: auto;
      padding: 28px;
      position: relative;
    }
    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 18px;
      gap: 16px;
    }
    .modal-title {
      font-family: Georgia, "Newsreader", serif;
      font-size: 22px;
      font-weight: 700;
      color: var(--charcoal);
      line-height: 1.25;
    }
    .modal-close {
      background: none;
      border: none;
      font-size: 22px;
      cursor: pointer;
      color: var(--light-charcoal);
      padding: 4px;
      line-height: 1;
    }
    .modal-close:hover { color: var(--charcoal); }
    .modal-body {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .modal-url-box {
      background: var(--bg-parchment);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 10px 14px;
      font-size: 13px;
      word-break: break-all;
      color: var(--charcoal);
    }
    .modal-meta-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      font-size: 13px;
      border-top: 1px solid var(--border-light);
      padding-top: 14px;
    }
    .modal-meta-item {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .meta-label {
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: var(--muted-charcoal);
      font-weight: 600;
    }
    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      margin-top: 24px;
      padding-top: 16px;
      border-top: 1px solid var(--border-light);
    }

    .hidden { display: none !important; }

    /* Responsive adjustments */
    @media (max-width: 640px) {
      body { padding: 16px 12px; }
      .brand-title { font-size: 22px; }
      .brand-icon { width: 36px; height: 36px; font-size: 20px; }
      .pairing-wrapper { margin: 20px auto; }
      .card { padding: 24px 18px; }
      .pin-input { width: 42px; height: 50px; font-size: 22px; }
      .bookmarks-container.view-grid { grid-template-columns: 1fr; }
    }
  </style>
</head>
<body>
  <div class="container">
    <header role="banner">
      <div class="brand">
        <div class="brand-icon" aria-hidden="true">P</div>
        <div>
          <h1 class="brand-title">PixelMarks</h1>
          <div class="brand-subtitle">PC Connect &bull; Local Wi-Fi Companion</div>
        </div>
      </div>
      <div class="header-actions">
        <div id="statusBadge" class="status-badge" role="status" aria-live="polite">
          <span class="status-dot"></span>
          <span id="statusText">Disconnected</span>
        </div>
        <button id="refreshBtn" class="btn btn-secondary btn-sm hidden" aria-label="Sync bookmarks from Android device" title="Sync newest bookmarks from phone">&#8635; Sync</button>
        <button id="disconnectBtn" class="btn btn-secondary btn-sm hidden" aria-label="Disconnect PC companion">Disconnect</button>
      </div>
    </header>

    <!-- 1. PAIRING SCREEN -->
    <div id="pairingView" class="pairing-wrapper" role="region" aria-label="Pairing with Android phone">
      <div class="card">
        <div class="pairing-header">
          <h2 class="pairing-title">Connect to Your Phone</h2>
          <p class="pairing-desc">
            Access and browse your PixelMarks library directly from your PC browser over your home network.
          </p>
        </div>

        <div class="network-notice" role="note">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0;">
            <path d="M5 12.55a11 11 0 0 1 14.08 0"></path>
            <path d="M1.42 9a16 16 0 0 1 21.16 0"></path>
            <path d="M8.53 16.11a6 6 0 0 1 6.95 0"></path>
            <line x1="12" y1="20" x2="12.01" y2="20"></line>
          </svg>
          <span>Your PC and Android phone must be on the <strong>same Wi-Fi or local network</strong>.</span>
        </div>

        <div class="pin-container" id="pinInputs" aria-label="6-digit Pairing Code">
          <input type="text" class="pin-input" maxlength="1" inputmode="numeric" pattern="[0-9]*" data-idx="0" aria-label="PIN digit 1" autofocus>
          <input type="text" class="pin-input" maxlength="1" inputmode="numeric" pattern="[0-9]*" data-idx="1" aria-label="PIN digit 2">
          <input type="text" class="pin-input" maxlength="1" inputmode="numeric" pattern="[0-9]*" data-idx="2" aria-label="PIN digit 3">
          <span class="pin-divider" aria-hidden="true">&ndash;</span>
          <input type="text" class="pin-input" maxlength="1" inputmode="numeric" pattern="[0-9]*" data-idx="3" aria-label="PIN digit 4">
          <input type="text" class="pin-input" maxlength="1" inputmode="numeric" pattern="[0-9]*" data-idx="4" aria-label="PIN digit 5">
          <input type="text" class="pin-input" maxlength="1" inputmode="numeric" pattern="[0-9]*" data-idx="5" aria-label="PIN digit 6">
        </div>
        <div class="pin-hint">Type or paste the 6 digits &bull; Connection initiates automatically</div>

        <button id="connectBtn" class="btn btn-primary btn-block">Connect to Phone</button>

        <div id="waitingBox" class="waiting-box" role="status" aria-live="assertive">
          <div class="spinner" aria-hidden="true"></div>
          <strong style="color:var(--terracotta);">Confirm on your phone!</strong>
          <p style="font-size:13px; color:var(--muted-charcoal); margin-top:4px;">
            A pairing prompt appeared on your Android screen. Tap <strong>&ldquo;Allow Connection&rdquo;</strong> to authorize access.
          </p>
        </div>

        <div id="errorBox" class="msg-box msg-error" role="alert" aria-live="polite"></div>
      </div>
    </div>

    <!-- 2. CONNECTED DASHBOARD -->
    <div id="dashboardView" class="dashboard-layout hidden" role="main">
      <!-- Collections Sidebar -->
      <aside class="sidebar" role="navigation" aria-label="Sidebar navigation">
        <div class="sidebar-section-title">Catalogue</div>
        <div id="navAll" class="nav-item active" tabindex="0" role="button" aria-pressed="true" onclick="selectCollection(null)">
          <div class="nav-left">
            <span class="nav-icon" aria-hidden="true">&#128218;</span>
            <span class="nav-name">All Bookmarks</span>
          </div>
          <span id="allCountBadge" class="nav-count">0</span>
        </div>
        <div id="navPinned" class="nav-item" tabindex="0" role="button" aria-pressed="false" onclick="selectFilter('pinned')">
          <div class="nav-left">
            <span class="nav-icon" style="color:var(--terracotta);" aria-hidden="true">&#128204;</span>
            <span class="nav-name">Pinned</span>
          </div>
          <span id="pinnedCountBadge" class="nav-count">0</span>
        </div>
        <div id="navFav" class="nav-item" tabindex="0" role="button" aria-pressed="false" onclick="selectFilter('favorites')">
          <div class="nav-left">
            <span class="nav-icon" style="color:var(--accent-gold);" aria-hidden="true">&#9733;</span>
            <span class="nav-name">Favorites</span>
          </div>
          <span id="favCountBadge" class="nav-count">0</span>
        </div>
        <div id="navArchived" class="nav-item" tabindex="0" role="button" aria-pressed="false" onclick="selectFilter('archived')">
          <div class="nav-left">
            <span class="nav-icon" style="color:var(--muted-charcoal);" aria-hidden="true">&#128451;</span>
            <span class="nav-name">Archived</span>
          </div>
          <span id="archivedCountBadge" class="nav-count">0</span>
        </div>

        <div class="sidebar-section-title" style="margin-top:18px;">Collections</div>
        <div id="collectionsList">
          <!-- Dynamic collections injected here -->
        </div>
      </aside>

      <!-- Main Explorer -->
      <div class="explorer-main">
        <!-- Collection Dossier Banner (Appears when specific collection is chosen) -->
        <div id="collectionDossier" class="collection-dossier hidden">
          <div class="dossier-left">
            <div id="dossierBadge" class="dossier-badge" aria-hidden="true">&#128193;</div>
            <div class="dossier-meta">
              <div id="dossierBreadcrumb" class="dossier-breadcrumb">Collection Ledger</div>
              <h2 id="dossierTitle" class="dossier-title">Collection</h2>
              <div id="dossierSubtitle" class="dossier-subtitle">0 bookmarks &bull; Curated folder</div>
            </div>
          </div>
          <button class="btn btn-secondary btn-sm" onclick="selectCollection(null)">View All Bookmarks</button>
          <div id="dossierSubcollections" class="dossier-subcollections hidden"></div>
        </div>

        <!-- Explorer Toolbar -->
        <div class="explorer-toolbar" role="region" aria-label="Search and filter controls">
          <div class="toolbar-row">
            <div class="search-wrapper">
              <input type="text" id="searchInput" class="search-input" placeholder="Search public bookmarks by title, URL, or notes..." aria-label="Search public bookmarks">
              <button id="searchClearBtn" class="search-clear" aria-label="Clear search input">&times;</button>
            </div>

            <select id="sortSelect" class="sort-select" aria-label="Sort bookmarks by">
              <option value="newest">Newest First</option>
              <option value="oldest">Oldest First</option>
              <option value="title">Title (A&ndash;Z)</option>
              <option value="visits">Most Visited</option>
            </select>

            <div class="view-toggle-group" role="group" aria-label="View format">
              <button id="viewGridBtn" class="view-toggle-btn active" aria-pressed="true" title="Grid cards view">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M3 3h8v8H3zm10 0h8v8h-8zM3 13h8v8H3zm10 0h8v8h-8z"/></svg>
                <span>Cards</span>
              </button>
              <button id="viewListBtn" class="view-toggle-btn" aria-pressed="false" title="Compact list view">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z"/></svg>
                <span>List</span>
              </button>
            </div>
          </div>

          <div class="toolbar-info">
            <span id="currentFilterText">All Public Bookmarks</span>
            <span id="visibleCountText">0 bookmarks</span>
          </div>
        </div>

        <!-- Bookmarks List/Grid Container -->
        <div id="bookmarksContainer" class="bookmarks-container view-grid" role="region" aria-label="Bookmarks collection">
          <!-- Dynamic cards or rows injected here -->
        </div>
      </div>
    </div>
  </div>

  <!-- Detail Modal -->
  <div id="detailModal" class="modal-backdrop hidden" role="dialog" aria-modal="true" aria-labelledby="modalBookmarkTitle">
    <div class="modal-dialog">
      <div class="modal-header">
        <h3 id="modalBookmarkTitle" class="modal-title">Bookmark Details</h3>
        <button id="closeDetailModalBtn" class="modal-close" aria-label="Close dialog">&times;</button>
      </div>
      <div class="modal-body">
        <div class="modal-url-box">
          <div class="meta-label">Target URL</div>
          <a id="modalUrlLink" href="#" target="_blank" rel="noopener noreferrer" style="color:var(--terracotta); font-weight:600; text-decoration:none;"></a>
        </div>
        <div id="modalDescBlock" class="hidden">
          <div class="meta-label">Notes / Description</div>
          <p id="modalDescText" style="color:var(--charcoal-soft); font-size:14px; line-height:1.5; margin-top:4px;"></p>
        </div>
        <div class="modal-meta-grid">
          <div class="modal-meta-item">
            <span class="meta-label">Collection</span>
            <span id="modalCollectionText" style="font-weight:600;">None</span>
          </div>
          <div class="modal-meta-item">
            <span class="meta-label">Visits</span>
            <span id="modalVisitsText">0</span>
          </div>
          <div class="modal-meta-item">
            <span class="meta-label">Created</span>
            <span id="modalCreatedText">&ndash;</span>
          </div>
          <div class="modal-meta-item">
            <span class="meta-label">Updated</span>
            <span id="modalUpdatedText">&ndash;</span>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button id="modalCopyBtn" class="btn btn-secondary btn-sm">Copy Link</button>
        <a id="modalOpenBtn" href="#" target="_blank" rel="noopener noreferrer" class="btn btn-primary btn-sm">Open in Browser &rarr;</a>
      </div>
    </div>
  </div>

  <!-- Disconnect Confirm Dialog -->
  <div id="disconnectModal" class="modal-backdrop hidden" role="dialog" aria-modal="true" aria-labelledby="disconnectModalTitle">
    <div class="modal-dialog" style="max-width:440px;">
      <div class="modal-header">
        <h3 id="disconnectModalTitle" class="modal-title">Disconnect Session?</h3>
        <button id="closeDisconnectModalBtn" class="modal-close" aria-label="Cancel disconnect">&times;</button>
      </div>
      <div class="modal-body">
        <p style="font-size:14px; color:var(--muted-charcoal); line-height:1.5;">
          Disconnecting will revoke your current browser token. You will need to enter a fresh 6-digit PIN on your phone to reconnect.
        </p>
      </div>
      <div class="modal-footer">
        <button id="cancelDisconnectBtn" class="btn btn-secondary">Cancel</button>
        <button id="confirmDisconnectBtn" class="btn btn-primary" style="background:#C62828;">Disconnect</button>
      </div>
    </div>
  </div>

  <script>
    // State Store
    var allBookmarks = [];
    var allCollections = [];
    var activeFilter = null; // null = all, 'pinned', 'favorites', 'archived', or number (collectionId)
    var searchQuery = '';
    var sortOrder = 'newest';
    var currentViewMode = 'grid'; // 'grid' | 'list'
    var selectedBookmark = null;

    // DOM Elements
    var pairingView = document.getElementById('pairingView');
    var dashboardView = document.getElementById('dashboardView');
    var pinInputs = Array.from(document.querySelectorAll('.pin-input'));
    var connectBtn = document.getElementById('connectBtn');
    var waitingBox = document.getElementById('waitingBox');
    var errorBox = document.getElementById('errorBox');
    var statusBadge = document.getElementById('statusBadge');
    var statusText = document.getElementById('statusText');
    var refreshBtn = document.getElementById('refreshBtn');
    var disconnectBtn = document.getElementById('disconnectBtn');
    var collectionsList = document.getElementById('collectionsList');
    var bookmarksContainer = document.getElementById('bookmarksContainer');
    var searchInput = document.getElementById('searchInput');
    var searchClearBtn = document.getElementById('searchClearBtn');
    var sortSelect = document.getElementById('sortSelect');
    var viewGridBtn = document.getElementById('viewGridBtn');
    var viewListBtn = document.getElementById('viewListBtn');

    var navAll = document.getElementById('navAll');
    var navPinned = document.getElementById('navPinned');
    var navFav = document.getElementById('navFav');
    var navArchived = document.getElementById('navArchived');

    var allCountBadge = document.getElementById('allCountBadge');
    var pinnedCountBadge = document.getElementById('pinnedCountBadge');
    var favCountBadge = document.getElementById('favCountBadge');
    var archivedCountBadge = document.getElementById('archivedCountBadge');

    var currentFilterText = document.getElementById('currentFilterText');
    var visibleCountText = document.getElementById('visibleCountText');

    var collectionDossier = document.getElementById('collectionDossier');
    var dossierTitle = document.getElementById('dossierTitle');
    var dossierSubtitle = document.getElementById('dossierSubtitle');
    var dossierBreadcrumb = document.getElementById('dossierBreadcrumb');
    var dossierSubcollections = document.getElementById('dossierSubcollections');

    var detailModal = document.getElementById('detailModal');
    var closeDetailModalBtn = document.getElementById('closeDetailModalBtn');
    var modalBookmarkTitle = document.getElementById('modalBookmarkTitle');
    var modalUrlLink = document.getElementById('modalUrlLink');
    var modalDescBlock = document.getElementById('modalDescBlock');
    var modalDescText = document.getElementById('modalDescText');
    var modalCollectionText = document.getElementById('modalCollectionText');
    var modalVisitsText = document.getElementById('modalVisitsText');
    var modalCreatedText = document.getElementById('modalCreatedText');
    var modalUpdatedText = document.getElementById('modalUpdatedText');
    var modalCopyBtn = document.getElementById('modalCopyBtn');
    var modalOpenBtn = document.getElementById('modalOpenBtn');

    var disconnectModal = document.getElementById('disconnectModal');
    var closeDisconnectModalBtn = document.getElementById('closeDisconnectModalBtn');
    var cancelDisconnectBtn = document.getElementById('cancelDisconnectBtn');
    var confirmDisconnectBtn = document.getElementById('confirmDisconnectBtn');

    // Security: Strict HTML Escaping
    function escapeHtml(s) {
      if (s === null || s === undefined) return '';
      return String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
    }

    // Security: Strict URL Scheme Validation (Only http: and https: allowed)
    function sanitizeUrl(raw) {
      if (!raw || typeof raw !== 'string') return '#';
      var trimmed = raw.trim();
      if (!/^https?:\/\//i.test(trimmed)) {
        return '#';
      }
      try {
        var parsed = new URL(trimmed);
        if (parsed.protocol === 'http:' || parsed.protocol === 'https:') {
          return parsed.href;
        }
      } catch (_) {}
      return '#';
    }

    // Extract Domain Safely
    function getDomain(urlStr) {
      if (!urlStr) return '';
      try {
        var u = new URL(urlStr);
        return u.hostname.replace(/^www\./i, '');
      } catch (_) {
        return '';
      }
    }

    // Format Relative & Absolute Dates
    function formatDate(ts) {
      if (!ts) return '';
      try {
        var d = new Date(Number(ts));
        return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
      } catch (_) {
        return '';
      }
    }

    // PIN Input Handling
    pinInputs.forEach(function(input, idx) {
      input.addEventListener('input', function(e) {
        var val = input.value.replace(/[^0-9]/g, '');
        input.value = val ? val.slice(-1) : '';
        if (input.value) {
          input.classList.add('has-value');
          if (idx < pinInputs.length - 1) {
            pinInputs[idx + 1].focus();
          } else {
            var code = getEnteredCode();
            if (code.length === 6) attemptPairing(code);
          }
        } else {
          input.classList.remove('has-value');
        }
      });

      input.addEventListener('keydown', function(e) {
        if (e.key === 'Backspace' && !input.value && idx > 0) {
          pinInputs[idx - 1].focus();
          pinInputs[idx - 1].value = '';
          pinInputs[idx - 1].classList.remove('has-value');
        } else if (e.key === 'ArrowLeft' && idx > 0) {
          pinInputs[idx - 1].focus();
        } else if (e.key === 'ArrowRight' && idx < pinInputs.length - 1) {
          pinInputs[idx + 1].focus();
        } else if (e.key === 'Enter') {
          var code = getEnteredCode();
          if (code.length === 6) attemptPairing(code);
        }
      });

      input.addEventListener('paste', function(e) {
        e.preventDefault();
        var pasteText = (e.clipboardData || window.clipboardData).getData('text') || '';
        var digits = pasteText.replace(/[^0-9]/g, '');
        if (digits.length >= 6) {
          fillCode(digits.slice(0, 6));
          attemptPairing(digits.slice(0, 6));
        } else if (digits.length > 0) {
          fillCode(digits);
        }
      });
    });

    function getEnteredCode() {
      return pinInputs.map(function(i) { return i.value.trim(); }).join('');
    }

    function fillCode(digits) {
      for (var i = 0; i < pinInputs.length; i++) {
        if (i < digits.length) {
          pinInputs[i].value = digits[i];
          pinInputs[i].classList.add('has-value');
        } else {
          pinInputs[i].value = '';
          pinInputs[i].classList.remove('has-value');
        }
      }
      var focusIdx = Math.min(digits.length, pinInputs.length - 1);
      pinInputs[focusIdx].focus();
    }

    function showError(msg) {
      errorBox.textContent = msg;
      errorBox.classList.add('visible');
      waitingBox.classList.remove('visible');
      connectBtn.disabled = false;
    }

    function clearError() {
      errorBox.textContent = '';
      errorBox.classList.remove('visible');
    }

    // Auto-check session-scoped token on load (Prefer session-scoped storage per security requirements)
    var savedToken = sessionStorage.getItem('pixelmarks_token');
    if (savedToken) {
      loadDashboard(savedToken);
    }

    connectBtn.addEventListener('click', function() {
      var code = getEnteredCode();
      if (code.length !== 6) {
        showError('Please enter all 6 digits of the pairing code.');
        var firstEmpty = pinInputs.find(function(i) { return !i.value; });
        if (firstEmpty) firstEmpty.focus();
        return;
      }
      attemptPairing(code);
    });

    async function attemptPairing(code) {
      clearError();
      waitingBox.classList.add('visible');
      connectBtn.disabled = true;

      try {
        var res = await fetch('/api/v1/pair', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            pairingCode: code,
            deviceName: 'PC Browser Companion'
          })
        });
        var data = await res.json();
        if (res.ok && data.token) {
          sessionStorage.setItem('pixelmarks_token', data.token);
          waitingBox.classList.remove('visible');
          loadDashboard(data.token);
        } else {
          showError(data.message || 'Pairing was rejected or timed out. Please check your phone.');
          pinInputs.forEach(function(i) { i.value = ''; i.classList.remove('has-value'); });
          pinInputs[0].focus();
        }
      } catch (err) {
        showError('Network error connecting to phone. Ensure both devices are on the same Wi-Fi.');
      }
    }

    async function loadDashboard(token) {
      statusBadge.className = 'status-badge connected';
      statusText.textContent = 'Connected (Live)';
      refreshBtn.classList.remove('hidden');
      disconnectBtn.classList.remove('hidden');
      pairingView.classList.add('hidden');
      dashboardView.classList.remove('hidden');

      try {
        var [bmRes, colRes] = await Promise.all([
          fetch('/api/v1/bookmarks', { headers: { 'Authorization': 'Bearer ' + token } }),
          fetch('/api/v1/collections', { headers: { 'Authorization': 'Bearer ' + token } })
        ]);

        if (bmRes.status === 401 || colRes.status === 401) {
          sessionStorage.removeItem('pixelmarks_token');
          statusBadge.className = 'status-badge';
          statusText.textContent = 'Session Revoked';
          refreshBtn.classList.add('hidden');
          disconnectBtn.classList.add('hidden');
          dashboardView.classList.add('hidden');
          pairingView.classList.remove('hidden');
          showError('Your PC session was revoked or expired on your Android device. Please pair again.');
          return;
        }

        allBookmarks = await bmRes.json();
        allCollections = await colRes.json();

        renderSidebar();
        renderFilteredBookmarks();
      } catch (err) {
        bookmarksContainer.innerHTML = '<div class="empty-state"><div class="empty-title">Error Loading Library</div><p>' + escapeHtml(err.message) + '</p></div>';
      }
    }

    function renderSidebar() {
      allCountBadge.textContent = allBookmarks.length;
      pinnedCountBadge.textContent = allBookmarks.filter(function(b) { return b.isPinned; }).length;
      favCountBadge.textContent = allBookmarks.filter(function(b) { return b.isFavorite; }).length;
      archivedCountBadge.textContent = allBookmarks.filter(function(b) { return b.isArchived; }).length;

      var bmsByCol = {};
      allBookmarks.forEach(function(b) {
        if (b.collectionId) {
          bmsByCol[b.collectionId] = (bmsByCol[b.collectionId] || 0) + 1;
        }
      });

      if (!allCollections || allCollections.length === 0) {
        collectionsList.innerHTML = '<div style="font-size:12px; color:var(--muted-charcoal); padding:8px 12px;">No public collections</div>';
        return;
      }

      // Group root and children
      var rootCols = allCollections.filter(function(c) { return !c.parentId; });
      var childMap = {};
      allCollections.filter(function(c) { return c.parentId; }).forEach(function(c) {
        if (!childMap[c.parentId]) childMap[c.parentId] = [];
        childMap[c.parentId].push(c);
      });

      var html = '';
      rootCols.forEach(function(c) {
        html += renderNavCollectionItem(c, bmsByCol, false);
        if (childMap[c.id]) {
          childMap[c.id].forEach(function(sub) {
            html += renderNavCollectionItem(sub, bmsByCol, true);
          });
        }
      });

      collectionsList.innerHTML = html;
    }

    function renderNavCollectionItem(c, bmsByCol, isSub) {
      var count = bmsByCol[c.id] || 0;
      var color = c.color || '#C85A32';
      var isActive = activeFilter === c.id;
      var subClass = isSub ? 'nav-subitem' : '';
      return '<div class="nav-item ' + subClass + ' ' + (isActive ? 'active' : '') + '" tabindex="0" role="button" aria-pressed="' + isActive + '" onclick="selectCollection(' + c.id + ')">' +
        '<div class="nav-left">' +
          '<span class="nav-dot" style="background-color:' + color + ';"></span>' +
          '<span class="nav-name">' + escapeHtml(c.name) + '</span>' +
        '</div>' +
        '<span class="nav-count">' + count + '</span>' +
      '</div>';
    }

    window.selectFilter = function(filterKey) {
      activeFilter = filterKey;
      updateNavHighlight();
      renderFilteredBookmarks();
    };

    window.selectCollection = function(colId) {
      activeFilter = colId;
      updateNavHighlight();
      renderFilteredBookmarks();
    };

    function updateNavHighlight() {
      navAll.classList.toggle('active', activeFilter === null);
      navPinned.classList.toggle('active', activeFilter === 'pinned');
      navFav.classList.toggle('active', activeFilter === 'favorites');
      navArchived.classList.toggle('active', activeFilter === 'archived');
      renderSidebar();
    }

    function renderFilteredBookmarks() {
      var filtered = allBookmarks.slice();

      // Collection / Special Filter
      if (activeFilter === 'pinned') {
        filtered = filtered.filter(function(b) { return b.isPinned; });
        currentFilterText.textContent = 'Pinned Bookmarks (' + filtered.length + ')';
        collectionDossier.classList.add('hidden');
      } else if (activeFilter === 'favorites') {
        filtered = filtered.filter(function(b) { return b.isFavorite; });
        currentFilterText.textContent = 'Favorites (' + filtered.length + ')';
        collectionDossier.classList.add('hidden');
      } else if (activeFilter === 'archived') {
        filtered = filtered.filter(function(b) { return b.isArchived; });
        currentFilterText.textContent = 'Archived Bookmarks (' + filtered.length + ')';
        collectionDossier.classList.add('hidden');
      } else if (typeof activeFilter === 'number') {
        var colObj = allCollections.find(function(c) { return c.id === activeFilter; });
        var colName = colObj ? colObj.name : 'Collection';
        filtered = filtered.filter(function(b) { return b.collectionId === activeFilter; });
        currentFilterText.textContent = 'Folder: ' + colName + ' (' + filtered.length + ')';

        // Render Dossier Header
        collectionDossier.classList.remove('hidden');
        dossierTitle.textContent = colName;
        dossierSubtitle.textContent = filtered.length + ' bookmarks in this collection';

        if (colObj && colObj.parentId) {
          var parentObj = allCollections.find(function(c) { return c.id === colObj.parentId; });
          dossierBreadcrumb.textContent = parentObj ? 'Collections / ' + parentObj.name : 'Collections';
        } else {
          dossierBreadcrumb.textContent = 'Collection Ledger';
        }

        // Subcollections of this collection
        var subcols = allCollections.filter(function(c) { return c.parentId === activeFilter; });
        if (subcols.length > 0) {
          dossierSubcollections.classList.remove('hidden');
          dossierSubcollections.innerHTML = '<span style="font-size:11px; text-transform:uppercase; color:var(--muted-charcoal); font-weight:700;">Sub-folders:</span> ' +
            subcols.map(function(s) {
              return '<span class="subcol-pill" onclick="selectCollection(' + s.id + ')">&#128193; ' + escapeHtml(s.name) + '</span>';
            }).join('');
        } else {
          dossierSubcollections.classList.add('hidden');
        }
      } else {
        currentFilterText.textContent = 'All Public Bookmarks (' + filtered.length + ')';
        collectionDossier.classList.add('hidden');
      }

      // Search Filter
      var q = searchQuery.trim().toLowerCase();
      if (q) {
        filtered = filtered.filter(function(b) {
          return (b.title && b.title.toLowerCase().includes(q)) ||
                 (b.url && b.url.toLowerCase().includes(q)) ||
                 (b.description && b.description.toLowerCase().includes(q)) ||
                 (b.collectionName && b.collectionName.toLowerCase().includes(q));
        });
        currentFilterText.textContent = 'Search: "' + searchQuery + '" (' + filtered.length + ' results)';
      }

      // Sort
      if (sortOrder === 'newest') {
        filtered.sort(function(a, b) { return (b.createdAt || 0) - (a.createdAt || 0); });
      } else if (sortOrder === 'oldest') {
        filtered.sort(function(a, b) { return (a.createdAt || 0) - (b.createdAt || 0); });
      } else if (sortOrder === 'title') {
        filtered.sort(function(a, b) { return (a.title || '').localeCompare(b.title || ''); });
      } else if (sortOrder === 'visits') {
        filtered.sort(function(a, b) { return (b.visitCount || 0) - (a.visitCount || 0); });
      }

      visibleCountText.textContent = filtered.length + ' of ' + allBookmarks.length + ' bookmarks';

      if (filtered.length === 0) {
        bookmarksContainer.innerHTML = '<div class="empty-state" style="grid-column: 1 / -1;">' +
          '<div class="empty-icon">&#128269;</div>' +
          '<div class="empty-title">No bookmarks found</div>' +
          '<p>No bookmarks match the current filter or search criteria.</p>' +
          (q || activeFilter !== null ? '<button class="btn btn-secondary" style="margin-top:14px;" onclick="resetFilters()">View All Bookmarks</button>' : '') +
        '</div>';
        return;
      }

      var colMap = {};
      allCollections.forEach(function(c) { colMap[c.id] = c; });

      if (currentViewMode === 'grid') {
        bookmarksContainer.className = 'bookmarks-container view-grid';
        bookmarksContainer.innerHTML = filtered.map(function(b) {
          return renderGridCard(b, colMap);
        }).join('');
      } else {
        bookmarksContainer.className = 'bookmarks-container view-list';
        bookmarksContainer.innerHTML = filtered.map(function(b) {
          return renderListRow(b, colMap);
        }).join('');
      }
    }

    function renderGridCard(b, colMap) {
      var safeUrl = sanitizeUrl(b.url);
      var domain = getDomain(b.url);
      var col = b.collectionId ? colMap[b.collectionId] : null;
      var colHtml = '';
      if (col) {
        colHtml = '<span class="badge-pill clickable" onclick="selectCollection(' + col.id + ')">' +
          '<span style="width:6px; height:6px; border-radius:50%; background:' + (col.color || '#C85A32') + ';"></span>' +
          escapeHtml(col.name) +
        '</span>';
      }

      var pinBadge = b.isPinned ? '<span class="badge-pill" style="color:var(--terracotta); font-weight:700;">&#128204; Pinned</span>' : '';
      var favBadge = b.isFavorite ? '<span class="badge-pill" style="color:var(--accent-gold); font-weight:700;">&#9733; Favorite</span>' : '';
      var descHtml = b.description ? '<div class="card-desc">' + escapeHtml(b.description) + '</div>' : '';
      var dateHtml = b.createdAt ? formatDate(b.createdAt) : '';

      var initial = (domain ? domain.charAt(0).toUpperCase() : 'W');

      return '<div class="card-item">' +
        '<div class="card-top">' +
          '<div class="domain-strip">' +
            '<div class="domain-tag">' +
              '<span class="favicon-avatar">' + escapeHtml(initial) + '</span>' +
              '<span>' + escapeHtml(domain) + '</span>' +
            '</div>' +
            '<span>' + escapeHtml(dateHtml) + '</span>' +
          '</div>' +
          '<a href="' + escapeHtml(safeUrl) + '" target="_blank" rel="noopener noreferrer" class="card-title">' +
            escapeHtml(b.title || b.url) +
          '</a>' +
          descHtml +
          '<div class="card-badges">' +
            pinBadge +
            favBadge +
            colHtml +
          '</div>' +
        '</div>' +
        '<div class="card-actions">' +
          '<div class="card-actions-left">' +
            '<button class="btn btn-secondary btn-sm" onclick="copyLink(this, \'' + escapeHtml(safeUrl) + '\')">Copy</button>' +
            '<button class="btn btn-secondary btn-sm" onclick="openDetails(' + b.id + ')">Details</button>' +
          '</div>' +
          '<a href="' + escapeHtml(safeUrl) + '" target="_blank" rel="noopener noreferrer" class="btn btn-primary btn-sm">' +
            'Open &rarr;' +
          '</a>' +
        '</div>' +
      '</div>';
    }

    function renderListRow(b, colMap) {
      var safeUrl = sanitizeUrl(b.url);
      var domain = getDomain(b.url);
      var col = b.collectionId ? colMap[b.collectionId] : null;
      var colHtml = col ? '<span class="badge-pill">' + escapeHtml(col.name) + '</span>' : '';
      var initial = (domain ? domain.charAt(0).toUpperCase() : 'W');

      return '<div class="list-item">' +
        '<div class="list-left">' +
          '<span class="favicon-avatar" style="width:28px; height:28px; font-size:12px;">' + escapeHtml(initial) + '</span>' +
          '<div class="list-text">' +
            '<a href="' + escapeHtml(safeUrl) + '" target="_blank" rel="noopener noreferrer" class="list-title">' +
              escapeHtml(b.title || b.url) +
            '</a>' +
            '<div class="list-sub">' +
              '<span>' + escapeHtml(domain) + '</span>' +
              (b.createdAt ? '<span>&bull; Added ' + escapeHtml(formatDate(b.createdAt)) + '</span>' : '') +
              colHtml +
            '</div>' +
          '</div>' +
        '</div>' +
        '<div class="list-actions">' +
          '<button class="btn btn-secondary btn-sm" onclick="copyLink(this, \'' + escapeHtml(safeUrl) + '\')">Copy</button>' +
          '<button class="btn btn-secondary btn-sm" onclick="openDetails(' + b.id + ')">Details</button>' +
          '<a href="' + escapeHtml(safeUrl) + '" target="_blank" rel="noopener noreferrer" class="btn btn-primary btn-sm">Open</a>' +
        '</div>' +
      '</div>';
    }

    window.resetFilters = function() {
      activeFilter = null;
      searchQuery = '';
      searchInput.value = '';
      searchClearBtn.style.display = 'none';
      updateNavHighlight();
      renderFilteredBookmarks();
    };

    window.copyLink = function(btn, url) {
      if (!url || url === '#') return;
      navigator.clipboard.writeText(url).then(function() {
        var prev = btn.textContent;
        btn.textContent = 'Copied!';
        btn.style.color = 'var(--success)';
        setTimeout(function() {
          btn.textContent = prev;
          btn.style.color = '';
        }, 1800);
      });
    };

    window.openDetails = function(bId) {
      var b = allBookmarks.find(function(item) { return item.id === bId; });
      if (!b) return;
      selectedBookmark = b;

      var safeUrl = sanitizeUrl(b.url);
      modalBookmarkTitle.textContent = b.title || b.url;
      modalUrlLink.href = safeUrl;
      modalUrlLink.textContent = b.url;

      if (b.description) {
        modalDescBlock.classList.remove('hidden');
        modalDescText.textContent = b.description;
      } else {
        modalDescBlock.classList.add('hidden');
      }

      modalCollectionText.textContent = b.collectionName || 'Uncategorized';
      modalVisitsText.textContent = b.visitCount || 0;
      modalCreatedText.textContent = b.createdAt ? formatDate(b.createdAt) : 'Unknown';
      modalUpdatedText.textContent = b.updatedAt ? formatDate(b.updatedAt) : 'Never';

      modalOpenBtn.href = safeUrl;
      modalCopyBtn.onclick = function() { copyLink(modalCopyBtn, safeUrl); };

      detailModal.classList.remove('hidden');
    };

    closeDetailModalBtn.addEventListener('click', function() {
      detailModal.classList.add('hidden');
    });

    detailModal.addEventListener('click', function(e) {
      if (e.target === detailModal) detailModal.classList.add('hidden');
    });

    // View Switcher Handlers
    viewGridBtn.addEventListener('click', function() {
      currentViewMode = 'grid';
      viewGridBtn.classList.add('active');
      viewGridBtn.setAttribute('aria-pressed', 'true');
      viewListBtn.classList.remove('active');
      viewListBtn.setAttribute('aria-pressed', 'false');
      renderFilteredBookmarks();
    });

    viewListBtn.addEventListener('click', function() {
      currentViewMode = 'list';
      viewListBtn.classList.add('active');
      viewListBtn.setAttribute('aria-pressed', 'true');
      viewGridBtn.classList.remove('active');
      viewGridBtn.setAttribute('aria-pressed', 'false');
      renderFilteredBookmarks();
    });

    searchInput.addEventListener('input', function() {
      searchQuery = searchInput.value;
      searchClearBtn.style.display = searchQuery ? 'block' : 'none';
      renderFilteredBookmarks();
    });

    searchClearBtn.addEventListener('click', function() {
      searchInput.value = '';
      searchQuery = '';
      searchClearBtn.style.display = 'none';
      renderFilteredBookmarks();
      searchInput.focus();
    });

    sortSelect.addEventListener('change', function() {
      sortOrder = sortSelect.value;
      renderFilteredBookmarks();
    });

    refreshBtn.addEventListener('click', function() {
      var t = sessionStorage.getItem('pixelmarks_token');
      if (t) loadDashboard(t);
    });

    disconnectBtn.addEventListener('click', function() {
      disconnectModal.classList.remove('hidden');
    });

    cancelDisconnectBtn.addEventListener('click', function() {
      disconnectModal.classList.add('hidden');
    });

    closeDisconnectModalBtn.addEventListener('click', function() {
      disconnectModal.classList.add('hidden');
    });

    disconnectModal.addEventListener('click', function(e) {
      if (e.target === disconnectModal) disconnectModal.classList.add('hidden');
    });

    confirmDisconnectBtn.addEventListener('click', async function() {
      disconnectModal.classList.add('hidden');
      var t = sessionStorage.getItem('pixelmarks_token');
      if (t) {
        try {
          await fetch('/api/v1/revoke', {
            method: 'POST',
            headers: { 'Authorization': 'Bearer ' + t }
          });
        } catch (_) {}
      }
      sessionStorage.removeItem('pixelmarks_token');
      dashboardView.classList.add('hidden');
      refreshBtn.classList.add('hidden');
      disconnectBtn.classList.add('hidden');
      statusBadge.className = 'status-badge';
      statusText.textContent = 'Disconnected';
      pairingView.classList.remove('hidden');
      pinInputs.forEach(function(i) { i.value = ''; i.classList.remove('has-value'); });
      pinInputs[0].focus();
    });

    // Global Key Listener: Escape closes any modal
    document.addEventListener('keydown', function(e) {
      if (e.key === 'Escape') {
        detailModal.classList.add('hidden');
        disconnectModal.classList.add('hidden');
      }
    });
  </script>
</body>
</html>
"""
