/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {Link} from '@carbon/react';
import {Container} from './styled';

const Disclaimer: React.FC = () => {
  return window.clientConfig?.isEnterprise ? null : (
    <Container>
      Non-Production License. If you would like information on production usage,
      please refer to our{' '}
      <Link
        href="https://legal.camunda.com/#self-managed-non-production-terms"
        target="_blank"
        inline
      >
        terms & conditions page
      </Link>{' '}
      or{' '}
      <Link href="https://camunda.com/contact/" target="_blank" inline>
        contact sales
      </Link>
      .
    </Container>
  );
};

export {Disclaimer};
