/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import styled, { createGlobalStyle } from "styled-components";
import { FC, ReactNode } from "react";
import { background, bodyShort01 } from "@carbon/elements";
import AppHeader from "src/components/layout/AppHeader";
import ErrorBoundary from "src/components/global/ErrorBoundary";
import { License } from "src/utility/api/headers";

const GlobalStyle = createGlobalStyle`
  body {
    background: ${background};
    font-size: ${bodyShort01.fontSize};
    font-weight: ${bodyShort01.fontSize};
    line-height: ${bodyShort01.lineHeight};
    letter-spacing: ${bodyShort01.letterSpacing};

    margin: 0;
    padding: 0;
    box-sizing: border-box;
    -webkit-font-smoothing: antialiased;
  }
  * {
    box-sizing: border-box;
  }
`;

const AppRootWrapper = styled.div`
  height: 100vh;
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: auto 1fr;
  grid-template-areas:
    "header"
    "main";
  position: relative;
`;

const GridHeader = styled.div`
  grid-area: header;
`;

const GridMain = styled.div`
  grid-area: main;
  overflow: auto;
  position: relative;
  display: grid;
  grid-template-rows: 1fr auto;
  grid-template-columns: 1fr;
  padding-top: 48px;
`;

const GridMainContent = styled.div`
  grid-area: 1 / 1 / 1 / 4;
`;

interface Props {
  license: License | null;
  children?: ReactNode;
}

const AppContent: FC<Props> = ({ license, children }) => {
  return (
    <>
      <GridHeader>
        <AppHeader license={license} />
      </GridHeader>
      <GridMain>
        <GridMainContent>{children}</GridMainContent>
      </GridMain>
    </>
  );
};

interface Props {
  license: License | null;
  children?: ReactNode;
}

const AppRoot: FC<Props> = ({ license, children }) => {
  return (
    <AppRootWrapper>
      <ErrorBoundary>
        <GlobalStyle />
        <AppContent license={license}>{children}</AppContent>
      </ErrorBoundary>
    </AppRootWrapper>
  );
};

export default AppRoot;
