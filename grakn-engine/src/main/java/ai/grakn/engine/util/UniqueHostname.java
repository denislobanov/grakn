/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016  Grakn Labs Ltd
 *
 * Grakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grakn. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package ai.grakn.engine.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class UniqueHostname {
    private String name;
    private static UniqueHostname instance = null;
    private final Logger LOG = LoggerFactory.getLogger(UniqueHostname.class);

    private UniqueHostname() {
        try {
            name = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e) {
            LOG.error("Could not get system hostname: ", e);
        }

        name += UUID.randomUUID().toString();
    }

    public static synchronized UniqueHostname getInstance() {
        if(instance == null)
            instance = new UniqueHostname();

        return instance;
    }

    public String name() {
        return name;
    }
}
