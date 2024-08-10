/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {useEffect, useRef} from 'react';
import {observer} from 'mobx-react';
import {DecisionViewer as DmnJsDecisionViewer} from 'modules/dmn-js/DecisionViewer';
import {Container, ViewerCanvas} from './styled';

type Props = {
  xml: string | null;
  decisionViewId: string | null;
  highlightableRules?: number[];
};

const DecisionViewer: React.FC<Props> = observer(
  ({xml, decisionViewId, highlightableRules = []}) => {
    const decisionViewer = useRef<DmnJsDecisionViewer | null>(null);
    const decisionViewerRef = useRef<HTMLDivElement | null>(null);

    if (decisionViewer.current === null) {
      decisionViewer.current = new DmnJsDecisionViewer();
    }

    useEffect(() => {
      if (
        decisionViewerRef.current === null ||
        xml === null ||
        decisionViewId === null
      ) {
        return;
      }

      decisionViewer.current!.render(
        decisionViewerRef.current,
        xml,
        decisionViewId,
      );
    }, [decisionViewId, xml]);

    useEffect(() => {
      return () => {
        decisionViewer.current?.reset();
      };
    }, []);

    return (
      <Container
        $highlightableRows={highlightableRules}
        data-testid="decision-viewer"
      >
        <ViewerCanvas ref={decisionViewerRef} />
      </Container>
    );
  },
);

export {DecisionViewer};
