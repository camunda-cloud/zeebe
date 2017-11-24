import React from 'react';

import {ErrorBoundary} from '../ErrorBoundary';

import {Number, Json, Table, Heatmap, Chart} from './views';

export default class ReportView extends React.Component {
  render() {
    let config;

    if (this.props.report) {
      const {data} = this.props.report;
      const result = this.props.report.result;

      switch(data.visualization) {
        case 'number':
          config = {
            component: Number,
            props: {data: result}
          }; break;
        case 'table':
          config = {
            component: Table,
            props: {data: result}
          }; break;
        case 'heat':
          config = {
            component: Heatmap,
            props: {data: result, process: data.processDefinitionId}
          }; break;
        case 'bar':
        case 'line':
        case 'pie':
          config = {
            component: Chart,
            props: {data: result, type: data.visualization}
          }; break;
        default:
          config = {
            component: Json,
            props: {data}
          }; break;
      }
    } else {
      config = {
        component: Json,
        props: {data: null}
      };
    }

    config.props.errorMessage = 'Cannot display data. Choose another visualization.';
    const Component = config.component;

    return (<ErrorBoundary>
      <Component {...config.props} />
    </ErrorBoundary>);
  }
}
