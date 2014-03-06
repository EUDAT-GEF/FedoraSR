FedoraSR
========

Fedora Commons safe replication code, to be used in conjunction with an iRODS environment configured according to the EUDAT B2SAFE service specifications. 

This package can be used for replicating any mounted collection. The invocation is 

    java -jar ./FedoraSR-1.0.4-jar-with-dependencies.jar --epic-pid-pass xxxxxxxx \
        --irods-source-path /vzSRC/source --irods-target-path /vzDST/destination

