import {Page, Locator} from '@playwright/test';

class ModelerHomePage {
  private page: Page;
  readonly modelerPageBanner: Locator;
  readonly createNewProjectButton: Locator;
  readonly projectNameInput: Locator;
  readonly chooseBpmnTemplateButton: Locator;
  readonly diagramTypeDropdown: Locator;
  readonly bpmnTemplateOption: Locator;
  readonly optimizeProjectFolder: Locator;
  readonly optimizeUserTaskFlowDiagram: Locator;
  readonly htoProjectFolder: Locator;
  readonly htoUserFlowDiagram: Locator;
  readonly formTemplateOption: Locator;
  readonly projectBreadcrumb: Locator;
  readonly openOrganizationsButton: Locator;
  readonly manageButton: Locator;
  readonly webModelerProjectFolder: Locator;
  readonly webModelerUserFlowDiagram: Locator;
  readonly connectorsProjectFolder: Locator;

  constructor(page: Page) {
    this.page = page;
    this.modelerPageBanner = page
      .locator('a')
      .filter({hasText: 'Camunda logoModeler'});
    this.createNewProjectButton = page.getByRole('button', {
      name: 'New project',
    });
    this.projectNameInput = page.locator('[data-test="editable-input"]');
    this.chooseBpmnTemplateButton = page.getByRole('button', {
      name: 'Choose BPMN template',
    });
    this.diagramTypeDropdown = page.locator('[data-test="diagram-dropdown"]');
    this.bpmnTemplateOption = page
      .locator('[data-test="create-bpmn-diagram"]')
      .getByText('BPMN Diagram');
    this.optimizeProjectFolder = page.getByText('Optimize Project').first();
    this.optimizeUserTaskFlowDiagram = page
      .getByText('Optimize User Task Flow')
      .first();
    this.htoProjectFolder = page.getByText('HTO Project').first();
    this.htoUserFlowDiagram = page.getByText('User_Task_Process').first();
    this.formTemplateOption = page
      .locator('[data-test="create-form"]')
      .getByText('Form');
    this.projectBreadcrumb = page.locator('[data-test="breadcrumb-project"]');
    this.openOrganizationsButton = page.getByLabel('Open Organizations');
    this.manageButton = page.getByRole('button', {name: 'Manage'});
    this.webModelerUserFlowDiagram = page
      .getByText('Web Modeler Test Diagram')
      .first();
    this.webModelerProjectFolder = page
      .getByText('Web Modeler Project')
      .first();
    this.connectorsProjectFolder = page.getByText('Connectors Project').first();
  }

  async clickCreateNewProjectButton(): Promise<void> {
    await this.createNewProjectButton.click({timeout: 30000});
  }

  async enterNewProjectName(name: string): Promise<void> {
    await this.projectNameInput.click();
    await this.projectNameInput.fill(name);
    await this.projectNameInput.press('Enter');
  }

  async clickChooseBpmnTemplateButton(): Promise<void> {
    await this.chooseBpmnTemplateButton.click();
  }

  async clickDiagramTypeDropdown(): Promise<void> {
    await this.diagramTypeDropdown.click();
  }

  async clickBpmnTemplateOption(): Promise<void> {
    await this.bpmnTemplateOption.click();
  }

  async clickOptimizeProjectFolder(): Promise<void> {
    try {
      await this.optimizeProjectFolder.click({timeout: 180000});
    } catch {
      await this.clickCreateNewProjectButton();
      await this.enterNewProjectName('Optimize Project');
    }
  }

  async clickOptimizeUserTaskFlowDiagram(): Promise<void> {
    await this.optimizeUserTaskFlowDiagram.click({timeout: 60000});
  }

  async clickHTOProjectFolder(): Promise<void> {
    try {
      await this.htoProjectFolder.click({timeout: 180000});
    } catch {
      await this.clickCreateNewProjectButton();
      await this.enterNewProjectName('HTO Project');
    }
  }

  async clickHTOUserFlowDiagram(): Promise<void> {
    await this.htoUserFlowDiagram.click({timeout: 60000});
  }

  async clickFormOption(): Promise<void> {
    await this.formTemplateOption.click();
  }

  async clickProjectBreadcrumb(): Promise<void> {
    await this.projectBreadcrumb.click();
  }

  async clickOpenOrganizationsButton(): Promise<void> {
    await this.openOrganizationsButton.click({timeout: 30000});
  }

  async clickManageButton(): Promise<void> {
    await this.manageButton.click({timeout: 30000});
  }

  async clickWebModelerProjectFolder(): Promise<void> {
    try {
      await this.webModelerProjectFolder.click({timeout: 180000});
    } catch {
      await this.clickCreateNewProjectButton();
      await this.enterNewProjectName('Web Modeler Project');
    }
  }

  async clickWebModelerUserFlowDiagram(): Promise<void> {
    await this.webModelerUserFlowDiagram.click({timeout: 60000});
  }

  async clickConnectorsProjectFolder(): Promise<void> {
    try {
      await this.connectorsProjectFolder.click({timeout: 180000});
    } catch {
      await this.clickCreateNewProjectButton();
      await this.enterNewProjectName('Connectors Project');
    }
  }
}

export {ModelerHomePage};
