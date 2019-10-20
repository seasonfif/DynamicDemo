package com.seasonfif.loadlib;

import com.seasonfif.modulebase.IPerson;

public class Worker implements IPerson {
    @Override
    public String eat() {
        return WorkerUtil.getTag("eat");
    }

    @Override
    public String play() {
        return "play";
    }

    @Override
    public String sleep() {
        return "sleep";
    }

    @Override
    public String work() {
        return "work";
    }
}
