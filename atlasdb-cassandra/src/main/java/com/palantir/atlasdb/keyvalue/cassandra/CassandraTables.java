/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.keyvalue.cassandra;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.cassandra.thrift.CfDef;
import org.apache.cassandra.thrift.KsDef;
import org.apache.thrift.TException;

import com.palantir.atlasdb.cassandra.CassandraKeyValueServiceConfig;
import com.palantir.common.base.FunctionCheckedException;
import com.palantir.common.base.Throwables;

class CassandraTables {
    private final CassandraClientPool clientPool;
    private final CassandraKeyValueServiceConfig config;

    CassandraTables(CassandraClientPool clientPool, CassandraKeyValueServiceConfig config) {
        this.clientPool = clientPool;
        this.config = config;
    }

    Set<String> getExisting() {
        String keyspace = config.getKeyspaceOrThrow();

        try {
            return clientPool.runWithRetry(new FunctionCheckedException<CassandraClient, Set<String>, Exception>() {
                @Override
                public Set<String> apply(CassandraClient client) throws Exception {
                    return getExisting(client, keyspace);
                }

                @Override
                public String toString() {
                    return "describe_keyspace(" + keyspace + ")";
                }
            });
        } catch (Exception e) {
            throw Throwables.throwUncheckedException(e);
        }
    }

    private Set<String> getExisting(CassandraClient client, String keyspace) throws TException {
        return getTableNames(client, keyspace, CfDef::getName);
    }

    Set<String> getExistingLowerCased() throws TException {
        return getExistingLowerCased(config.getKeyspaceOrThrow());
    }

    private Set<String> getExistingLowerCased(String keyspace) throws TException {
        return clientPool.runWithRetry((client) -> getExistingLowerCased(client, keyspace));
    }

    private Set<String> getExistingLowerCased(CassandraClient client, String keyspace) throws TException {
        return getTableNames(client, keyspace, cf -> cf.getName().toLowerCase());
    }

    private Set<String> getTableNames(CassandraClient client, String keyspace,
            Function<CfDef, String> nameGetter) throws TException {
        KsDef ks = client.rawClient().describe_keyspace(keyspace);

        return ks.getCf_defs().stream()
                .map(nameGetter)
                .collect(Collectors.toSet());
    }
}
