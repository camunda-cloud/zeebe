/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {useEffect, useState} from 'react';
import {DangerButton} from 'modules/components/OperationItem/DangerButton';
import {OperationItems} from 'modules/components/OperationItems';
import {DeleteButtonContainer} from 'modules/components/DeleteDefinition/styled';
import {InlineLoading, Link, ListItem, Stack} from '@carbon/react';
import {DeleteDefinitionModal} from 'modules/components/DeleteDefinitionModal';
import {operationsStore} from 'modules/stores/operations';
import {panelStatesStore} from 'modules/stores/panelStates';
import {StructuredList} from 'modules/components/StructuredList';
import {UnorderedList} from 'modules/components/DeleteDefinitionModal/Warning/styled';
import {notificationsStore} from 'modules/stores/notifications';
import {tracking} from 'modules/tracking';
import {observer} from 'mobx-react';
import {processInstancesStore} from 'modules/stores/processInstances';

type Props = {
  processDefinitionId: string;
  processName: string;
  processVersion: string;
};

const ProcessOperations: React.FC<Props> = observer(
  ({processDefinitionId, processName, processVersion}) => {
    const [isDeleteModalVisible, setIsDeleteModalVisible] =
      useState<boolean>(false);

    const [isOperationRunning, setIsOperationRunning] = useState(false);
    const {runningInstancesCount} = processInstancesStore.state;

    useEffect(() => {
      processInstancesStore.fetchRunningInstancesCount();

      return () => {
        setIsOperationRunning(false);
        processInstancesStore.setRunningInstancesCount(-1);
      };
    }, [processDefinitionId]);

    return (
      <>
        <DeleteButtonContainer>
          {isOperationRunning && (
            <InlineLoading data-testid="delete-operation-spinner" />
          )}
          <OperationItems>
            <DangerButton
              title={
                runningInstancesCount > 0
                  ? 'Only process definitions without running instances can be deleted.'
                  : `Delete Process Definition "${processName} - Version ${processVersion}"`
              }
              type="DELETE"
              disabled={isOperationRunning || runningInstancesCount !== 0}
              onClick={() => {
                tracking.track({
                  eventName: 'definition-deletion-button',
                  resource: 'process',
                  version: processVersion,
                });

                setIsDeleteModalVisible(true);
              }}
            />
          </OperationItems>
        </DeleteButtonContainer>
        <DeleteDefinitionModal
          title="Delete Process Definition"
          description="You are about to delete the following process definition:"
          confirmationText="Yes, I confirm I want to delete this process definition."
          isVisible={isDeleteModalVisible}
          warningTitle="Deleting a process definition will permanently remove it and will
        impact the following:"
          warningContent={
            <Stack gap={6}>
              <UnorderedList nested>
                <ListItem>
                  All the deleted process definition’s finished process
                  instances will be deleted from the application.
                </ListItem>
                <ListItem>
                  All decision and process instances referenced by the deleted
                  process instances will be deleted.
                </ListItem>
                <ListItem>
                  If a process definition contains user tasks, they will be
                  deleted from Tasklist.
                </ListItem>
              </UnorderedList>
              <Link
                href="https://docs.camunda.io/docs/components/operate/userguide/delete-resources/"
                target="_blank"
              >
                For a detailed overview, please view our guide on deleting a
                process definition
              </Link>
            </Stack>
          }
          bodyContent={
            <StructuredList
              headerColumns={[
                {
                  cellContent: 'Process Definition',
                },
              ]}
              rows={[
                {
                  key: `${processName}-v${processVersion}`,
                  columns: [
                    {cellContent: `${processName} - Version ${processVersion}`},
                  ],
                },
              ]}
              label="Process Details"
            />
          }
          onClose={() => setIsDeleteModalVisible(false)}
          onDelete={() => {
            setIsOperationRunning(true);
            setIsDeleteModalVisible(false);

            tracking.track({
              eventName: 'definition-deletion-confirmation',
              resource: 'process',
              version: processVersion,
            });

            operationsStore.applyDeleteProcessDefinitionOperation({
              processDefinitionId,
              onSuccess: () => {
                setIsOperationRunning(false);
                panelStatesStore.expandOperationsPanel();

                notificationsStore.displayNotification({
                  kind: 'success',
                  title: 'Operation created',
                  isDismissable: true,
                });
              },
              onError: (statusCode: number) => {
                setIsOperationRunning(false);

                notificationsStore.displayNotification({
                  kind: 'error',
                  title: 'Operation could not be created',
                  subtitle:
                    statusCode === 403
                      ? 'You do not have permission'
                      : undefined,
                  isDismissable: true,
                });
              },
            });
          }}
        />
      </>
    );
  },
);

export {ProcessOperations};
