#!/bin/sh
cat pre.tpl | sed s/ONTOLOGYNAME/$2/g > $2.owl

jar tf $1 \
| sed s/\\//\\./g \
| sed s/\\.class$// \
| sed s/\\$\\$/\\./g  \
| sed /.*\\.$/d \
| sed 's/\(.*\)/\<Declaration\>\<NamedIndividual\ IRI=\"#\1\"\/\>\<\/Declaration\>\<ClassAssertion\>\<Class IRI=\"#JVMClass\"\/\>\<NamedIndividual\ IRI=\"#\1\"\/\>\<\/ClassAssertion\>/' \
>> $2.owl

# jar tf $1 \
# | sed s/\\//\\./g \
# | sed s/\\.class$// \
# | sed s/\\$\\$/\\./g  \
# | sed /.*\\.$/d \
# | sed 's/\(.*\)/\<ClassAssertion\>\<Class IRI=\"#JVMClass\"\/\>\<NamedIndividual\ IRI=\"#\1\"\/\>\<\/ClassAssertion\>/' \
# >> $2.owl


cat post.tpl >> $2.owl