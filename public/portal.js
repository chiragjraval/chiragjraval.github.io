async function loadApplications() {
  const status = document.getElementById("portal-status");
  const applicationList = document.getElementById("application-list");

  try {
    const response = await fetch("./shared/config/site.json");
    if (!response.ok) {
      throw new Error(`Application registry returned HTTP ${response.status}`);
    }

    const site = await response.json();
    document.title = site.name;
    document.querySelectorAll("[data-site-name]").forEach((element) => {
      element.textContent = site.name;
    });
    document.querySelectorAll("[data-site-tagline]").forEach((element) => {
      element.textContent = site.tagline;
    });

    for (const application of site.applications) {
      const card = document.createElement("a");
      card.className = "application-card";
      card.href = application.path;

      const title = document.createElement("h3");
      title.textContent = application.name;
      const description = document.createElement("p");
      description.textContent = application.description;

      card.append(title, description);
      applicationList.append(card);
    }

    status.hidden = true;
  } catch (error) {
    console.error("Unable to load the Financial Data Lab application registry:", error);
    status.textContent = `Unable to load applications: ${error.message}`;
  }
}

loadApplications();
