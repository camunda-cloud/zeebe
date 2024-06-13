/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import { FC, useEffect, useState } from "react";
import { EditFormModal, UseEntityModalProps } from "src/components/modal";
import useTranslate from "src/utility/localization";
import { useApiCall } from "src/utility/api/hooks";
import { useNotifications } from "src/components/notifications";
import { Group, updateGroup } from "src/utility/api/groups";
import { TextField } from "src/components/form";

const EditModal: FC<UseEntityModalProps<Group>> = ({
  entity: group,
  open,
  onClose,
  onSuccess,
}) => {
  const { t } = useTranslate();
  const { enqueueNotification } = useNotifications();

  const [callUpdateGroup, { namedErrors, loading }, reset] =
    useApiCall(updateGroup);

  const [name, setName] = useState("");

  const handleSubmit = async () => {
    if (group) {
      const { success } = await callUpdateGroup({
        id: group.id,
        name: name.trim(),
      });

      if (success) {
        enqueueNotification({
          kind: "success",
          title: t("Group has been updated."),
        });
        onSuccess();
      }
    }
  };

  useEffect(() => {
    if (open) {
      setName(group?.name || "");
      reset();
    }
  }, [open]);

  return (
    <EditFormModal
      size="sm"
      open={open}
      headline={t("Rename group")}
      onSubmit={handleSubmit}
      onClose={onClose}
      loading={loading}
      loadingDescription={t("Updating group")}
    >
      <TextField
        label={t("Name")}
        value={name}
        placeholder={t("My group")}
        onChange={setName}
        errors={namedErrors?.name}
        autofocus
      />
    </EditFormModal>
  );
};

export default EditModal;
