This pom constrains the jars that can be referenced by resource interface jars.

The list of dependencies needs to be carefully curated to prevent people from bringing tangly transitive dependencies around
with their clients.


All resource interface jars must have one and only one dependency on this in their pom. It will look like this:


    <dependencies>
        <dependency>
            <groupId>com.opower</groupId>
            <artifactId>rest-interface-base</artifactId>
            <version>1.0</version>
            <type>pom</type>
            <scope>provided</scope>
        </dependency>
    </dependencies>
