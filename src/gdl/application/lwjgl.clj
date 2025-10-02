(ns gdl.application.lwjgl
  (:require [com.badlogic.gdx.backends.lwjgl3.application :as application]
            [com.badlogic.gdx.backends.lwjgl3.application.configuration :as config]
            [org.lwjgl.system.configuration :as lwjgl-system])
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)))

(defprotocol Listener
  (create [_ context])
  (dispose [_])
  (render [_])
  (resize [_ width height])
  (pause [_])
  (resume [_]))

(defn start! [listener config]
  (lwjgl-system/set-glfw-library-name! "glfw_async")
  (application/start! (reify ApplicationListener
                        (create [_]
                          (let [state {:ctx/audio    Gdx/audio
                                       :ctx/files    Gdx/files
                                       :ctx/graphics Gdx/graphics
                                       :ctx/input    Gdx/input}]
                            ;(set! Gdx/app      nil)
                            ;(set! Gdx/graphics nil)
                            ;(set! Gdx/audio    nil)
                            ;(set! Gdx/input    nil)
                            ;(set! Gdx/files    nil)
                            ;(set! Gdx/net      nil)
                            ;(set! Gdx/gl       nil)
                            ;(set! Gdx/gl20     nil)
                            ;(set! Gdx/gl30     nil)
                            ;(set! Gdx/gl31     nil)
                            ;(set! Gdx/gl32     nil)
                            (create listener state)))
                        (dispose [_]
                          (dispose listener))
                        (render [_]
                          (render listener))
                        (resize [_ width height]
                          (resize listener width height))
                        (pause [_]
                          (pause listener))
                        (resume [_]
                          (resume listener)))
                      (config/create config)))
