import React from 'react';

import {ReportView} from 'components';
import {Link} from 'react-router-dom';

import './DashboardReport.css';

export default class DashboardReport extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      data: undefined
    };
  }

  componentDidMount() {
    this.loadReportData();
  }

  loadReportData = async () => {
    this.setState({
      data: await this.props.loadReport(this.props.report)
    });
  };

  getName = () => {
    const {name, reportDefinition} = this.state.data;

    return name || (reportDefinition && reportDefinition.name);
  };

  render() {
    if (!this.state.data) {
      return 'loading...';
    }

    return (
      <div className="DashboardReport__wrapper">
        <div className="DashboardReport__header">
          {this.props.disableNameLink ? (
            <span className="DashboardReport__heading">{this.getName()}</span>
          ) : (
            <Link to={`/report/${this.props.report.id}`} className="DashboardReport__heading">
              {this.getName()}
            </Link>
          )}
        </div>
        <div className="DashboardReport__visualization">
          {this.state.data.errorMessage ? (
            this.state.data.errorMessage
          ) : (
            <ReportView
              disableReportScrolling={this.props.disableReportScrolling}
              report={this.state.data}
            />
          )}
        </div>
        {this.props.addons &&
          this.props.addons.map(addon =>
            React.cloneElement(addon, {
              report: this.props.report,
              loadReportData: this.loadReportData,
              tileDimensions: this.props.tileDimensions
            })
          )}
      </div>
    );
  }
}
