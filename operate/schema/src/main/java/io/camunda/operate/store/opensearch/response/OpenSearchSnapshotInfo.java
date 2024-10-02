/*
 * Copyright Camunda Services GmbH
 *
 * BY INSTALLING, DOWNLOADING, ACCESSING, USING, OR DISTRIBUTING THE SOFTWARE (“USE”), YOU INDICATE YOUR ACCEPTANCE TO AND ARE ENTERING INTO A CONTRACT WITH, THE LICENSOR ON THE TERMS SET OUT IN THIS AGREEMENT. IF YOU DO NOT AGREE TO THESE TERMS, YOU MUST NOT USE THE SOFTWARE. IF YOU ARE RECEIVING THE SOFTWARE ON BEHALF OF A LEGAL ENTITY, YOU REPRESENT AND WARRANT THAT YOU HAVE THE ACTUAL AUTHORITY TO AGREE TO THE TERMS AND CONDITIONS OF THIS AGREEMENT ON BEHALF OF SUCH ENTITY.
 * “Licensee” means you, an individual, or the entity on whose behalf you receive the Software.
 *
 * Permission is hereby granted, free of charge, to the Licensee obtaining a copy of this Software and associated documentation files to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject in each case to the following conditions:
 * Condition 1: If the Licensee distributes the Software or any derivative works of the Software, the Licensee must attach this Agreement.
 * Condition 2: Without limiting other conditions in this Agreement, the grant of rights is solely for non-production use as defined below.
 * "Non-production use" means any use of the Software that is not directly related to creating products, services, or systems that generate revenue or other direct or indirect economic benefits.  Examples of permitted non-production use include personal use, educational use, research, and development. Examples of prohibited production use include, without limitation, use for commercial, for-profit, or publicly accessible systems or use for commercial or revenue-generating purposes.
 *
 * If the Licensee is in breach of the Conditions, this Agreement, including the rights granted under it, will automatically terminate with immediate effect.
 *
 * SUBJECT AS SET OUT BELOW, THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * NOTHING IN THIS AGREEMENT EXCLUDES OR RESTRICTS A PARTY’S LIABILITY FOR (A) DEATH OR PERSONAL INJURY CAUSED BY THAT PARTY’S NEGLIGENCE, (B) FRAUD, OR (C) ANY OTHER LIABILITY TO THE EXTENT THAT IT CANNOT BE LAWFULLY EXCLUDED OR RESTRICTED.
 */
package io.camunda.operate.store.opensearch.response;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OpenSearchSnapshotInfo {

  private String snapshot;
  private String uuid;

  private SnapshotState state;

  private List<Object> failures = List.of();

  private Long startTimeInMillis;

  private Long endTimeInMillis;

  private Map<String, Object> metadata = Map.of();

  public String getSnapshot() {
    return snapshot;
  }

  public OpenSearchSnapshotInfo setSnapshot(final String snapshot) {
    this.snapshot = snapshot;
    return this;
  }

  public String getUuid() {
    return uuid;
  }

  public OpenSearchSnapshotInfo setUuid(final String uuid) {
    this.uuid = uuid;
    return this;
  }

  public SnapshotState getState() {
    return state;
  }

  public OpenSearchSnapshotInfo setState(final SnapshotState state) {
    this.state = state;
    return this;
  }

  public List<Object> getFailures() {
    return failures;
  }

  public OpenSearchSnapshotInfo setFailures(final List<Object> failures) {
    this.failures = failures;
    return this;
  }

  public Long getStartTimeInMillis() {
    return startTimeInMillis;
  }

  public OpenSearchSnapshotInfo setStartTimeInMillis(final Long startTimeInMillis) {
    this.startTimeInMillis = startTimeInMillis;
    return this;
  }

  public Long getEndTimeInMillis() {
    return endTimeInMillis;
  }

  public OpenSearchSnapshotInfo setEndTimeInMillis(final Long endTimeInMillis) {
    this.endTimeInMillis = endTimeInMillis;
    return this;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public OpenSearchSnapshotInfo setMetadata(final Map<String, Object> metadata) {
    this.metadata = metadata;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        snapshot, uuid, state, failures, startTimeInMillis, endTimeInMillis, metadata);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final OpenSearchSnapshotInfo that = (OpenSearchSnapshotInfo) o;
    return Objects.equals(snapshot, that.snapshot)
        && Objects.equals(uuid, that.uuid)
        && Objects.equals(state, that.state)
        && Objects.equals(failures, that.failures)
        && Objects.equals(startTimeInMillis, that.startTimeInMillis)
        && Objects.equals(endTimeInMillis, that.endTimeInMillis)
        && Objects.equals(metadata, that.metadata);
  }

  @Override
  public String toString() {
    return "SnapshotInfo{"
        + "snapshot='"
        + snapshot
        + '\''
        + ", uuid='"
        + uuid
        + '\''
        + ", state='"
        + state
        + '\''
        + ", failures="
        + failures
        + ", startTimeInMillis="
        + startTimeInMillis
        + ", endTimeInMillis="
        + endTimeInMillis
        + ", metadata="
        + metadata
        + '}';
  }
}
