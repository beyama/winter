package io.jentz.winter.compilertest;

import javax.inject.Inject;

public class OnlyInjectedMembersExtended extends OnlyInjectedMembers {
    @Inject
    NoArgumentInjectConstructorWithInjectedFields field2;
}
