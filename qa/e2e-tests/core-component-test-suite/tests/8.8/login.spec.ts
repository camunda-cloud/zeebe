import {test} from '@fixtures/8.8';
import {navigateToApp} from '@pages/8.7/UtilitiesPage';
import {captureScreenshot, captureFailureVideo} from '@setup';

test.skip('Login Tests', () => {
  test.afterEach(async ({page}, testInfo) => {
    await captureScreenshot(page, testInfo);
    await captureFailureVideo(page, testInfo);
  });

  test('Basic Login on Operate', async ({
    page,
    operateLoginPage,
    operateHomePage,
  }) => {
    await navigateToApp(page, 'operate');
    await operateLoginPage.login('demo', 'demo');
    await operateHomePage.operateBannerIsVisible();
  });

  test('Basic Login on TaskList', async ({
    page,
    taskListLoginPage,
    taskPanelPage,
  }) => {
    await navigateToApp(page, 'tasklist');
    await taskListLoginPage.login('demo', 'demo');
    await taskPanelPage.taskListBannerIsVisible();
  });
});
