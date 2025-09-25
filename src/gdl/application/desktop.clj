(ns gdl.application.desktop
  (:require [com.badlogic.gdx.backends.lwjgl3.application.config :as application-config]
            [com.badlogic.gdx.plattform :as plattform])
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application)))

(defprotocol Listener
  (create [_ context])
  (dispose [_])
  (render [_])
  (resize [_ width height])
  (pause [_])
  (resume [_]))

(defn start! [{:keys [listener config]}]
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          (create listener {:ctx/app      Gdx/app
                                            :ctx/audio    Gdx/audio
                                            :ctx/files    Gdx/files
                                            :ctx/graphics Gdx/graphics
                                            :ctx/input    Gdx/input
                                            :ctx/plattform plattform/impl}))
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
                      (application-config/create config)))

(require 'com.badlogic.gdx.application
         'com.badlogic.gdx.audio
         'com.badlogic.gdx.files
         'com.badlogic.gdx.graphics
         'com.badlogic.gdx.input
         'com.badlogic.gdx.utils.disposable)
