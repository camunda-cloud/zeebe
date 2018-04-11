import React from 'react';
import classnames from 'classnames';
import DateRange from './DateRange';
import DateInput from './DateInput';

import './DateFields.css';

export default class DateFields extends React.PureComponent {
  constructor(props) {
    super(props);

    this.state = {
      popupOpen: false,
      currentlySelectedField: null,
      minDate: null,
      maxDate: null
    };
  }

  componentDidUpdate() {
    const {popupOpen} = this.state;

    if (popupOpen) {
      document.addEventListener('click', this.hidePopup);
      document.addEventListener('keydown', this.closeOnEscape);
    } else {
      document.removeEventListener('click', this.hidePopup);
      document.removeEventListener('keydown', this.closeOnEscape);
    }
    this.props.enableAddButton(true);
  }

  componentWillUnmount() {
    document.removeEventListener('click', this.hidePopup);
    document.removeEventListener('keydown', this.closeOnEscape);
  }

  handleKeyPress = evt => {
    if (this.state.popupOpen && evt.key === 'Escape') {
      evt.stopPropagation();
    }
  };

  render() {
    return (
      <div className="DateFields" onKeyDown={this.handleKeyPress}>
        <div className="DateFields__inputContainer">
          <DateInput
            className={classnames('DateInput__start', {
              'DateInput__start--highlight': this.isFieldSelected('startDate')
            })}
            format={this.props.format}
            onDateChange={this.setStartDate}
            onFocus={() => {
              this.setState({currentlySelectedField: 'startDate'});
            }}
            onSubmit={this.submitStart}
            onClick={this.toggleDateRangeForStart}
            date={this.props.startDate}
            enableAddButton={this.props.enableAddButton}
          />
          <span className="DateFields__divider">to</span>
          <DateInput
            className={classnames('DateInput__end', {
              'DateInput__start--highlight': this.isFieldSelected('endDate')
            })}
            ref={this.saveEndDateField}
            format={this.props.format}
            onDateChange={this.setEndDate}
            onFocus={() => {
              this.setState({currentlySelectedField: 'endDate'});
            }}
            onSubmit={this.submitEnd}
            onClick={this.toggleDateRangeForEnd}
            date={this.props.endDate}
            enableAddButton={this.props.enableAddButton}
          />
        </div>
        {this.state.popupOpen && (
          <div
            onClick={this.stopClosingPopup}
            className={classnames('DateFields__range', {
              'DateFields__range--left': this.isFieldSelected('startDate'),
              'DateFields__range--right': this.isFieldSelected('endDate')
            })}
          >
            <DateRange
              format={this.props.format}
              onDateChange={this.onDateRangeChange}
              startDate={this.props.startDate}
              endDate={this.props.endDate}
            />
          </div>
        )}
      </div>
    );
  }

  submitStart = () => {
    this.setState({currentlySelectedField: 'endDate'});
    document.querySelector('.DateInput__end').focus();
  };

  submitEnd = () => {
    this.hidePopup();
    document.querySelector('.DateInput__end').blur();
  };

  closeOnEscape = event => {
    if (event.key === 'Escape') {
      this.hidePopup();
    }
  };

  saveEndDateField = input => (this.endDateField = input);

  onDateRangeChange = date => {
    this.props.onDateChange(this.state.currentlySelectedField, date);

    if (this.isFieldSelected('startDate')) {
      this.setState({
        currentlySelectedField: 'endDate'
      });
      this.endDateField.focus();
    } else {
      setTimeout(this.hidePopup, 350);
    }
  };

  stopClosingPopup = ({nativeEvent: event}) => {
    // https://stackoverflow.com/questions/24415631/reactjs-syntheticevent-stoppropagation-only-works-with-react-events
    event.stopImmediatePropagation();
  };

  hidePopup = () => {
    this.setState({
      popupOpen: false,
      currentlySelectedField: null,
      minDate: null,
      maxDate: null
    });
  };

  isFieldSelected(field) {
    return this.state.currentlySelectedField === field;
  }

  setStartDate = date => this.props.onDateChange('startDate', date);
  setEndDate = date => this.props.onDateChange('endDate', date);

  toggleDateRangeForStart = event => this.toggleDateRangePopup(event, 'startDate');
  toggleDateRangeForEnd = event => this.toggleDateRangePopup(event, 'endDate');

  toggleDateRangePopup = (event, field) => {
    this.stopClosingPopup(event);

    if (this.state.popupOpen) {
      return this.setState({
        currentlySelectedField: field
      });
    }

    this.setState({
      popupOpen: true,
      currentlySelectedField: field
    });
  };
}
