/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {Stack as BaseStack, Checkbox as BaseCheckbox} from '@carbon/react';
import styled from 'styled-components';

const Stack = styled(BaseStack)`
  align-items: center;
`;

const CheckBox = styled(BaseCheckbox)`
  label {
    padding-top: 2px;
  }
`;

export {Stack, CheckBox};
