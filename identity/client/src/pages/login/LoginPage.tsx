/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import React, {useCallback, useState} from "react";
import Page from "src/components/layout/Page.tsx";
import useTranslate from "src/utility/localization";
import {Button, Link, PasswordInput, TextInput} from "@carbon/react";
import "./LoginPage.scss";
import {login} from "src/utility/auth";
import {useLocation} from "react-router-dom";
import {getCopyrightNoticeText} from "src/utility/copyright.ts";
import camundaLogo from "src/assets/images/camunda.svg";

interface Props {
  onSuccess: () => void;
}

const LoginForm: React.FC<Props> = ({ onSuccess }) => {
  const { t } = useTranslate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const submit = useCallback(() => {
    login(username, password).then((success) => {
      if (success) {
        onSuccess();
      }
    });
  }, [onSuccess, username, password]);
  return (
    <div className="LoginForm">
      <TextInput
        id="username"
        name="username"
        value={username}
        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
          setUsername(e.target.value)
        }
        labelText={t("Username")}
        invalid={false}
        invalidText={undefined}
        placeholder={t("")}
      />
      <PasswordInput
        id="password"
        name="password"
        onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
          setPassword(e.target.value)
        }
        value={password}
        type="password"
        hidePasswordLabel={t("Hide password")}
        showPasswordLabel={t("Show password")}
        onKeyDown={(e: React.KeyboardEvent) => {
          if (e.key === "Enter") {
            submit();
          }
        }}
        labelText={t("Password")}
        invalid={false}
        invalidText={undefined}
        placeholder={t("loginPasswordFieldPlaceholder")}
      />
      <Button onClick={submit}>Login</Button>
    </div>
  );
};

function getRedirectUrl(queryString: string) {
  const params = new URLSearchParams(queryString);
  const next = params.get("next");
  if (!next || !/^(\/\w+)+$/.test(next)) {
    return null;
  }
  return next;
}

function getPageTitle(redirectUrl: string | null): string {
  if (redirectUrl?.startsWith("/identity")) {
    return "Identity";
  }
  if (redirectUrl?.startsWith("/tasklist")) {
    return "Tasklist";
  }
  if (redirectUrl?.startsWith("/operate")) {
    return "Operate";
  }
  return "Login";
}

export const LoginPage: React.FC = () => {
  const location = useLocation();
  const redirectUrl = getRedirectUrl(location.search);
  const onSuccess = useCallback(() => {
    window.location.href = redirectUrl ?? "/identity/users";
  }, [redirectUrl]);
  const hasProductionLicense = false; // FIXME
  return (
    <Page className="LoginPage">
      <div className="content">
        <div className="header">
            <img src={camundaLogo} alt="Camunda"/>
            <h1>{getPageTitle(redirectUrl)}</h1>
        </div>
        <LoginForm onSuccess={onSuccess} />
        {!hasProductionLicense && <div className="license-info">
          Non-Production License. If you would like information on production usage,
          please refer to our{' '}
          <Link
              href="https://legal.camunda.com/#self-managed-non-production-terms"
              target="_blank"
              inline
          >
            terms & conditions page
          </Link>{' '}
          or{' '}
          <Link href="https://camunda.com/contact/" target="_blank" inline>
            contact sales
          </Link>
          .
        </div>}
      </div>
      <div className="copyright-notice">{getCopyrightNoticeText()}</div>
    </Page>
  );
};
