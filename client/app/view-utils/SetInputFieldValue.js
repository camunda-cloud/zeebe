import {Scope} from './Scope';
import {jsx} from './jsx';
import {updateOnlyWhenStateChanges} from './updateOnlyWhenStateChanges';

export function setInputValue(input, value) {
  const {selectionStart, selectionEnd, selectionDirection} = input;

  input.value = value || '';
  input.selectionStart = selectionStart;
  input.selectionEnd = selectionEnd;
  input.selectionDirection = selectionDirection;
}

function InputSetter() {
  return (input) => {
    return updateOnlyWhenStateChanges(
      setInputValue.bind(null, input)
    );
  };
}

export function SetInputFieldValue({getValue}) {
  return <Scope selector={getValue}>
    <InputSetter/>
  </Scope>;
}
