/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.workspace;

import com.google.inject.ImplementedBy;

/**
 * This interface give ability part stack manipulate visibility an size in container.
 *
 * @author Evgen Vidolob
 */
@ImplementedBy(WorkBenchPartControllerImpl.class)
public interface WorkBenchPartController {

    /**
     * Get part stack size.
     *
     * @return the size
     */
    double getSize();

    /**
     * Set part stack size.
     *
     * @param size
     *         size which need set
     */
    void setSize(double size);

    /**
     * Show/hide part stack.
     *
     * @param hidden
     *         <code>true</code> hides part, <code>false</code> display part
     */
    void setHidden(boolean hidden);
}
