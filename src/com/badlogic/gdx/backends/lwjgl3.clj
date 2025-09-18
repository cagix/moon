(ns com.badlogic.gdx.backends.lwjgl3
  (:require [com.badlogic.gdx.utils.shared-library-loader :as shared-library-loader]))

(require 'com.badlogic.gdx.backends.lwjgl3.init.listener)
(require 'com.badlogic.gdx.backends.lwjgl3.init.net)
(require 'com.badlogic.gdx.backends.lwjgl3.init.clipboard)
(require 'com.badlogic.gdx.backends.lwjgl3.init.sync)
(require 'com.badlogic.gdx.backends.lwjgl3.init.window)
(require 'com.badlogic.gdx.backends.lwjgl3.init.add-window)
(require 'com.badlogic.gdx.backends.lwjgl3.init.gl-emulation)
(require 'com.badlogic.gdx.backends.lwjgl3.init.glfw)
(require 'com.badlogic.gdx.backends.lwjgl3.init.logger)
(require 'com.badlogic.gdx.backends.lwjgl3.init.gdx)
(require 'com.badlogic.gdx.backends.lwjgl3.init.audio)
(require 'com.badlogic.gdx.backends.lwjgl3.init.files)
(require 'com.badlogic.gdx.backends.lwjgl3.init.main-loop)
(require 'com.badlogic.gdx.backends.lwjgl3.init.config)
(require 'com.badlogic.gdx.backends.lwjgl3.init.application)

(defn start-application! [listener config]
  (-> {:init/listener listener
       :init/config config}
      com.badlogic.gdx.backends.lwjgl3.init.listener/do!
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
      com.badlogic.gdx.backends.lwjgl3.init.main-loop/do!))

(defn- call [[f params]]
  (f params))

(defn start! [_ctx {:keys [os->executions listener config]}]
  (doseq [[f params] (os->executions (shared-library-loader/operating-system))]
    (f params))
  (start-application! (call listener)
                      config))
