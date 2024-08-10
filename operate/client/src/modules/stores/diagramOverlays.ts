/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {makeAutoObservable} from 'mobx';

type Overlay = {
  payload?: unknown;
  container: HTMLElement;
  flowNodeId: string;
  type: string;
};

type State = {
  overlays: Overlay[];
};

const DEFAULT_STATE: State = {
  overlays: [],
};

class DiagramOverlays {
  state: State = {...DEFAULT_STATE};

  constructor() {
    makeAutoObservable(this);
  }

  addOverlay = (overlay: Overlay) => {
    this.state.overlays.push(overlay);
  };

  removeOverlay = (overlayType: Overlay['type']) => {
    this.state.overlays = this.state.overlays.filter(
      ({type}) => type !== overlayType,
    );
  };

  reset = () => {
    this.state = {...DEFAULT_STATE};
  };
}

export const diagramOverlaysStore = new DiagramOverlays();
