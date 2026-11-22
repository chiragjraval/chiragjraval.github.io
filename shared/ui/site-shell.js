import { siteConfig } from "../config/site.js";

const shellStyles = `
  :host {
    --shell-sidebar-width: 15rem;
    color: var(--fdl-color-text, #17324d);
    display: block;
    font-family: var(--fdl-font-family, "Segoe UI", Arial, sans-serif);
    min-height: 100vh;
  }

  * {
    box-sizing: border-box;
  }

  .layout {
    background: var(--fdl-color-canvas, #f3f7fb);
    display: grid;
    grid-template-columns: var(--shell-sidebar-width) minmax(0, 1fr);
    min-height: 100vh;
  }

  .sidebar {
    background: var(--fdl-color-surface, #fff);
    border-right: 1px solid var(--fdl-color-border, #d7e2ec);
    height: 100vh;
    overflow-y: auto;
    padding: 1rem 0.75rem;
    position: sticky;
    top: 0;
  }

  .brand {
    align-items: center;
    color: var(--fdl-color-text, #17324d);
    display: flex;
    font-size: 0.95rem;
    font-weight: 700;
    gap: 0.65rem;
    margin: 0 0 1.25rem;
    padding: 0.35rem 0.5rem;
    text-decoration: none;
  }

  .brand-mark {
    align-items: center;
    background: var(--fdl-color-brand-strong, #0b3c70);
    border-radius: 0.4rem;
    color: #fff;
    display: inline-flex;
    font-size: 0.72rem;
    height: 1.75rem;
    justify-content: center;
    letter-spacing: 0.02em;
    width: 1.75rem;
  }

  .nav-label {
    color: var(--fdl-color-muted, #587086);
    font-size: 0.68rem;
    font-weight: 700;
    letter-spacing: 0.07em;
    margin: 0 0 0.35rem;
    padding: 0 0.6rem;
    text-transform: uppercase;
  }

  nav {
    display: grid;
    gap: 0.15rem;
  }

  .nav-link {
    border-radius: 0.4rem;
    color: var(--fdl-color-text, #17324d);
    display: block;
    font-size: 0.875rem;
    line-height: 1.35;
    padding: 0.55rem 0.6rem;
    text-decoration: none;
  }

  .nav-link[data-level="1"] {
    margin-left: 0.75rem;
  }

  .nav-link:hover {
    background: var(--fdl-color-canvas, #f3f7fb);
  }

  .nav-link[aria-current="page"] {
    background: #e8f1f8;
    color: var(--fdl-color-brand-strong, #0b3c70);
    font-weight: 700;
  }

  a:focus-visible,
  button:focus-visible {
    outline: 3px solid var(--fdl-color-accent, #38a3a5);
    outline-offset: 2px;
  }

  .content {
    min-width: 0;
    padding: 1.25rem 1.5rem;
  }

  .mobile-header,
  .backdrop {
    display: none;
  }

  @media (max-width: 760px) {
    .layout {
      display: block;
    }

    .mobile-header {
      align-items: center;
      background: var(--fdl-color-surface, #fff);
      border-bottom: 1px solid var(--fdl-color-border, #d7e2ec);
      display: flex;
      gap: 0.75rem;
      min-height: 3.5rem;
      padding: 0 0.85rem;
      position: sticky;
      top: 0;
      z-index: 12;
    }

    .menu-button {
      align-items: center;
      background: transparent;
      border: 1px solid var(--fdl-color-border, #d7e2ec);
      border-radius: 0.4rem;
      color: var(--fdl-color-text, #17324d);
      cursor: pointer;
      display: inline-flex;
      font: inherit;
      font-size: 0.85rem;
      gap: 0.5rem;
      min-height: 2.5rem;
      padding: 0.35rem 0.65rem;
    }

    .menu-icon {
      display: grid;
      gap: 3px;
      width: 0.95rem;
    }

    .menu-icon span {
      background: currentColor;
      height: 2px;
      width: 100%;
    }

    .mobile-brand {
      color: var(--fdl-color-text, #17324d);
      font-size: 0.9rem;
      font-weight: 700;
      text-decoration: none;
    }

    .sidebar {
      height: calc(100dvh - 3.5rem);
      left: 0;
      max-width: 84vw;
      position: fixed;
      top: 3.5rem;
      transform: translateX(-102%);
      transition: transform 140ms ease, visibility 140ms ease;
      visibility: hidden;
      width: var(--shell-sidebar-width);
      z-index: 11;
    }

    .sidebar.is-open {
      transform: translateX(0);
      visibility: visible;
    }

    .backdrop {
      background: #10243b66;
      border: 0;
      display: block;
      inset: 3.5rem 0 0;
      padding: 0;
      position: fixed;
      visibility: hidden;
      z-index: 10;
    }

    .backdrop.is-open {
      visibility: visible;
    }

    .content {
      padding: 1rem 0.85rem;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    *,
    *::before,
    *::after {
      scroll-behavior: auto !important;
      transition-duration: 0.01ms !important;
    }
  }
`;

