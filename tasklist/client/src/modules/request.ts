/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {reactQueryClient} from './react-query/reactQueryClient';
import {authenticationStore} from './stores/authentication';
import {captureCsrfToken, getCsrfHeaders} from './csrf';

type RequestError =
  | {
      variant: 'network-error';
      response: null;
      networkError: unknown;
    }
  | {
      variant: 'failed-response';
      response: Response;
      networkError: null;
    };

async function request(
  input: RequestInfo,
  {skipSessionCheck} = {skipSessionCheck: false},
): Promise<
  | {
      response: Response;
      error: null;
    }
  | {
      response: null;
      error: RequestError;
    }
> {
  try {
    if (input instanceof Request) {
      Object.entries(getCsrfHeaders()).forEach(([name, value]) =>
        input.headers.set(name, value),
      );
    }

    const response = await fetch(input);

    captureCsrfToken(response);

    if (response.ok) {
      authenticationStore.activateSession();
    }

    if (!skipSessionCheck && response.status === 401) {
      authenticationStore.disableSession();
      reactQueryClient.clear();
    }

    if (response.ok) {
      return {
        response,
        error: null,
      };
    }

    return {
      response: null,
      error: {
        response,
        networkError: null,
        variant: 'failed-response',
      },
    };
  } catch (error) {
    return {
      response: null,
      error: {
        response: null,
        networkError: error,
        variant: 'network-error',
      },
    };
  }
}

function isRequestError(error: unknown): error is {
  variant: 'network-error';
  response: null;
  networkError: Error;
} {
  return (
    error !== null &&
    typeof error === 'object' &&
    'variant' in error &&
    error.variant === 'network-error'
  );
}

export {request, isRequestError};
export type {RequestError};
