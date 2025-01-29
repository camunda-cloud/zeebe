import {Page, Locator, expect} from '@playwright/test';
import {ConnectorMarketplacePage} from './ConnectorMarketplacePage';

class ModelerCreatePage {
  private page: Page;
  readonly generalPanel: Locator;
  readonly taskDefinitionPanel: Locator;
  readonly processIdInput: Locator;
  readonly elemendIdInput: Locator;
  readonly startEventElement: Locator;
  readonly appendTaskButton: Locator;
  readonly changeTypeButton: Locator;
  readonly userTaskOption: Locator;
  readonly serviceTaskOption: Locator;
  readonly appendEndEventButton: Locator;
  readonly startInstanceMainButton: Locator;
  readonly startInstanceSubButton: Locator;
  readonly viewProcessInstanceLink: Locator;
  readonly nameInput: Locator;
  readonly jobTypeInput: Locator;
  readonly diagramBreadcrumb: Locator;
  readonly editDiagramNameButton: Locator;
  readonly diagramNameInput: Locator;
  readonly variableInput: Locator;
  readonly embedFormButton: Locator;
  readonly embedButton: Locator;
  readonly newForm: Locator;
  readonly deployMainButton: Locator;
  readonly deploySubButton: Locator;
  readonly cancelButton: Locator;
  readonly restConnectorOption: Locator;
  readonly marketPlaceButton: Locator;
  readonly clientIdTextbox: Locator;
  readonly clientSecretTextbox: Locator;
  readonly rememberCredentialsCheckbox: Locator;
  readonly webhookStartEventConnectorOption: Locator;
  readonly webhookIdInput: Locator;
  readonly implementationSection: Locator;
  readonly implementationOptions: Locator;
  readonly assignmentSection: Locator;
  readonly priorityInput: Locator;
  readonly secondElement: Locator;
  readonly firstElement: Locator;
  readonly appendElementButton: Locator;
  readonly firstPlacedGateway: Locator;
  readonly secondPlacedGateway: Locator;
  readonly secondPlacedElement: Locator;
  readonly appendPreButton: Locator;
  readonly appendGatewayButton: Locator;
  readonly parallelGatewayOption: Locator;
  readonly connectToOtherElementButton: Locator;
  readonly canvas: Locator;
  readonly intermediateBoundaryEvent: Locator;
  readonly intermediateWebhookConnectorOption: Locator;
  readonly correlationKeyProcessInput: Locator;
  readonly correlationKeyPayloadInput: Locator;
  readonly playTab: Locator;
  readonly continueToPlayButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.generalPanel = page.getByTitle('General').first();
    this.taskDefinitionPanel = page.getByTitle('Task definition').first();
    this.processIdInput = page.getByLabel('ID', {exact: true});
    this.elemendIdInput = page.getByLabel('ID', {exact: true});
    this.startEventElement = page.locator('.djs-hit').first();
    this.appendPreButton = page.locator('.djs-create-pad-icon > svg').first();
    this.appendTaskButton = page.getByTitle('Append task');
    this.changeTypeButton = page.getByLabel('Change element');
    this.userTaskOption = page.getByRole('listitem', {name: 'User Task'});
    this.serviceTaskOption = page.getByRole('listitem', {name: 'Service Task'});
    this.appendEndEventButton = page.getByTitle('Append end event');
    this.appendGatewayButton = page.getByTitle('append gateway');
    this.parallelGatewayOption = page.getByText('parallel gateway');
    this.startInstanceMainButton = page.getByRole('button', {name: 'Run'});
    this.startInstanceSubButton = page.getByRole('button', {
      name: 'Start instance',
    });
    this.viewProcessInstanceLink = page.getByRole('link', {
      name: 'View process instance',
    });
    this.nameInput = page.getByLabel('Name', {exact: true});
    this.jobTypeInput = page.getByRole('textbox', {name: /job type/i});
    this.diagramBreadcrumb = page.locator('[data-test="breadcrumb-diagram"]');
    this.editDiagramNameButton = page.getByText('Edit name');
    this.diagramNameInput = page.locator('[data-test="editable-input"]');
    this.variableInput = page.getByLabel('Variables');
    this.embedFormButton = page.getByRole('button', {name: 'Link form'});
    this.embedButton = page.locator('[data-test="confirm-move"]');
    this.newForm = page.locator('[data-test="item-New form"]');
    this.deployMainButton = page.locator('[data-test="deploy-button"]');
    this.continueToPlayButton = page.getByRole('button', {name: 'Continue'});
    this.deploySubButton = page
      .getByRole('dialog')
      .getByRole('button', {name: 'Deploy'});
    this.cancelButton = page.getByRole('button', {name: 'Cancel'});
    this.restConnectorOption = page.getByRole('listitem', {
      name: 'REST Outbound Connector',
    });
    this.marketPlaceButton = page.getByTitle(
      'Browse Marketplace for more Connectors',
    );
    this.clientIdTextbox = page.getByLabel('Client ID');
    this.clientSecretTextbox = page.getByLabel('Client secret');
    this.rememberCredentialsCheckbox = page.getByText('Remember credentials');
    this.webhookStartEventConnectorOption = page.getByRole('listitem', {
      name: 'Webhook Start Event Connector',
      exact: true,
    });
    this.webhookIdInput = page.getByLabel('Webhook ID');
    this.secondElement = page.locator('[class="djs-element djs-shape"]').last();
    this.firstElement = page.locator('[class="djs-element djs-shape"]').first();
    this.implementationSection = page.locator(
      '[data-group-id="group-userTaskImplementation"]',
    );
    this.implementationOptions = page.locator(
      '#bio-properties-panel-userTaskImplementation',
    );
    this.assignmentSection = page.locator(
      '[data-group-id="group-assignmentDefinition"]',
    );
    this.priorityInput = page.getByRole('textbox', {name: 'priority'});
    this.appendElementButton = page
      .locator('[class="djs-context-pad open"]')
      .first();
    this.firstPlacedGateway = page
      .locator('[data-element-id*="Gateway"]')
      .first();
    this.secondPlacedGateway = page
      .locator('[data-element-id*="Gateway"]')
      .last();
    this.secondPlacedElement = page
      .locator('[class= "djs-element djs-shape"]')
      .last();
    this.connectToOtherElementButton = page
      .getByLabel('Connect to other element')
      .locator('path');
    this.canvas = page.locator('rect').nth(1);
    this.intermediateBoundaryEvent = page.getByTitle(
      'Append intermediate/boundary',
    );
    this.intermediateWebhookConnectorOption = page.getByRole('listitem', {
      name: 'Webhook Intermediate Event',
    });
    this.correlationKeyPayloadInput = page.getByLabel(
      'Correlation key (payload)',
    );
    this.correlationKeyProcessInput = page.getByLabel(
      'Correlation key (process)',
    );
    this.playTab = page.getByRole('tab', {
      name: 'Play',
    });
  }

  async switchToPlay(): Promise<void> {
    await this.playTab.click();
  }

  async clickGeneralPropertiesPanel(): Promise<void> {
    const isExpanded =
      (await this.generalPanel
        .locator('.bio-properties-panel-group-header open')
        .count()) > 0;
    if (!isExpanded) {
      await this.generalPanel.click({timeout: 60000});
    }
  }

  async clickIdInput(): Promise<void> {
    await this.processIdInput.click();
  }

  async clickTaskDefinitionPropertiesPanel(): Promise<void> {
    await this.taskDefinitionPanel.click();
  }

  async clickProcessIdInput(): Promise<void> {
    await this.processIdInput.click();
  }

  async fillIdInput(id: string): Promise<void> {
    await this.processIdInput.fill(id);
  }

  async clickElemendIdInput(): Promise<void> {
    await this.elemendIdInput.click();
  }

  async fillElementIdInput(id: string): Promise<void> {
    await this.elemendIdInput.fill(id);
  }

  async clickStartEventElement(): Promise<void> {
    await this.startEventElement.dblclick({force: true});
  }

  async selectStartEventElement(): Promise<void> {
    await this.startEventElement.click({force: true});
  }

  async clickAppendTaskButton(): Promise<void> {
    await this.appendPreButton.hover({timeout: 5000});
    await this.appendTaskButton.click({timeout: 90000});
  }

  async clickAppendGatewayButton(): Promise<void> {
    await this.appendGatewayButton.click({timeout: 90000});
  }

  async clickChangeTypeButton(): Promise<void> {
    try {
      await this.changeTypeButton.click({timeout: 180000, force: true});
    } catch {
      await this.page.reload();
      if (await this.secondElement.isVisible({timeout: 6000})) {
        await this.secondElement.click({timeout: 90000, force: true});
        await this.changeTypeButton.click({timeout: 90000, force: true});
      } else {
        await this.firstElement.click({timeout: 90000, force: true});
        await this.changeTypeButton.click({timeout: 90000, force: true});
      }
    }
  }

  async clickUserTaskOption(): Promise<void> {
    const maxRetries = 3;

    for (let retries = 0; retries < maxRetries; retries++) {
      try {
        if (retries === 0) {
          await this.userTaskOption.click({timeout: 30000, force: true});
        } else {
          await this.changeTypeButton.click({force: true, timeout: 30000});
          await this.userTaskOption.click({timeout: 30000, force: true});
        }
        return;
      } catch (error) {
        console.error(`Click attempt ${retries + 1} failed: ${error}`);
        await new Promise((resolve) => setTimeout(resolve, 10000));
      }
    }

    throw new Error(`Failed to click the button after ${maxRetries} attempts.`);
  }

  async clickServiceTaskOption(): Promise<void> {
    const maxRetries = 3;

    for (let retries = 0; retries < maxRetries; retries++) {
      try {
        if (retries === 0) {
          await this.serviceTaskOption.click({timeout: 30000, force: true});
        } else {
          await this.changeTypeButton.click({force: true, timeout: 30000});
          await this.serviceTaskOption.click({timeout: 30000, force: true});
        }
        return;
      } catch (error) {
        console.error(`Click attempt ${retries + 1} failed: ${error}`);
        await new Promise((resolve) => setTimeout(resolve, 10000));
      }
    }

    throw new Error(`Failed to click the button after ${maxRetries} attempts.`);
  }

  async clickAppendEndEventButton(parentElement?: string): Promise<void> {
    try {
      await this.appendEndEventButton.click({timeout: 120000});
    } catch (error) {
      await this.page.reload();
      const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));
      await sleep(10000);
      if (parentElement == undefined) {
        await this.clickSecondPlacedElement();
      } else if (parentElement == 'parallelGateway') {
        await this.clickSecondPlacedGateway();
      }
      await this.appendElementButton.hover();
      await this.appendEndEventButton.click({timeout: 120000});
    }
  }

  async clickFirstPlacedElement(): Promise<void> {
    await this.firstElement.click({timeout: 90000});
  }

  async clickSecondPlacedElement(): Promise<void> {
    await this.secondPlacedElement.click({timeout: 60000});
  }

  async clickFirstPlacedGateway(): Promise<void> {
    try {
      await this.firstPlacedGateway.click({timeout: 60000});
      await this.appendElementButton.isVisible({timeout: 60000});
    } catch (error) {
      await this.page.reload();
      const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));
      await sleep(10000);
      await this.firstPlacedGateway.click({timeout: 60000});
    }
  }
  async clickSecondPlacedGateway(): Promise<void> {
    try {
      await this.secondPlacedGateway.click({timeout: 60000});
      await this.appendElementButton.isVisible({timeout: 60000});
    } catch (error) {
      await this.page.reload();
      const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));
      await sleep(10000);
      await this.secondPlacedGateway.click({timeout: 60000});
    }
  }

  async clickStartInstanceMainButton(): Promise<void> {
    await this.startInstanceMainButton.click({timeout: 120000});
  }

  async clickStartInstanceSubButton(): Promise<void> {
    await this.startInstanceSubButton.click({timeout: 120000});
  }

  async clickViewProcessInstanceLink(): Promise<void> {
    await this.viewProcessInstanceLink.click({timeout: 90000});
  }

  async clickNameInput(): Promise<void> {
    await this.nameInput.click();
  }

  async fillNameInput(name: string): Promise<void> {
    await this.nameInput.fill(name);
  }

  async clickJobTypeInput(): Promise<void> {
    await this.jobTypeInput.click();
  }

  async fillJobTypeInput(name: string): Promise<void> {
    await this.jobTypeInput.fill(name);
  }

  async clickDiagramBreadcrumb(): Promise<void> {
    await this.diagramBreadcrumb.click({timeout: 60000});
  }

  async clickEditDiagramNameButton(): Promise<void> {
    await this.editDiagramNameButton.click();
  }

  async enterDiagramName(name: string): Promise<void> {
    await this.diagramNameInput.fill(name, {timeout: 60000});
    await this.diagramNameInput.press('Enter', {timeout: 60000});
  }

  async clickVariableInput(): Promise<void> {
    await this.variableInput.click();
  }

  async fillVariableInput(variable: string): Promise<void> {
    await this.variableInput.fill(variable);
  }

  async clickEmbedFormButton(): Promise<void> {
    await this.embedFormButton.click({timeout: 90000});
  }

  async clickEmbedButton(): Promise<void> {
    await this.embedButton.click();
  }

  async clickNewForm(): Promise<void> {
    await this.newForm.click({timeout: 120000});
  }

  async clickDeployMainButton(): Promise<void> {
    await this.deployMainButton.click({timeout: 90000});
  }

  async clickDeploySubButton(): Promise<void> {
    await this.deploySubButton.click({timeout: 90000});
  }

  async clickCancelButton(): Promise<void> {
    await this.cancelButton.click();
  }

  async clickRestConnectorOption(): Promise<void> {
    const maxRetries = 3;

    for (let retries = 0; retries < maxRetries; retries++) {
      try {
        if (retries === 0) {
          // First attempt
          await this.restConnectorOption.click({timeout: 60000});
        } else if (retries === 1) {
          // Second attempt
          await this.changeTypeButton.click({force: true, timeout: 60000});
          await this.restConnectorOption.click({timeout: 90000});
        } else {
          // Third and subsequent attempts
          await this.clickMarketPlaceButton();
          const connectorMarketplacePage = new ConnectorMarketplacePage(
            this.page,
          );
          await connectorMarketplacePage.clickSearchForConnectorTextbox();
          await connectorMarketplacePage.fillSearchForConnectorTextbox(
            'REST Connector',
          );
          const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));
          await sleep(10000);
          await connectorMarketplacePage.downloadConnectorToProject();
          await this.clickChangeTypeButton();
          await this.restConnectorOption.click({timeout: 120000});
        }
        return;
      } catch (error) {
        console.error(`Click attempt ${retries + 1} failed: ${error}`);
        await new Promise((resolve) => setTimeout(resolve, 10000));
      }
    }

    throw new Error(`Failed to click the button after ${maxRetries} attempts.`);
  }

  async clickMarketPlaceButton(): Promise<void> {
    const maxRetries = 3;

    for (let retries = 0; retries < maxRetries; retries++) {
      try {
        if (retries === 0) {
          await this.marketPlaceButton.click({timeout: 30000});
        } else {
          await this.changeTypeButton.click({force: true, timeout: 30000});
          await this.marketPlaceButton.click({timeout: 30000});
        }
        return;
      } catch (error) {
        console.error(`Click attempt ${retries + 1} failed: ${error}`);
        await new Promise((resolve) => setTimeout(resolve, 10000));
      }
    }

    throw new Error(`Failed to click the button after ${maxRetries} attempts.`);
  }

  async completeDeploymentEndpointConfiguration(): Promise<void> {
    await expect(this.clientIdTextbox).toBeVisible({timeout: 30000});
    await this.clientIdTextbox.click({timeout: 30000});
    await this.clientIdTextbox.fill('test', {timeout: 30000});
    await this.clientSecretTextbox.click({timeout: 30000});
    await this.clientSecretTextbox.fill(
      process.env.DISTRO_QA_E2E_TESTS_KEYCLOAK_CLIENTS_SECRET!,
    );
    await this.rememberCredentialsCheckbox.click({timeout: 30000});
  }

  async completePlayConfiguration(): Promise<void> {
    const timeout: number = 30000;
    let attempts = 0;
    const maxRetries = 2;

    while (attempts < maxRetries) {
      try {
        if (attempts > 0) await this.switchToPlay(); // Call switchToPlay on subsequent attempts
        await this.continueToPlayButton.click({timeout});
        await expect(
          this.page.getByText('Play environment is ready'),
        ).toBeVisible({timeout: 90000});

        return; // Exit if successful
      } catch (error) {
        if (attempts >= maxRetries - 1) throw error;
        await this.page.reload();
        attempts++;
      }
    }
  }

  async clickWebhookStartEventConnectorOption(): Promise<void> {
    const maxRetries = 3;

    for (let retries = 0; retries < maxRetries; retries++) {
      try {
        if (retries === 0) {
          await this.webhookStartEventConnectorOption.click({timeout: 30000});
        } else {
          await this.changeTypeButton.click({force: true, timeout: 30000});
          await this.webhookStartEventConnectorOption.click({timeout: 90000});
        }
        return;
      } catch (error) {
        console.error(`Click attempt ${retries + 1} failed: ${error}`);
        await new Promise((resolve) => setTimeout(resolve, 10000));
      }
    }

    throw new Error(`Failed to click the button after ${maxRetries} attempts.`);
  }

  async clickWebhookIdInput(): Promise<void> {
    await this.webhookIdInput.click();
  }

  async clearWebhookIdInput(): Promise<void> {
    await this.webhookIdInput.clear();
  }

  async fillWebhookIdInput(webhookId: string): Promise<void> {
    await this.webhookIdInput.fill(webhookId);
  }

  async hoverOnStartEvent(): Promise<void> {
    await this.startEventElement.hover();
  }

  async instanceStartedAssertion(): Promise<void> {
    try {
      await expect(this.page.getByText('Instance started!')).toBeVisible({
        timeout: 180000,
      });
    } catch {
      await this.clickStartInstanceMainButton();
      await this.completeDeploymentEndpointConfiguration();
      await this.clickStartInstanceSubButton();
      await expect(this.page.getByText('Instance started!')).toBeVisible({
        timeout: 180000,
      });
    }
  }

  async assertImplementationOption(implementationType: string): Promise<void> {
    await this.expandImplementationOptionIfNecessary();
    expect(await this.implementationOptions.inputValue()).toEqual(
      implementationType,
    );
  }

  async chooseImplementationOption(implementationType: string): Promise<void> {
    const isExpanded =
      (await this.implementationSection
        .locator('.bio-properties-panel-group-header open')
        .count()) > 0;
    if (!isExpanded) {
      await this.implementationSection.click();
    }
    await this.implementationOptions.selectOption(implementationType);
  }

  private async expandImplementationOptionIfNecessary(): Promise<void> {
    const isExpanded =
      (await this.implementationSection
        .locator('.bio-properties-panel-group-header empty open')
        .count()) > 0;

    if (!isExpanded) {
      await this.implementationSection.click();
    }
  }

  async clickAppendElementButton(): Promise<void> {
    try {
      const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));
      await sleep(10000);
      await this.appendPreButton.hover({timeout: 60000, force: true});
    } catch (error) {
      const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));
      await sleep(10000);
      await this.appendPreButton.hover({timeout: 90000, force: true});
    }
  }

  async clickParallelGatewayOption(): Promise<void> {
    await this.parallelGatewayOption.click({timeout: 90000});
  }
  async clickConnectToOtherElementButton(): Promise<void> {
    await this.connectToOtherElementButton.click({timeout: 60000});
  }
  async clickPriorityInput(): Promise<void> {
    try {
      await this.priorityInput.click();
    } catch (error) {
      await this.assignmentSection.click({timeout: 60000});
      await this.priorityInput.click({timeout: 60000});
    }
  }
  async fillPriorityInput(priority: string): Promise<void> {
    await this.priorityInput.fill(priority, {timeout: 60000});
  }
  async clickCanvas(): Promise<void> {
    await this.canvas.click({timeout: 60000});
  }
  async addParallelUserTasks(
    numberOfTasks: number,
    taskName: string,
  ): Promise<void> {
    try {
      await this.clickAppendElementButton();
    } catch (error) {
      await this.page.reload();
      const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));
      await sleep(10000);
      await this.clickFirstPlacedElement();
      await this.appendElementButton.hover();
    }
    await this.clickAppendGatewayButton();
    await this.clickChangeTypeButton();
    await this.clickParallelGatewayOption();
    await this.clickAppendElementButton();
    await this.clickAppendTaskButton();
    await this.clickChangeTypeButton();
    await this.clickUserTaskOption();
    await this.clickGeneralPropertiesPanel();
    await this.clickNameInput();
    await this.fillNameInput(taskName + '1');
    await this.clickIdInput();
    await this.fillIdInput(taskName + '1');
    await this.assertImplementationOption('zeebeUserTask');
    await this.clickAppendElementButton();
    await this.clickAppendGatewayButton();
    await this.clickChangeTypeButton();
    await this.clickParallelGatewayOption();
    for (let i = 2; i <= numberOfTasks; i++) {
      await this.clickFirstPlacedGateway();
      await this.clickAppendElementButton();
      await this.clickAppendTaskButton();
      await this.clickChangeTypeButton();
      await this.clickUserTaskOption();
      await this.clickNameInput();
      await this.fillNameInput(taskName + i);
      await this.clickIdInput();
      await this.fillIdInput(taskName + i);
      await this.assertImplementationOption('zeebeUserTask');
      await this.clickConnectToOtherElementButton();
      await this.clickSecondPlacedGateway();
      await this.clickCanvas();
    }
    await this.clickSecondPlacedGateway();
  }
  async setPriority(priority: number, numberOfTasks: number): Promise<void> {
    for (let i = 1; i <= numberOfTasks; i++) {
      const userTaskId = 'priorityTest' + i;
      const priorityUserTask = this.page.locator(
        `g[data-element-id="${userTaskId}"]`,
      );
      await priorityUserTask.click();
      await this.clickPriorityInput();
      await this.fillPriorityInput(priority.toString());
      await this.clickCanvas();
      priority += 25;
    }
  }

  async clickIntermediateBoundaryEvent(): Promise<void> {
    await this.intermediateBoundaryEvent.click({timeout: 60000});
  }

  async clickIntermediateWebhookConnectorOption(): Promise<void> {
    await this.intermediateWebhookConnectorOption.click({timeout: 60000});
  }

  async clickCorrelationKeyProcessInput(): Promise<void> {
    await this.correlationKeyProcessInput.click({timeout: 60000});
  }

  async fillCorrelationKeyProcessInput(
    correlationKeyProcess: string,
  ): Promise<void> {
    await this.correlationKeyProcessInput.fill(correlationKeyProcess);
  }

  async clickCorrelationKeyPayloadInput(): Promise<void> {
    await this.correlationKeyPayloadInput.click({timeout: 60000});
  }

  async fillCorrelationKeyPayloadInput(
    correlationKeyPayload: string,
  ): Promise<void> {
    await this.correlationKeyPayloadInput.fill(correlationKeyPayload);
  }
}

export {ModelerCreatePage};
