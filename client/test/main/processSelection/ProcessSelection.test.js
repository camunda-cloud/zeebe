import {jsx} from 'view-utils';
import {mountTemplate, createMockComponent} from 'testHelpers';
import {expect} from 'chai';
import sinon from 'sinon';
import {ProcessSelection, __set__, __ResetDependency__} from 'main/processSelection/ProcessSelection';
import {LOADED_STATE, INITIAL_STATE} from 'utils/loading';

describe('<ProcessSelection>', () => {
  let PreviewCard;
  let loadProcessDefinitions;
  let openDefinition;
  let setVersionForProcess;
  let addDestroyEventCleanUp;
  let node;
  let update;
  let state;

  beforeEach(() => {
    PreviewCard = createMockComponent('PreviewCard');
    __set__('PreviewCard', PreviewCard);

    loadProcessDefinitions = sinon.spy();
    __set__('loadProcessDefinitions', loadProcessDefinitions);

    openDefinition = sinon.spy();
    __set__('openDefinition', openDefinition);

    setVersionForProcess = sinon.spy();
    __set__('setVersionForProcess', setVersionForProcess);

    addDestroyEventCleanUp = sinon.spy();
    __set__('addDestroyEventCleanUp', addDestroyEventCleanUp);

    ({node, update} = mountTemplate(<ProcessSelection />));
  });

  afterEach(() => {
    __ResetDependency__('PreviewCard');
    __ResetDependency__('loadProcessDefinitions');
    __ResetDependency__('openDefinition');
    __ResetDependency__('setVersionForProcess');
    __ResetDependency__('addDestroyEventCleanUp');
  });

  it('should add destroy event clean up', () => {
    expect(addDestroyEventCleanUp.called).to.eql(true);
  });

  it('should display a hint when no process Definitions are present', () => {
    update({processDefinitions:{
      state: LOADED_STATE,
      data: {
        list: []
      }
    }});

    expect(node.querySelector('.no-definitions')).to.exist;
  });

  it('should load the list of available definitions', () => {
    update({processDefinitions: {
      state: INITIAL_STATE
    }});

    expect(loadProcessDefinitions.calledOnce).to.eql(true);
  });

  describe('single version', () => {
    beforeEach(() => {
      state = {
        processDefinitions: {
          state: LOADED_STATE,
          data: {
            list: [{
              id: 'processId',
              key: 'processKey',
              name: 'processName',
              version: 4,
              bpmn20Xml: 'some xml'
            }]
          }
        }
      };

      update(state);
    });

    it('should display a preview of the definition', () => {
      expect(node.textContent).to.include('PreviewCard');
    });
  });
});
