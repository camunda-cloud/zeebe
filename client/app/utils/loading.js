export const INITIAL_STATE = 'INITIAL';
export const LOADING_STATE = 'LOADING';
export const LOADED_STATE = 'LOADED';
export const ERROR_STATE = 'ERROR';

export function addLoading(next, ...properties) {
  const loadTypes = getLoadTypes(properties);
  const loadedTypes = getLoadedTypes(properties);
  const resetTypes = getResetTypes(properties);
  const errorTypes = getErrorTypes(properties);

  return (state, action) => {
    const loadIdx = loadTypes.indexOf(action.type);
    const loadedIdx = loadedTypes.indexOf(action.type);
    const errorIdx = errorTypes.indexOf(action.type);
    const resetIdx = resetTypes.indexOf(action.type);

    let newState = next(state, action) || {};

    newState = properties.reduce((state, prop) => {
      if (!state[prop]) {
        return {
          ...state,
          [prop]: {state: INITIAL_STATE}
        };
      }

      return state;
    }, newState);

    if (loadIdx !== -1) {
      return {
        ...newState,
        [properties[loadIdx]]: {
          state: LOADING_STATE
        }
      };
    } else if (loadedIdx !== -1) {
      return {
        ...newState,
        [properties[loadedIdx]]: {
          state: LOADED_STATE,
          data: action.result
        }
      };
    } else if (resetIdx !== -1) {
      return {
        ...newState,
        [properties[resetIdx]]: {
          state: INITIAL_STATE
        }
      };
    } else if (errorIdx !== -1) {
      return {
        ...newState,
        [properties[errorIdx]]: {
          state: ERROR_STATE,
          error: action.error
        }
      };
    }

    return newState;
  };
}

function getLoadTypes(properties) {
  return properties.map(prop => 'LOAD_' + prop.toUpperCase());
}
function getResetTypes(properties) {
  return properties.map(prop => 'RESET_' + prop.toUpperCase());
}
function getLoadedTypes(properties) {
  return properties.map(prop => 'LOADED_' + prop.toUpperCase());
}
function getErrorTypes(properties) {
  return properties.map(prop => 'ERROR_' + prop.toUpperCase());
}

export function createResetActionFunction(name) {
  return () => {
    return {
      type: 'RESET_' + name.toUpperCase()
    };
  };
}

export function createLoadingActionFunction(name) {
  return () => {
    return {
      type: 'LOAD_' + name.toUpperCase()
    };
  };
}

export function createResultActionFunction(name) {
  return result => {
    return {
      type: 'LOADED_' + name.toUpperCase(),
      result
    };
  };
}

export function createErrorActionFunction(name) {
  return error => {
    return {
      type: 'ERROR_' + name.toUpperCase(),
      error
    };
  };
}

export function isInitial({state}) {
  return state === INITIAL_STATE;
}

export function isLoading({state}) {
  return state === LOADING_STATE;
}

export function isLoaded({state}) {
  return state === LOADED_STATE;
}

export function isError({state}) {
  return state === ERROR_STATE;
}
