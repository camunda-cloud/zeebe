import {addChild, addChildren} from './jsx';
import {runUpdate} from './runUpdate';
import {updateOnlyWhenStateChanges} from './updateOnlyWhenStateChanges';
import {DESTROY_EVENT} from './events';
import {$document} from './dom';
import {createProxyNode} from './createProxyNode';

const truePredicate = () => true;

export function Match({didStateChange, children}) {
  assertAllChildrenAreCase(children);

  return (parentNode, eventsBus) => {
    let lastChild;
    let update;
    const startMarker = $document.createComment('START MATCH');

    parentNode.appendChild(startMarker);

    const proxyNode = createProxyNode(parentNode, startMarker);

    return updateOnlyWhenStateChanges(
      (state) => {
        const child = findChild(children, state);

        if (child) {
          if (child !== lastChild) {
            update = replaceChild(proxyNode, update, eventsBus, child);
            lastChild = child;
          }

          runUpdate(update, state);
        } else {
          removeChild(proxyNode, update);
          update = undefined;
          lastChild = undefined;
        }
      },
      didStateChange
    );
  };
}

function findChild(children, state) {
  return children.find(({predicate}) => predicate(state));
}

function replaceChild(proxyNode, update, eventsBus, child) {
  removeChild(proxyNode, update);

  return addChild(proxyNode, eventsBus, child, true);
}

function removeChild(proxyNode, {eventsBus} = {}) {
  proxyNode.removeChildren();

  if (eventsBus) {
    eventsBus.fireEvent(DESTROY_EVENT, {});
  }
}

function assertAllChildrenAreCase(children) {
  return children.every(({predicate}) => typeof predicate === 'function');
}

export function Case({predicate, children}) {
  const component = (node, eventsBus) => {
    return addChildren(node, eventsBus, children);
  };

  component.predicate = predicate;

  return component;
}

export function Default({children}) {
  const component = (node, eventsBus) => {
    return addChildren(node, eventsBus, children);
  };

  component.predicate = truePredicate;

  return component;
}