function normalizePath(path) {
  if (!path || path === "/") {
    return "/";
  }

  return `/${path.replace(/^\/+|\/+$/g, "")}/`;
}

class FdlSiteShell extends HTMLElement {
  constructor() {
    super();
    this.attachShadow({ mode: "open" });
    this.handleKeyDown = this.handleKeyDown.bind(this);
  }

  connectedCallback() {
    if (this.shadowRoot.childElementCount > 0) {
      return;
    }

    const style = document.createElement("style");
    style.textContent = shellStyles;

    const layout = document.createElement("div");
    layout.className = "layout";

    const mobileHeader = document.createElement("header");
    mobileHeader.className = "mobile-header";

    const menuButton = document.createElement("button");
    menuButton.className = "menu-button";
    menuButton.type = "button";
    menuButton.setAttribute("aria-expanded", "false");
    menuButton.setAttribute("aria-controls", "site-navigation");

    const menuIcon = document.createElement("span");
    menuIcon.className = "menu-icon";
    menuIcon.setAttribute("aria-hidden", "true");
    for (let index = 0; index < 3; index += 1) {
      menuIcon.append(document.createElement("span"));
    }

    const menuText = document.createElement("span");
    menuText.textContent = "Menu";
    menuButton.append(menuIcon, menuText);

    const mobileBrand = document.createElement("a");
    mobileBrand.className = "mobile-brand";
    mobileBrand.href = "/";
    mobileBrand.textContent = siteConfig.name;
    mobileHeader.append(menuButton, mobileBrand);

    const sidebar = document.createElement("aside");
    sidebar.className = "sidebar";
    sidebar.id = "site-navigation";

    const brand = document.createElement("a");
    brand.className = "brand";
    brand.href = "/";

    const mark = document.createElement("span");
    mark.className = "brand-mark";
    mark.setAttribute("aria-hidden", "true");
    mark.textContent = "CR";

    const brandName = document.createElement("span");
    brandName.textContent = siteConfig.name;
    brand.append(mark, brandName);

    const navLabel = document.createElement("p");
    navLabel.className = "nav-label";
    navLabel.textContent = "Workspace";

    const nav = document.createElement("nav");
    nav.setAttribute("aria-label", "Site navigation");
    const currentPath = normalizePath(this.getAttribute("active-path") || window.location.pathname);

    for (const item of siteConfig.navigation) {
      const link = document.createElement("a");
      link.className = "nav-link";
      link.href = item.path;
      link.textContent = item.label;
      link.dataset.level = String(item.level || 0);

      if (normalizePath(item.path) === currentPath) {
        link.setAttribute("aria-current", "page");
      }

      link.addEventListener("click", () => this.closeMenu());
      nav.append(link);
    }

    sidebar.append(brand, navLabel, nav);

    const backdrop = document.createElement("button");
    backdrop.className = "backdrop";
    backdrop.type = "button";
    backdrop.tabIndex = -1;
    backdrop.setAttribute("aria-label", "Close site navigation");
    backdrop.addEventListener("click", () => this.closeMenu());

    const content = document.createElement("main");
    content.className = "content";
    const slot = document.createElement("slot");
    content.append(slot);

    layout.append(mobileHeader, sidebar, backdrop, content);
    this.shadowRoot.append(style, layout);

    menuButton.addEventListener("click", () => {
      if (menuButton.getAttribute("aria-expanded") === "true") {
        this.closeMenu();
      } else {
        this.openMenu();
      }
    });

    this.shadowRoot.addEventListener("keydown", this.handleKeyDown);
    this.menuButton = menuButton;
    this.sidebar = sidebar;
    this.backdrop = backdrop;
  }

  handleKeyDown(event) {
    if (event.key === "Escape" && this.menuButton?.getAttribute("aria-expanded") === "true") {
      this.closeMenu();
      this.menuButton.focus();
    }
  }

  openMenu() {
    this.sidebar.classList.add("is-open");
    this.backdrop.classList.add("is-open");
    this.menuButton.setAttribute("aria-expanded", "true");
  }

  closeMenu() {
    if (!this.sidebar || !this.menuButton) {
      return;
    }

    this.sidebar.classList.remove("is-open");
    this.backdrop.classList.remove("is-open");
    this.menuButton.setAttribute("aria-expanded", "false");
  }
}

if (!customElements.get("fdl-site-shell")) {
  customElements.define("fdl-site-shell", FdlSiteShell);
}
