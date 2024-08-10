/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import './index.css';
import Splitter, {SplitDirection} from '@devbookhq/splitter';
import {getStateLocally, storeStateLocally} from 'modules/utils/localStorage';

type Props = {
  direction: SplitDirection.Vertical | SplitDirection.Horizontal;
  minHeights?: number[];
  minWidths?: number[];
  panelId: string;
  children: React.ReactNode;
};

const ResizablePanel: React.FC<Props> = ({
  children,
  direction,
  minHeights,
  minWidths,
  panelId,
}) => {
  const cursorResizingClassName =
    direction === SplitDirection.Vertical ? 'nsResizing' : 'ewResizing';

  return (
    <Splitter
      classes={[`${direction}Panel`, `${direction}Panel`]}
      direction={direction}
      minHeights={minHeights}
      minWidths={minWidths}
      initialSizes={getStateLocally('panelStates')[panelId] ?? [50, 50]}
      gutterClassName={`custom-gutter-${direction}`}
      draggerClassName={`custom-dragger-${direction}`}
      onResizeStarted={() => {
        document.body.classList.add(cursorResizingClassName);
      }}
      onResizeFinished={(_, newSizes) => {
        storeStateLocally({[panelId]: newSizes}, 'panelStates');
        document.body.classList.remove(cursorResizingClassName);
      }}
    >
      {children}
    </Splitter>
  );
};

export {ResizablePanel, SplitDirection};
