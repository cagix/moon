(ns gdl.application
  (:require [com.badlogic.gdx.backends.lwjgl3 :as lwjgl])
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)))

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
                         (create listener {:ctx/app      Gdx/app
                                           :ctx/audio    Gdx/audio
                                           :ctx/files    Gdx/files
                                           :ctx/graphics Gdx/graphics
                                           :ctx/input    Gdx/input}))
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

(defn post-runnable!
  "Posts a Runnable on the main loop thread."
  [f]
  (.postRunnable Gdx/app f))

(require 'com.badlogic.gdx.graphics
         'com.badlogic.gdx.scenes.scene2d.actor
         'com.badlogic.gdx.scenes.scene2d.group
         'com.badlogic.gdx.scenes.scene2d.stage
         'com.badlogic.gdx.scenes.scene2d.ui.horizontal-group
         'com.badlogic.gdx.scenes.scene2d.ui.label
         'com.badlogic.gdx.scenes.scene2d.ui.stack
         'com.badlogic.gdx.scenes.scene2d.ui.table
         'com.badlogic.gdx.scenes.scene2d.ui.widget
         'com.badlogic.gdx.scenes.scene2d.ui.widget-group
         'com.badlogic.gdx.scenes.scene2d.ui.window)
