/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.portal.events;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Type;
import org.jasig.portal.dao.usertype.FunctionalNameType;
import org.jasig.portal.security.IPerson;



@Entity
@Table(name = "UPE_PDEL_LAYOUT_EVENT")
@Inheritance(strategy=InheritanceType.JOINED)
@PrimaryKeyJoinColumn(name="EVENT_ID")
public final class PortletDeletedFromLayoutPortalEvent extends LayoutPortalEvent {
    private static final long serialVersionUID = 1L;

    @Column(name="OLD_PARENT_FOLDER_ID", length=50, nullable=false)
    private final String oldParentFolderId;
    
    @Column(name = "FNAME", length = 255, nullable = false)
    @Type(type = "fname")
    private final String fname;
    
    @SuppressWarnings("unused")
    private PortletDeletedFromLayoutPortalEvent() {
        super();
        this.oldParentFolderId = null;
        this.fname = null;
    }

    PortletDeletedFromLayoutPortalEvent(PortalEventBuilder portalEventBuilder, IPerson layoutOwner, long layoutId,
            String oldParentFolderId, String fname) {
        super(portalEventBuilder, layoutOwner, layoutId);
        Validate.notNull(oldParentFolderId, "oldParentFolderId");
        FunctionalNameType.validate(fname);
        
        this.oldParentFolderId = oldParentFolderId;
        this.fname = fname;
    }

    /**
     * @return the oldParentFolderId
     */
    public String getOldParentFolderId() {
        return this.oldParentFolderId;
    }

    /**
     * @return the fname
     */
    public String getFname() {
        return this.fname;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return super.toString() + 
                ", oldParentFolderId=" + this.oldParentFolderId + 
                ", fname=" + this.fname + "]";
    }
    
    
}
