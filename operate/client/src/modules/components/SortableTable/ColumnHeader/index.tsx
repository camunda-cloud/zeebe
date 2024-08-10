/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {useLocation, useNavigate} from 'react-router-dom';
import {getSortParams} from 'modules/utils/filter';
import {Header, TableHeader} from '@carbon/react';

const INITIAL_SORT_ORDER = 'desc';

function toggleSorting(
  search: string,
  sortKey: string,
  currentSortOrder?: 'asc' | 'desc',
) {
  const params = new URLSearchParams(search);
  if (currentSortOrder === undefined) {
    params.set('sort', `${sortKey}+${INITIAL_SORT_ORDER}`);
    return params.toString();
  }

  params.set(
    'sort',
    `${sortKey}+${currentSortOrder === 'asc' ? 'desc' : 'asc'}`,
  );
  return params.toString();
}

type Props = {
  label: string | React.ReactNode;
  sortKey: string;
  isDefault?: boolean;
  isDisabled?: boolean;
  onSort?: (sortKey: string) => void;
} & React.ComponentProps<typeof Header>;

const ColumnHeader: React.FC<Props> = ({
  sortKey,
  label,
  isDefault = false,
  isDisabled = false,
  onSort,
  ...rest
}) => {
  const navigate = useNavigate();
  const location = useLocation();
  const existingSortParams = getSortParams(location.search);

  const isActive =
    existingSortParams !== null
      ? existingSortParams.sortBy === sortKey
      : isDefault;

  const displaySortIcon = isActive && !isDisabled;
  const currentSortOrder =
    existingSortParams?.sortOrder === undefined && isDefault
      ? INITIAL_SORT_ORDER
      : existingSortParams?.sortBy === sortKey
        ? existingSortParams?.sortOrder
        : undefined;

  return (
    <TableHeader
      {...rest}
      onClick={() => {
        onSort?.(sortKey);
        navigate({
          search: toggleSorting(location.search, sortKey, currentSortOrder),
        });
      }}
      isSortHeader={!isDisabled}
      title={`Sort by ${label}`}
      aria-label={`Sort by ${label}`}
      sortDirection={
        displaySortIcon
          ? currentSortOrder === 'asc'
            ? 'ASC'
            : currentSortOrder === 'desc'
              ? 'DESC'
              : 'NONE'
          : 'NONE'
      }
      isSortable={!isDisabled}
    >
      {label}
    </TableHeader>
  );
};

export {ColumnHeader};
