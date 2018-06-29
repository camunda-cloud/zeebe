import React from 'react';
import PropTypes from 'prop-types';
import {Redirect} from 'react-router-dom';

import {login} from './api';
import {REQUIRED_FIELD_ERROR, LOGIN_ERROR} from './constants';
import * as Styled from './styled';

import withSharedState from 'modules/components/withSharedState';

class Login extends React.Component {
  static propTypes = {
    location: PropTypes.object.isRequired,
    clearStateLocally: PropTypes.func.isRequired
  };

  state = {
    username: '',
    password: '',
    forceRedirect: false,
    error: null
  };

  handleLogin = async e => {
    e.preventDefault();
    const {username, password} = this.state;

    if (username.length === 0 || password.length === 0) {
      return this.setState({error: REQUIRED_FIELD_ERROR});
    }

    try {
      await login({username, password});
      this.props.clearStateLocally();
      this.setState({forceRedirect: true});
    } catch (e) {
      this.setState({error: LOGIN_ERROR});
    }
  };

  handleInputChange = ({target: {name, value}}) => {
    this.setState({[name]: value});
  };

  render() {
    const {username, password, forceRedirect, error} = this.state;

    // case of successful login
    if (forceRedirect) {
      const locationState = this.props.location.state || {referrer: '/'};
      return <Redirect to={locationState.referrer} />;
    }

    // default render
    return (
      <Styled.Login onSubmit={this.handleLogin}>
        <Styled.LoginHeader>
          <Styled.Logo />
          <Styled.LoginTitle>Operate</Styled.LoginTitle>
        </Styled.LoginHeader>
        <Styled.LoginForm>
          {error && <Styled.FormError>{error}</Styled.FormError>}
          <Styled.UsernameInput
            value={username}
            name="username"
            type="text"
            onChange={this.handleInputChange}
            placeholder="Username"
            aria-label="User Name"
          />
          <Styled.PasswordInput
            value={password}
            name="password"
            type="password"
            onChange={this.handleInputChange}
            placeholder="Password"
            aria-label="Password"
          />
          <Styled.SubmitButton type="submit" title="Log in">
            Log in
          </Styled.SubmitButton>
        </Styled.LoginForm>
      </Styled.Login>
    );
  }
}

export default withSharedState(Login);
