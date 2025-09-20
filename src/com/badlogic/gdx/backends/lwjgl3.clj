(ns com.badlogic.gdx.backends.lwjgl3
  (:require com.badlogic.gdx.backends.lwjgl3.init.os-settings
            com.badlogic.gdx.backends.lwjgl3.init.config
            com.badlogic.gdx.backends.lwjgl3.init.application
            com.badlogic.gdx.backends.lwjgl3.init.gl-emulation
            com.badlogic.gdx.backends.lwjgl3.init.glfw
            com.badlogic.gdx.backends.lwjgl3.init.logger
            com.badlogic.gdx.backends.lwjgl3.init.gdx
            com.badlogic.gdx.backends.lwjgl3.init.audio
            com.badlogic.gdx.backends.lwjgl3.init.files
            com.badlogic.gdx.backends.lwjgl3.init.net
            com.badlogic.gdx.backends.lwjgl3.init.clipboard
            com.badlogic.gdx.backends.lwjgl3.init.sync
            com.badlogic.gdx.backends.lwjgl3.init.window
            com.badlogic.gdx.backends.lwjgl3.init.add-window
            com.badlogic.gdx.backends.lwjgl3.init.main-loop))

(defn start-application!
  [listener config os->executions]
  (reduce (fn [ctx f]
            (f ctx))
          {:init/listener listener
           :init/config config
           :init/os->executions os->executions}
          [com.badlogic.gdx.backends.lwjgl3.init.os-settings/do!
           com.badlogic.gdx.backends.lwjgl3.init.config/do!
           com.badlogic.gdx.backends.lwjgl3.init.application/do!
           com.badlogic.gdx.backends.lwjgl3.init.gl-emulation/before-glfw
           com.badlogic.gdx.backends.lwjgl3.init.glfw/do!
           com.badlogic.gdx.backends.lwjgl3.init.logger/do!
           com.badlogic.gdx.backends.lwjgl3.init.gdx/set-app!
           com.badlogic.gdx.backends.lwjgl3.init.audio/do!
           com.badlogic.gdx.backends.lwjgl3.init.gdx/set-audio!
           com.badlogic.gdx.backends.lwjgl3.init.files/do!
           com.badlogic.gdx.backends.lwjgl3.init.gdx/set-files!
           com.badlogic.gdx.backends.lwjgl3.init.net/do!
           com.badlogic.gdx.backends.lwjgl3.init.clipboard/do!
           com.badlogic.gdx.backends.lwjgl3.init.gdx/set-net!
           com.badlogic.gdx.backends.lwjgl3.init.sync/do!
           com.badlogic.gdx.backends.lwjgl3.init.window/do!
           com.badlogic.gdx.backends.lwjgl3.init.gl-emulation/after-window-creation
           com.badlogic.gdx.backends.lwjgl3.init.add-window/do!
           com.badlogic.gdx.backends.lwjgl3.init.main-loop/do!]))
