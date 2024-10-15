/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {SVGProps} from 'react';

export default function OptimizeDashboard(props: SVGProps<SVGSVGElement>): JSX.Element {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="currentColor"
      {...props}
    >
      <path
        fill-rule="evenodd"
        d="M21,3 L3,3 C1.9,3 1,3.9 1,5 L1,17 C1,18.1 1.9,19 3,19 L10,19 L10,21 L8,21 L8,23 L16,23 L16,21 L14,21 L14,19 L21,19 C22.1,19 23,18.1 23,17 L23,5 C23,3.9 22.1,3 21,3 Z M21,17 L3,17 L3,5 L21,5 L21,17 Z"
      />
      <path d="M17 6.79121L13.2195 7.09613L13.9516 8.1606L12.2287 9.45889L11.9848 9.65712L12 9.90101L12.3244 13.7934L10.2317 13.5595H10.1707L10.125 13.5747L7 15.2821L10.1946 13.9151L12.5945 14.4132L12.9299 14.4894V14.1082L13.0151 10.1613L14.6427 9.16534L15.3689 10.2211L17 6.79121Z" />
    </svg>
  );
}
