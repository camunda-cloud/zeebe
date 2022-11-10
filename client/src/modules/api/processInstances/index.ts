/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {request} from 'modules/request';

async function getOperation(batchOperationId: string) {
  return request({url: `/api/operations?batchOperationId=${batchOperationId}`});
}

export {getOperation};
