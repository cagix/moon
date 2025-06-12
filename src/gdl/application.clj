(ns gdl.application
  (:require [gdl.app]
            [gdl.files]
            [gdl.input]
            [gdl.utils.disposable]
            [gdx.backends.lwjgl.application.config :as application-config]
            [gdx.input]
            [gdx.utils.shared-library-loader :as shared-library-loader])
  (:import (com.badlogic.gdx ApplicationAdapter
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application)))

(defn- execute! [[f params]]
  (f params))

(defn start!
  [os-config
   lwjgl3-config
   {:keys [create! dispose! render! resize!]}]
  (run! execute! (get os-config (shared-library-loader/operating-system)))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (create! {:ctx/app      Gdx/app
                                    :ctx/audio    Gdx/audio
                                    :ctx/files    Gdx/files
                                    :ctx/graphics Gdx/graphics
                                    :ctx/input    Gdx/input}))
                        (dispose []
                          (dispose!))
                        (render []
                          (render!))
                        (resize [width height]
                          (resize! width height)))
                      (application-config/create lwjgl3-config)))

(extend-type com.badlogic.gdx.Application
  gdl.app/Application
  (post-runnable! [this runnable]
    (.postRunnable this runnable)))

(extend-type com.badlogic.gdx.Files
  gdl.files/Files
  (internal [this path]
    (.internal this path)))

(extend com.badlogic.gdx.Input
  gdl.input/Input
  {:button-just-pressed? gdx.input/button-just-pressed?
   :key-pressed?         gdx.input/key-pressed?
   :key-just-pressed?    gdx.input/key-just-pressed?
   :mouse-position       gdx.input/mouse-position
   :set-processor!       gdx.input/set-processor!})

(extend-type com.badlogic.gdx.utils.Disposable
  gdl.utils.disposable/Disposable
  (dispose! [object]
    (.dispose object)))
