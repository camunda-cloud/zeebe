import {Page, Locator, expect} from '@playwright/test';

function cardinalToOrdinal(numberValue: number): string {
  const realOrderIndex = numberValue.toString();

  if (['11', '12', '13'].includes(realOrderIndex.slice(-2))) {
    return `${realOrderIndex}th`;
  }

  switch (realOrderIndex.slice(-1)) {
    case '1':
      return `${realOrderIndex}st`;
    case '2':
      return `${realOrderIndex}nd`;
    case '3':
      return `${realOrderIndex}rd`;
    default:
      return `${realOrderIndex}th`;
  }
}

class TaskDetailsPage {
  private page: Page;
  readonly assignToMeButton: Locator;
  readonly completeButton: Locator;
  readonly unassignButton: Locator;
  readonly assignee: Locator;
  readonly completeTaskButton: Locator;
  readonly addVariableButton: Locator;
  readonly detailsPanel: Locator;
  readonly detailsHeader: Locator;
  readonly pendingTaskDescription: Locator;
  readonly pickATaskHeader: Locator;
  readonly emptyTaskMessage: Locator;
  readonly nameInput: Locator;
  readonly addressInput: Locator;
  readonly ageInput: Locator;
  readonly variablesTable: Locator;
  readonly nameColumnHeader: Locator;
  readonly valueColumnHeader: Locator;
  readonly form: Locator;
  readonly numberInput: Locator;
  readonly incrementButton: Locator;
  readonly decrementButton: Locator;
  readonly dateInput: Locator;
  readonly timeInput: Locator;
  readonly checkbox: Locator;
  readonly selectDropdown: Locator;
  readonly tagList: Locator;
  readonly detailsInfo: Locator;
  readonly textInput: Locator;
  readonly assignedToMeText: Locator;
  readonly textBox: Locator;

  constructor(page: Page) {
    this.page = page;
    this.assignToMeButton = page.getByRole('button', {name: 'Assign to me'});
    this.completeButton = page.getByRole('button', {name: 'Complete'});
    this.unassignButton = page.getByRole('button', {name: 'Unassign'});
    this.assignee = page.getByTestId('assignee');
    this.completeTaskButton = page.getByRole('button', {name: 'Complete Task'});
    this.addVariableButton = page.getByRole('button', {name: 'Add Variable'});
    this.detailsPanel = this.page.getByRole('complementary', {
      name: 'Task details right panel',
    });
    this.detailsHeader = page.getByTitle('Task details header');
    this.pendingTaskDescription = page.getByText('Pending task');
    this.pickATaskHeader = page.getByRole('heading', {
      name: 'Pick a task to work on',
    });
    this.emptyTaskMessage = page.getByRole('heading', {
      name: /task has no variables/i,
    });
    this.nameInput = page.getByLabel('Full Name');
    this.addressInput = page.getByLabel('Address*');
    this.ageInput = page.getByLabel('Age');
    this.variablesTable = page.getByTestId('variables-table');
    this.nameColumnHeader = this.variablesTable.getByRole('columnheader', {
      name: 'Name',
    });
    this.valueColumnHeader = this.variablesTable.getByRole('columnheader', {
      name: 'Value',
    });
    this.form = page.getByTestId('embedded-form');
    this.numberInput = this.form.getByLabel('Count');
    this.incrementButton = page.getByRole('button', {name: 'Increment'});
    this.decrementButton = page.getByRole('button', {name: 'Decrement'});
    this.dateInput = page.getByLabel('Date of Birth');
    this.timeInput = page.getByPlaceholder('hh:mm ?m');
    this.checkbox = this.form.getByLabel('Agree');
    this.selectDropdown = this.form.getByText('Select').last();
    this.tagList = page.getByPlaceholder('Search');
    this.detailsInfo = page.getByTestId('details-info');
    this.textInput = page.locator('[class="fjs-input"]');
    this.assignedToMeText = this.page
      .getByTestId('assignee')
      .getByText('Assigned to me');
    this.textBox = this.page.getByLabel('Text Box');
  }

  async clickAssignToMeButton() {
    await this.assignToMeButton.click({timeout: 60000});
    await expect(this.page.getByText('Assigning...')).not.toBeVisible({
      timeout: 180000,
    });
    await expect(this.assignedToMeText).toBeVisible({
      timeout: 60000,
    });
  }

  async clickUnassignButton() {
    await this.unassignButton.click();
  }

  async clickCompleteTaskButton() {
    await this.completeTaskButton.click({timeout: 120000});
  }

  async clickAddVariableButton() {
    await this.addVariableButton.click();
  }

  async replaceExistingVariableValue(values: {name: string; value: string}) {
    const {name, value} = values;
    await this.page.getByTitle(name).clear();
    await this.page.getByTitle(name).fill(value);
  }

  getNthVariableNameInput(nth: number) {
    return this.page.getByRole('textbox', {
      name: `${cardinalToOrdinal(nth)} variable name`,
    });
  }

  getNthVariableValueInput(nth: number) {
    return this.page.getByRole('textbox', {
      name: `${cardinalToOrdinal(nth)} variable value`,
    });
  }

  async addVariable(payload: {name: string; value: string}) {
    const {name, value} = payload;

    this.clickAddVariableButton();
    await this.getNthVariableNameInput(1).fill(name);
    await this.getNthVariableValueInput(1).fill(value);
  }

  async fillNumber(number: string): Promise<void> {
    await this.numberInput.fill(number);
  }

  async clickIncrementButton(): Promise<void> {
    await this.incrementButton.click();
  }

  async clickDecrementButton(): Promise<void> {
    await this.decrementButton.click();
  }

  async fillDate(date: string): Promise<void> {
    await this.dateInput.click();
    await this.dateInput.fill(date);
    await this.dateInput.press('Enter');
  }

  async enterTime(time: string): Promise<void> {
    await this.timeInput.click();
    await this.page.getByText(time).click();
  }

  async checkCheckbox(): Promise<void> {
    await this.checkbox.check();
  }

  async selectDropdownValue(value: string): Promise<void> {
    await this.selectDropdown.click();
    await this.page.getByText(value).click();
  }

  async clickRadioButton(radioBtnLabel: string): Promise<void> {
    await this.page.getByText(radioBtnLabel).click();
  }

  async checkChecklistBox(label: string): Promise<void> {
    await this.page.getByLabel(label).check();
  }

  async enterTwoValuesInTagList(value1: string, value2: string): Promise<void> {
    await this.tagList.click();
    await this.page.getByText(value1).click();
    await this.page.getByText(value2, {exact: true}).click();
  }

  async clickTextInput(): Promise<void> {
    const maxRetries = 10;
    let attempts = 0;

    while (attempts < maxRetries) {
      try {
        await this.textInput.click({timeout: 90000});
        break;
      } catch (error) {
        attempts += 1;

        if (attempts < maxRetries) {
          console.warn(
            `Attempt ${attempts} failed. Reloading page and retrying...`,
          );
          const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));
          await sleep(10000);
          await this.page.reload();
        } else {
          throw new Error(
            `Failed to click text input after ${maxRetries} attempts`,
          );
        }
      }
    }
  }

  async fillTextInput(value: string): Promise<void> {
    await this.textInput.fill(value, {timeout: 90000});
  }

  async fillTextBox(text: string): Promise<void> {
    await this.textBox.fill(text);
  }
  async fillName(nameInput: string): Promise<void> {
    await this.nameInput.fill(nameInput);
  }
}
export {TaskDetailsPage};
