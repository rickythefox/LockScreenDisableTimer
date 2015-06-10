package se.amphisys.lockscreendisabletimer.app;

import com.google.inject.AbstractModule;

public class RoboModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(KeyguardHandler.class);
        bind(IntentHelper.class);
//        bind(new TypeLiteral<Dao<App, Integer>>() { }).toProvider(AppDaoProvider.class);
//        bind(new TypeLiteral<Dao<BlacklistEntry, Integer>>() { }).toProvider(BlacklistDaoProvider.class);
    }
}
