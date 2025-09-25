(ns gdl.application
  (:require [com.badlogic.gdx :as gdx]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl])
  (:import (com.badlogic.gdx ApplicationListener)))

(defprotocol Listener
  (create [_ context])
  (dispose [_])
  (render [_])
  (resize [_ width height])
  (pause [_])
  (resume [_]))

(defn start! [{:keys [listener config]}]
  (lwjgl/application (reify ApplicationListener
                       (create [_]
                         (create listener {:ctx/app      (gdx/app)
                                           :ctx/audio    (gdx/audio)
                                           :ctx/files    (gdx/files)
                                           :ctx/graphics (gdx/graphics)
                                           :ctx/input    (gdx/input)}))
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
                     config))

(require 'com.badlogic.gdx.scenes.scene2d.ui.horizontal-group
         'com.badlogic.gdx.scenes.scene2d.ui.stack)
