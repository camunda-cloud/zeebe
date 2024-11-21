/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda
 * Services GmbH under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. Licensed under the Camunda License 1.0.
 * You may not use this file except in compliance with the Camunda License 1.0.
 */
import { FC, useState } from "react";
import TextField from "src/components/form/TextField";
import { useApiCall } from "src/utility/api";
import useTranslate from "src/utility/localization";
import { FormModal, UseModalProps } from "src/components/modal";
import { createTenant } from "src/utility/api/tenants";

const AddTenantModal: FC<UseModalProps> = ({ open, onClose, onSuccess }) => {
  const { t } = useTranslate("tenants");
  const [apiCall, { loading, namedErrors }] = useApiCall(createTenant);
  const [name, setName] = useState("");
  const [tenantId, setTenantId] = useState("");

  const submitDisabled = loading || !name || !tenantId;

  const handleSubmit = async () => {
    const { success } = await apiCall({
      name,
      tenantId,
    });

    if (success) {
      onSuccess();
    }
  };

  return (
    <FormModal
      headline={t("Create new tenant")}
      open={open}
      onClose={onClose}
      loading={loading}
      submitDisabled={submitDisabled}
      confirmLabel={t("Create tenant")}
      onSubmit={handleSubmit}
    >
      <TextField
        label={t("Tenant name")}
        placeholder={t("Enter tenant name")}
        onChange={setName}
        value={name}
        errors={namedErrors?.name}
        autoFocus
      />
      <TextField
        label={t("Tenant ID")}
        placeholder={t("Enter tenant ID")}
        onChange={setTenantId}
        value={tenantId}
        errors={namedErrors?.tenantId}
      />
    </FormModal>
  );
};

export default AddTenantModal;
