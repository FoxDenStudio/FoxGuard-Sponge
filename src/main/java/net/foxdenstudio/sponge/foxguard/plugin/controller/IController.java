package net.foxdenstudio.sponge.foxguard.plugin.controller;

import net.foxdenstudio.sponge.foxguard.plugin.handler.IHandler;
import net.foxdenstudio.sponge.foxguard.plugin.object.ILinkable;

import javax.sql.DataSource;

public interface IController extends IHandler, ILinkable {

    void configureLinks(DataSource dataSource);

}
