/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {Cell} from 'table-js/lib/components';

import {createComponentVNode, createVNode, createTextVNode, Component} from 'inferno';

export default function HitsColumnAddon() {
  const entryPoints = {rules: {}};

  const CustomCell = class CustomCell extends Component {
    componentDidMount() {
      if (this.props.summary) {
        entryPoints.summary = this.el;
      } else {
        entryPoints.rules[this.props.row.id] = this.el;
      }
    }

    render() {
      return createComponentVNode(2, Cell, {
        ref: (el) => (this.el = el),
        className: 'CustomCell',
      });
    }
  };

  function addHitsColumn(components) {
    components.onGetComponent('table.foot', () => (props) => {
      const emptyCells = [];
      for (let i = 0; i < props.cols.length + 1; i++) {
        emptyCells.push(createVNode(1, 'td'));
      }
      return createVNode(
        1,
        'tfoot',
        null,
        createVNode(
          1,
          'tr',
          null,
          [
            createVNode(1, 'td', null, createTextVNode('Total'), 2),
            ...emptyCells,
            createComponentVNode(2, CustomCell, {summary: true}),
          ],
          0
        ),
        2
      );
    });
    components.onGetComponent('cell', ({cellType}) => {
      if (cellType === 'after-label-cells') {
        return () => createVNode(1, 'th', 'hit header', createTextVNode('Hits'), 2);
      }
      if (cellType === 'after-rule-cells') {
        return CustomCell;
      }
    });
  }
  addHitsColumn.$inject = ['components'];

  return {
    entryPoints,
    Addon: {
      __init__: ['hitsColumn'],
      hitsColumn: ['type', addHitsColumn],
    },
  };
}
