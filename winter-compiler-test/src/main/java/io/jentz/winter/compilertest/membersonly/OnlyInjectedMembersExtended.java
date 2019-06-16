package io.jentz.winter.compilertest.membersonly;

import javax.inject.Inject;

import io.jentz.winter.compilertest.injectconstructor.NoArgumentInjectConstructorWithInjectedFields;

public class OnlyInjectedMembersExtended extends OnlyInjectedMembers {
    @Inject
    NoArgumentInjectConstructorWithInjectedFields field2;
}
