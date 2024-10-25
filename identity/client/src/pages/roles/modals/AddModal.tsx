/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
import { FC, useState } from "react";
import TextField from "src/components/form/TextField";
import { useApiCall } from "src/utility/api";
import useTranslate from "src/utility/localization";
import { FormModal, UseModalProps } from "src/components/modal";
import { createRole } from "src/utility/api/roles";
import EntityList from "src/components/entityList";
import useAllPermissions from "src/pages/roles/modals/useAllPermissions.tsx";

const AddModal: FC<UseModalProps> = ({ open, onClose, onSuccess }) => {
  const { t } = useTranslate("roles");
  const [apiCall, { loading, namedErrors }] = useApiCall(createRole);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const { permissions, setPermissions, onSelect, onUnselect, availableItems } =
    useAllPermissions();

  const handleSubmit = async () => {
    const { success } = await apiCall({
      name,
      description,
      permissions,
    });

    if (success) {
      onSuccess();
    }
  };

  return (
    <FormModal
      open={open}
      headline={t("Create role")}
      onClose={onClose}
      onSubmit={handleSubmit}
      loading={loading}
      loadingDescription={t("Adding role")}
      confirmLabel={t("Create role")}
    >
      <TextField
        label={t("Name")}
        value={name}
        placeholder={t("Name")}
        onChange={setName}
        errors={namedErrors?.name}
        autoFocus
      />
      <TextField
        label={t("Description")}
        value={description}
        placeholder={t("Role description")}
        onChange={setDescription}
        errors={namedErrors?.description}
      />
      <EntityList
        isInsideModal
        data={availableItems}
        headers={[
          { header: t("Permission"), key: "permission" },
          { header: t("Description"), key: "description" },
        ]}
        loading={loading}
        batchSelection={{
          onSelect,
          onUnselect,
          onSelectAll: (selected) =>
            setPermissions(selected.map(({ permission }) => permission)),
          isSelected: ({ permission }) => permissions.includes(permission),
        }}
      />
    </FormModal>
  );
};

export default AddModal;
