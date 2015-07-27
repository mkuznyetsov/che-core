/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.workspace.server.event;

import org.eclipse.che.api.core.model.UsersWorkspace;
import org.eclipse.che.api.core.notification.EventOrigin;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDo;

import java.util.Map;


/**
 * @author Sergii Leschenko
 */
@EventOrigin("workspace")
public interface BeforeCreateWorkspaceEvent {

    UsersWorkspace getWorkspace();

    String getAccountId();

}