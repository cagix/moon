(ns com.badlogic.gdx.backends.lwjgl3)

(require 'init.listener)
(require 'init.net)
(require 'init.clipboard)
(require 'init.sync)
(require 'init.window)
(require 'init.add-window)
(require 'init.gl-emulation)
(require 'init.glfw)
(require 'init.logger)
(require 'init.gdx)
(require 'init.audio)
(require 'init.files)
(require 'init.main-loop)
(require 'init.config)
(require 'init.application)

(defn start-application! [listener config]
  (-> {:init/listener listener
       :init/config config}
      init.listener/do!
      init.config/do!
      init.application/do!
      init.gl-emulation/before-glfw
      init.glfw/do!
      init.logger/do!
      init.gdx/set-app!
      init.audio/do!
      init.gdx/set-audio!
      init.files/do!
      init.gdx/set-files!
      init.net/do!
      init.clipboard/do!
      init.gdx/set-net!
      init.sync/do!
      init.window/do!
      init.gl-emulation/after-window-creation
      init.add-window/do!
      init.main-loop/do!))

(defn- call [[f params]]
  (f params))

(defn start! [_ctx {:keys [listener config]}]
  (start-application! (call listener)
                      config))
