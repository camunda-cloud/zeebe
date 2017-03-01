/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.tngp.client.impl.cmd.taskqueue;

import java.util.Map;

public class TaskEvent
{
    private TaskEventType event;
    private long lockTime;
    private int lockOwner;
    private int retries;
    private String type;
    private Map<String, String> headers;
    private byte[] payload;

    public TaskEventType getEvent()
    {
        return event;
    }

    public void setEvent(TaskEventType event)
    {
        this.event = event;
    }

    public Long getLockTime()
    {
        return lockTime;
    }

    public void setLockTime(long lockTime)
    {
        this.lockTime = lockTime;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public Map<String, String> getHeaders()
    {
        return headers;
    }

    public void setHeaders(Map<String, String> headers)
    {
        this.headers = headers;
    }

    public byte[] getPayload()
    {
        return payload;
    }

    public void setPayload(byte[] payload)
    {
        this.payload = payload;
    }

    public int getLockOwner()
    {
        return lockOwner;
    }

    public void setLockOwner(int lockOwner)
    {
        this.lockOwner = lockOwner;
    }

    public void reset()
    {
        event = null;
        lockTime = -1L;
        lockOwner = -1;
        retries = -1;
        type = null;
        headers = null;
        payload = null;
    }

    public int getRetries()
    {
        return retries;
    }

    public void setRetries(int retries)
    {
        this.retries = retries;
    }

}
