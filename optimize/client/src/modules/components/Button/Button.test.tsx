/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {shallow} from 'enzyme';

import Button from './Button';

it('should render without crashing', () => {
  shallow(<Button />);
});

it('renders a <button> element by default', () => {
  const node = shallow(<Button />);

  expect(node).toHaveDisplayName('button');
});

it('renders a label as provided as a property', () => {
  const text = 'Click Me';

  const node = shallow(<Button>{text}</Button>);
  expect(node).toIncludeText(text);
});

it('renders a modifier class name based on the type provided as a property', () => {
  const node = shallow(<Button primary />);

  expect(node).toHaveClassName('primary');
});

it('renders the id as provided as a property', () => {
  const id = 'my-button';

  const node = shallow(<Button id={id} />);
  expect(node).toMatchSelector('#' + id);
});

it('does render the title as provided as a property', () => {
  const titleText = 'my-button';

  const node = shallow(<Button title={titleText} />);
  expect(node).toMatchSelector('button[title="' + titleText + '"]');
});

it('does merge and render classNames provided as a property', () => {
  const node = shallow(<Button className={'foo'} />);
  expect(node).toMatchSelector('.Button.foo');
});

it('adds an "active" class when "active" prop was provided', () => {
  const node = shallow(<Button active />);
  expect(node).toMatchSelector('.Button.active');
});

it('executes a click handler as provided as a property', () => {
  const handler = jest.fn();
  const node = shallow(<Button onClick={handler} />);

  node.simulate('click');
  expect(handler).toHaveBeenCalled();
});
