/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {
  type UseQueryOptions,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import {api} from 'modules/api';
import {type RequestError, request} from 'modules/request';
import type {UserTask} from '@vzeta/camunda-api-zod-schemas/tasklist';

function getUseTaskQueryKey(userTaskKey: UserTask['userTaskKey']) {
  return ['task', userTaskKey];
}

function useTask(
  userTaskKey: UserTask['userTaskKey'],
  options?: Pick<
    UseQueryOptions<UserTask, RequestError | Error>,
    | 'enabled'
    | 'refetchOnWindowFocus'
    | 'refetchOnReconnect'
    | 'refetchInterval'
  >,
) {
  return useQuery<UserTask, RequestError | Error>({
    ...options,
    queryKey: getUseTaskQueryKey(userTaskKey),
    queryFn: async () => {
      const {response, error} = await request(api.getTask(userTaskKey));

      if (response !== null) {
        return response.json();
      }

      throw error ?? new Error('Could not fetch task');
    },
    placeholderData: (previousData) => previousData,
  });
}

function useRemoveFormReference(task: UserTask) {
  const client = useQueryClient();

  function removeFormReference() {
    client.setQueryData<UserTask>(
      getUseTaskQueryKey(task.userTaskKey),
      (cachedTask) => {
        if (cachedTask === undefined) {
          return cachedTask;
        }
        const {formKey: _, ...updatedTask} = cachedTask;

        return updatedTask;
      },
    );
  }

  return {removeFormReference};
}

export {useTask, useRemoveFormReference, getUseTaskQueryKey};
